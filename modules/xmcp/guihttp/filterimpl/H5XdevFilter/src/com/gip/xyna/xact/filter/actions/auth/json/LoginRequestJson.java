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

public class LoginRequestJson {
  
  private final static String USERNAME = "username";
  private final static String PASSWORD = "password";
  private final static String FORCE_FLAG = "force";
  private final static String PATH = "path";

  private String username;
  private String password;
  private boolean force = true;
  private String path;
  
  public LoginRequestJson() {}
  
  public LoginRequestJson(String username, String password, boolean force, String path) {
    this.username = username;
    this.password = password;
    this.force = force;
    this.path = path;
  }

  public String getUsername() {
    return username;
  }
  
  public String getPassword() {
    return password;
  }
  
  public boolean isForce() {
    return force;
  }

  public String getPath() {
    return path;
  }


  public static JsonVisitor<LoginRequestJson> getJsonVisitor() {
    return new UserCredentialsJsonVisitor();
  }
  
  private static class UserCredentialsJsonVisitor extends EmptyJsonVisitor<LoginRequestJson> {
    LoginRequestJson lrj = new LoginRequestJson();
    
    @Override
    public LoginRequestJson get() {
      return lrj;
    }
    @Override
    public LoginRequestJson getAndReset() {
      LoginRequestJson ret = lrj;
      lrj = new LoginRequestJson();
      return ret;
    }
    
    @Override
    public void attribute(String label, String value, Type type) {
      if (label.equals(USERNAME)) {
        lrj.username = value;
        return;
      } else if (label.equals(PASSWORD)) {
        lrj.password = value;
      } else if (label.equals(FORCE_FLAG)) {
        lrj.force = Boolean.valueOf(value);
      } else if (label.contentEquals(PATH)) {
        lrj.path = value;
      }
    }

  }

}
