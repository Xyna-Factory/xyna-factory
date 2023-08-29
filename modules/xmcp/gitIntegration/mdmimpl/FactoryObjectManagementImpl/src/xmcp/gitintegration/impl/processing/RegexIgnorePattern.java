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
package xmcp.gitintegration.impl.processing;



import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import xmcp.gitintegration.FactoryContentItem;



public abstract class RegexIgnorePattern<T extends FactoryContentItem> implements IgnorePatternInterface<T> {

  protected final String TAG_NAME;


  public RegexIgnorePattern(String tag) {
    TAG_NAME = tag;
  }


  @Override
  public String getPattern() {
    return TAG_NAME + ":" + "<regex>";
  }


  @Override
  public boolean validate(String value) {
    if (value.startsWith(TAG_NAME + ":")) {
      return false;
    }
    try {
      String regex = getRegexPart(value);
      Pattern.compile(regex);
    } catch (PatternSyntaxException e) {
      return false;
    }
    return true;
  }


  protected String getRegexPart(String value) {
    return value.substring(value.indexOf(":") + 1);
  }


}