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
package xmcp.xypilot.impl.gen.util;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;

/**
 * Generates the different kinds of promts for XyPilot
 */
public class GeneratorUtils {

    private ImportHandler importHandler;
    private static String indent = "  ";
    private String lineComment = "// ";

    // literals are numbers and booleans
    private static final String LITERAL_REGEX = StringUtils.LITERAL_REGEX;

    // a string literal is a string that is surrounded by double quotes
    private static final String STRING_REGEX = StringUtils.STRING_REGEX;
    private static final Pattern STRING_PATTERN = Pattern.compile(STRING_REGEX);

    // a literal surrounded by quotes, e.g. "12", "true"
    private static final String ENQUOTED_LITERAL_REGEX = "\"(" + LITERAL_REGEX + ")\"";
    private static final Pattern ENQUOTED_LITERAL_PATTERN = Pattern.compile(ENQUOTED_LITERAL_REGEX);

    // a sting with escaped quotes at the beginning and end, e.g. "\"hello\""
    private static final String ENQUOTED_STRING_REGEX = "\\\"\\\\\\\"((?:[^\\\"\\\\]|\\\\.)*)\\\\\\\"\\\"";

    // a mapping parameter identifier, e.g. "%0%", "%1%"
    private static final String MAPPING_PARAM_ID_REGEX = "%(\\d+)%";
    private static final Pattern MAPPING_PARAM_ID_PATTERN = Pattern.compile(MAPPING_PARAM_ID_REGEX);


    public GeneratorUtils() {
        importHandler = new ImportHandler();
    }


    public GeneratorUtils(Set<String> imports) {
        importHandler = new ImportHandler(imports);
    }


    /**
     * Returns the getter name for the given variable.
     *
     * @param var
     * @return
     */
    public static String getter(AVariable var) {
        return "get" + StringUtils.capitalize(var.getVarName());
    }


    /**
     * Returns the setter name for the given variable.
     *
     * @param var
     * @return
     */
    public static String setter(AVariable var) {
        return "set" + StringUtils.capitalize(var.getVarName());
    }

    /**
     * Returns the addTo function name for the given variable or an empty string if the variable is not a list.
     *
     * @param var
     * @return
     */
    public static String addTo(AVariable var) {
        return var.isList() ? "addTo" + StringUtils.capitalize(var.getVarName()) : "";
    }


    /**
     * Trims the given label and replaces all non-word characters with an underscore, such that the result is a valid Java identifier.
     * The first character is set to lower case.
     */
    public static String identifier(String label) {
        return StringUtils.toIdentifier(label);
    }


    /**
     * Removes quotation marks from literals (numbers, booleans) in the given expression.
     * @param expression
     * @return
     */
    public static String dequoteLiterals(String expression) {
        return ENQUOTED_LITERAL_PATTERN.matcher(expression).replaceAll(match -> {
            // remove quotation marks arround the literal if it is not inside a string
            Matcher stringMatcher = STRING_PATTERN.matcher(expression);
            return stringMatcher.results().anyMatch(
                result -> result.start() < match.start() && match.end() < result.end()
            ) ? "$0" : "$1";
        });
    }


    /**
     * Removes escaped quotation marks (only from the start and end) of the given expression, e.g. "\"hello\"" -> "hello"
     * @param expression
     * @return
     */
    public static String unescapeStringLiterals(String expression) {
        return expression.replaceAll(ENQUOTED_STRING_REGEX, "\"$1\"");
    }


    /**
     * Replaces the mapping identifyiers with the corresponding varNames.
     * Id < numInputs will be replaced by '[inputInstanceName.]var'. Else they will be replaced by
     * '[outputInstanceName.]var'
     *
     * @param expression
     * @param mapping
     * @return
     */
    public static String replaceMappingIds(
        String expression,
        StepMapping mapping,
        String inputInstanceName,
        String outputInstanceName
    ) {
        String in = inputInstanceName == null || inputInstanceName.isEmpty() ? "" : inputInstanceName + ".";
        String out = outputInstanceName == null || outputInstanceName.isEmpty() ? "" : outputInstanceName + ".";

        return MAPPING_PARAM_ID_PATTERN.matcher(expression).replaceAll(match -> {
            // check if the match is inside a string
            Matcher stringMatcher = STRING_PATTERN.matcher(expression);
            boolean isInString = stringMatcher.results().anyMatch(
                result -> result.start() < match.start() && match.end() < result.end()
            );
            // don't replace the id if it is inside a string
            if (isInString) {
                return "$0";
            }

            // try to parse the id, and replace it with the corresponding varName
            try {
                int id = Integer.parseInt(match.group(1)); //%id%
                return id < mapping.getInputVars().size() // input var
                    ? in + mapping.getInputVars().get(id).getVarName()
                    : id < mapping.getInputVars().size() + mapping.getOutputVars().size() // output var
                        ? out + mapping.getOutputVars().get(id - mapping.getInputVars().size()).getVarName()
                        : "$0"; // unknown var
            } catch (NumberFormatException e) {
                return "$0"; // unknown var
            }
        });
    }


    /**
     * Converts a mapping expression to proper Java.
     * I.e., converts References (`%x%`) into proper variables (`[inputInstanceName].var` or
     * `[outputInstanceName].var`), removes quotation-marks around non-String literals and unescapes string literals.
     *
     *
     * @param expression
     * @return
     */
    public static String turnMappingExpressionToJava(
        String expression,
        StepMapping mapping,
        String inputInstanceName,
        String outputInstanceName
    ) {
        String ret = expression;
        ret = ret.replace("=", " = ");
        ret = replaceMappingIds(ret, mapping, inputInstanceName, outputInstanceName);
        ret = dequoteLiterals(ret);
        ret = unescapeStringLiterals(ret);
        return ret.trim();
    }


    /**
     * Converts the given (multiline) string to a single line text. Seperated as sentences by ".".
     * @param text
     * @return
     */
    public static String toSingleLineText(String text) {
        return text.lines()
            .map((line) -> line.trim())
            .filter((line) -> !line.isEmpty())
            .map((line) -> line.endsWith(".") ? line.substring(0, line.length() - 1) : line)
            .collect(Collectors.joining(". "));
    }


    /**
     * If the operation has no return type, "void" is returned.
     * If the operation has only one return type, this method is equivalent to finalType.
     * If there are multiple variables, all of them are wrapped in a Container<>.
     *
     * @param op The operation of which the return types should be stringified
     * @return String representation of the return types with no leading or trailing
     *         spaces
     */
    public String stringifyReturnTypes(Operation op, boolean resolveTypes, boolean allowCollections) {
        return stringifyReturnTypes(op.getOutputVars(), resolveTypes, allowCollections);
    }

    /**
     * If the operation has no return type, "void" is returned.
     * If the operation has only one return type, this method is equivalent to finalType.
     * If there are multiple variables, all of them are wrapped in a Container<>.
     *
     * @param op The operation of which the return types should be stringified
     * @return String representation of the return types with no leading or trailing
     *         spaces
     */
    public String stringifyReturnTypes(Operation op, boolean resolveTypes) {
        return stringifyReturnTypes(op.getOutputVars(), resolveTypes, true);
    }

    /**
     * If the list is empty, "void" is returned.
     * If the list has only one variable, this method is equivalent to finalType.
     * If there are multiple variables, all of them are wrapped in a Container<>.
     *
     * @param vars List of variables representing the return types
     * @return String representation of the return types with no leading or trailing
     *         spaces
     */
    public String stringifyReturnTypes(List<? extends AVariable> vars, boolean resolveTypes, boolean allowCollections) {
        if (vars.isEmpty()) {
            return "void";
        }

        if (vars.size() == 1) {
            return finalType(vars.get(0), resolveTypes, allowCollections);
        }

        String returnTypes = vars
                .stream()
                .map((var) -> finalType(var, resolveTypes, allowCollections))
                .collect(Collectors.joining(", "));
        return "Container<" + returnTypes + ">";
    }

    /**
     * If the list is empty, "void" is returned.
     * If the list has only one variable, this method is equivalent to finalType.
     * If there are multiple variables, all of them are wrapped in a Container<>.
     *
     * @param vars List of variables representing the return types
     * @return String representation of the return types with no leading or trailing
     *         spaces
     */
    public String stringifyReturnTypes(List<? extends AVariable> vars, boolean resolveTypes) {
        return stringifyReturnTypes(vars, resolveTypes, true);
    }


    /**
     * Optionally resolves the type of the var and also puts it into a List if necessary or an array if allowCollection is false
     *
     * @param var
     * @param resolveType Whether the type should be resolved or not
     * @param allowCollections Whether list types should be put into a List or an array
     * @return
     */
    public String finalType(AVariable var, boolean resolveType, boolean allowCollections) {
        String type = var.isJavaBaseType()
            ? var.getJavaTypeEnum().getJavaTypeName()
            : resolveType ? resolveType(var.getFQClassName()) : var.getDomOrExceptionObject().getOriginalSimpleName();

        if (var.isList()) {
            type = allowCollections ? "List<" + type + ">" : type + "[]";
        }
        return type;
    }


    /**
     * Optionally resolves the type of the var and also puts it into a List if necessary
     *
     * @param var
     * @param resolveType Whether the type should be resolved or not
     * @return
     */
    public String finalType(AVariable var, boolean resolveType) {
        return finalType(var, resolveType, true);
    }


    /**
     * Resolves the type of the var and also puts it into a List if necessary
     *
     * @param var
     * @return
     */
    public String finalType(AVariable var) {
        return finalType(var, true);
    }


    /**
     * Resolves the given FQN with respect to the imports. If the Type is imported, the simple name is used. Else the FQN
     * @param fqn
     * @return
     */
    public String resolveType(String fqn) {
        return this.importHandler.resolve(fqn);
    }


    /**
     * AVariable can be a JavaType or a XynaType. Finding out the name reqquires
     * different method calls.
     * This function wraps everything into one call and retuns the type (as FQN if
     * needed).
     *
     * @param var
     * @return type of the variable or "" if it couldn't extracted.
     */
    public String getType(AVariable var) {
        String ret = var.getFQClassName();
        if (ret != null) {
            return ret;
        }

        if (var.isJavaBaseType()) {
            return var.getJavaTypeEnum().getJavaTypeName();
        } else {
            return "";
        }
    }


    /**
     * Checks if the given DomOrExceptionGenerationBase is a DOM
     * @param domOrException
     * @return
     */
    public boolean isDOM(DomOrExceptionGenerationBase domOrException) {
        return domOrException instanceof DOM;
    }

    /**
     * Checks if the given DomOrExceptionGenerationBase is an ExceptionGeneration
     * @param domOrException
     * @return
     */
    public boolean isException(DomOrExceptionGenerationBase domOrException) {
        return domOrException instanceof ExceptionGeneration;
    }


    public ImportHandler getImportHandler() {
        return importHandler;
    }

    public void setImportHandler(ImportHandler importHandler) {
        this.importHandler = importHandler;
    }

    public static String getIndent() {
        return indent;
    }

    public static void setIndent(String newIndent) {
        indent = newIndent;
    }

    public String getLineComment() {
        return lineComment;
    }

    public void setLineComment(String lineComment) {
        this.lineComment = lineComment;
    }


//........................................................................................................................


    /**
     * Checks if the stringLiteral is one of the following but in quotations:
     *  - boolean Literal (`true` or `false`),
     *  - number Literal (int or float)
     * If it is, the return value won't be enquoted anymore; else the input String will be returned
     * @param stringLiteral
     * @return
     */
    public static String checkForKeyLiteral(String stringLiteral) {
        String enquoted = stringLiteral.substring(1, stringLiteral.length() - 1);
        boolean booleanLiteral = enquoted.equals("true") || enquoted.equals("false");
        if (booleanLiteral) {
            return enquoted;
        }
        try {
            Double.parseDouble(stringLiteral.substring(1, stringLiteral.length() - 1));
            return enquoted;
        } catch (NumberFormatException e) {

        }
        return stringLiteral;
    }

    /**
     * Properly formats the documentation as JavaDoc
     *
     * @param documentation
     * @param closeDoc
     * @param promptString
     * @param indent
     * @return
     */
    public StringBuilder documentationBlock(String documentation, boolean closeDoc, String promptString,
            String indent) {
        StringBuilder ret = new StringBuilder(indent + "/** " + promptString + "\n");

        ret.append(documentation.lines()
                .map((line) -> indent + " * " + line)
                .collect(Collectors.joining("\n")));

        if (closeDoc) {
            ret.append("\n" + indent + " */\n");
        }

        return ret;
    }


    /**
     * Finds the index of the next not-escaped occurence of the given targetChar in
     * the searchString starting with startIndex.
     * StartIndex won't be included
     *
     * @param searchString
     * @param startIndex
     * @param targetChar
     * @return returns index of targetChar in searchString; -1 if not found
     */
    public static int indexOfNextValid(String searchString, int startIndex, char targetChar) {
        startIndex = searchString.indexOf(targetChar, startIndex + 1);
        while (startIndex != -1 && searchString.charAt(startIndex) == targetChar
                && isEscaped(searchString, startIndex)) {
            startIndex = searchString.indexOf(targetChar, startIndex + 1);
        }
        if (startIndex == -1) {
            return -1;
        }
        return startIndex;
    }

    /**
     * Checks if the character at position charIndex in text is escaped. I.e., there
     * are an odd number of backslashes right before it
     *
     * @param text
     * @param charIndex
     * @return
     */
    public static boolean isEscaped(String text, int charIndex) {
        int backslashCount = 0;
        charIndex -= 1;
        while (charIndex >= 0 && text.charAt(charIndex) == '\\') {
            backslashCount += 1;
            charIndex -= 1;
        }

        return backslashCount % 2 != 0;
    }

}
