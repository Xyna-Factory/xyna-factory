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
