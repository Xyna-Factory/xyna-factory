/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 */
package xmcp.xypilot.impl.gen.parse;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;

import xmcp.xypilot.Mapping;
import xmcp.xypilot.MappingAssignment;
import xmcp.xypilot.impl.gen.model.MappingModel;
import xmcp.xypilot.impl.gen.pipeline.Parser;
import xmcp.xypilot.impl.gen.util.MappingUtils;
import xmcp.xypilot.impl.gen.util.StringUtils;

/**
 * Parses the completion returned by the ai server to mapping expressions.
 */
public class MappingParser implements Parser<Mapping, MappingModel> {

    // the name of the output instance
    private static final String OUTPUT_INSTANCE_NAME = "this";

    // a variable name starts with a letter or underscore and is followed by any number of letters, underscores or digits
    private static final String VARIABLE_REGEX = "[a-zA-Z_]\\w*";

    // matches an array access, e.g. '[0]', '[var]'
    // group 1: the array index expression between the brackets
    private static final String ARRAY_ACCESS_REGEX = "\\[(.+?)\\]";
    private static final Pattern ARRAY_ACCESS_PATTERN = Pattern.compile(ARRAY_ACCESS_REGEX);

    // a field access of struct, possibly with an array access, e.g. '.var1', '.var1[0]'
    private static final String MEMBER_ACCESS_REGEX = "\\." + VARIABLE_REGEX + "(?:\\[.+?\\])?";
    // a path to access a field in a struct, e.g. '.var1.var2.var3', possibly with multiple array accesses, e.g. '.var1[0].var2[1].var3[2]'
    private static final String PATH_ACCESS_REGEX = "(?:" + MEMBER_ACCESS_REGEX + ")*";

    // a mapping assignment is a line that assigns an expression to an output variable or any sub member of an output variable,
    // that is the left side of the assingment folows a restricted syntax with member access and expressions for array accesses, the right side can be any expression
    // examples: <out>.var = *, <out>.var[*].sub[*].subsub = ...;
    // group 1: output variable name
    // group 2: accessed array element on output variable, optional
    // group 3: sub path in output variable, possibly with multiple array accesses, optional, includes the leading dot
    // group 4: right hand side of assignment, can be any expression
    private static final String MAPPING_ASSIGNMENT_REGEX
        = OUTPUT_INSTANCE_NAME + "\\.(" + VARIABLE_REGEX + ")(?:\\[(.+?)\\])?" + "(" + PATH_ACCESS_REGEX + ")\\s*=\\s*(.*?);?\\s*$";
    private static final Pattern MAPPING_ASSIGNMENT_PATTERN = Pattern.compile(MAPPING_ASSIGNMENT_REGEX);

    // matches an array length expression, e.g. 'var.length', 'this.var[0].sub.length'
    // note: this does match the most common length accesses, but not all possible ones, this requires more soficisticated parsing, e.g. using an AST
    // group 1: the path to the array variable, possibly with multiple array accesses
    private static final String LENGTH_REGEX
        = "(?<!\\w)(" + VARIABLE_REGEX + PATH_ACCESS_REGEX + ")\\.length(?!\\w)";
    private static final Pattern LENGTH_PATTERN = Pattern.compile(LENGTH_REGEX);

    // literals are numbers and booleans
    private static final String LITERAL_REGEX = StringUtils.LITERAL_REGEX;

    // a free literal surrounded by non-word characters, e.g. 12, true
    // group 1: the literal
    private static final String FREE_LITERAL_REGEX = "(?<!\\w)(" + LITERAL_REGEX + ")(?!\\w)";
    private static final Pattern FREE_LITERAL_PATTERN = Pattern.compile(FREE_LITERAL_REGEX);

    // a string literal is a string that is surrounded by double quotes, this is used to not act on string literals
    private static final String STRING_REGEX = StringUtils.STRING_REGEX;
    private static final Pattern STRING_PATTERN = Pattern.compile(STRING_REGEX);

    // a comment, e.g. '// comment'
    private static final String COMMENT_REGEX = "\\s*\\/\\/.*$";
    private static final Pattern COMMENT_PATTERN = Pattern.compile(COMMENT_REGEX);


    @Override
    public Mapping parse(String input, MappingModel dataModel) {
        StepMapping stepMapping = dataModel.getMapping();

        // the template adds a "<out>.<var0>" to the first line if there are no expressions yet, to trigger the completion
        // we need to inlude this in the parsing as well to substitute the correct output variable index
        if (stepMapping.getRawExpressions().isEmpty() && !stepMapping.getOutputVars().isEmpty()) {
            input = stepMapping.getOutputVars().get(0).getVarName() + input;
            if (OUTPUT_INSTANCE_NAME != null && !OUTPUT_INSTANCE_NAME.isEmpty()) {
                input = OUTPUT_INSTANCE_NAME + "." + input;
            }
        }

        Mapping mapping = new Mapping();
        String lines[] = input.split("\\r?\\n");

        // first line can be the completion of the last expression
        if (lines.length != 0) {
            String completion = parseExpressionCompletion(lines[0], dataModel.getMapping());
            if (completion != null) {
                mapping.setExpressionCompletion(completion);
                lines[0] = ""; // handled
            }
        }

        List<MappingAssignment> assignments = Arrays.asList(lines).stream()
            .map(line -> parseAssignment(line, dataModel.getMapping()))
            .filter(assignment -> assignment != null)
            .collect(Collectors.toList());

        mapping.setAssignments(assignments);

        return mapping;
    }


    static String parseExpressionCompletion(String line, StepMapping mapping) {
        String completion = parseLine(line, mapping);
        if (!mapping.getRawExpressions().isEmpty()) {
            String lastExpression = mapping.getRawExpressions().get(mapping.getRawExpressions().size() - 1);

            if (!lastExpression.isBlank() && !completion.isEmpty()) {
                return completion;
            }
        }
        return null;
    }


    static MappingAssignment parseAssignment(String line, StepMapping mapping) {
        line = line.trim();
        Matcher assignmentMatcher = MAPPING_ASSIGNMENT_PATTERN.matcher(line);

        if (assignmentMatcher.matches()) {
            // extract the parts of the assignment
            String outputVarName = assignmentMatcher.group(1);
            String arrayElementExpression = assignmentMatcher.group(2);
            String subPath = assignmentMatcher.group(3);
            String rhs = assignmentMatcher.group(4);

            // parse the right hand side of the assignment
            rhs = parseLine(rhs, mapping);


            // retrieve the output variable
            AVariable outputVar = MappingUtils.getOutputVariableByName(mapping, outputVarName);

            if (outputVar != null) {

                int outputVarIndex = mapping.getOutputVars().indexOf(outputVar) + mapping.getInputVars().size();

                // parse array element expression on output variable, if any
                if (arrayElementExpression != null) {
                    arrayElementExpression = !outputVar.isList() ? null : parseExpression(arrayElementExpression, mapping);
                }

                if (!subPath.isEmpty()) {
                    // remove the leading dot
                    subPath = subPath.substring(1);

                    // parse all array element expressions in the sub path
                    subPath = ARRAY_ACCESS_PATTERN.matcher(subPath).replaceAll(
                        match -> String.format("[%s]", parseExpression(match.group(1), mapping))
                    );
                }

                // create the mapping assignment
                String expression = buildAssignmentExpression(outputVarIndex, arrayElementExpression, subPath, rhs);
                return new MappingAssignment(expression);
            }
        }

        return null;
    }


    /**
     * Creates a mapping assignment expression from the given parts.
     * The expression is of the form: %outputVarIndex%[arrayElementExpression].subPath=rhs
     * @param outputVarIndex
     * @param arrayElementExpression optional
     * @param subPath optional
     * @param rhs
     * @return
     */
    static String buildAssignmentExpression(int outputVarIndex, String arrayElementExpression, String subPath, String rhs) {
        if (subPath == null || subPath.isEmpty()) {
            return arrayElementExpression == null || arrayElementExpression.isEmpty()
                ? String.format("%%%d%%=%s", outputVarIndex, rhs)
                : String.format("%%%d%%[%s]=%s", outputVarIndex, arrayElementExpression, rhs);
        }
        return arrayElementExpression == null || arrayElementExpression.isEmpty()
            ? String.format("%%%d%%.%s=%s", outputVarIndex, subPath, rhs)
            : String.format("%%%d%%[%s].%s=%s", outputVarIndex, arrayElementExpression, subPath, rhs);
    }


    /**
     * Parses a line in as a mapping assignment.
     * @param line
     * @param mapping
     * @return
     */
    static String parseLine(String line, StepMapping mapping) {
        line = parseExpression(line, mapping);
        line = removeTrailingComments(line);
        line = removeTrailingSemicolon(line);
        return line.trim();
    }


    /**
     * Parses an expression in a mapping assignment:
     * Replaces all input variables with their index in the input struct, e.g. in.var -> %0%
     * Replaces all output variables with their index in the output struct, e.g. result.var -> %1%
     * Enquotes all literals, e.g. 12 -> "12", true -> "true"
     * String literals are escaped, e.g. "hallo" -> "\"hallo\""
     *
     * @param expression
     * @param mapping
     * @return
     */
    static String parseExpression(String expression, StepMapping mapping) {
        expression = escapeStrings(expression);
        expression = enquoteLiterals(expression);
        expression = replaceArrayLengthWithFunctionCall(expression);
        expression = replaceOutputVarsWithIndices(expression, mapping);
        expression = replaceInputVarsWithIndices(expression, mapping);
        return expression;
    }


    /**
     * Replaces all array length expressions with a function call, e.g. in.var.length -> length(in.var)
     * @param expression
     * @param mapping
     * @return
     */
    static String replaceArrayLengthWithFunctionCall(String expression) {

        // build a regex that matches all array length expressions, e.g. in.var.length
        return LENGTH_PATTERN.matcher(expression).replaceAll(match -> {
            String pathToArray = match.group(1);

            // to not act on string literals
            Matcher stringMatcher = STRING_PATTERN.matcher(expression);

            // replace the input variable with its %index%, except inside a string
            return stringMatcher.results().anyMatch(result -> result.start() < match.start() && match.end() < result.end())
                ? "$0"
                : String.format("length(%s)", pathToArray);
            }
        );
    }


    /**
     * Builds a regex that matches the given variables, e.g. (var1|var2|var3|...)
     * @param variables
     * @return
     */
    static String variableRegex(List<AVariable> variables) {
        return variables.isEmpty() ? "" : "(?<!\\w|\\.)(" + variables.stream().map(AVariable::getVarName).collect(Collectors.joining("|")) + ")(?!\\w)";
    }


    /**
     * Builds a regex that matches the given member variables, e.g. <instanceName>.(var1|var2|var3|...)
     * @param instanceName
     * @param variables
     * @return
     */
    static String variableRegex(String instanceName, List<AVariable> variables) {
        return variables.isEmpty() ? "" : String.format(
            "(?<!\\w|\\.)%s\\.(%s)(?!\\w)",
            instanceName,
            variables.stream().map(AVariable::getVarName).collect(Collectors.joining("|"))
        );
    }


    static String replaceMatchesWithIndices(String regex, String expression, List<String> matches, int offset) {
        if (regex.isEmpty()) {
            return expression;
        }

        Matcher matcher = Pattern.compile(regex).matcher(expression);

        return matcher.replaceAll(match -> {
            int index = matches.indexOf(match.group(1));

            // to not act on string literals
            Matcher stringMatcher = STRING_PATTERN.matcher(expression);

            // replace the match with its %index%, except inside a string
            return stringMatcher.results().anyMatch(result -> result.start() < match.start() && match.end() < result.end())
                ? "$0"
                : String.format("%%%d%%", offset + index);
            }
        );
    }


    static String replaceVariablesWithIndices(String expression, List<AVariable> variables, int offset) {
        List<String> variableNames = variables.stream().map(AVariable::getVarName).collect(Collectors.toList());
        return replaceMatchesWithIndices(variableRegex(variables), expression, variableNames, offset);
    }


    static String replaceVariablesWithIndices(String expression, String instanceName, List<AVariable> variables, int offset) {
        List<String> variableNames = variables.stream().map(AVariable::getVarName).collect(Collectors.toList());
        return replaceMatchesWithIndices(variableRegex(instanceName, variables), expression, variableNames, offset);
    }


    /**
     * Replaces all input variables with their index in the input struct, e.g. var -> %0%
     * @param expression
     * @param mapping
     * @return
     */
    static String replaceInputVarsWithIndices(String expression, StepMapping mapping) {
        return replaceVariablesWithIndices(expression, mapping.getInputVars(), 0);
    }


    /**
     * Replaces all output variables with their index in the output struct, e.g. this.var -> %1%
     * @param expression
     * @param mapping
     * @return
     */
    static String replaceOutputVarsWithIndices(String expression, StepMapping mapping) {
        return replaceVariablesWithIndices(expression, OUTPUT_INSTANCE_NAME, mapping.getOutputVars(), mapping.getInputVars().size());
    }


    /**
     * Enquotes all literals, e.g. 12 -> "12", true -> "true" in the given expression.
     * Literals that are inside a string are not enquoted.
     * @param expression
     * @return
     */
    static String enquoteLiterals(String expression) {
        // enquote all free literals
        return FREE_LITERAL_PATTERN.matcher(expression).replaceAll(match -> {
            // enquote the literal if it is not inside a string
            Matcher stringMatcher = STRING_PATTERN.matcher(expression);
            return stringMatcher.results().anyMatch(
                result -> result.start() < match.start() && match.end() < result.end()
            ) ? "$1" : "\"$1\"";
        });
    }


    static String escapeStrings(String expression) {
        return STRING_PATTERN.matcher(expression).replaceAll("\"\\\\\"$1\\\\\"\"");
    }


    static String removeTrailingComments(String expression) {
        return COMMENT_PATTERN.matcher(expression).replaceAll("");
    }


    static String removeTrailingSemicolon(String expression) {
        return expression.endsWith(";") ? expression.substring(0, expression.length() - 1) : expression;
    }

}
