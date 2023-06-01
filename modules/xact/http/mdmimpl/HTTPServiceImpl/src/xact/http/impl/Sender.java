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

import java.util.ArrayList;
import java.util.List;

import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.StringEntity;

import xact.http.Header;
import xact.http.HeaderField;
import xact.http.MediaType;
import xact.http.SendParameter;
import xact.http.enums.httpmethods.HTTPMethod;
import xact.http.enums.statuscode.HTTPStatusCode;
import xact.http.exceptions.HttpException;
import xact.templates.Document;

import com.gip.xyna.xact.StatusCode;
import com.gip.xyna.xprc.xsched.timeconstraint.AbsRelTime;


public class Sender {

  private HTTPStatusCode status;
  private Header requestHeader;
  private Header responseHeader;
  private StatusCodeSet expectedStatus;
  private String urlPath;
  private Method method;
  private AbsRelTime timeout;
  
  public Sender(SendParameter sendParameter) {
    requestHeader = sendParameter.getHeader();
    urlPath = URLUtils.urlPathToString( sendParameter.getURLPath() );
    method = Method.methodFor( sendParameter.getHTTPMethod());
    expectedStatus = StatusCodeSet.parseExpectedList( sendParameter.getExpectedHTTPStatusCode() );
    
    if( sendParameter.getTimeout() != null ) {
      timeout = sendParameter.getTimeout().toAbsRelTime();
    } else {
      timeout = new AbsRelTime(HTTPServiceServiceOperationImpl.DEFAULT_SEND_TIMEOUT.getMillis(), true);
    }
  }
  
  public HTTPStatusCode getHTTPStatusCode() {
    return status;
  }

  public Header getResponseHeader() {
    return responseHeader;
  }
  
  private enum Method {
    DELETE(false) {
      public HttpRequestBase createRequest(String uri) {        
        return new HttpDelete(uri);
      }
    },
    GET(false) {
      public HttpRequestBase createRequest(String uri) {
        return new HttpGet(uri);
      }
    },
    HEAD(false) {
      public HttpRequestBase createRequest(String uri) {
        return new HttpHead(uri);
      }
    },
    OPTIONS(false) {
      public HttpRequestBase createRequest(String uri) {
        return new HttpOptions(uri);
      }
    },
    PATCH(true) {
      public HttpRequestBase createRequest(String uri) {
        return new HttpPatch(uri);
      }
    },
    POST(true) {
      public HttpRequestBase createRequest(String uri) {
        return new HttpPost(uri);
      }
    },
    PUT(true) {
      public HttpRequestBase createRequest(String uri) {
        return new HttpPut(uri);
      }
    },
    TRACE(false) {
      public HttpRequestBase createRequest(String uri) {
        return new HttpTrace(uri);
      }
    }
    ;

    private boolean hasPayload;

    private Method(boolean hasPayload) {
      this.hasPayload = hasPayload;
    }
    
    public static Method methodFor(HTTPMethod httpMethod) {
      String className = httpMethod.getClass().getSimpleName();
      return valueOf(className);
    }
    
    public abstract HttpRequestBase createRequest(String uri);

    public void setEntity(HttpRequestBase request, AbstractHttpEntity entity) {
      if( hasPayload ) {
        ((HttpEntityEnclosingRequestBase)request).setEntity(entity);
      }
    }
    
  }

  private HttpRequestBase createRequest(HttpConnectionImpl hc, AbstractHttpEntity entity) {
    HttpRequestBase request = method.createRequest(urlPath);
    RequestConfig.Builder configBuilder;
    if (request.getConfig() != null) {
      configBuilder = RequestConfig.copy(request.getConfig());
    } else {
      configBuilder = RequestConfig.custom();
    }
    request.setConfig(configBuilder.setSocketTimeout((int) timeout.getTime()).build());
    
    if( entity != null ) {
      method.setEntity(request, entity);
    }
    
    if( requestHeader != null ) {
      if( requestHeader.getHeaderField() != null ) {
        for( HeaderField hf : requestHeader.getHeaderField() ) {
          request.setHeader(hf.getName(), hf.getValue() ); //TODO oder addHeader ? 
        }
      }
      if( requestHeader.getContentType() != null ) {
        request.setHeader( "Content-Type", requestHeader.getContentType().getMediaType() );
      }
    }
    return request;
  }

  private Header header(org.apache.http.Header[] allHeaders) {
    Header.Builder header = new Header.Builder();
    List<HeaderField> headerFields = new ArrayList<HeaderField>();
    for( org.apache.http.Header h : allHeaders ) {
      String name = h.getName();
      String value = h.getValue();
      headerFields.add( new HeaderField(name, value) );
      
      if( name.equalsIgnoreCase("Content-Type") ) {
        header.contentType(new MediaType(value));
      }
    }
    header.headerField(headerFields);
    return header.instance();
  }

  public void send(HttpConnectionImpl hc) throws HttpException {  
    HttpRequestBase request = createRequest(hc,null);
  
    HttpResponse response = hc.send( request );
    responseHeader = header(response.getAllHeaders());
    StatusLine statusLine = response.getStatusLine();
    status = StatusCode.newInstanceFor(statusLine.getStatusCode(),statusLine.getReasonPhrase());
  }

  public void send(HttpConnectionImpl hc, Document document) throws HttpException {
    HttpRequestBase request = createRequest(hc, stringEntity(document));
    HttpResponse response = hc.send( request );
    responseHeader = header(response.getAllHeaders());
    StatusLine statusLine = response.getStatusLine();
    status = StatusCode.newInstanceFor(statusLine.getStatusCode(),statusLine.getReasonPhrase());
  }

  private StringEntity stringEntity(Document document) {
    String mimeType = "text/plain";
    if( requestHeader.getContentType() != null ) {
      String mediaType = requestHeader.getContentType().getMediaType();
      int idx = mediaType.indexOf(';');
      if( idx < 0 ) {
        mimeType = mediaType;
      } else {
        mimeType = mediaType.substring(0, idx);
      }
    }
    org.apache.http.entity.ContentType contentType = 
        org.apache.http.entity.ContentType.create(mimeType, Consts.UTF_8); //TODO charset aus mediaType entnehmen
    StringEntity entity = new StringEntity( document.getText(), contentType );
    return entity;
  }

  public boolean isStatusExpected() {
    return expectedStatus.check(status);
  }

}
