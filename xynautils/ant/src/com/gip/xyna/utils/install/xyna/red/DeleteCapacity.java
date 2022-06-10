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


/**
 *
 */
public class DeleteCapacity extends CapacityTask {

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.install.xfbb.SOAPTask#getMessage()
   */
  @Override
  protected String getMessage() {
    return "<ns1:deleteCapacityRequest xmlns:ns1=\"" + NAMESPACE_MSGS + "\">" +
           "<ns1:name>" + getName() + "</ns1:name>" +
           "</ns1:deleteCapacityRequest>";
  }


  /* (non-Javadoc)
   * @see com.gip.xyna.utils.install.xfbb.SOAPTask#getOperation()
   */
  @Override
  protected String getSOAPAction() {
    return NAMESPACE_OPERATION + "/deleteCapacity";
  }

}
