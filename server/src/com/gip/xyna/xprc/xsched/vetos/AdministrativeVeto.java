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
package com.gip.xyna.xprc.xsched.vetos;

import com.gip.xyna.xprc.xsched.VetoInformationStorable;
import com.gip.xyna.xprc.xsched.scheduling.OrderInformation;

public class AdministrativeVeto {

  public static final String ADMIN_VETO_ORDERTYPE = "Administrative Veto";
  public static final Long ADMIN_VETO_ORDERID = -1L;
  public static final OrderInformation ADMIN_VETO_ORDER_INFORMATION = new OrderInformation(ADMIN_VETO_ORDERID,ADMIN_VETO_ORDERID,ADMIN_VETO_ORDERTYPE);
  
  private String name;
  private String documentation;

  public AdministrativeVeto(String name, String documentation) {
    this.name = name;
    this.documentation = documentation;
  }

  public VetoInformationStorable toVetoInformationStorable(int currentOwnBinding) {
    return new VetoInformationStorable(name, ADMIN_VETO_ORDER_INFORMATION, documentation, currentOwnBinding);
  }

  public String getName() {
    return name;
  }

  public String getDocumentation() {
    return documentation;
  }
}
