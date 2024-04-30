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

import xmcp.xypilot.ExceptionMessage;

public class ExceptionParserTest {

    @Test
    public void parseExceptionMsg() {
        String[] inputs = {
                "          \"DE\"        ,      \"Hallo\"             ",
                "  DE, Hallo",
                "\"futzi\",\"Hallo\"",
                "\"DE\"\"Hallo\",",
                "\"DE\", \"Noch ein Komma am Ende\", "
        };
        ExceptionMessage[] expectedMsgs = {
                new ExceptionMessage("DE", "Hallo"),
                null,
                null,
                null,
                new ExceptionMessage("DE", "Noch ein Komma am Ende")
        };

        for (int i = 0; i < inputs.length; i++) {
            String input = inputs[i];
            ExceptionMessage expected = expectedMsgs[i];

            ExceptionMessage got = ExceptionParser.parseExceptionMessage(input);
            if (expected == null) {
                assertEquals(expected, got);
                continue;
            }
            assertEquals(expected.getLanguage(), got.getLanguage());
            assertEquals(expected.getMessage(), got.getMessage());
        }
    }
}
