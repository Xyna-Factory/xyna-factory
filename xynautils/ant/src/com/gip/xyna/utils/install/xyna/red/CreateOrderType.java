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
package com.gip.xyna.utils.install.xyna.red;



import java.util.Iterator;
import java.util.Vector;



/**
 */
public class CreateOrderType extends OrderTypeTask {

  private Vector<Capacity> capacities = new Vector<Capacity>();


  /*
   * (non-Javadoc)
   * @see com.gip.xyna.utils.install.SOAPTask#getMessage()
   */
  @Override
  protected String getMessage() {
    StringBuffer buffer = new StringBuffer();
    buffer.append("<ns1:createOrderTypeRequest xmlns:ns1=\"" + NAMESPACE_MSGS + "\">");
    buffer.append("<ns2:orderTypeInformation xmlns:ns2=\"" + NAMESPACE_COM + "\">");
    buffer.append("<ns2:orderType>");
    buffer.append(getOrderType());
    buffer.append("</ns2:orderType>");
    buffer.append("<ns2:orderTypeVersion>");
    buffer.append(getOrderTypeVersion());
    buffer.append("</ns2:orderTypeVersion>");
    for (Iterator<Capacity> iter = capacities.iterator(); iter.hasNext();) {
      Capacity capacity = iter.next();
      buffer.append("<ns2:capacity>");
      buffer.append("<ns2:name>");
      buffer.append(capacity.getName());
      buffer.append("</ns2:name>");
      buffer.append("<ns2:usage>");
      buffer.append(capacity.getUsage());
      buffer.append("</ns2:usage>");
      buffer.append("</ns2:capacity>");
    }
    buffer.append("</ns2:orderTypeInformation>");
    buffer.append("</ns1:createOrderTypeRequest>");
    return buffer.toString();
  }


  /*
   * (non-Javadoc)
   * @see com.gip.xyna.utils.install.SOAPTask#getOperation()
   */
  @Override
  protected String getSOAPAction() {
    return NAMESPACE_OPERATION + "/createOrderType";
  }


  public Capacity createCapacity() {
    Capacity capacity = new Capacity();
    capacities.add(capacity);
    return capacity;
  }


}
