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
package xdnc.dhcpv6.parsing;

import java.util.ArrayList;
import java.util.List;


public class SharedNetwork {

  private String name;
  private String comment;
  private String linkAddresses;
  private List<Subnet> subnets;
  
  public SharedNetwork(String name, String comment) {
    this.name = name;
    this.comment = comment;
    subnets = new ArrayList<Subnet>();
  }

  public void setLinkAddresses(String linkAddresses) {
    this.linkAddresses = linkAddresses;
  }

  public void addSubnet(Subnet subnet) {
    subnets.add(subnet);
  }
  
  public String getName() {
    return name;
  }
  
  public String getComment() {
    return comment;
  }
  
  public String getLinkAddresses() {
    return linkAddresses;
  }
  
  public List<Subnet> getSubnets() {
    return subnets;
  }

}
