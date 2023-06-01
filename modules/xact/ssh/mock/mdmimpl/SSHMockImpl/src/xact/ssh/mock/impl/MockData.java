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
package xact.ssh.mock.impl;

import java.util.List;
import java.util.Stack;

import xact.ssh.mock.MockedDevice;
import xact.ssh.mock.impl.qa.QA;
import xact.ssh.server.SSHSessionCustomization;


public class MockData {

  private List<QA> qas;
  private Stack<String> context;
  private List<QA> motds;
  private String prompt;
  private MockedDevice mockedDevice;
  private String tempKey;
  private SSHSessionCustomization sshSessionCustomization;

  public MockData(List<QA> qas, List<QA> motds, 
      MockedDevice mockedDevice, SSHSessionCustomization sshSessionCustomization) {
    this.qas = qas;
    this.motds = motds;
    this.context = new Stack<String>();
    this.context.push("base");
    this.mockedDevice = mockedDevice;
    this.sshSessionCustomization = sshSessionCustomization;
    
  }

  public List<QA> getMotds() {
    return motds;
  }

  public List<QA> getQas() {
    return qas;
  }

  public String getCurrentContext() {
    return context.peek();
  }

  public void setCurrentContext(String newContext) {
    context.push(newContext);
  }

  public void removeCurrentContext() {
    context.pop();
  }

  public String getCurrentPrompt() {
    return prompt;
  }

  public void setPrompt(String prompt) {
    this.prompt = prompt;
  }
  
  public void putParam(String key, String value) {
    mockedDevice.putKeyValueString(key, value);
  }

  public void removeParam(String key) {
    mockedDevice.removeKeyValueString(key);
  }
  
  public String getParam(String key) {
    return mockedDevice.getKeyValueString(key);
  }

  public MockedDevice getMockedDevice() {
    return mockedDevice;
  }

  public String getTempKey() {
    return tempKey;
  }

  public void setTempKey(String tempKey) {
    this.tempKey = tempKey;
  }

  public SSHSessionCustomization getSSHSessionCustomization() {
    return sshSessionCustomization;
  }

  

}
