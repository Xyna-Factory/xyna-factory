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
package com.gip.xyna.xact.filter;



import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.WeakHashMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.StatusCode;
import com.gip.xyna.xact.trigger.HTTPStartParameter;
import com.gip.xyna.xact.trigger.HTTPTrigger;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement.TransientFile;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyLong;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;

import base.Host;
import base.Port;
import xact.http.Header;
import xact.http.HeaderField;
import xact.http.ManagedFileReference;
import xact.http.MediaType;
import xact.http.URLPath;
import xact.http.URLPathQuery;
import xact.http.enums.httpmethods.DELETE;
import xact.http.enums.httpmethods.GET;
import xact.http.enums.httpmethods.HEAD;
import xact.http.enums.httpmethods.HTTPMethod;
import xact.http.enums.httpmethods.OPTIONS;
import xact.http.enums.httpmethods.PATCH;
import xact.http.enums.httpmethods.POST;
import xact.http.enums.httpmethods.PUT;
import xact.http.enums.httpmethods.TRACE;
import xact.http.enums.statuscode.HTTPStatusCode;
import xact.http.enums.statuscode.InternalServerError;
import xact.templates.Document;
import xact.templates.PlainText;



public class HTTPForwardingFilter extends ConnectionFilter<HTTPTriggerConnection> {

  private static final long serialVersionUID = 1L;

  private static Logger logger = CentralFactoryLogging.getLogger(HTTPForwardingFilter.class);

  private volatile static Map<HTTPTrigger, HTTPStartParameter> startParameterMap = new WeakHashMap<HTTPTrigger, HTTPStartParameter>();

  private static final XynaPropertyLong maxContentLength = new XynaPropertyLong("xact.filter.http.httpforwarding.contentlength.max",
                                                                                Long.MAX_VALUE / 2)
      .setDefaultDocumentation(DocumentationLanguage.EN,
                               "Maximum supported content length of payloads processed by HTTP Forwarding Filter.");
  private static final XynaPropertyLong maxContentLengthIfNotSet =
      new XynaPropertyLong("xact.filter.http.httpforwarding.contentlength.maxifnotset", Long.MAX_VALUE / 2)
          .setDefaultDocumentation(DocumentationLanguage.EN,
                                   "Maximum supported content length of payloads processed by HTTP Forwarding Filter if content-length header field is not set.");


  /**
   * Analyzes TriggerConnection and creates XynaOrder if it accepts the connection.
   * This method returns a FilterResponse object, which includes the XynaOrder if the filter is responsible for the request.
   * # If this filter is not responsible the returned object must be: FilterResponse.notResponsible()
   * # If this filter is responsible the returned object must be: FilterResponse.responsible(XynaOrder order)
   * # If this filter is responsible but the request is handled without creating a XynaOrder the 
   *   returned object must be: FilterResponse.responsibleWithoutXynaorder()
   * # If this filter is responsible but the request should be handled by an older version of the filter in another application version, the returned
   *    object must be: FilterResponse.responsibleButTooNew().
   * @param tc
   * @return FilterResponse object
   * @throws XynaException caused by errors reading data from triggerconnection or having an internal error.
   *         Results in onError() being called by Xyna Processing.
   */
  public FilterResponse createXynaOrder(HTTPTriggerConnection tc) throws XynaException {
    try {   
      tc.readHeader();
      String contentByteLength = tc.getHeader().getProperty("content-length");
      if (!validateContentLength(contentByteLength)) {
        tc.sendError(HTTPTriggerConnection.HTTP_INTERNALERROR, "Internal Error");
        return FilterResponse.responsibleWithoutXynaorder();
      }
      tc.readPayload(maxContentLengthIfNotSet.get());
    } catch (InterruptedException e) {
      //fehler, der bereits beantwortet wurde
      return FilterResponse.responsibleWithoutXynaorder();
    }

    Header header = getHeader(tc);
    Document document = getDocument(tc, header);
    XynaOrder order =
        new XynaOrder(new DestinationKey("xact.http.ProcessHTTPRequest.port=" + getPortAsInt(tc)), getURLPath(tc), getHost(tc),
                      getPort(tc), getMethod(tc), header, document);
    return FilterResponse.responsible(order);
  }


  /**
   * Called when above XynaOrder returns successfully.
   * @param response by XynaOrder returned GeneralXynaObject
   * @param tc corresponding triggerconnection
   */
  public void onResponse(GeneralXynaObject response, HTTPTriggerConnection tc) {
    if (response instanceof Container) {
      Container resp = (Container) response;
      Document doc = (Document) resp.get(0);
      Header header = (Header) resp.get(1);

      String data = doc == null || doc.getText() == null ? "" : doc.getText();

      try {
        String status = evaluateStatusCode((HTTPStatusCode) resp.get(2));
        Pair<Properties, String> responseHeaderAndMIMEType = createResponseHeader(header);
        String mime = responseHeaderAndMIMEType.getSecond();
        Pair<InputStream, Long> outputAndLength = createOutputAndLength(doc, data, mime);

        try {
          tc.sendResponse(status, mime, responseHeaderAndMIMEType.getFirst(), outputAndLength.getFirst(), outputAndLength.getSecond());
          return; //ok
        } catch (Exception e) {
          logger.warn("Could not send http reply.", e);
        } finally {
          cleanup(doc);
        }
      } catch (InternalServerErrorException e) {
        //fehlerbehandlung unten
      }
    } else {
      logger.warn("got " + response + ". expected container.");
    }
    //fehlerbehandlung
    try {
      tc.sendError(HTTPTriggerConnection.HTTP_INTERNALERROR, "Internal Error");
    } catch (InterruptedException e) {
      //expected
    } catch (Exception e) {
      logger.warn("Could not send http reply.", e);
    }
  }
  
  /**
   * Called when above XynaOrder returns with error or if an XynaException occurs in generateXynaOrder().
   * @param e
   * @param tc corresponding triggerconnection
   */
  public void onError(XynaException[] e, HTTPTriggerConnection tc) {
    try {
      for (XynaException ex : e) {
        logger.debug("Order returned exception", ex);
      }
      tc.sendError(HTTPTriggerConnection.HTTP_INTERNALERROR, "Internal Error");
    } catch (InterruptedException ex) {
      //expected
    } catch (Exception ex) {
      logger.warn("Could not send http reply.", ex);
    }
  }


  /**
   * @return description of this filter
   */
  public String getClassDescription() {
    return "Forwards http requests to ordertype 'xact.http.ProcessHTTPRequest.port=<portnumber>'. Input=(" + URLPath.class.getName() + ", "
        + Host.class.getName() + ", " + Port.class.getName() + ", " + HTTPMethod.class.getName() + ", " + Header.class.getName() + ", "
        + Document.class.getName() + "), Output=(" + Document.class.getName() + ", " + Header.class.getName() + ", "
        + HTTPStatusCode.class.getName() + ")";
  }


  /**
   * Called once for each filter instance when it is deployed and again on each classloader change (e.g. when changing corresponding implementation jars).
   * @param triggerInstance trigger instance this filter instance is registered to
   */
  public void onDeployment(EventListener triggerInstance) {
    super.onDeployment(triggerInstance);
    String triggerInstanceName = triggerInstance.getTriggerInstanceIdentification().getInstanceName();
    long revisionOfTriggerInstance = triggerInstance.getTriggerInstanceIdentification().getRevision();
    HTTPStartParameter sp =
        (HTTPStartParameter) XynaFactory.getInstance().getActivation().getActivationTrigger()
            .getEventListenerInstanceByName(triggerInstanceName, revisionOfTriggerInstance, false).getStartParameter();
    startParameterMap.put((HTTPTrigger) triggerInstance, sp);
    maxContentLength.registerDependency(UserType.Filter, "HTTPForwardingFilter");
    maxContentLengthIfNotSet.registerDependency(UserType.Filter, "HTTPForwardingFilter");
  }


  /**
   * Called once for each filter instance when it is undeployed and again on each classloader change (e.g. when changing corresponding implementation jars).
   * @param triggerInstance trigger instance this filter instance is registered to
   */
  public void onUndeployment(EventListener triggerInstance) {
    super.onUndeployment(triggerInstance);
    startParameterMap.remove(triggerInstance);
    maxContentLengthIfNotSet.unregister();
    maxContentLength.unregister();
  }
  
  
  
  
  
  
  
  
  
  
  
  
  

  private boolean validateContentLength(String contentByteLength) {
    if (contentByteLength != null) {
      long cl;
      try {
        cl = Long.parseLong(contentByteLength);
        if (cl > maxContentLength.get()) {
          cl = -1;
        }
      } catch (NumberFormatException ex) {
        cl = -1;
      }
      if (cl < 0) {
        return false;
      }
    }
    return true; //ok
  }

  private static class InternalServerErrorException extends Exception {

    private static final long serialVersionUID = 1L;
    
  }
  
  private String evaluateStatusCode(HTTPStatusCode code) throws InternalServerErrorException {
    StatusCode statusCode = StatusCode.parse(code);
    String status;
    if (statusCode != null) {
      status = statusCode.getReason();
    } else if (code != null && code.getCode() > 0) {
      status = String.valueOf(code.getCode());
      if (code.getReason() != null && code.getReason().length() > 0) {
        status += " " + code.getReason();
      }
    } else {
      logger.warn("got undefined status code");
      throw new InternalServerErrorException();
    }
    return status;
  }

  private void cleanup(Document doc) {
    if (doc != null && doc.getDocumentType() instanceof ManagedFileReference) {
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement().remove(doc.getText());
    }
  }


  private Pair<InputStream, Long> createOutputAndLength(Document doc, String data, String mime) throws InternalServerErrorException {
    InputStream contentStream;
    Long contentLength;
    if (doc != null && doc.getDocumentType() instanceof ManagedFileReference) {
      TransientFile file =
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement().retrieve(doc.getText());
      contentStream = file.openInputStream();
      contentLength = file.getSize();
    } else {
      String charset = Constants.DEFAULT_ENCODING;
      if (data.length() > 0) {
        if (!mime.contains("charset=")) {
          mime += "; charset=" + charset;
        } else {
          charset = mime.substring(mime.indexOf("charset=") + 8);
          if (!Charset.isSupported(charset)) {
            charset = Constants.DEFAULT_ENCODING;
            logger.warn("unsupported charset " + charset + ".");
            throw new InternalServerErrorException();
          }
        }
      }
      byte[] bytes;
      try {
        bytes = data.getBytes(charset);
      } catch (UnsupportedEncodingException e) {
        logger.warn("unsupported charset " + charset + ": " + e.getMessage());
        throw new InternalServerErrorException();
      }
      contentLength = new Long(bytes.length);
      contentStream = new ByteArrayInputStream(bytes);
    }
    return Pair.of(contentStream, contentLength);
  }

  private Pair<Properties, String> createResponseHeader(Header header) {
    Properties responseHeader = new Properties();
    String mime = null;
    if (header != null) {
      if (header.getContentType() != null) {
        mime = header.getContentType().getMediaType();
      }
      if (header.getHeaderField() != null) {
        for (HeaderField hf : header.getHeaderField()) {
          if (hf.getName().equalsIgnoreCase("content-type")) {
            mime = hf.getValue();
            continue;
          }
          if (hf.getName().equalsIgnoreCase("content-length")) {
            //wird bei send response ermittelt!
            continue;
          }
          responseHeader.put(hf.getName(), hf.getValue());
        }
      }
    }
    if (mime == null) {
      mime = "text/plain";
    }
    return Pair.of(responseHeader, mime);
  }

  private Document getDocument(HTTPTriggerConnection tc, Header header) {
    String payload = tc.getPayload();
    //TODO documenttype sinnvoll aus content-type bestimmen
    return new Document.Builder().documentType(new PlainText()).text(payload).instance();
  }


  private Header getHeader(HTTPTriggerConnection tc) {
    Properties headerProps = tc.getHeader(); //alle lowercase
    String contentTypeString = (String) headerProps.get("content-type");
    Header.Builder hb = new Header.Builder().contentType(new MediaType(contentTypeString));
    List<HeaderField> headerFields = new ArrayList<HeaderField>();
    for (Entry<Object, Object> e : headerProps.entrySet()) {
      headerFields.add(new HeaderField.Builder().name(String.valueOf(e.getKey())).value(String.valueOf(e.getValue())).instance());
    }
    hb.headerField(headerFields);
    return hb.instance();
  }


  private HTTPMethod getMethod(HTTPTriggerConnection tc) {
    if (tc.getMethod().equals("GET")) {
      return new GET();
    } else if (tc.getMethod().equals("POST")) {
      return new POST();
    } else if (tc.getMethod().equals("DELETE")) {
      return new DELETE();
    } else if (tc.getMethod().equals("HEAD")) {
      return new HEAD();
    } else if (tc.getMethod().equals("OPTIONS")) {
      return new OPTIONS();
    } else if (tc.getMethod().equals("PUT")) {
      return new PUT();
    } else if (tc.getMethod().equals("TRACE")) {
      return new TRACE();
    } else if (tc.getMethod().equals("PATCH")) {
      return new PATCH();
    } else {
      throw new RuntimeException("unexpected http method: " + tc.getMethod());
    }
  }


  private Port getPort(HTTPTriggerConnection tc) {
    return new Port.Builder().value(getPortAsInt(tc)).instance();
  }


  private int getPortAsInt(HTTPTriggerConnection tc) {
    return startParameterMap.get((HTTPTrigger) tc.getTrigger()).getPort();
  }


  private Host getHost(HTTPTriggerConnection tc) {
    return new Host.Builder().hostname(tc.getTriggerIp()).instance();
  }


  private URLPath getURLPath(HTTPTriggerConnection tc) {
    List<URLPathQuery> queries = new ArrayList<URLPathQuery>();
    for (Entry<Object, Object> e : tc.getParas().entrySet()) {
      queries.add(new URLPathQuery.Builder().attribute(String.valueOf(e.getKey())).value(String.valueOf(e.getValue())).instance());
    }
    return new URLPath.Builder().path(tc.getUri()).query(queries).instance();
  }



}
