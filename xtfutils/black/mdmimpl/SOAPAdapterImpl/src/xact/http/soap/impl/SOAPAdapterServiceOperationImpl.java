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
package xact.http.soap.impl;


import org.apache.log4j.Logger;

import com.gip.xtfutils.httptools.http.HttpResponse;
import com.gip.xtfutils.httptools.soap.SoapInput;
import com.gip.xtfutils.httptools.soap.SoapTools;
import com.gip.xtfutils.xmltools.nav.XmlNavigator;
import com.gip.xtfutils.xmltools.nav.jdom.JdomNavigator;
import com.gip.xtfutils.xmltools.nav.jdom.SoapBuilder;
import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;

import xact.http.Request;
import xact.http.Response;
import xact.http.soap.*;
import xdev.map.XML;


public class SOAPAdapterServiceOperationImpl implements ExtendedDeploymentTask, SOAPAdapterServiceOperation {

  private static Logger _logger = CentralFactoryLogging.getLogger(SOAPAdapterServiceOperationImpl.class);


  public void onDeployment() throws XynaException {
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public void onUndeployment() throws XynaException {
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.;
    return null;
  }

  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }


  public SOAPRequest addSOAPEnvelope(Request request) {
    String opname = "addSOAPEnvelope";
    try {
      _logger.info("Entering Coded Service " + opname);
      SOAPRequest ret = new SOAPRequest();
      String xml = request.getXml();
      String soap = SoapBuilder.addSoapEnvelope(xml);
      ret.setSOAPRequest(soap);
      return ret;
    }
    catch (Exception e) {
      _logger.error("Error in Coded Service " + opname);
      throw new RuntimeException("Error in Coded Service " + opname, e);
    }
  }


  public Response removeOptionalSOAPEnvelope(SOAPResponse sOAPResponse) {
    String opname = "removeOptionalSOAPEnvelope";
    try {
      _logger.info("Entering Coded Service " + opname);
      Response ret = new Response();
      String xml = sOAPResponse.getSOAPResponse();
      String adapted = SoapBuilder.removeOptionalSoapEnvelope(xml);
      ret.setXml(adapted);
      return ret;
    }
    catch (Exception e) {
      _logger.error("Error in Coded Service " + opname);
      throw new RuntimeException("Error in Coded Service " + opname, e);
    }
  }


  public SOAPResponse sendSOAPRequest(SOAPRequest sOAPRequest, SOAPConfig sOAPConfig) {
    String opname = "sendSOAPRequest";
    try {
      _logger.info("Entering Coded Service " + opname);
      SoapInput inp = new SoapInput();
      inp.setPayload(sOAPRequest.getSOAPRequest());
      inp.setUrl(sOAPConfig.getURL());
      inp.setSoapAction(inp.getSoapAction());
      HttpResponse resp = SoapTools.invokeWebservice(inp);
      _logger.info("Received http response: " + resp);
      SOAPResponse ret = new SOAPResponse();
      ret.setSOAPResponse(resp.getResponsePayload());
      return ret;
    }
    catch (Exception e) {
      _logger.error("Error in Coded Service " + opname);
      throw new RuntimeException("Error in Coded Service " + opname, e);
    }
  }


  public XML addSOAPEnvelopeToXML(XML request) {
    String opname = "addSOAPEnvelopeToXML";
    try {
      _logger.info("Entering Coded Service " + opname);
      XML ret = new XML();
      String xml = request.getXmlString();
      String soap = SoapBuilder.addSoapEnvelope(xml);
      ret.setXmlString(soap);
      return ret;
    }
    catch (Exception e) {
      _logger.error("Error in Coded Service " + opname);
      throw new RuntimeException("Error in Coded Service " + opname, e);
    }
  }


  public XML removeOptionalSOAPEnvelopeFromXML(XML input) {
    String opname = "removeOptionalSOAPEnvelopeFromXML";
    try {
      _logger.info("Entering Coded Service " + opname);
      XML ret = new XML();
      String xml = input.getXmlString();
      String adapted = SoapBuilder.removeOptionalSoapEnvelope(xml);
      ret.setXmlString(adapted);
      return ret;
    }
    catch (Exception e) {
      _logger.error("Error in Coded Service " + opname);
      throw new RuntimeException("Error in Coded Service " + opname, e);
    }

  }

  public XML addSOAPEnvelopeToXMLWithoutDefaultNamespace(XML request) {
    String opname = "addSOAPEnvelopeToXMLWithoutDefaultNamespace";
    try {
      _logger.info("Entering Coded Service " + opname);
      XML ret = new XML();
      String xml = request.getXmlString();
      String soap = SoapBuilder.addSoapEnvelopeWithoutDefaultNamespace(xml);
      ret.setXmlString(soap);
      return ret;
    }
    catch (Exception e) {
      _logger.error("Error in Coded Service " + opname);
      throw new RuntimeException("Error in Coded Service " + opname, e);
    }
  }


  public XML addSOAPEnvelopeWithWSSecUserData(XML request, WSSecurityUserData wsSecData) {
    String opname = "addSOAPEnvelopeWithWSSecUserData";
    try {
      _logger.info("Entering Coded Service " + opname);
      XML ret = new XML();
      String xml = request.getXmlString();
      String soap = SoapBuilder.addSoapEnvelopeWithWsSecData(xml, wsSecData.getUsername(),
                                                             wsSecData.getPassword(), wsSecData.getIdAttribute());
      ret.setXmlString(soap);
      return ret;
    }
    catch (Exception e) {
      _logger.error("Error in Coded Service " + opname);
      throw new RuntimeException("Error in Coded Service " + opname, e);
    }
  }


  public WSSecurityUserData readWSSecurityUserData(XML xmlIn) {
    String opname = "readWSSecurityUserData";
    try {
      _logger.info("Entering Coded Service " + opname);
      WSSecurityUserData ret = new WSSecurityUserData();
      String xml = xmlIn.getXmlString();

      XmlNavigator nav = new JdomNavigator(xml);
      nav.descend(SoapBuilder.Constant.XmlTagName.HEADER);
      nav.descend(SoapBuilder.Constant.XmlTagName.WsSecurity.SECURITY);
      nav.descend(SoapBuilder.Constant.XmlTagName.WsSecurity.USERNAME_TOKEN);

      ret.setIdAttribute(nav.getAttributeValue("Id"));
      ret.setUsername(nav.getChildText(SoapBuilder.Constant.XmlTagName.WsSecurity.USERNAME));
      ret.setPassword(nav.getChildText(SoapBuilder.Constant.XmlTagName.WsSecurity.PASSWORD));
      return ret;
    }
    catch (Exception e) {
      _logger.error("Error in Coded Service " + opname);
      throw new RuntimeException("Error in Coded Service " + opname, e);
    }
  }

}
