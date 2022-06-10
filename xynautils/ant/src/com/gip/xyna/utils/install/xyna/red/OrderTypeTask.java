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



import org.apache.tools.ant.BuildException;



/**
 */
public abstract class OrderTypeTask extends SOAPTask {

  protected static final String NAMESPACE_MSGS = "http://www.gip.com/xyna/1.5/xsd/factorymanager/ordertypemanager/messages/1.0";
  protected static final String NAMESPACE_COM = "http://www.gip.com/xyna/1.5/xsd/factorymanager/ordertypemanager/common/1.0";
  protected static final String NAMESPACE_OPERATION = "http://www.gip.com/xyna/1.5/wsdl/factorymanager/ordertypemanager/service/1.0";

  private String orderType;
  private String orderTypeVersion;


  /*
   * (non-Javadoc)
   * @see com.gip.xyna.utils.install.SOAPTask#getService()
   */
  @Override
  protected String getService() {
    return "/OrderTypeManager/OrderTypeManager";
  }


  /**
   * @param orderType the orderType to set
   */
  public void setOrderType(String orderType) {
    this.orderType = orderType;
  }


  /**
   * @return the orderType
   */
  protected String getOrderType() {
    if ((orderType == null) || orderType.equals("")) {
      throw new BuildException("Parameter 'orderType' not set.");
    }
    return orderType;
  }


  /**
   * @param orderTypeVersion the orderTypeVersion to set
   */
  public void setOrderTypeVersion(String orderTypeVersion) {
    this.orderTypeVersion = orderTypeVersion;
  }


  /**
   * @return the orderTypeVersion
   */
  protected String getOrderTypeVersion() {
    if ((orderTypeVersion == null) || orderTypeVersion.equals("")) {
      throw new BuildException("Parameter 'orderTypeVersion' not set.");
    }
    return orderTypeVersion;
  }

}
