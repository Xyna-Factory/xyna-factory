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



import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.gip.xyna.utils.soap.SOAPUtils;
import com.gip.xyna.utils.soap.SoapRequest;
import com.gip.xyna.utils.soap.SoapResponse;



/**
 */
public abstract class SOAPTask extends Task {

  private int httpPort = -1;
  private String host;


  @Override
  public void execute() throws BuildException {
    try {
      SOAPUtils soapUtils = new SOAPUtils("http", getHost(), getHttpPort(), getCheckedService());
      SoapRequest request = new SoapRequest(getCheckedMessage());
      soapUtils.setSoapAction(getCheckedSOAPAction());
      log("SOAP-Message: " + request.getAsXML());
      log("Call operation: " + getCheckedSOAPAction());
      SoapResponse response = null;
      response = soapUtils.sendSOAPMessage(request);
      if (response.hasError()) {
        log(response.getXMLString());
        throw new BuildException("SOAP response contains error.", response.getError());
      }
    }
    catch (IOException e) {
      log(e.getMessage());
      throw new BuildException(e.getMessage(), e);
    }
    catch (BuildException be) {
      throw be;
    }
    catch (Exception e) {
      log(e.getMessage());
      throw new BuildException(e.getMessage(), e);
    }
  }


  private String getCheckedService() {
    String service = getService();
    if ((service == null) || service.equals("")) {
      throw new BuildException("Parameter 'service' not set.");
    }
    return service;
  }


  protected abstract String getService();


  private String getCheckedMessage() {
    String message = getMessage();
    if ((message == null) || message.equals("")) {
      throw new BuildException("Parameter 'message' not set.");
    }
    return message;
  }


  protected abstract String getMessage();


  private String getCheckedSOAPAction() {
    String soapAction = getSOAPAction();
    if ((soapAction == null) || soapAction.equals("")) {
      throw new BuildException("Parameter 'soapAction' not set.");
    }
    return soapAction;
  }


  protected abstract String getSOAPAction();


  /**
   * @param httpPort the httpPort to set
   */
  public void setHttpPort(int httpPort) {
    this.httpPort = httpPort;
  }


  /**
   * @return the httpPort
   */
  protected int getHttpPort() {
    if (httpPort < 0) {
      throw new BuildException("Parameter 'httpPort' not set.");
    }
    return httpPort;
  }


  /**
   * @param host the host to set
   */
  public void setHost(String host) {
    this.host = host;
  }


  /**
   * @return the host
   */
  protected String getHost() {
    if ((host == null) || host.equals("")) {
      throw new BuildException("Parameter 'host' not set.");
    }
    return host;
  }
}
