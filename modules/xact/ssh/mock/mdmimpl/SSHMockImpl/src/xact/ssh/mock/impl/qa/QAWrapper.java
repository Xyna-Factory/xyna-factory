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
package xact.ssh.mock.impl.qa;

/**
 * Hilfe zum Bau eines Decorator 
 *
 */
public class QAWrapper implements QA {

  protected QA qa;
  
  public QAWrapper(QA qa) {
    this.qa = qa;
  }

  @Override
  public boolean matches(CurrentQAData data) {
    return qa.matches(data);
  }

  @Override
  public String getPrompt(CurrentQAData data) {
    return qa.getPrompt(data);
  }

  @Override
  public String getResponse(CurrentQAData data) {
    return qa.getResponse(data);
  }

  @Override
  public void handle(CurrentQAData data) {
    qa.handle(data);
  }

  @Override
  public String getExecution() {
    return qa.getExecution();
  }

}
