package xmcp.xypilot.impl.gen.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ParserUtilsTest {

    @Test
    public void getQuotedTest() {
        String[] inputs = {
                "\"huhu\"",
                "huhu",
                "    \"huhu\",   "
        };
        String[] expectedVals = {
                "huhu",
                null,
                null,
        };

        for (int i = 0; i < inputs.length; i++) {
            String input = inputs[i];
            String expected = expectedVals[i];

            String got = ParserUtils.getQuoted(input);

            assertEquals(expected, got);
        }
    }
}
