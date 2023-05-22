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
package xact.soap.impl;



import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Iterator;
import java.util.List;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeader;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.StringUtils;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

import xact.soap.HeaderField;
import xact.soap.SOAPServicesServiceOperation;
import xact.soap.WebserviceConnectInformation;
import xact.soap.WebserviceResponseBody;
import xact.templates.Document;



public class SOAPServicesServiceOperationImpl implements ExtendedDeploymentTask, SOAPServicesServiceOperation {

  private static final Logger logger = CentralFactoryLogging.getLogger(SOAPServicesServiceOperationImpl.class);


  public void onDeployment() throws XynaException {
    // TODO do something on deployment, if required
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }


  public void onUndeployment() throws XynaException {
    // TODO do something on undeployment, if required
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
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.;
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.;
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.;
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }


  public Container sendRequest(WebserviceConnectInformation webserviceConnectInformation,
                               List<? extends HeaderField> headers, Document document) {

    SOAPMessage response = performWebserviceCall(webserviceConnectInformation, document, headers);
    SOAPBody soapBody;
    try {
      soapBody = response.getSOAPBody();
    } catch (SOAPException e) {
      logger.error("Failed to parse response: " + response);
      throw new RuntimeException("Unexpected response message, see logfile for details", e);
    }

    // FIXME exceptionhandling
    if (soapBody.hasFault()) {
      throw new RuntimeException("Request failed: " + soapBody.getFault().getTextContent());
    }

    XynaObjectList<HeaderField> headersXynaObjectList =
        new XynaObjectList<HeaderField>(HeaderField.class, new HeaderField[0]);
    Iterator<MimeHeader> mimeHeadersIterator = response.getMimeHeaders().getAllHeaders();
    while (mimeHeadersIterator.hasNext()) {
      MimeHeader nextHeader = mimeHeadersIterator.next();
      headersXynaObjectList.add(new HeaderField(nextHeader.getName(), nextHeader.getValue()));
    }

    org.w3c.dom.Document d = soapBody.getOwnerDocument();
    String soapNameSpace = getSoapNamespace(d);
    Element body = XMLUtils.getChildElementByName(d.getDocumentElement(), soapNameSpace + ":Body");
    if (body == null) {
      body = searchBody(d);
    }
    List<Element> elements = XMLUtils.getChildElements(body);
    StringBuilder result = new StringBuilder();
    result.append("<body>\n");
    for (Element e : elements) {
      result.append(XMLUtils.getXMLString(e, false));
    }
    result.append("</body>");
    return new Container(new WebserviceResponseBody(result.toString()), headersXynaObjectList);

  }

  private static String getSoapNamespace(org.w3c.dom.Document d) {
    String nodeName = d.getDocumentElement().getNodeName();
    if (nodeName != null && 
        nodeName.contains(":") && 
        nodeName.endsWith(":Envelope")) {
      String[] nameSplit = StringUtils.fastSplit(nodeName, ':', -1);
      return nameSplit[0];
    } else {
      return "soapenv";
    }
  }
  
  
  private static Element searchBody(org.w3c.dom.Document doc) {
    String nodeName = doc.getDocumentElement().getNodeName();
    if (nodeName != null && 
        nodeName.contains(":") && 
        nodeName.endsWith(":Envelope")) {
      for (Element child : XMLUtils.getChildElements(doc.getDocumentElement())) {
        String childName = child.getNodeName(); 
        if (childName != null && 
            childName.contains(":") && 
            childName.endsWith(":Body")) {
          return child;
        }
      }
    }
    return null;
  }


  /**
   * Returns the body Element
   */
  private static SOAPMessage performWebserviceCall(final WebserviceConnectInformation webserviceConnectInformation,
                                                   Document document, List<? extends HeaderField> headers) {
    StringBuilder connectString = new StringBuilder("http");
    if (webserviceConnectInformation.getWebserviceEndpoint().getUseHTTPS()) {
      connectString.append("s");
    }
    connectString.append("://");
    connectString.append(webserviceConnectInformation.getWebserviceEndpoint().getHost());
    connectString.append(":");
    connectString.append(webserviceConnectInformation.getWebserviceEndpoint().getPort());

    org.w3c.dom.Document parsedDocument = null;
    String documentText = document.getText();
    boolean isRawDocument = documentText.contains("/schemas.xmlsoap.org/soap/envelope");
    if (!isRawDocument) {
      try {
        parsedDocument = XMLUtils.parseString(document.getText(), true);
      } catch (XPRC_XmlParsingException e3) {
        throw new RuntimeException("invalid xml specified", e3);
      } catch (Ex_FileAccessException e3) {
        throw new RuntimeException("File not found", e3); // why would this ever happen?
      }
    }

    SOAPConnection con;
    URL endpoint;
    try {
      endpoint = new URL(new URL(connectString.toString()),
              webserviceConnectInformation.getWebserviceEndpoint().getPathWithLeadingSlash(),
              new URLStreamHandler() {
        
                @Override
                protected URLConnection openConnection(URL url) throws IOException {
                  URL target = new URL(url.toString());
                  URLConnection connection = target.openConnection();
                  if (webserviceConnectInformation.getConnectTimeoutMS() > 0) {
                    connection.setConnectTimeout(webserviceConnectInformation.getConnectTimeoutMS());
                  }
                  if (webserviceConnectInformation.getReadTimeoutMS() > 0) {
                    connection.setReadTimeout(webserviceConnectInformation.getReadTimeoutMS());
                  }
                  return connection;
                }
                
              });
    } catch (MalformedURLException e) {
      throw new RuntimeException("Invalid connect parameters", e);
    }

    ClassLoader oldContexClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(SOAPServicesServiceOperationImpl.class.getClassLoader());
    SOAPMessage msg;
    try {
      con = SOAPConnectionFactory.newInstance().createConnection();
      if (isRawDocument) {
        MimeHeaders headersObject = new MimeHeaders();
        msg =
            MessageFactory.newInstance()
                .createMessage(headersObject, new ByteArrayInputStream(documentText.getBytes()));
        if (headers != null && headers.size() > 0) {
          for (HeaderField e : headers) {
            msg.getMimeHeaders().addHeader(e.getKey(), e.getValue());
          }
        }
      } else {
        msg = MessageFactory.newInstance().createMessage();
        msg.getSOAPBody().addDocument(parsedDocument);
        if (headers != null && headers.size() > 0) {
          for (HeaderField e : headers) {
            msg.getMimeHeaders().addHeader(e.getKey(), e.getValue());
          }
        }
      }

      if (logger.isTraceEnabled()) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        msg.writeTo(baos);
        logger.trace("Sending SOAP message: " + baos.toString());
      }

    } catch (SOAPException e2) {
      throw new RuntimeException("Failed to create webservice message: " + e2.getMessage(), e2);
    } catch (IOException e) {
      throw new RuntimeException(e); // just because of ByteArrayInputStream interface, should never happen
    } finally {
      Thread.currentThread().setContextClassLoader(oldContexClassLoader);
    }

    SOAPMessage response;
    try {
      response = con.call(msg, endpoint);
    } catch (SOAPException e1) {
      throw new RuntimeException("Error during webservice communication: " + e1.getMessage(), e1);
    }

    if (logger.isTraceEnabled()) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try {
        response.writeTo(baos);
      } catch (SOAPException e) {
        logger.warn("Failed to obtain response string for log entry creation", e);
      } catch (IOException e) {
        logger.warn("Failed to obtain response string for log entry creation", e);
      }
      logger.trace("Received SOAP response: " + baos.toString());
    }

    return response;

  }


  public List<? extends HeaderField> addElementToHeader(List<? extends HeaderField> arg0, HeaderField arg1) {
    throw new RuntimeException("unsupported");
  }

}
