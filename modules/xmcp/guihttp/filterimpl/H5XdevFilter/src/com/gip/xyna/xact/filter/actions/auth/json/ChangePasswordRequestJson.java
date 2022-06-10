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

public class ChangePasswordRequestJson {
  
  private static final String OLD_PASSWORD = "oldPassword";
  private static final String NEW_PASSWORD = "newPassword";

  private String oldPassword;
  private String newPassword;
  
  public ChangePasswordRequestJson() {}
  
  public ChangePasswordRequestJson(String oldPassword, String newPassword) {
    this.oldPassword = oldPassword;
    this.newPassword= newPassword;
  }

  
  public String getOldPassword() {
    return oldPassword;
  }
  
  
  public String getNewPassword() {
    return newPassword;
  }
  
  public static JsonVisitor<ChangePasswordRequestJson> getJsonVisitor() {
    return new ChangePasswordJsonVisitor();
  }
  
  private static class ChangePasswordJsonVisitor extends EmptyJsonVisitor<ChangePasswordRequestJson> {
    ChangePasswordRequestJson lrj = new ChangePasswordRequestJson();
    
    @Override
    public ChangePasswordRequestJson get() {
      return lrj;
    }
    @Override
    public ChangePasswordRequestJson getAndReset() {
      ChangePasswordRequestJson ret = lrj;
      lrj = new ChangePasswordRequestJson();
      return ret;
    }
    
    @Override
    public void attribute(String label, String value, Type type) {
      if (label.equals(OLD_PASSWORD)) {
        lrj.oldPassword = value;
      } else if (label.equals(NEW_PASSWORD)) {
        lrj.newPassword = value;
      }
    }

  }

}
