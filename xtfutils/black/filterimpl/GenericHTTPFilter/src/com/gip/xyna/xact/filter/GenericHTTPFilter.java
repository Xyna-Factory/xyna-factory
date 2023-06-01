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
package com.gip.xyna.xact.filter;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;

import xact.http.HTTPRequest;
import xact.http.HTTPResponse;
import xact.http.URI;


public class GenericHTTPFilter extends ConnectionFilter<HTTPTriggerConnection> {

  private static final long serialVersionUID = 1L;
  private static Logger _logger = CentralFactoryLogging.getLogger(GenericHTTPFilter.class);

  public static class Constant {
    public static final String PREFIX_PROPERTY_NAME_DESTINATION = "xact.filter.GenericHTTP.";
    public static final String PROPERTY_NAME_DEFAULT_WF = "xact.filter.GenericHTTP.defaultWF";
    public static final String DEFAULT_WF = "xact.http.ForwardHttpMessage";
  }

  
  /**
   * Analyzes TriggerConnection and creates XynaOrder if it accepts the connection. The method return a FilterResponse
   * object, which can include the XynaOrder if the filter is responsibleb for the request. # If this filter is not
   * responsible the returned object must be: FilterResponse.notResponsible() # If this filter is responsible the
   * returned object must be: FilterResponse.responsible(XynaOrder order) # If this filter is responsible but it handle
   * the request without creating a XynaOrder the returned object must be: FilterResponse.responsibleWithoutXynaorder()
   * # If this filter is responsible but the version of this filter is too new the returned object must be:
   * FilterResponse.responsibleButTooNew(). The trigger will try an older version of the filter.
   * @param tc
   * @return FilterResponse object
   * @throws XynaException caused by errors reading data from triggerconnection or having an internal error. results in
   *           onError() being called by Xyna Processing.
   */
  public FilterResponse createXynaOrder(HTTPTriggerConnection tc) throws XynaException {

    try {
      tc.read();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    String uri = tc.getUri();
    String message = tc.getPayload();
    int port = tc.getSocket().getLocalPort();
    if (!_logger.isDebugEnabled()) {
      _logger.info("#### GenericHTTPFilter: port = " + port + ", uri = " + uri + ", received message.");
    } else {
      _logger.debug("#### GenericHTTPFilter: port = " + port + ", uri = " + uri + ", message = " + message);
    }

    HTTPRequest request = new HTTPRequest();
    request.setRequest(message);
    URI uriWf = new URI();
    uriWf.setValue(uri);
    DestinationKey destKey = new DestinationKey(getWorkflowFromPort(port));

    XynaOrder xynaOrder = new XynaOrder(destKey, uriWf, request);
    return FilterResponse.responsible(xynaOrder);

  }

  /**
   * called when above XynaOrder returns successfully.
   * @param response by XynaOrder returned XynaObject
   * @param tc corresponding triggerconnection
   */
  public void onResponse(XynaObject response, HTTPTriggerConnection tc) {
    HTTPResponse resp = (HTTPResponse) response;
    try {
      String xml = resp.getResponse();
      _logger.debug("Going to send response: \n " + xml);

      tc.sendResponse(xml);
    }
    catch (Exception e) {
      _logger.error("Error in filter: ", e);
    }
  }


  /**
   * called when above XynaOrder returns with error or if an XynaException occurs in generateXynaOrder().
   * @param e
   * @param tc corresponding triggerconnection
   */
  public void onError(XynaException[] e, HTTPTriggerConnection tc) {
    try {
      if (e.length > 0 && e[0] != null) {
        tc.sendError(e[0].getMessage());
      } else {
        tc.sendError("Unknown error occured");
      }
    } catch (Exception ex) {
      String errorMessage = "Error in filter while trying to send error";
      if (e.length > 0) {
        errorMessage += " (original error was: " + e[0].getMessage() + ")";
      }
      _logger.error(errorMessage, ex);
    }
  }


  /**
   * @return description of this filter
   */
  public String getClassDescription() {
    return "GenericHTTPFilter";
  }


  private String getWorkflowFromPort(int port) {
    String propName = Constant.PREFIX_PROPERTY_NAME_DESTINATION + port;
    String val = getXynaProperty(propName);
    if ((val != null) && (val.trim().length() > 0)) {
      return val;
    }
    val = getXynaProperty(Constant.PROPERTY_NAME_DEFAULT_WF);
    if ((val != null) && (val.trim().length() > 0)) {
      return val;
    }
    return Constant.DEFAULT_WF;
  }


  private static String getXynaProperty(String propname) {
    String val =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration()
            .getProperty(propname);
    _logger.debug("Got value for property '" + propname + "': " + val);
    if ((val == null) || (val.trim().length() < 1)) {
      return "";
    }
    return val;
  }

}
