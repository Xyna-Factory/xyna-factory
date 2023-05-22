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
package com.gip.xyna.utils.install.xyna.red;



/**
 */
public class DeleteOrderType extends OrderTypeTask {


  /*
   * (non-Javadoc)
   * @see com.gip.xyna.utils.install.SOAPTask#getMessage()
   */
  @Override
  protected String getMessage() {
    return "<ns1:deleteOrderTypeRequest xmlns:ns1=\"" + NAMESPACE_MSGS + "\">" +
    "<ns1:orderType>" + getOrderType() + "</ns1:orderType>" + "<ns1:orderTypeVersion>" 
    + getOrderTypeVersion() + "</ns1:orderTypeVersion>" + "</ns1:deleteOrderTypeRequest>";
  }


  /*
   * (non-Javadoc)
   * @see com.gip.xyna.utils.install.SOAPTask#getOperation()
   */
  @Override
  protected String getSOAPAction() {
    return NAMESPACE_OPERATION + "/deleteOrderType";
  }


}
