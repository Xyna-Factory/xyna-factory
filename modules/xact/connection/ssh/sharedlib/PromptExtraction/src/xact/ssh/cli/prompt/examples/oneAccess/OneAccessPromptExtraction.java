/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package xact.ssh.cli.prompt.examples.oneAccess;

import java.util.regex.Pattern;

import xact.ssh.cli.prompt.ExtractionRule;
import xact.ssh.cli.prompt.PromptExtract;
import xact.ssh.cli.prompt.PromptExtractor;
import xact.ssh.cli.prompt.rules.RegExpExtractionRule;

/**
 * Example-Implementation of a PromptExtractor-Extension for an oneAccess-DeviceType
 * all rules are based of xact.OneAccess.impl.OneAccess1440InstanceOperationImpl
 */
public class OneAccessPromptExtraction extends PromptExtractor {

  
  public static enum OneAccessRule implements ExtractionRule {
    REGULAR {
      private Pattern responseCompletePattern = Pattern.compile("^[\\S\\s\\r\\n]*[\\r\\n]{1}([^#>\\s!]+[#|>])[\\s]*$");
      private int groupIndex = 1;
      @Override
      public String extract(String input) {
        return RegExpExtractionRule.extract(responseCompletePattern, groupIndex, input);
      }
    },
    USERNAME {
      private Pattern responseCompletePatternUsername = Pattern.compile("^[\\S\\s\\r\\n]*[\\r\\n]{1}(Username:)$");
      private int groupIndex = 1;
      @Override
      public String extract(String input) {
        return RegExpExtractionRule.extract(responseCompletePatternUsername, groupIndex, input);
      }
    },
    PASSWORD {
      private Pattern responseCompletePatternPassword = Pattern.compile("^[\\S\\s\\r\\n]*[\\r\\n]{1}(Password:)$");
      private int groupIndex = 1;
      @Override
      public String extract(String input) {
        return RegExpExtractionRule.extract(responseCompletePatternPassword, groupIndex, input);
      }
    },
    FACTORY_SETTING_RESTORATION {
      private String FACTORY_SETTING_RESTORATION_STRING = "Are you sure you want to restore factory settings?(Y/N):";
      @Override
      public String extract(String input) {
        if (input.contains(FACTORY_SETTING_RESTORATION_STRING)) {
          return FACTORY_SETTING_RESTORATION_STRING;
        } else {
          return null;
        }
      }
    },
    POWER_ON_REBOOT {
      private String POWER_ON_REBOOT = "Power on reboot requested by user";
      @Override
      public String extract(String input) {
        if (input.contains(POWER_ON_REBOOT)) {
          return POWER_ON_REBOOT;
        } else {
          return null;
        }
      }
    };

    public abstract String extract(String input);
    
  }
  
  public OneAccessPromptExtraction() {
    super();
    initPatterns();
  }

  private void initPatterns() {
    for (OneAccessRule rule : OneAccessRule.values()) {
      addRule(rule);
    }
  }
  
  
  /**
   * Possible MDM-Operation Implementations using a class like this 
   */
  
  private final static OneAccessPromptExtraction oape = new OneAccessPromptExtraction();
  
  // just return the extracted prompt
  private String extractPrompt_example1(String response) {
    PromptExtract extract = oape.extractPrompt(response);
    return extract.getExtract();
  }
  
  
  // return a unique identifier for each matching rule
  private String extractPrompt_example2(String response) {
    PromptExtract extract = oape.extractPrompt(response);
    if (extract.getMatchingRule() instanceof OneAccessRule) {
      return extract.getMatchingRule().toString(); // OneAccessRule.toString equals their name used during Enum-Declaration
    } else {
      return "UNMATCHED";
    }
  }
  
  
  // adjust result according to matched rule, commented XynaFactory-access given as example what would have been possible in a modelled type
  private String extractPrompt_example3(String response) {
    xact.ssh.cli.prompt.PromptExtract extract = oape.extractPrompt(response);
    if (extract.getMatchingRule() instanceof xact.ssh.cli.prompt.examples.oneAccess.OneAccessPromptExtraction.OneAccessRule) {
      xact.ssh.cli.prompt.examples.oneAccess.OneAccessPromptExtraction.OneAccessRule oneAccessRule =
        (xact.ssh.cli.prompt.examples.oneAccess.OneAccessPromptExtraction.OneAccessRule) extract.getMatchingRule();
      switch (oneAccessRule) {
        case REGULAR :
        case PASSWORD :
        case USERNAME :
          return extract.getExtract();
        case FACTORY_SETTING_RESTORATION :
          //return com.gip.xyna.XynaFactory.getInstance().getXynaMultiChannelPortal().getProperty("xact.ssh.cli.prompt.factorySettingRestorationIdentifier");
          return "FACTORY_RESET";
        case POWER_ON_REBOOT :
          //return com.gip.xyna.XynaFactory.getInstance().getXynaMultiChannelPortal().getProperty("xact.ssh.cli.prompt.powerOnRebootIdentifier");
          return "REQUESTED_REBOOT";
        default :
          return "";
      }
    } else {
      return "";
    }
  }
  
}
