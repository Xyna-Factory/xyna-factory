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

import java.util.Map;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.FilterAction;
import com.gip.xyna.xact.filter.JsonFilterActionInstance;
import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.filter.actions.auth.utils.AuthUtils;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaPlainSessionCredentials;
import com.gip.xyna.xnwh.securestorage.SecureStorage;

import xmcp.EncryptionData;

public class DecodeAction  implements FilterAction {
  
  @Override
  public FilterActionInstance act(URLPath arg0, HTTPTriggerConnection tc) throws XynaException {
    JsonFilterActionInstance jfai = new JsonFilterActionInstance();
    
    XynaPlainSessionCredentials creds = AuthUtils.readCredentialsFromRequest(tc);
    String sessionId = creds.getSessionId();
    String token = creds.getToken();
    
    if (sessionId == null || token == null) {
      AuthUtils.replyError(tc, jfai, new RuntimeException("SessionId or Token missing."));
    }
    
    EncryptionData request = (EncryptionData) Utils.convertJsonToGeneralXynaObjectUsingGuiHttp(tc.getPayload());
    if(!request.getEncrypted()) {
      AuthUtils.replyError(tc, jfai, new RuntimeException("Data is not encrypted!"));
    }
    
    for(int i=0; i<request.getValues().size(); i++) {
      String data = request.getValues().get(i).replace("%3d", "=").replace("-", "+").replace("_", "/");
      String result = SecureStorage.staticDecrypt(sessionId + token, data);
      request.getValues().set(i, result);
    }
    request.unversionedSetEncrypted(false);
    
    jfai.sendJson(tc, Utils.xoToJson(request));
    
    return jfai;
  }

  @Override
  public void appendIndexPage(HTMLPart arg0) {
  }

  @Override
  public String getTitle() {
    return "Decode";
  }

  @Override
  public boolean hasIndexPageChanged() {
    return false;
  }

  @Override
  public boolean match(URLPath path, Method method) {
    return method == Method.POST && path.getPathLength() == 1 && path.getPathElement(0).equals(PathElements.DECODE);
  }

}
