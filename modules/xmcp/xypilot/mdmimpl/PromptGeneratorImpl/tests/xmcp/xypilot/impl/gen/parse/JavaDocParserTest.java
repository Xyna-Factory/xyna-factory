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
