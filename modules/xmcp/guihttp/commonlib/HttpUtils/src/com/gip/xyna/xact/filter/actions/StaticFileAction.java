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
package com.gip.xyna.xact.filter.actions;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.FilterAction;
import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;

/**
 * Liefert statische Files aus
 * (Beipiel /favicon.ico, CSS)
 */
public class StaticFileAction implements FilterAction {

  private XynaPropertyString staticFileLocation;
  private Map<String,FileData> staticFiles;
  
  public StaticFileAction(XynaPropertyString staticFileLocation) {
    staticFiles = new HashMap<String,FileData>();
    this.staticFileLocation = staticFileLocation;
    
  }

  public boolean match(URLPath url, Method method) {
    return Method.GET == method && staticFiles.containsKey(url.getPath());
  }

  public FilterActionInstance act(URLPath url, HTTPTriggerConnection tc) throws XynaException {
    DefaultFilterActionInstance dfai = new DefaultFilterActionInstance();
    FileData fd = staticFiles.get(tc.getUri());
    if( fd == null ) {
      dfai.sendError(tc, "No data found" );
    }
    String etag = fd.getEtag();
    String inm = (String) tc.getHeader().get("if-none-match");
    if( etag.equals(inm) ) {
      dfai.setProperty("Etag", etag );
      dfai.setProperty("Cache-Control", "max-age=36000");
      dfai.sendNotModified(tc, fd.getMime() );
    } else {
      try {
        dfai.setProperty("Etag", etag );
        dfai.setProperty("Cache-Control", "max-age=36000");
        dfai.sendStream(tc, fd.getMime(), fd.getStream(staticFileLocation) );
      } catch(Exception e) {
        dfai.sendError(tc, "failed: "+e.getMessage() );
      }
    }
    return dfai;
  }

  public String getTitle() {
    return null; //nicht auf index.html aufnehmen
  }

  public void appendIndexPage(HTMLPart body) {
    //nichts
  }

  public boolean hasIndexPageChanged() {
    return false;
  }
  
  

  public void addFavIcon(String fileName) {
    String uri = "/favicon.ico";
    staticFiles.put( uri, new FileData(uri, "image/x-icon", fileName) );
  }

  public void addCSS(String uri, String fileName) {
    staticFiles.put( uri, new FileData(uri, "text/css", fileName) );
  }

  private static class FileData {

    private String mime;
    private String uri;
    private String fileName;

    public FileData(String uri, String mime, String fileName) {
      this.uri = uri;
      this.mime = mime;
      this.fileName = fileName;
    }

    public String getEtag() {
      return uri; //TODO besser
    }

    public InputStream getStream(XynaPropertyString staticFileLocation) throws FileNotFoundException {
      return new FileInputStream(staticFileLocation.get()+"/"+fileName);
    }

    public String getMime() {
      return mime;
    }
    
  }
  
}
