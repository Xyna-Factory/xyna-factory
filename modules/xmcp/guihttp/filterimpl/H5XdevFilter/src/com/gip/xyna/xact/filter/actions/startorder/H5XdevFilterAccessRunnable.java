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
package com.gip.xyna.xact.filter.actions.startorder;



import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.filter.URLPath.URLPathQuery;
import com.gip.xyna.xact.filter.actions.auth.utils.AuthUtils;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.RunnableForFilterAccess;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaPlainSessionCredentials;

import xact.http.enums.httpmethods.HTTPMethod;



public class H5XdevFilterAccessRunnable implements RunnableForFilterAccess {

  private static final long serialVersionUID = 1L;
  private XynaPlainSessionCredentials creds;
  private transient List<Endpoint> endpoints;

  public H5XdevFilterAccessRunnable(HTTPTriggerConnection tc, List<Endpoint> endpoints) {
    creds = AuthUtils.readCredentialsFromRequest(tc);
    this.endpoints = endpoints;
  }


  @Override
  public Object execute(Object... parameters) throws XynaException {
    xact.http.URLPath tmpUrl = (xact.http.URLPath) parameters[0];
    HTTPMethod tmpMethod = (xact.http.enums.httpmethods.HTTPMethod) parameters[1];
    String payload = parameters.length > 2 ? (String)parameters[2] : null;
    Method method = Method.valueOf(tmpMethod.getClass().getSimpleName());
    List<URLPathQuery> query = new ArrayList<>();
    if (tmpUrl.getQuery() != null) {
      query = tmpUrl.getQuery().stream().map(x -> new URLPathQuery(x.getAttribute(), x.getValue())).collect(Collectors.toList());
    }
    URLPath url = new URLPath(tmpUrl.getPath(), query,  null);
    for(Endpoint endpoint : endpoints) {
      if(endpoint.match(url, method)) {
        return endpoint.execute(creds, url, method, payload);
      }
    }
    
    
    return null;
  }
  
  @Override
  public boolean serialize() { return false; }
}
