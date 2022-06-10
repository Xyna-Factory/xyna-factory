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

public class DHCPSubnetDatatype {

  private long subnetid;
  private long sharednetworkid;
  private String subnet;
  private String mask;
  private String fixedattributes;
  private String attributes;
  private String migrationstate;

  
  public DHCPSubnetDatatype(long subnetid, long sharednetworkid, String subnet, String mask, String fixedattributes, String attributes, String migrationstate)
  {
    this.setSubnetid(subnetid);
    this.setSharednetworkid(sharednetworkid);
    this.setSubnet(subnet);
    this.setMask(mask);
    this.setFixedattributes(fixedattributes);
    this.setAttributes(attributes);
    this.setMigrationstate(migrationstate);
  }


  public long getSubnetid() {
    return subnetid;
  }


  public void setSubnetid(long subnetid) {
    this.subnetid = subnetid;
  }


  public long getSharednetworkid() {
    return sharednetworkid;
  }


  public void setSharednetworkid(long sharednetworkid) {
    this.sharednetworkid = sharednetworkid;
  }


  public String getSubnet() {
    return subnet;
  }


  public void setSubnet(String subnet) {
    this.subnet = subnet;
  }


  public String getMask() {
    return mask;
  }


  public void setMask(String mask) {
    this.mask = mask;
  }


  public String getFixedattributes() {
    return fixedattributes;
  }


  public void setFixedattributes(String fixedattributes) {
    this.fixedattributes = fixedattributes;
  }


  public String getAttributes() {
    return attributes;
  }


  public void setAttributes(String attributes) {
    this.attributes = attributes;
  }


  public String getMigrationstate() {
    return migrationstate;
  }


  public void setMigrationstate(String migrationstate) {
    this.migrationstate = migrationstate;
  }



  
  
}
