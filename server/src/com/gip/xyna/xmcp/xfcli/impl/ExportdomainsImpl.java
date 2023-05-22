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



import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Exportdomains;



public class ExportdomainsImpl extends XynaCommandImplementation<Exportdomains> {

  private static final Logger logger = CentralFactoryLogging.getLogger(ExportdomainsImpl.class);


  public void execute(OutputStream statusOutputStream, Exportdomains payload) throws XynaException {

    try {

      File scriptFile = new File(payload.getScriptName());
      if (scriptFile.exists()) {
        writeToCommandLine(statusOutputStream, "The script '" + payload.getScriptName() + "' could not be created\n");
        logger.warn("Could not create import script, file already exists");
        return;
      }

      if (!scriptFile.createNewFile()) {
        writeToCommandLine(statusOutputStream, "The script '" + payload.getScriptName() + "' could not be created\n");
        logger.warn("Could not create import script, insufficient rights?");
        return;
      }

      BufferedOutputStream scriptStream = new BufferedOutputStream(new FileOutputStream(scriptFile));

      if (!ExportrolesImpl.generateDomainImports(factory, scriptStream)) {
        writeToCommandLine(statusOutputStream, "The script '" + payload.getScriptName() + "' could not be created\n");
        logger.warn("Could not create import script, error while writing to file");
        scriptStream.close();
        scriptFile.delete();
        return;
      }

      scriptStream.flush();
      scriptStream.close();

      writeToCommandLine(statusOutputStream, "The script '" + payload.getScriptName() + "' was succesfully created\n");

    } catch (IOException e) {
      throw new Ex_FileAccessException(payload.getScriptName(), e);
    }
  }

}
