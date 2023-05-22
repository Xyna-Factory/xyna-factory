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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.exceptions.exceptioncode.ExceptionCodeManagement.CodeGroup;
import com.gip.xyna.utils.exceptions.exceptioncode.ExceptionCodeManagement.Pattern;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listcodegroups;



public class ListcodegroupsImpl extends XynaCommandImplementation<Listcodegroups> {

  public void execute(OutputStream statusOutputStream, Listcodegroups payload) throws XynaException {

    CodeGroup[] codeGroups =
        XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getExceptionManagement().listCodeGroups();
    Arrays.sort(codeGroups, new Comparator<CodeGroup>() {

      public int compare(CodeGroup o1, CodeGroup o2) {
        if (o1.getPatterns().size() == 0) {
          if (o2.getPatterns().size() == 0) {
            return o1.getCodeGroupName().compareTo(o2.getCodeGroupName());
          }
          return -1;
        }
        if (o2.getPatterns().size() == 0) {
          return 1;
        }
        Pattern p1 = o1.getPattern(o1.getIndexOfCurrentlyUsedPattern());
        Pattern p2 = o2.getPattern(o2.getIndexOfCurrentlyUsedPattern());
        String s1 = p1.getPrefix() + "[[]]" + p1.getSuffix();
        String s2 = p2.getPrefix() + "[[]]" + p2.getSuffix();
        int comparedPattern = s1.compareTo(s2);
        if (comparedPattern != 0) {
          return comparedPattern;
        }
        return p1.getStartIndex() - p2.getStartIndex();
      }

    });
    for (CodeGroup codeGroup : codeGroups) {
      String codeGroupString =
          codeGroup.getCodeGroupName() + " currentPattern = " + codeGroup.getIndexOfCurrentlyUsedPattern() + "\n";
      writeToCommandLine(statusOutputStream, codeGroupString);
      for (int i = 0; i < codeGroup.getPatterns().size(); i++) {
        Pattern pattern = codeGroup.getPattern(i);
        String patternString =
            "  " + i + ". pattern: " + pattern.getPrefix() + "[[]]" + pattern.getSuffix() + " (padding="
                + pattern.getPadding() + ") " + " start=" + pattern.getStartIndex() + " end=" + pattern.getEndIndex()
                + " next=" + (pattern.getCurrentIndex() + 1) + "\n";
        writeToCommandLine(statusOutputStream, patternString);
      }
    }
  
  }

}
