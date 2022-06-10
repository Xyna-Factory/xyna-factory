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


public class DppFixedAttributeDatatype {
  private long dppfixedattributeid;
  private String name;
  private String eth0;
  private String eth1;
  private String eth2;
  private String eth3;
  private String domainname;
  private String failover;
  private String eth1peer;
  

  
  public DppFixedAttributeDatatype(long dppfixedattributeid, String name, String eth0, String eth1, String eth2, String eth3, String domainname, String failover, String eth1peer)
  {
    this.setDppfixedattributeid(dppfixedattributeid);
    this.setName(name);
    this.setEth0(eth0);
    this.setEth1(eth1);
    this.setEth2(eth2);
    this.setEth3(eth3);
    this.setDomainname(domainname);
    this.setFailover(failover);
    this.setEth1peer(eth1peer);
  }



  public long getDppfixedattributeid() {
    return dppfixedattributeid;
  }



  public void setDppfixedattributeid(long dppfixedattributeid) {
    this.dppfixedattributeid = dppfixedattributeid;
  }



  public String getName() {
    return name;
  }



  public void setName(String name) {
    this.name = name;
  }



  public String getEth0() {
    return eth0;
  }



  public void setEth0(String eth0) {
    this.eth0 = eth0;
  }



  public String getEth1() {
    return eth1;
  }



  public void setEth1(String eth1) {
    this.eth1 = eth1;
  }



  public String getEth2() {
    return eth2;
  }



  public void setEth2(String eth2) {
    this.eth2 = eth2;
  }



  public String getEth3() {
    return eth3;
  }



  public void setEth3(String eth3) {
    this.eth3 = eth3;
  }



  public String getFailover() {
    return failover;
  }



  public void setFailover(String failover) {
    this.failover = failover;
  }



  public String getDomainname() {
    return domainname;
  }



  public void setDomainname(String domainname) {
    this.domainname = domainname;
  }



  public String getEth1peer() {
    return eth1peer;
  }



  public void setEth1peer(String eth1peer) {
    this.eth1peer = eth1peer;
  }



  
  
  
}
