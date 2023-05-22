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
package xact.ssh.cli.prompt.rules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import xact.ssh.cli.prompt.ExtractionRule;


/**
 * Utility class f�r RegExp-based ExtractionRules
 */
public class RegExpExtractionRule implements ExtractionRule {
  
  private final Pattern pattern;
  private final int groupIndex;
  
  public RegExpExtractionRule(String regExp, int groupIndexForExtraction) {
    this(Pattern.compile(regExp), groupIndexForExtraction);
  }
  
  
  public RegExpExtractionRule(Pattern pattern, int groupIndexForExtraction) {
    this.pattern = pattern;
    this.groupIndex = groupIndexForExtraction;
  }
  
  
  public String extract(String input) {
    return RegExpExtractionRule.extract(pattern, groupIndex, input);
  }
  
  
  public static String extract(Pattern pattern, int groupIndex, String input) {
    Matcher matcher = pattern.matcher(input);
    if (matcher.matches()) {
      if (matcher.groupCount() >= groupIndex) {
        return matcher.group(groupIndex);
      }
    }
    return null;
  }
  
}
