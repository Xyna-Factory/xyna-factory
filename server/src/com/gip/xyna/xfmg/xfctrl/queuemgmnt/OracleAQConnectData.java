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

package com.gip.xyna.xfmg.xfctrl.queuemgmnt;



public class OracleAQConnectData extends QueueConnectData {

  private static final long serialVersionUID = 1L;

  private String jdbcUrl = null;
  private String userName = null;
  private String password = null;


  public OracleAQConnectData() {
  }

  public static OracleAQConnectData build() {
    return new OracleAQConnectData();
  }

  @Override
  public String toString() {
    return "OracleAQConnectData { jdbcUrl: " + jdbcUrl + ", userName: " + userName + " } ";
  }


  public String getJdbcUrl() {
    return jdbcUrl;
  }


  public void setJdbcUrl(String jdbcUrl) {
    this.jdbcUrl = jdbcUrl;
  }


  public String getUserName() {
    return userName;
  }


  public void setUserName(String userName) {
    this.userName = userName;
  }


  public String getPassword() {
    return password;
  }


  public void setPassword(String password) {
    this.password = password;
  }

}
