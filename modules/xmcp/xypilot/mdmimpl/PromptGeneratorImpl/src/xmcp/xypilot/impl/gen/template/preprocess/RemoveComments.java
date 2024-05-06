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
package xmcp.xypilot.impl.gen.template.preprocess;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RemoveComments implements TemplatePreprocessor {

    private static final String COMMENTS_REGEX = "(\\n)?<#--.*?-->(\\n)?|(\\n)?^[ \t]*<#--.*?-->[ \t]*$(\\n)?";
    private static final Pattern COMMENTS_PATTERN = Pattern.compile(COMMENTS_REGEX, Pattern.DOTALL | Pattern.MULTILINE);

    @Override
    public Reader process(Reader reader) throws IOException {
        BufferedReader br = new BufferedReader(reader);
        // read full string
        StringBuilder lines = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            lines.append(line).append("\n");
        }
        Matcher comments = COMMENTS_PATTERN.matcher(lines.toString());

        String result = comments.replaceAll(match -> {
            // find out how many new lines are around the comment = number of matched groups
            int numNewLinesAroundComment = 0;
            for (int i = 1; i <= comments.groupCount(); i++) {
                if (match.group(i) != null) {
                    numNewLinesAroundComment++;
                }
            }
            // if there is a new line before and after the comment, replace with one new line when comment is removed
            return numNewLinesAroundComment == 2 ? "\n" : "";
        });

        return new StringReader(result);
    }

}
