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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpHost;

import xact.http.URLPath;
import xact.http.URLPathQuery;


public class URLUtils {

  

  public static String urlPathToString(URLPath urlPath) {
    StringBuilder sb = new StringBuilder();
    if( urlPath.getPath() != null ) {
      sb.append(urlPath.getPath());
    }
    
    if( urlPath.getQuery() != null && ! urlPath.getQuery().isEmpty() ) {
      String sep = "?";
      for( URLPathQuery q : urlPath.getQuery() ) {
        sb.append(sep).append(encode(q.getAttribute()))
        .append("=").append(encode(q.getValue()));
        sep = "&";
      }
    }
    if( urlPath.getFragment() != null ) {
      sb.append("#").append(urlPath.getFragment());
    }
    return sb.toString();
  }

  public static HttpHost parseToHttpHost(String urlString) {
  //Pattern p = Pattern.compile("(\\w+)://([\\w\\.]+)(:\\d+)?/.*");
    
    try {
      URL url = new URL(urlString);
      return new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Malformed url", e);
    }
  }

  public static URLPath parseURLPath(String urlString) {
    try {
      URL url = new URL(urlString);
      List<URLPathQuery> queryList = null;
      if( url.getQuery() != null ) {
        queryList = new ArrayList<URLPathQuery>();
        for( String kvp : url.getQuery().split("&|;") ) {
          String attribute = null;
          String value = null;
          int idx = kvp.indexOf('=');
          if( idx > 0 ) {
            attribute = kvp.substring(0,idx);
            value= kvp.substring(idx+1);
          } else {
            attribute = kvp;
          }
          queryList.add( new URLPathQuery( decode(attribute), decode(value) ));
        }
      }
      return new URLPath(url.getPath(), queryList, url.getRef()); 
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("Malformed url", e);
    }
  }

  private static String decode(String string) {
    if( string == null ) {
      return null;
    } else {
      try {
        string = URLDecoder.decode(string, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e); //UTF-8 sollte immer passen
      }
      return string;
    }
  }
  
  private static String encode(String string) {
    if( string == null ) {
      return null;
    } else {
      try {
        string = URLEncoder.encode(string, "UTF-8");
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e); //UTF-8 sollte immer passen
      }
      return string;
    }
    
  }

}
