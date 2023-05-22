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
package xact.ssh.cli.prompt.devices.cisco;



import java.util.regex.Pattern;

import xact.ssh.cli.prompt.ExtractionRule;
import xact.ssh.cli.prompt.PromptExtractor;
import xact.ssh.cli.prompt.rules.RegExpExtractionRule;



/**
 * Example-Implementation of a PromptExtractor-Extension for an oneAccess-DeviceType all rules are based of
 * xact.OneAccess.impl.OneAccess1440InstanceOperationImpl
 */
public class CiscoPromptExtraction extends PromptExtractor {

  /*
   * private static Pattern responseCompletePattern =
   * Pattern.compile("^[\\S\\s\\r\\n]*[\\r\\n]{1}[^#>\\s!]+[#|>][\\s]*$"); private static Pattern
   * responseCompletePatternUsername = Pattern.compile("^[\\s]*Username:[\\s]{0,1}$"); private static Pattern
   * responseCompletePatternPassword = Pattern.compile("^[\\s]*Password:[\\s]{0,1}$"); private static Pattern
   * responseCompletePatternConfirm = Pattern.compile("^[\\s\\S]*\\[confirm\\][\\s]*$"); private static Pattern
   * responseCompletePatternSave = Pattern.compile("^[\\s\\S]*\\[yes/no\\]:[\\s]*$");
   */

  public static enum CiscoRule implements ExtractionRule {
    REGULAR {
      private Pattern responseCompletePattern = Pattern.compile("^[\\S\\s\\r\\n]*[\\r\\n]{1}([^#>\\s!]+[#|>])[\\s]*$");
      private int groupIndex = 1;
      @Override
      public String extract(String input) {
        return RegExpExtractionRule.extract(responseCompletePattern, groupIndex, input);
      }
    },
    USERNAME {
      private Pattern responseCompletePatternUsername = Pattern.compile("^[\\s]*(Username:)[\\s]{0,1}$");
      private int groupIndex = 1;
      @Override
      public String extract(String input) {
        return RegExpExtractionRule.extract(responseCompletePatternUsername, groupIndex, input);
      }
    },
    PASSWORD {
      private Pattern responseCompletePatternPassword = Pattern.compile("^[\\s]*(Password:)[\\s]{0,1}$");
      private int groupIndex = 1;
      @Override
      public String extract(String input) {
        return RegExpExtractionRule.extract(responseCompletePatternPassword, groupIndex, input);
      }
    },
    CONFIRM {
      private Pattern responseCompletePatternConfirm = Pattern.compile("^[\\s\\S]*(\\[confirm\\])[\\s]*$");
      private int groupIndex = 1;
      @Override
      public String extract(String input) {
        return RegExpExtractionRule.extract(responseCompletePatternConfirm, groupIndex, input);
      }
    },
    SAVE {
      private Pattern responseCompletePatternSave = Pattern.compile("^[\\S\\s\\r\\n]*[\\r\\n]{1}([\\s\\S]*\\[yes/no\\]:)[\\s]*$");
      private int groupIndex = 1;
      @Override
      public String extract(String input) {
        return RegExpExtractionRule.extract(responseCompletePatternSave, groupIndex, input);
      }
    },
    DESTINATION {
      private Pattern responseCompletePatternSave = Pattern.compile("^[\\s\\S]*(Destination filename \\[.*\\]\\?.*)\\s*$");
      private int groupIndex = 1;
      @Override
      public String extract(String input) {
        return RegExpExtractionRule.extract(responseCompletePatternSave, groupIndex, input);
      }
    };

    public abstract String extract(String input);

  }


  public CiscoPromptExtraction() {
    super();
    initPatterns();
  }


  private void initPatterns() {
    for (CiscoRule rule : CiscoRule.values()) {
      addRule(rule);
    }
  }
  
//  public static void main(String argh[])
//  {
//    String test = " reload\n\nSystem configuration has been modified. Save? [yes/no]:   ";
//    CiscoPromptExtraction pe = new CiscoPromptExtraction();
//    System.out.println("+"+pe.extractPrompt(test).getExtract()+"+");
//  }
  


}
