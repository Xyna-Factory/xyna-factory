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
package com.gip.xyna.xact.filter.actions;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.FilterAction;
import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;

public class OptionsAction implements FilterAction {

  private static final Logger logger = CentralFactoryLogging.getLogger(OptionsAction.class);
  private XynaPropertyString propertyACAO;
  
  public OptionsAction(XynaPropertyString propertyACAO) {
    this.propertyACAO = propertyACAO;
  }


  @Override
  public boolean match(URLPath url, Method method) {
    return Method.OPTIONS == method;
  }

  @Override
  public FilterActionInstance act(URLPath url, HTTPTriggerConnection tc) throws XynaException {
    DefaultFilterActionInstance dfai = new DefaultFilterActionInstance();
    String host = tc.getHeader().getProperty("host");
    String origin = tc.getHeader().getProperty("origin");
    String allow_method = tc.getHeader().getProperty("access-control-request-method");
    String allow_headers = tc.getHeader().getProperty("access-control-request-headers");
    String allow_credentials = tc.getHeader().getProperty("access-control-request-credentials");
    
    if (logger.isDebugEnabled()) {
      logger.debug("Options : host="+host+", origin="+origin+", allow_method="+allow_method+", allow_headers="+allow_headers+", allow_credentials="+allow_credentials);
    }
    
    setAccessControlParameter(tc, dfai);

    // set HSTS (HTTP-Strict-Transport-Security) if configured
    // TODO: read Xyna Property
    // if set, add HSTS header
    int maxAge = 2 * 365 * 24 * 60 * 60; // two standard years in s
    dfai.setProperty("Strict-Transport-Security", new StringBuilder().append("max-age=").append(maxAge).append("; includeSubDomains").toString());
    
    dfai.sendProperties(tc);
    return dfai;
  }

  public void setAccessControlParameter(HTTPTriggerConnection tc, DefaultFilterActionInstance dfai) {
    dfai.setProperty("Access-Control-Allow-Credentials", "true");
    String origin = tc.getHeader().getProperty("origin");
    if (origin == null || origin.isEmpty()) {
      //browser ist verantwortlich, cross-origin requests so zu erstellen, dass er auch origin angibt. ansonsten ist es ein same-origin request.
      return;
    }
    setAllowedOrigin(tc, dfai, origin);
    
    //erlaube einfach das was angefragt wird. achtung: der browser könnte die antwort cachen. schöner wäre es, wenn man hier alles angibt
    String allow_method = tc.getHeader().getProperty("access-control-request-method");
    String allow_headers = tc.getHeader().getProperty("access-control-request-headers");

    if( allow_method != null ) {
      dfai.setProperty("Access-Control-Allow-Methods", allow_method);
    }
    if( allow_headers != null ) {
      dfai.setProperty("Access-Control-Allow-Headers", allow_headers);
    }
  }


  /*
   * - falls der im request angegebene origin erlaubt ist, wird er in Access-Control-Allow-Origin zurückgegeben
   * - falls er nicht erlaubt ist
   *   - wird der erste in der property angegebene origin zurückgegeben (oder "*" falls vorhanden)
   *   - falls die property leer ist, wird versucht RMI_HOSTNAME_REGISTRY zurückgegeben
   *     - falls RMI_HOSTNAME_REGISTRY auf einen hostname konfiguriert ist, der nicht localhost ist => ok
   *     - ansonsten (IP oder localhost) => nok => dann wird Access-Control-Allow-Origin nicht gesetzt
   * 
   * ein origin ist erlaubt, falls er in der xynaproperty als einer der kommaseparierten werte enthalten ist oder wenn die xynaproperty "*" enthält
   * oder wenn die property leer ist UND wenn der origin er dem RMI_HOSTNAME_REGISTRY wert entspricht und dieser keine IP oder localhost enthält.
   * => localhost ist nur erlaubt, wenn localhost in der "propertyACAO" enthalten ist
   * => RMI_HOSTNAME_REGISTRY wird nur betrachtet, falls acao-property leer ist.
   */
  private void setAllowedOrigin(HTTPTriggerConnection tc, DefaultFilterActionInstance dfai, String origin) {
    String[] allowedOrigins  = getAllowedOrigins();
    if (isAllowed(allowedOrigins, origin)) {
      dfai.setProperty("Access-Control-Allow-Origin", origin);
    } else {
      if (allowedOrigins.length > 0) {
        dfai.setProperty("Access-Control-Allow-Origin", allowedOrigins[0]);
      }
    }
  }


  private boolean isAllowed(String[] allowedOrigins, String origin) {
    if (allowedOrigins.length == 0) {
      return false;
    }
    if (origin == null || origin.isEmpty()) {
      return false;
    }
    if (allowedOrigins.length == 1 && allowedOrigins[0].equals("*")) {
      return true;
    }
    for (String s : allowedOrigins) {
      if (s.equals(origin)) {
        return true;
      }
    }
    return false;
  }


  private String[] getAllowedOrigins() {
    String acao = propertyACAO.get().trim();
    List<String> l = new ArrayList<>();
    if (acao.length() > 0) {
      String[] allowedOrigins = acao.split(",");
      for (int i = 0; i < allowedOrigins.length; i++) {
        String a = allowedOrigins[i];
        a = a.trim();
        if (a.equals("*")) {
          return new String[] {"*"};
        }
        if (a.length() > 0) {
          l.add(a);
        }
      }
    }
    if (l.size() == 0) {
      String rmiprop = XynaProperty.RMI_HOSTNAME_REGISTRY.get();
      if (rmiprop.equals("localhost")) {
        return new String[0];
      } else if (rmiprop.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
        //ip
        return new String[0];
      } else {
        return new String[] {rmiprop};
      }
    }
    return l.toArray(new String[0]);
  }


  @Override
  public String getTitle() {
    return null;
  }

  @Override
  public void appendIndexPage(HTMLPart body) {
  }

  @Override
  public boolean hasIndexPageChanged() {
    return false;
  }
  

}
