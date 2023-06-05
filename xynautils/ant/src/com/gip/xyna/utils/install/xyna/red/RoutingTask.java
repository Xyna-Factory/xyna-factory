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
package com.gip.xyna.utils.install.xyna.red;



import com.gip.xyna.utils.exceptions.ExceptionHandler;
import com.gip.xyna.utils.xml.serializer.XynaFaultSerializer;

import org.apache.tools.ant.BuildException;



/**
 */
public abstract class RoutingTask extends SOAPTask {

  protected static final String NAMESPACE_MSGS = "http://www.gip.com/xyna/1.5.10/dispatchermanager/messages/1.0";
  protected static final String NAMESPACE_OPERATION = "http://www.gip.com/xyna/1.5.10/dispatchermanager/service/1.0";

  private String dispatcherName;
  private String orderType;
  private String orderTypeVersion;
  private String url;
  private String operation;
  private String synchronous;


  @Override
  protected String getService() {
    return "/DispatcherManager/DispatcherManager";
  }


  /**
   * @param dispatcherName the dispatcherName to set
   */
  public void setDispatcherName(String dispatcherName) {
    this.dispatcherName = dispatcherName;
  }


  /**
   * @return the dispatcherName
   */
  protected String getDispatcherName() {
    if ((dispatcherName == null) || dispatcherName.equals("")) {
      throw new BuildException("Parameter 'dispatcherName' not set.");
    }
    return dispatcherName;
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


  /**
   * @param url the url to set
   */
  public void setUrl(String url) {
    this.url = url;
  }


  /**
   * @return the url
   */
  protected String getUrl() {
    if ((url == null) || url.equals("")) {
      throw new BuildException("Parameter 'url' not set.");
    }
    return url;
  }


  /**
   * @param operation the operation to set
   */
  public void setOperation(String operation) {
    this.operation = operation;
  }


  /**
   * @return the operation
   */
  protected String getOperation() {
    if ((operation == null) || operation.equals("")) {
      throw new BuildException("Parameter 'operation' not set.");
    }
    return operation;
  }


  /**
   * @param synchronous the synchronous to set
   */
  public void setSynchronous(String synchronous) {
    this.synchronous = synchronous;
  }


  /**
   * @return the synchronous
   */
  protected String getSynchronous() {
    if ((synchronous == null) || synchronous.equals("")) {
      throw new BuildException("Parameter 'synchronous' not set.");
    }
    return synchronous;
  }


  public void execute() throws BuildException {
    try {
      super.execute();
    }
    catch (BuildException e) {
      // falls fehlermeldung = XYNA-04000 (routing darf nicht geaendert werden) => ok, kein fehler
      try {
        if (e.getException() != null) {
          String s = new XynaFaultSerializer(ExceptionHandler.toXynaFault((Exception)e.getException())).toXMLString();
          log("setrouting got exception: " + s);
          if (s.indexOf("XYNA-04000") > -1) {
            log("Caught XYNA-04000 (no fatal error)");
            return;
          }
        }
      }
      catch (Exception f) {
        // ignore
      }
      throw e;
    }
  }
}
