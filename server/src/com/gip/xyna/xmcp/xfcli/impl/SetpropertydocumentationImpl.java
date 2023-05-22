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
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Setpropertydocumentation;



public class SetpropertydocumentationImpl extends XynaCommandImplementation<Setpropertydocumentation> {

  public void execute(OutputStream statusOutputStream, Setpropertydocumentation payload) throws XynaException {
    if (XynaFactory.getInstance().getFactoryManagement() == null
        || XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS() == null
        || XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration() == null) {
      throw new RuntimeException("Command may not be executed right now.");
    }
    
    DocumentationLanguage lang;
    
    if(payload.getLanguage() == null){
      lang = DocumentationLanguage.EN;  //default ist EN
    }
    else{
      try {
        lang = DocumentationLanguage.valueOf(payload.getLanguage());
      } catch (IllegalArgumentException e) {
        writeToCommandLine(statusOutputStream, "Invalid language '" + payload.getLanguage()
            + "'; EN and DE are valid languages.\n");
        return;
      }
    }
    
    // whitespace support is already included within the general cli routines
    boolean isDocSet = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration()
      .addPropertyDocumentation(payload.getKey(), lang, payload.getDocumentation());
    
    if(!isDocSet){
      writeToCommandLine(statusOutputStream, "Unknown property '" + payload.getKey() + "'\n");
    }
  }

}
