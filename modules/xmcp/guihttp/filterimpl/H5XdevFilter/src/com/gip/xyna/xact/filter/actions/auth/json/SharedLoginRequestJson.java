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
package com.gip.xyna.xact.filter.actions.auth.json;

import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;

public class SharedLoginRequestJson {
  
  private final static String SESSIONID = "sessionId";
  private final static String TOKEN = "token";
  private final static String PATH = "path";

  private String sessionId;
  private String token;
  private String path;
  
  public SharedLoginRequestJson() {}
  
  public SharedLoginRequestJson(String sessionid, String token, String path) {
    this.sessionId = sessionid;
    this.token = token;
    this.path = path;
  }

  public String getSessionId() {
    return sessionId;
  }
  
  public String getToken() {
    return token;
  }
  
  public String getPath() {
    return path;
  }
  
  public static JsonVisitor<SharedLoginRequestJson> getJsonVisitor() {
    return new SessionCredentialsJsonVisitor();
  }
  
  private static class SessionCredentialsJsonVisitor extends EmptyJsonVisitor<SharedLoginRequestJson> {
    SharedLoginRequestJson lrj = new SharedLoginRequestJson();
    
    @Override
    public SharedLoginRequestJson get() {
      return lrj;
    }
    @Override
    public SharedLoginRequestJson getAndReset() {
      SharedLoginRequestJson ret = lrj;
      lrj = new SharedLoginRequestJson();
      return ret;
    }
    
    @Override
    public void attribute(String label, String value, Type type) {
      if (label.equals(SESSIONID)) {
        lrj.sessionId = value;
        return;
      } else if (label.equals(TOKEN)) {
        lrj.token = value;
      } else if (label.equals(PATH)) {
        lrj.token = value;
      }
    }

  }

}
