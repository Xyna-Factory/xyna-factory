/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package xfmg.oas.generation.tools;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;

import xfmg.oas.generation.storage.OasImportHistoryStorage;
import xmcp.oas.fman.storables.OAS_ImportHistory;


public class OasImportStatusHandler {

  private static Logger _logger = CentralFactoryLogging.getLogger(OasImportStatusHandler.class);
  
  public static enum AppType {
    PROVIDER, CLIENT, DATA_MODEL
  }
  
  public static class Constants {
    public static final String STATUS_VALIDATION = "VALIDATION";
    public static final String STATUS_PARSING = "PARSING";
    public static final String STATUS_APP_BINARY_GENERATION = "APP_BINARY_GENERATION";
    public static final String STATUS_APP_IMPORT = "APP_IMPORT";
    
  }
  
  private OAS_ImportHistory _input;
  
  // optional
  private AppType _appType;
  
  
  public OasImportStatusHandler(OAS_ImportHistory input, AppType appType) {
    this._input = input;
    this._appType = appType;
  }
  
  // set app type
  
  
  public void storeStatusValidation() {
    store(Constants.STATUS_VALIDATION);
  }
  
  public void storeStatusParsing() {
    // method build status, check optional
    store(Constants.STATUS_PARSING + " (" + _appType.toString() + ")");
  }
  
  public void storeStatusAppBinaryGen() {
    store(Constants.STATUS_APP_BINARY_GENERATION + " (" + _appType.toString() + ")");
  }
  
  public void storeStatusAppImport() {
    store(Constants.STATUS_APP_IMPORT + " (" + _appType.toString() + ")");
  }
  
  
  private void store(String status) {
    String fullStatus = status;
    _input.setImportStatus(fullStatus);
    try {
      new OasImportHistoryStorage().storeOasImportHistory(_input);
    } catch (Exception e) {
      if (_logger.isErrorEnabled()) {
        _logger.error("Error changing oas import history status. " + e.getMessage(), e);
      }
    }
  }
  
}
