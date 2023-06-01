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

import java.util.ArrayList;
import java.util.List;

import com.gip.xtfutils.xmltools.nav.jdom.SoapBuilder;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.SocketNotAvailableException;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import org.apache.log4j.Logger;

import xact.http.Request;
import xact.http.Response;
import xact.http.URI;


public class GenericSOAPFilter extends ConnectionFilter<HTTPTriggerConnection> {

  public static class Constant {
    public static final String PREFIX_PROPERTY_NAME_DESTINATION = "xact.filter.SOAPFilter.";
    public static final String PROPERTY_NAME_DEFAULT_WF = "xact.filter.SOAPFilter.defaultWF";
    public static final String DEFAULT_WF = "xact.http.ForwardHttpMessage";
  }

  private static Logger _logger = CentralFactoryLogging.getLogger(GenericSOAPFilter.class);

  /**
   * Analyzes TriggerConnection and creates XynaOrder if it accepts the connection.
   * The method return a FilterResponse object, which can include the XynaOrder if the filter is responsibleb for the request.
   * # If this filter is not responsible the returned object must be: FilterResponse.notResponsible()
   * # If this filter is responsible the returned object must be: FilterResponse.responsible(XynaOrder order)
   * # If this filter is responsible but it handle the request without creating a XynaOrder the
   *   returned object must be: FilterResponse.responsibleWithoutXynaorder()
   * # If this filter is responsible but the version of this filter is too new the returned
   *    object must be: FilterResponse.responsibleButTooNew(). The trigger will try an older version of the filter.
   * @param tc
   * @return FilterResponse object
   * @throws XynaException caused by errors reading data from triggerconnection or having an internal error.
   *         results in onError() being called by Xyna Processing.
   */
  public FilterResponse createXynaOrder(HTTPTriggerConnection tc) throws XynaException {
    //return FilterResponse.notResponsible() if next filter should be tried
    String response = "FAILED";
    try {
      tc.read();
      String uri = tc.getUri();
      String message = tc.getPayload();
      int port = tc.getSocket().getLocalPort();
      _logger.info("#### GenericSOAPFilter: port = " + port + ", uri = " + uri + ", received message.");
      _logger.debug("#### GenericSOAPFilter: port = " + port + ", uri = " + uri + ", message = " + message);

      List<String> requestsXml = SoapBuilder.getSoapBodyContent(message);
      List<Request> requests = new ArrayList<Request>();

      for (String requestNode : requestsXml){
        Request request = new Request();
        request.setXml(requestNode);
        requests.add(request);
      }
      URI uriWf = new URI();
      uriWf.setValue(uri);

      //DestinationKey destKey = new DestinationKey("xact.http.ForwardHttpMessage");
      DestinationKey destKey = new DestinationKey(getWorkflowFromPort(port));

      XynaOrder xynaOrder = new XynaOrder(destKey, uriWf,
                                          new XynaObjectList<Request>(requests,Request.class));
      return FilterResponse.responsible(xynaOrder);
    }
    catch (Throwable e) {
      tc.sendResponse(response);
      _logger.error("Error in filter: ", e);
      throw new XynaException("Error in filter", e);
    }
  }


  /**
   * called when above XynaOrder returns successfully.
   * @param response by XynaOrder returned XynaObject
   * @param tc corresponding triggerconnection
   */
  public void onResponse(XynaObject response, HTTPTriggerConnection tc) {
    Response resp = (Response) response;
    try {
      String xml = resp.getXml();
      String soap = SoapBuilder.addSoapEnvelope(xml);
      _logger.debug("Going to send response: \n " + soap);

      tc.sendResponse(soap);
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
      tc.sendResponse("FAILED");
    }
    catch (Exception ex) {
      _logger.error("Error in filter: ", ex);
    }
  }

  /**
   * @return description of this filter
   */
  public String getClassDescription() {
    //TODO implementation
    //TODO update dependency xml file
    return null;
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
    try {
      String val = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().
                      getConfiguration().getProperty(propname);
      _logger.debug("Got value for property '" + propname + "': " + val);
      if ((val == null) || (val.trim().length() < 1)) {
        return "";
      }
      return val;
    }
    catch (Exception e) {
      _logger.debug("", e);
      return "";
    }
  }

}
