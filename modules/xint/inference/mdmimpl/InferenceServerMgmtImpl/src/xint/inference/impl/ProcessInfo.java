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
package xint.inference.impl;



public class ProcessInfo {

  private final long pid;
  private final String command;
  private final String user;
  private final String[] args;


  public ProcessInfo(long pid, String command, String user, String[] args) {
    this.pid = pid;
    this.command = command;
    this.user = user;
    this.args = args;
  }


  public long getPid() {
    return pid;
  }


  public String getCommand() {
    return command;
  }


  public String getUser() {
    return user;
  }


  public String[] getArgs() {
    return args;
  }


}
