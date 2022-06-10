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
package com.gip.xyna.xmcp;

import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;


public class SynchronousSuccesfullOrderExecutionResponse extends SuccesfullOrderExecutionResponse {

  private static final long serialVersionUID = 5150516008528746228L;
  
  private final String payloadXml;
  private final GeneralXynaObject response;
  
  
  SynchronousSuccesfullOrderExecutionResponse(GeneralXynaObject response, Long orderId) {
    super(orderId);
    this.payloadXml = response.toXml();
    this.response = null;
  }


  public SynchronousSuccesfullOrderExecutionResponse(GeneralXynaObject response, long id, ResultController controller) {
    super(id);
    this.payloadXml = controller.getDefaultWrappingTypeForXMOMTypes() == WrappingType.XML && response != null ? response.toXml() : null;
    this.response = controller.getDefaultWrappingTypeForXMOMTypes() == WrappingType.ORIGINAL ? response : null;
  }
  
  public SynchronousSuccesfullOrderExecutionResponse(String payloadXML, long orderId) {
    super(orderId);
    this.payloadXml = payloadXML;
    this.response = null;
  }

  /**
   * nur enthalten, falls im ResultController WrappingType XML angegeben wurde
   */
  public String getPayloadXML() {
    return payloadXml;
  }
  
  /**
   * nur enthalten, falls im ResultController WrappingType ORIGINAL angegeben wurde
   */
  public GeneralXynaObject getResponse() {
    return response;
  }
  
  @Override
  public boolean isSynchronousResponse() {
    return true;
  }

}
