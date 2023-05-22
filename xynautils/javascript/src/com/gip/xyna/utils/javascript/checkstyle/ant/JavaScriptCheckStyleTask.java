/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.utils.javascript.checkstyle.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

import com.gip.xyna.utils.javascript.checkstyle.JavaScriptCheckStyle;
import com.gip.xyna.utils.javascript.checkstyle.ValidationFailureException;

/**
 * Simple JavaScript check style task for Ant. Note that this is prototype software! It should be
 * thrown away as soon as there are better alternatives to perform the checks it does.
 *
 */
public final class JavaScriptCheckStyleTask extends Task {

    private static final int DEFAULT_LINE_LENGTH = 120;
    private static final String DEFAULT_CHAR_SET = "UTF-8";

    private int maxLineLength = DEFAULT_LINE_LENGTH;
    private String charSet = DEFAULT_CHAR_SET;

    private final List<FileSet> fileSets = new ArrayList<FileSet>();

    public void setMaxLineLength(final int maxLineLength) {
        if (maxLineLength < 1) {
            throw new IllegalArgumentException(
                    "Expected max line length larger than zero, but was <" + maxLineLength + ">.");
        }
        this.maxLineLength = maxLineLength;
    }

    public void setCharacterSet(final String charSet) {
        if (charSet == null) {
            throw new IllegalArgumentException("Character set may not be null.");
        }
        this.charSet = charSet;
    }

    public void addFileset(FileSet fileset) {
        fileSets.add(fileset);
    }

    public void execute() {
        int countProcessed = 0;
        int countFailed = 0;
        for (FileSet fileSet : fileSets) {
            DirectoryScanner ds = fileSet.getDirectoryScanner();
            String[] includedFiles = ds.getIncludedFiles();
            JavaScriptCheckStyle styleChecker = new JavaScriptCheckStyle(maxLineLength, charSet);
            for (String fileName : includedFiles) {
                File file = new File(fileSet.getDir(), fileName);
                try {
                    styleChecker.validateFile(file);
                } catch (ValidationFailureException e) {
                    log("ERROR: Validation of file <" + fileName + "> failed: " + e.getMessage());
                    countFailed++;
                }
                countProcessed++;
            }
        }
        if (countFailed > 0) {
            throw new BuildException("JavaScript style check error on <"
                    + countFailed + "> files out of <" + countProcessed + ">.");
        } else {
            log("JavaScript style check was successful for all <" + countProcessed + "> files.");
        }
    }
}
