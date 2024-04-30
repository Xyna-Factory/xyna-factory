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
