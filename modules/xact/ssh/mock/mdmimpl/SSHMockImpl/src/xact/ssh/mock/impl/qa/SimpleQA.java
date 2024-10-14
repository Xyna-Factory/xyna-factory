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

import xact.ssh.mock.impl.QAParser.LineType;

public class SimpleQA implements QA {
  
  private final String question;
  private final String answer;
  private final String execution;
  
  public SimpleQA(String question, String answer, EnumMap<LineType,String> parameter) {
    this.question = question;
    this.answer = answer;
    this.execution = parameter.get(LineType.Execution);
  }

  @Override
  public boolean matches(CurrentQAData data) {
    return data.getTrimmedQuestion().equals(question);
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