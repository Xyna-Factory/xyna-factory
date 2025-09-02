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
package xact.http.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.apache.http.HttpClientConnection;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Lookup;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContexts;
import org.apache.log4j.Logger;

import xact.http.Authentication;
import xact.http.ConnectParameter;
import xact.http.ConnectParameterHostPort;
import xact.http.ConnectParameterURL;
import xact.http.ManagedKeyStoreAuthentication;
import xact.http.KeyStoreWithoutTrustChecksAuthentication;
import xact.http.NoAuthentication;
import xact.http.UserPasswordAuthentication;
import xact.http.enums.Scheme;
import xact.http.enums.SchemeHTTP;
import xact.http.enums.SchemeHTTPS;
import xact.http.exceptions.ConnectException;
import xact.http.exceptions.HttpException;
import xact.http.exceptions.TimeoutException;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.keymgmt.KeyManagement;
import com.gip.xyna.xprc.xsched.timeconstraint.AbsRelTime;

import java.net.ProxySelector;
import org.apache.http.impl.conn.SystemDefaultRoutePlanner;
import org.apache.http.message.BasicHttpRequest;


public class HttpConnectionImpl {


  private static Logger logger = CentralFactoryLogging.getLogger(HttpConnectionImpl.class);

  private HttpHost host;
  private Authentication authentication;
  private AbsRelTime timeout;
  private int retries;

  private HttpClientContext context;
  private CloseableHttpClient httpClient;
  private String userAgent;
  private CloseableHttpResponse lastResponse;

  public HttpConnectionImpl(ConnectParameter connectParameter) {
    extract( connectParameter );
  }

  private void extract(ConnectParameter connectParameter) {
    this.authentication = connectParameter.getAuthentication();
    if( this.authentication instanceof NoAuthentication ) {
      this.authentication = null;
    }
    if( connectParameter.getTimeout() != null ) {
      timeout = connectParameter.getTimeout().toAbsRelTime();
    } else {
      timeout = new AbsRelTime(HTTPServiceServiceOperationImpl.DEFAULT_CONNECT_TIMEOUT.getMillis(), true);
    }
    this.userAgent = connectParameter.getUserAgent();
    if( userAgent == null ) {
      userAgent = HTTPServiceServiceOperationImpl.DEFAULT_USER_AGENT.get();
    }
    host = host(connectParameter);
    retries = connectParameter.getRetries();
  }

  private HttpHost host(ConnectParameter connectParameter) {
    if( connectParameter instanceof ConnectParameterHostPort ) {
      ConnectParameterHostPort cphp = (ConnectParameterHostPort)connectParameter;
      return new HttpHost(cphp.getHost(), cphp.getPort(), scheme(cphp.getScheme()) );
    } else if ( connectParameter instanceof ConnectParameterURL ) {
      ConnectParameterURL cpu = (ConnectParameterURL)connectParameter;
      return URLUtils.parseToHttpHost( cpu.getHTTPURLString().getUrl() );
    } else {
      throw new IllegalArgumentException("Unexpected ConnectParameter of type "+ connectParameter.getClass().getSimpleName() );
    }
  }

  private String scheme(Scheme scheme) {
    if( scheme == null ) {
      return "http";
    } else if( scheme instanceof SchemeHTTP ) {
      return "http";
    } else if( scheme instanceof SchemeHTTPS ) {
      return "https";
    } else {
      throw new IllegalArgumentException("Unexpected Scheme of type "+ scheme.getClass().getSimpleName() );
    }
  }

  public HttpHost getHost() {
    return host;
  }


  public void connect(boolean https) throws ConnectException, TimeoutException {

    context = HttpClientContext.create();
    context.setTargetHost(host);

    httpClient = HttpClients.custom()
        .setConnectionManager(createConnectionManager(https, createConnectionFactoryLookup()))
        .setUserAgent(userAgent)
        .setRoutePlanner( createHttpRoutePlanner(new HttpRoute(host,null,https)) )
        .setDefaultCredentialsProvider( createCredentialsProvider() )
        .setRetryHandler(new CountBasedRetryHandler(retries))
        .build();

  }

  private Lookup<ConnectionSocketFactory> createConnectionFactoryLookup() throws ConnectException {
    return RegistryBuilder.<ConnectionSocketFactory>create()
                          .register("http", new PlainConnectionSocketFactory())
                          .register("https", createSslSocketFactory())
                          .build();
  }

  private LayeredConnectionSocketFactory createSslSocketFactory() throws ConnectException {
    try {
      SSLContext sslcontext;
      if ((authentication instanceof ManagedKeyStoreAuthentication) ||
          (authentication instanceof KeyStoreWithoutTrustChecksAuthentication)) {
        KeyManagement km = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getKeyManagement();
        String keystoreName = null;
        KeyManagerFactory kmf = null;
        sslcontext = SSLContext.getInstance("TLS");
        TrustManager[] trustManagers = null;
        if (authentication instanceof ManagedKeyStoreAuthentication) {
          ManagedKeyStoreAuthentication mksa = (ManagedKeyStoreAuthentication)authentication;
          keystoreName = mksa.getIdentityKeyStoreName();
          TrustManagerFactory tmf = null;
          if (mksa.getTrustManagerKeyStoreName() != null &&
              mksa.getTrustManagerKeyStoreName().length() > 0) {
            Map<String, String> params = new HashMap<String, String>();
            tmf = km.getKeyStore(mksa.getTrustManagerKeyStoreName(), TrustManagerFactory.class, params);
          }
          trustManagers = tmf == null ? null : tmf.getTrustManagers();
          
        } else if (authentication instanceof KeyStoreWithoutTrustChecksAuthentication) {
          trustManagers = new TrustManager[] { new TrustManagerTrustAll() };
          keystoreName = ((KeyStoreWithoutTrustChecksAuthentication)authentication).getIdentityKeyStoreName();
        }
        if ((keystoreName != null) && (keystoreName.length() > 0)) {
          Map<String, String> params = new HashMap<String, String>();
          kmf = km.getKeyStore(keystoreName, KeyManagerFactory.class, params);
        }
        sslcontext.init(kmf == null ? null : kmf.getKeyManagers(),
                        trustManagers,
                        null);
      } else {
        sslcontext = SSLContexts.createSystemDefault();
      }
      SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                                                                        sslcontext,
                                                                        new String[] { "TLSv1.3","TLSv1.2","TLSv1.1","TLSv1" },
                                                                        null,
                                                                        SSLConnectionSocketFactory.getDefaultHostnameVerifier());

      return sslsf;
    } catch (Exception e) {
      throw new ConnectException(e);
    }
  }

  private CredentialsProvider createCredentialsProvider() {
    if( authentication == null ) {
      return null;
    } else if( authentication instanceof UserPasswordAuthentication ) {
      UserPasswordAuthentication upa = (UserPasswordAuthentication)authentication;
      CredentialsProvider credsProvider = new BasicCredentialsProvider();
      credsProvider.setCredentials(
              new AuthScope(host),
              new UsernamePasswordCredentials(upa.getUsername(), upa.getPassword()));
      return credsProvider;
    } else if (authentication instanceof ManagedKeyStoreAuthentication) {
      return null;
    } else if (authentication instanceof KeyStoreWithoutTrustChecksAuthentication) {
      return null;
    } else {
      throw new IllegalArgumentException("Unexpected Authentication of type "+ authentication.getClass().getSimpleName() );
    }

  }



  private HttpRoutePlanner createHttpRoutePlanner(final HttpRoute route) {
    return new HttpRoutePlanner() {
      public HttpRoute determineRoute(HttpHost arg0, HttpRequest arg1, HttpContext arg2) throws org.apache.http.HttpException {
        return new SystemDefaultRoutePlanner(ProxySelector.getDefault()).determineRoute(route.getTargetHost(), arg1, arg2);
      }
    };
  }


  private HttpClientConnectionManager createConnectionManager(boolean https, Lookup<ConnectionSocketFactory> lookup) throws TimeoutException, ConnectException {
    BasicHttpClientConnectionManager conManager = new BasicHttpClientConnectionManager(lookup);

    //FIXME setSocketConfig
    /*
    conManager.setSocketConfig(route.getTargetHost(),SocketConfig.custom().
                                setSoTimeout(5000).build());
    */

    try {
      HttpRoute route = new SystemDefaultRoutePlanner(ProxySelector.getDefault()).determineRoute(host, new BasicHttpRequest("", ""), context);

      // Request new connection. This can be a long process
      ConnectionRequest connRequest = conManager.requestConnection(route, null);
      HttpClientConnection connection = connRequest.get(timeout.getTime(), TimeUnit.MILLISECONDS);
      int usedRetries = 0;
      IOException exception = null;
      boolean success = false;
      if (!connection.isOpen()) {
        do {
          try {
            // establish connection based on its route info
            conManager.connect(connection, route, (int) timeout.getTime(), context);
            // and mark it as route complete
            conManager.routeComplete(connection, route, context);
            success = true;
          } catch (IOException e) {
            usedRetries++;
            exception = e;
          }
        } while (!success && usedRetries < retries);
        
        if(!success) {
          throw new ConnectException(exception);
        }
      }

      conManager.releaseConnection(connection, null, 0, TimeUnit.SECONDS);

    } catch (ConnectionPoolTimeoutException e ) {
      throw new TimeoutException(e);
    } catch (InterruptedException e) {
      throw new ConnectException(e);
    } catch (ExecutionException e) {
      throw new ConnectException(e);
    } catch (org.apache.http.HttpException e) {
      throw new ConnectException(e);
    }


    return conManager;
  }

  public void close() throws HttpException {
    try {
      httpClient.close();
    } catch (IOException e) {
      throw new HttpException(e);
    }
  }


  public HttpResponse send(HttpRequestBase request) throws HttpException {
    if( lastResponse != null ) {
      try {
        lastResponse.close();
      } catch (IOException e) {
        logger.warn("Could not close response", e);
      }
    }
    try {
      lastResponse = httpClient.execute(request);
    } catch (ClientProtocolException e) {
      throw new HttpException(e);
    } catch (IOException e) {
      throw new HttpException(e);
    }
    return lastResponse;
  }

  public HttpEntity receive() {
    if( lastResponse != null ) {
      return lastResponse.getEntity();
    }
    return null;
  }

  
  private static class CountBasedRetryHandler implements HttpRequestRetryHandler {

    private final int retries;
    
    public CountBasedRetryHandler(int retries) {
      this.retries = retries;
    }

    @Override
    public boolean retryRequest(IOException arg0, int executionCount, HttpContext arg2) {
      return executionCount < retries;
    }

  }
}
