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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.OutputStream;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Encrypt;
import com.gip.xyna.xnwh.exceptions.XNWH_EncryptionException;
import com.gip.xyna.xnwh.securestorage.SecureStorage;



public class EncryptImpl extends XynaCommandImplementation<Encrypt> {

  private static final String HEX = "hex";
  private static final String BASE64 = "base64";
  
  
  public void execute(OutputStream statusOutputStream, Encrypt payload) throws XynaException {
    
    String representationMethod = payload.getMethod();
    String result = null;
    if( representationMethod == null ) { 
      //default
      result = hex(payload.getKey(), payload.getValue());
    } else if( HEX.equalsIgnoreCase(representationMethod) ) {
      result = hex(payload.getKey(), payload.getValue());
    } else if( BASE64.equalsIgnoreCase(representationMethod) ) {
      result = base64(payload.getKey(), payload.getValue());
    } else {
      result = "Invalid representation method";
    }
    
    writeLineToCommandLine(statusOutputStream, result);
  }

  private String base64(String key, String value) throws XNWH_EncryptionException {
    return XynaFactory.getInstance().getXynaMultiChannelPortal().getSecureStorage().encrypt(key, value);
  }

  private String hex(String key, String value) throws XNWH_EncryptionException {
    return SecureStorage.byteArrayToHexString(SecureStorage.encryptString(key, value));
  }

}
