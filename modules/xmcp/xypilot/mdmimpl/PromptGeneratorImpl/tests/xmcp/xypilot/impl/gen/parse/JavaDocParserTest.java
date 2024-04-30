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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class JavaDocParserTest {

    @Test
    public void cleanUpDocTest() {
        String input = new StringBuilder()
                .append("   * This is some nice comment\n")
                .append("   * With a second line\n")
                .append("   * @return Container<A, B, C>\n")
                .append("   * \n")
                .append("   * @author Hutzi\n")
                .append("   * <p>New paragraph\n")
                .append("   * <p>\n")
                .append("   * <ul>\n")
                .append("   * <li>Starting\n")
                .append("   * <li>a\n")
                .append("   * <li>fesh\n")
                .append("   * <li>list!\n")
                .append("   * </ul>\n")
                .toString();

        String expected = new StringBuilder()
                .append("This is some nice comment\n")
                .append("With a second line\n")
                .append("@return Container\n")
                .append("\n")
                .append("New paragraph\n")
                .append("\n")
                .append("\n")
                .append(" - Starting\n")
                .append(" - a\n")
                .append(" - fesh\n")
                .append(" - list!\n")
                .toString();

        String got = JavaDocParser.cleanUpDocumentation(input);
        assertEquals(expected, got);
    }
}
