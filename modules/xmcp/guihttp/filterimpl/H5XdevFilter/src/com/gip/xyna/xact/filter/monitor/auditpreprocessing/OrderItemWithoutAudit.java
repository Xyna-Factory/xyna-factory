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

package com.gip.xyna.xact.filter.monitor.auditpreprocessing;



import java.util.List;

import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xprc.xprcods.orderarchive.audit.AuditImport;



public class OrderItemWithoutAudit {

  private OrderItemMetaData meta;
  private List<AuditImport> imports;


  public OrderItemMetaData getMeta() {
    return meta;
  }


  public void setMeta(OrderItemMetaData meta) {
    this.meta = meta;
  }


  public List<AuditImport> getImports() {
    return imports;
  }


  public void setImports(List<AuditImport> imports) {
    this.imports = imports;
  }


  public static class OrderItemMetaData {

    private Long orderID;
    private RuntimeContext rtc;
    private String destination; //workflow FQN


    public Long getOrderID() {
      return orderID;
    }


    public void setOrderID(Long orderID) {
      this.orderID = orderID;
    }


    public RuntimeContext getRtc() {
      return rtc;
    }


    public void setRtc(RuntimeContext rtc) {
      this.rtc = rtc;
    }


    public String getDestination() {
      return destination;
    }


    public void setDestination(String destination) {
      this.destination = destination;
    }
  }
}
