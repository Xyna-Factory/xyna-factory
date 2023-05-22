/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.xfmg.xfctrl.netconfmgmt;



import java.net.InetAddress;



public class InternetAddressBean {

  private final String id;
  private final InetAddress inetAddress;
  private final String documentation;

  public InternetAddressBean(String id, InetAddress inetAddress, String documentation) {
    this.id = id;
    this.inetAddress = inetAddress;
    this.documentation = documentation;
  }


  public String getId() {
    return id;
  }


  public InetAddress getInetAddress() {
    return inetAddress;
  }

  
  public String getDocumentation() {
    return documentation;
  }

}
