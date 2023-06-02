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
package com.gip.xyna.xnwh.persistence.oracle;

import com.gip.xyna.xnwh.persistence.Command;
import com.gip.xyna.xnwh.persistence.PreparedCommand;


public class OraclePreparedCommand implements PreparedCommand {

  private Command cmd;
  
  public OraclePreparedCommand(Command cmd) {
    this.cmd = cmd;    
  }
  
  public String getTable() {
    return cmd.getTable();
  }
  
  public String getSqlString() {
    return cmd.getSqlString();
  }

}
