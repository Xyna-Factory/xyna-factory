/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package xact.ssh.mock.impl.qa;

import java.util.EnumMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import xact.ssh.mock.impl.QAParser.LineType;

public class SimpleQAPattern implements QA {
  
  private final Pattern questionPattern;
  private final String answer;
  private final String execution;
  
  public SimpleQAPattern(String question, String answer, EnumMap<LineType,String> parameter) {
    this.questionPattern = Pattern.compile(question);
    this.answer = answer;
    this.execution = parameter.get(LineType.Execution);
  }

  @Override
  public boolean matches(CurrentQAData data) {
    Matcher m = questionPattern.matcher(data.getTrimmedQuestion());
    if( m.matches() ) {
      data.setQuestionMatcher(m);
      return true;
    }
    return false;
  }

  @Override
  public String getResponse(CurrentQAData data) {
    return answer;
  }

  @Override
  public String getPrompt(CurrentQAData data) {
    return data.getMockData().getCurrentPrompt();
  }

  @Override
  public void handle(CurrentQAData data) {
  }

  @Override
  public String getExecution() {
    return execution;
  }

}