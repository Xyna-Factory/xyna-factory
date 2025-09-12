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
package xfmg.oas.generation.cli.impl;


import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;

import xfmg.oas.generation.cli.generated.Buildoasapplication;
import xfmg.oas.generation.tools.OASApplicationData;
import xfmg.oas.generation.tools.OasAppBuilder;
import xfmg.oas.generation.tools.ValidationResult;


public class BuildoasapplicationImpl extends XynaCommandImplementation<Buildoasapplication> {

  private static Logger logger = CentralFactoryLogging.getLogger(BuildoasapplicationImpl.class);
  private OasAppBuilder _builder = new OasAppBuilder();
  
  
  public void execute(OutputStream statusOutputStream, Buildoasapplication payload) throws XynaException {
    String specFile = payload.getPath();
    String target = "/tmp/" + payload.getApplicationName();
    
    if(specFile.endsWith(".zip")) {
      specFile = OasAppBuilder.decompressArchive(specFile);
    }
    
    ValidationResult result = _builder.validate(specFile);
    StringBuilder errors = new StringBuilder("Validation found errors:");
    if (!result.getErrors().isEmpty()) {
      logger.error("Spec: " + specFile + " contains errors.");
      result.getErrors().forEach(error -> {
        logger.error(error);
        errors.append(" ");
        errors.append(error);
      });
    }
    if (!result.getWarnings().isEmpty()) {
      logger.warn("Spec: " + specFile + " contains warnings.");
      result.getWarnings().forEach(warning -> logger.warn(warning));
    }
    if (!result.getErrors().isEmpty()) {
      throw new RuntimeException(errors.toString());
    }

    createAppAndPrintId(statusOutputStream, "xmom-data-model", target + "_datatypes", specFile, "datamodel");
    if (payload.getBuildProvider()) {
      createAppAndPrintId(statusOutputStream, "xmom-server", target + "_provider", specFile, "provider");
    }
    if (payload.getBuildClient()) {
      createAppAndPrintId(statusOutputStream, "xmom-client", target + "_client", specFile, "client");
    }
    writeToCommandLine(statusOutputStream, "Done.");
  }
  
  
  private void createAppAndPrintId(OutputStream statusOutputStream, String generator, String target, String specFile, String type) {
    try (OASApplicationData appData = _builder.createOasApp(generator, target, specFile)) {
      writeToCommandLine(statusOutputStream, type + " ManagedFileId: " + appData.getId() + " ");
    } catch (IOException e) {
      writeToCommandLine(statusOutputStream, "Could not clean up temporary files for " + type);
      if (logger.isWarnEnabled()) {
        logger.warn("Could not clean up temporary files for " + type, e);
      }
    }
  }
  
}
