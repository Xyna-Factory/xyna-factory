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
package com.gip.xyna.xact.filter.session;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;

public class XmomGuiSession {


  private final String id;
  private final String token;
  private final String user;
  private final Role role;


  public XmomGuiSession(String id, String token) throws XynaException {
    this.id = id;
    this.token = token;
    this.user = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getSessionManagement().resolveSessionToUser(id);
    this.role = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getSessionManagement().getRole(id);
  }


  public String getId() {
    return id;
  }
  
  
  public String getToken() {
    return token;
  }

  
  public String getUser() {
    return user;
  }
  

  public Role getRole() {
    return role;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    return result;
  }


  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    XmomGuiSession other = (XmomGuiSession) obj;
    if (id == null) {
      if (other.id != null)
        return false;
    } else if (!id.equals(other.id))
      return false;
    return true;
  }

}
