/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.juno.ws.handler;


public class HeaderDataBean {
  
  private String username;
  private String password;
  private TableHandler tablehandler;
  
  
  public HeaderDataBean() {
  }
  
  public HeaderDataBean(String username, String password, TableHandler tablehandler) {
    this();
    this.username = username;
    this.password = password;
    this.tablehandler = tablehandler;
  }
  
  public String getUsername() {
    return username;
  }
  
  public void setUsername(String username) {
    this.username = username;
  }
  
  public String getPassword() {
    return password;
  }
  
  public void setPassword(String password) {
    this.password = password;
  }
  
  public TableHandler getTablehandler() {
    return tablehandler;
  }
  
  public void setTablehandler(TableHandler tablehandler) {
    this.tablehandler = tablehandler;
  }

}
