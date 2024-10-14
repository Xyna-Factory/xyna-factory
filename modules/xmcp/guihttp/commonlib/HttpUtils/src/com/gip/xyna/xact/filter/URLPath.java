/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xact.trigger.HTTPTriggerConnection;

public class URLPath {

  private String path;
  private List<URLPathQuery> queryList;
  private String fragment;
  private ArrayList<Integer> indexPath;
  
  public URLPath(String path, List<URLPathQuery> queryList, String fragment) {
    this.path = path;
    this.queryList = queryList;
    this.fragment = fragment;
  }
  
  @Override
  public String toString() {
    return "URLPath("+path+","+queryList+","+fragment+")";
  }
  
  public URLPathQuery getQuery(String attribute) {
    if( queryList == null ) {
      return null;
    }
    for( URLPathQuery q : queryList ) {
      if( q.getAttribute().equals(attribute) ) {
        return q;
      }
    }
    return null; //nichts gefunden
  }
  
  public List<URLPathQuery> getQueryList() {
    return queryList;
  }
  
  public static URLPath parseURLPath( HTTPTriggerConnection tc ) {
    List<URLPathQuery> queryList = new ArrayList<URLPathQuery>();
    for (String key : tc.getParameters().keySet()) {
      for(String value : tc.getParameters().get(key)) {
        queryList.add(  new URLPathQuery(key, value) );
      }
    }
    return new URLPath(tc.getUri(), queryList, null);
  }
  
  
  public static URLPath parseURLPath(String urlString) {
    try {
      URL url = new URL("http://www.gip.com"+urlString);
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
      throw new IllegalArgumentException("Malformed url <"+urlString+">", e);
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
  

  public String getPath() {
    return path;
  }

  public String getSubPath(int startIndex) {
    return path.substring(indexPath(startIndex));
  }

  /*
   * separiert an '/'
   */
  public String getSubPath(int startIndex, int endIndex) {
    return path.substring(indexPath(startIndex), indexPath(endIndex));
  }
  
  public String decodeSubPath(int startIndex, int endIndex) {
    String sub = getSubPath(startIndex,endIndex);
    return decode(sub);
  }
  
  /*
   * gibt den character-index des index-ten '/' im string zurück.
   * 
   * beispiel:
   * path = /aaa/bbb/ccc
   *   indexPath(0) = 0
   *   indexPath(1) = 4
   *   indexPath(2) = 8
   */
  private int indexPath(int index) {
    if( indexPath == null ) {
      indexPath = new ArrayList<Integer>();
      int idx = 0;
      while( idx >= 0 ) {
        indexPath.add(idx);
        idx = path.indexOf('/',idx+1);
      }
      indexPath.add(path.length());
    }
    return indexPath.get(index);
  }
  
  public int getPathLength() {
    indexPath(0);
    return indexPath.size()-1;
  }
  

  public URLPath subURL(int index) {
    //TODO indexPath umrechnen?
    return new URLPath( getSubPath(index), queryList, fragment);
  }
  
  public String getPathElement(int index) {
    String sub = getSubPath(index,index+1).substring(1);
    return decode(sub);
  }
  
  public static class URLPathQuery {

    private String attribute;
    private String value;

    public URLPathQuery(String attribute, String value) {
      this.attribute = attribute;
      this.value = value;
    }
    
    @Override
    public String toString() {
      return attribute+"="+value;
    }

    public String getAttribute() {
      return attribute;
    }

    public String getValue() {
      return value;
    }
  }

 

}
