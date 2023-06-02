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
package com.gip.xyna.xnwh.persistence;


public class ODSPreparedCommand implements PreparedCommand {

  private PreparedCommand innerPC;
  private Command command;
  
  public ODSPreparedCommand(Command command, PreparedCommand innerPC) {
    this.innerPC = innerPC;
    this.command = command;
  }
  
  public String getTable() {
    return innerPC.getTable();
  }
  
  public void setInnerPreparedCommand(PreparedCommand newPC) {
    this.innerPC = newPC;
  }

  public PreparedCommand getInnerPreparedCommand() {
    return innerPC;
  }

  public Command getCommand() {
    return command;
  }

}
