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
package com.gip.xyna.xact.filter.actions;

import java.util.List;

import com.gip.xyna.xact.filter.FilterAction;
import com.gip.xyna.xact.filter.HTMLBuilder;
import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.SocketNotAvailableException;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;

/**
 *
 */
public class IndexAction implements FilterAction {

  private byte[] INDEX_HTML = null;

  private List<FilterAction> allFilterActions;
  private String applicationVersion;
  private String name;
  private String basepath;

  public IndexAction(List<FilterAction> allFilterActions, String applicationVersion, String name) {
    this(allFilterActions,applicationVersion,name, "/");
  }
  public IndexAction(List<FilterAction> allFilterActions, String applicationVersion, String name, String basepath) {
    this.allFilterActions = allFilterActions;
    this.applicationVersion = applicationVersion;
    this.name = name;
    this.basepath = basepath;
  }

  public boolean match(URLPath url, Method method) {
    if( Method.GET == method ) {
      String uri = url.getPath();
      return uri.equals(basepath) || uri.equals(basepath+"/") || uri.equals(basepath+"/index.html");
    }
    return false;
  }

  public String getTitle() {
    return null;
  }
  
  public void appendIndexPage(HTMLPart body) {
    //nichts zu tun
  }
  
  public boolean hasIndexPageChanged() {
    return false;
  }
  
  public FilterActionInstance act(URLPath url, HTTPTriggerConnection tc) throws SocketNotAvailableException {
    DefaultFilterActionInstance dfai = new DefaultFilterActionInstance();
    dfai.sendHtml(tc, getOrCreateIndexHtml());
    return dfai;
  }

  public byte[] getOrCreateIndexHtml() {
    boolean changed = false;
    if( INDEX_HTML != null ) {
      for( FilterAction fa : allFilterActions ) {
        if( fa.hasIndexPageChanged() ) {
          changed = true;
          break;
        }
      }
      if( !changed ) {
        return INDEX_HTML;
      }
    }
    HTMLBuilder html = new HTMLBuilder(name);
    html.head().css("gipstyle.css");
    
    HTMLPart body = html.body();
    body.heading(1, name+" in "+applicationVersion);
    for( FilterAction fa : allFilterActions ) {
      String title = fa.getTitle();
      if( title != null ) {
        body.heading(2,title);
        fa.appendIndexPage( body );
        body.lineBreak();
      }
    }
    
    INDEX_HTML = DefaultFilterActionInstance.getBytes(html.toHTML());
    return INDEX_HTML;
  }

}
