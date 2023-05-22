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

import com.gip.xyna.xmcp.PluginDescription.ParameterUsage;
import com.gip.xyna.xmcp.xfcli.PluginDescriptionUtils;
import com.gip.xyna.xmcp.xfcli.ReturnCode;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;

import java.io.File;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xfmg.exceptions.XFMG_DuplicateKeyStoreName;
import com.gip.xyna.xfmg.exceptions.XFMG_KeyStoreImportError;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownKeyStoreType;
import com.gip.xyna.xfmg.xfctrl.keymgmt.JavaSecurityKeyStore;
import com.gip.xyna.xfmg.xfctrl.keymgmt.JavaSecurityStoreType;
import com.gip.xyna.xfmg.xfctrl.keymgmt.KeyManagement;
import com.gip.xyna.xfmg.xfctrl.keymgmt.KeyStore;
import com.gip.xyna.xfmg.xfctrl.keymgmt.KeyStoreType;
import com.gip.xyna.xfmg.xfctrl.keymgmt.KeyStoreTypeIdentifier;
import com.gip.xyna.xmcp.xfcli.generated.Importkeystore;



public class ImportkeystoreImpl extends XynaCommandImplementation<Importkeystore> {

  public void execute(OutputStream statusOutputStream, Importkeystore payload) throws XynaException {
    List<String> parameters = toList(payload.getImportParameters());
    
    KeyManagement keyMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getKeyManagement();
    
    ReturnCode returnCode;
    if (payload.getKeystoretype().equals("ExternalFile")) {
      payload.setKeystoretype(JavaSecurityStoreType.NAME);
    }
    KeyStoreTypeIdentifier ksti = new KeyStoreTypeIdentifier(payload.getKeystoretype(), "1.0.0");
    KeyStoreType<?> kst = keyMgmt.getRegisteredKeyStoreType(ksti);
    if (parameters.contains("help")) {
      String help = PluginDescriptionUtils.help(kst.getTypeDescription(), parameters, ParameterUsage.Create, "import");
      writeToCommandLine(statusOutputStream, help);
      returnCode = ReturnCode.SUCCESS;
    } else {
      returnCode = importKeyStore(ksti, payload.getName(), payload.getFile(), parameters);
    }
    writeEndToCommandLine(statusOutputStream, returnCode);
  }
  
  private ReturnCode importKeyStore(KeyStoreTypeIdentifier ksti, String name, String filename, List<String> parameters) throws XFMG_KeyStoreImportError, XFMG_UnknownKeyStoreType, XFMG_DuplicateKeyStoreName {
    KeyManagement keyMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getKeyManagement();
    keyMgmt.importKeyStore(ksti, name, new File(filename), StringParameter.listToMap(parameters));
    return ReturnCode.SUCCESS;
  }
  
  private List<String> toList(String[] strings) {
    if (strings == null || strings.length == 0) {
      return Collections.emptyList();
    }
    return Arrays.asList(strings);
  }

}
