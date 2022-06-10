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
package com.gip.www.juno.WS.ConfigFile.Tools;

public class DHCPSharedNetworkDatatype {

  private long sharednetworkid;
  private long standortid;
  private String sharednetwork;
  private long cpednsid;
  private String linkaddresses;
  private String migrationstate;

  
  public DHCPSharedNetworkDatatype(long sharednetworkid, long standortid, String sharednetwork, long cpednsid, String linkaddresses, String migrationstate)
  {
    this.setSharednetworkid(sharednetworkid);
    this.setStandortid(standortid);
    this.setSharednetwork(sharednetwork);
    this.setCpednsid(cpednsid);
    this.setLinkaddresses(linkaddresses);
    this.setMigrationstate(migrationstate);
  }




  public long getSharednetworkid() {
    return sharednetworkid;
  }


  public void setSharednetworkid(long sharednetworkid) {
    this.sharednetworkid = sharednetworkid;
  }



  public String getMigrationstate() {
    return migrationstate;
  }


  public void setMigrationstate(String migrationstate) {
    this.migrationstate = migrationstate;
  }




  public long getStandortid() {
    return standortid;
  }




  public void setStandortid(long standortid) {
    this.standortid = standortid;
  }




  public String getSharednetwork() {
    return sharednetwork;
  }




  public void setSharednetwork(String sharednetwork) {
    this.sharednetwork = sharednetwork;
  }




  public String getLinkaddresses() {
    return linkaddresses;
  }




  public void setLinkaddresses(String linkaddresses) {
    this.linkaddresses = linkaddresses;
  }




  public long getCpednsid() {
    return cpednsid;
  }




  public void setCpednsid(long cpednsid) {
    this.cpednsid = cpednsid;
  }



  
  
}
