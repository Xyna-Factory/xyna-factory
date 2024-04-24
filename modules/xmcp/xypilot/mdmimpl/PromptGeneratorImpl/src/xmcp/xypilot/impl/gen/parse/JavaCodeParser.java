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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import xmcp.xypilot.impl.gen.model.DomMethodModel;
import xmcp.xypilot.impl.gen.pipeline.Parser;
import xmcp.xypilot.impl.gen.util.ParserUtils;
import xmcp.xypilot.metrics.Code;


/**
 * Parses the completion returned by the ai server to a suitable service method implementation.
 */
public class JavaCodeParser implements Parser<Code, DomMethodModel> {

    /**
     * Removes code indentation, as well as generics around "Container"
     *
     * @param code
     */
    @Override
    public Code parse(String code, DomMethodModel dataModel) {
        return new Code(
            cleanUpCode(code)
        );
    }


    /**
     * Removes code indentation, as well as generics around "Container"
     *
     * @param code
     */
    public static String cleanUpCode(String code) {
        String indent = ParserUtils.indent.repeat(2);
        return code.lines()
            .map((line) -> {
                line = line.replaceAll("Container<.*?>(\\(?)", "Container$1");
                if (line.startsWith(indent)) {
                    return line.replaceFirst(indent, "");
                }
                return line;
            })
            .collect(Collectors.joining("\n"));
    }

    /**
     * Removes comments and string literals
     *
     * @param code
     */
    public static String removeCommentsAndStrings(String code) {
        // Remove all comments, keep strings:
        // //.*                     Match // and rest of line
        // |                        or
        // /\*(?s:.*?)\*/           Match /* and */, with any characters in-between, incl. linebreaks
        // |                        or
        // ("                       Start capture group and match "
        //   (?:                    Start repeating group:
        //     (?<!\\)(?:\\\\)*\\"  Match escaped " optionally prefixed by escaped \'s
        //     |                    or
        //     [^\r\n"]             Match any character except " and linebreak
        //  )*                      End of repeating group
        // ")                       Match terminating ", and end of capture group
        //
        // $1: Keep captured string literal
        String strippedCode = code.replaceAll("//.*|/\\*(?s:.*?)\\*/|(\"(?:(?<!\\\\)(?:\\\\\\\\)*\\\\\"|[^\r\n\"])*\")", "$1");

        // remove strings
        return strippedCode.lines()
            .map((line) -> {
                line = line.replaceAll("\".*\"", "");
                return line;
            })
            .collect(Collectors.joining("\n"));
    }

    /**
     * Returns all code lines that contain only comments
     *
     * @param code
     */
    public static String commentLinesOfCode(String code) {
        Pattern codeComments = Pattern.compile("(^[\t ]*|[\r\n][\t ]*)//.*|(^[\t ]*|[\r\n][\t ]*)/\\*(?s:.*?)\\*/");
        Matcher m = codeComments.matcher(code);
        StringBuilder comments = new StringBuilder();
        while (m.find()) {
            comments.append(m.group(0));
        }
        return comments.toString();
    }
}
