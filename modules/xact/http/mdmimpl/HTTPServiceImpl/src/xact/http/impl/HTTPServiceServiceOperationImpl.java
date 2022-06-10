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
package xact.http.impl;



import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.HttpHost;
import org.apache.log4j.Logger;

import xact.http.ConnectParameter;
import xact.http.HTTPConnection;
import xact.http.HTTPServiceServiceOperation;
import xact.http.HTTPURLString;
import xact.http.Header;
import xact.http.HeaderField;
import xact.http.SendParameter;
import xact.http.URLPath;
import xact.http.enums.SchemeHTTP;
import xact.http.enums.SchemeHTTPS;
import xact.http.enums.statuscode.HTTPStatusCode;
import xact.http.exceptions.ConnectException;
import xact.http.exceptions.ConnectionAlreadyClosedException;
import xact.http.exceptions.HttpException;
import xact.http.exceptions.TimeoutException;
import xact.http.exceptions.UnexpectedHTTPResponseException;
import xact.templates.Document;
import xfmg.xfctrl.filemgmt.ManagedFileId;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaExceptionBase;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyDuration;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;

import base.Text;


public class HTTPServiceServiceOperationImpl implements ExtendedDeploymentTask, HTTPServiceServiceOperation {

  private static ConcurrentHashMap<String,HttpConnectionImpl> httpConnectionImpls;
  private static Logger logger = CentralFactoryLogging.getLogger(HTTPServiceServiceOperationImpl.class);
  private final static String NAME = "xact.http.HTTPService";
  
  public static XynaPropertyDuration DEFAULT_SEND_TIMEOUT = new XynaPropertyDuration("xact.http.default_send_timeout", "10 s")
  .setDefaultDocumentation(DocumentationLanguage.EN, "default send timeout")
  .setDefaultDocumentation(DocumentationLanguage.DE, "Default Send-Timeout");
  
  public static XynaPropertyString DEFAULT_USER_AGENT = new XynaPropertyString("xact.http.default_user_agent", "XynaFactory HTTP-Service") //FIXME
    .setDefaultDocumentation(DocumentationLanguage.EN, "default user agent")
    .setDefaultDocumentation(DocumentationLanguage.DE, "Default UserAgent");
  public static XynaPropertyDuration DEFAULT_CONNECT_TIMEOUT = new XynaPropertyDuration("xact.http.default_connect_timeout", "10 s")
  .setDefaultDocumentation(DocumentationLanguage.EN, "default connect timeout")
  .setDefaultDocumentation(DocumentationLanguage.DE, "Default Connect-Timeout");

  
  public void onDeployment() throws XynaException {
    // TODO do something on deployment, if required
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
    
    httpConnectionImpls = new ConcurrentHashMap<String,HttpConnectionImpl>();

    DEFAULT_USER_AGENT.registerDependency(UserType.Service, NAME);
    DEFAULT_CONNECT_TIMEOUT.registerDependency(UserType.Service, NAME);
  }

  public void onUndeployment() throws XynaException {
    // TODO do something on undeployment, if required
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
    
    if ( httpConnectionImpls != null ) {
      for( HttpConnectionImpl hc : httpConnectionImpls.values() ) {
        hc.close();
      }
    }
    DEFAULT_USER_AGENT.unregister();
    DEFAULT_CONNECT_TIMEOUT.unregister();
  }

  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.
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

  private String storeHttpConnectionImpl(String id, HttpConnectionImpl hc) {
    if( id == null ) {
      do {
        id = String.valueOf(System.identityHashCode(hc) )+"_"+System.currentTimeMillis();
      } while( httpConnectionImpls.putIfAbsent(id, hc) != null );
    } else {
      httpConnectionImpls.putIfAbsent(id, hc);
    }
    return id;
  }

  private HttpConnectionImpl retrieveHttpConnectionImpl(HTTPConnection httpConnection) throws ConnectionAlreadyClosedException {
    HttpConnectionImpl hc = httpConnectionImpls.get(httpConnection.getId() );
    if( hc == null ) {
      throw new ConnectionAlreadyClosedException();
    }
    return hc;
  }
  
  private HttpConnectionImpl removeHttpConnectionImpl(HTTPConnection httpConnection) throws ConnectionAlreadyClosedException {
    HttpConnectionImpl hc = httpConnectionImpls.remove(httpConnection.getId() );
    if( hc == null ) {
      throw new ConnectionAlreadyClosedException();
    }
    return hc;
  }
  
  private Container statusHeaderOrException(Sender sender) throws UnexpectedHTTPResponseException {
    HTTPStatusCode status = sender.getHTTPStatusCode();
    if( sender.isStatusExpected() ) {
      return new Container(status, sender.getResponseHeader());
    } else {
      String codeReason = status.getCode()+" "+status.getReason();
      throw new UnexpectedHTTPResponseException(codeReason, status, sender.getResponseHeader());
    }
  }

  
  public HTTPConnection getHTTPConnection(ConnectParameter connectParameter) throws ConnectException, TimeoutException {
    HttpConnectionImpl hc = new HttpConnectionImpl(connectParameter);
    hc.connect(false);
    String id = storeHttpConnectionImpl(null, hc);
    return buildHTTPConnection(id, hc);
  }
  
  private HTTPConnection buildHTTPConnection(String id, HttpConnectionImpl hc) {
    HttpHost host = hc.getHost();
    return new HTTPConnection.Builder().
        id(id).
        host(host.getHostName()).
        scheme( host.getSchemeName().equals("https") ? new SchemeHTTPS() : new SchemeHTTP()).
        port(host.getPort()).
        instance();
  }

  public void reconnect(HTTPConnection httpConnection, ConnectParameter connectParameter) throws ConnectException, TimeoutException {
    HttpConnectionImpl hc = null;
    try {
      hc = retrieveHttpConnectionImpl(httpConnection);
      hc.close();
    } catch( ConnectionAlreadyClosedException e) {
      //ok, Connection wurde vor dem reconnect ordentlich geschlossen 
    } catch (HttpException e) {
      logger.warn("Failed to close old connection before reconnect", e );
      //soll aber reconnect nicht weiter stören
    }
    hc = new HttpConnectionImpl(connectParameter);
    hc.connect(false);
    storeHttpConnectionImpl(httpConnection.getId(), hc);
  }

  
  public Document receiveDocument(HTTPConnection httpConnection) throws HttpException, ConnectionAlreadyClosedException {
    HttpConnectionImpl hc = retrieveHttpConnectionImpl(httpConnection);
    Receiver receiver = new Receiver();
    return receiver.getDocument(hc);
  }
  
  public ManagedFileId retrieveFile(HTTPConnection httpConnection, Text fileName, Text scope) throws XynaExceptionBase, HttpException, ConnectionAlreadyClosedException{
    HttpConnectionImpl hc = retrieveHttpConnectionImpl(httpConnection);
    Receiver receiver = new Receiver();
    InputStream is = receiver.getStream(hc);
    ManagedFileId result = new ManagedFileId();
    result.setId(XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getFileManagement().store(scope.getText(), fileName.getText(), is));
    return result;
  }
  
  public Container send(HTTPConnection httpConnection, SendParameter sendParameter) throws UnexpectedHTTPResponseException, HttpException, TimeoutException, ConnectionAlreadyClosedException {
    HttpConnectionImpl hc = retrieveHttpConnectionImpl(httpConnection);
    Sender sender = new Sender(sendParameter);
    sender.send(hc);
    return statusHeaderOrException(sender);
  }

  public Container sendDocument(HTTPConnection httpConnection, SendParameter sendParameter, Document document) throws UnexpectedHTTPResponseException, HttpException, TimeoutException, ConnectionAlreadyClosedException {
    HttpConnectionImpl hc = retrieveHttpConnectionImpl(httpConnection);
    Sender sender = new Sender(sendParameter);
    sender.send(hc, document);
    return statusHeaderOrException(sender);
  }

  public void closeConnection(HTTPConnection httpConnection) {
    HttpConnectionImpl hc;
    try {
      hc = removeHttpConnectionImpl(httpConnection);
      hc.close();
    } catch (ConnectionAlreadyClosedException e) {
      //nichts loggen, keine spannende information
    } catch (HttpException e) {
      logger.debug("Error closing connection", e);
    }
  }

  public URLPath parseURLPath(HTTPURLString httpUrlString) {
    return URLUtils.parseURLPath(httpUrlString.getUrl());
  }

  public HeaderField extractHeaderField(Header header, HeaderField field) {
    if( header.getHeaderField() == null ) {
      return null;
    }
    if( field == null || field.getName() == null ) {
      return null;
    }
    for( HeaderField hf : header.getHeaderField() ) {
      if( field.getName().equalsIgnoreCase(hf.getName()) ) {
        return hf;
      }
    }
    return null;
  }

}
