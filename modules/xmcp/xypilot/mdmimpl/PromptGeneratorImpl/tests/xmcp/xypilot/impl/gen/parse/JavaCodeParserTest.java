package xmcp.xypilot.impl.gen.parse;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import java.io.File;
import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileWriteException;

import xmcp.xypilot.impl.util.DOMUtils;

public class JavaCodeParserTest {

    @Test
    public void parseCode() throws Ex_FileWriteException {
        String testFile = FileUtils.readFileAsString(new File(DOMUtils.resourceDirectory, "testFile.javat"));
        String removedCommentsStringsFile = FileUtils.readFileAsString(new File(DOMUtils.resourceDirectory, "testFile_removedCommentsStrings.javat"));
        String removedCodeFile = FileUtils.readFileAsString(new File(DOMUtils.resourceDirectory, "testFile_removedCode.javat"));

        String strippedCode = JavaCodeParser.removeCommentsAndStrings(testFile);
        String comments = JavaCodeParser.commentLinesOfCode(testFile);
        assertEquals(removedCommentsStringsFile, strippedCode);
        assertEquals(removedCodeFile, comments);
    }
}
