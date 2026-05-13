/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package xact.ssh.server.auth;


public class UserData {
  
  private String user;
  private String password;
  private String expectedIp;
  private String expectedPort;
  
  public UserData(String user, String password, String expectedIp, String expectedPort) {
    this.user = user;
    this.password = password;
    this.expectedIp = expectedIp;
    this.expectedPort = expectedPort;
  }

  
  public String getUser() {
    return user;
  }

  
  public String getPassword() {
    return password;
  }

  
  public String getExpectedIp() {
    return expectedIp;
  }

  
  public String getExpectedPort() {
    return expectedPort;
  }
  
}