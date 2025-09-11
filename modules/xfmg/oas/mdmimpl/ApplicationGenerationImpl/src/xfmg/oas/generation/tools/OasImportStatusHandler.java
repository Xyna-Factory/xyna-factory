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

import java.util.Optional;

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
    public static final String STATUS_VALIDATION = "validation";
    public static final String STATUS_PARSING = "parsing";
    public static final String STATUS_APP_BINARY_GENERATION = "app_binary_generation";
    public static final String STATUS_APP_IMPORT = "app_import";
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_ERROR = "error";
  }
  
  private Optional<OAS_ImportHistory> _storable = Optional.empty();
  private Optional<AppType> _appType = Optional.empty();
  
  
  public void setStorable(OAS_ImportHistory storable) {
    this._storable = Optional.ofNullable(storable);
  }
  
  
  public void setAppType(AppType appType) {
    this._appType = Optional.ofNullable(appType);
  }
  
  
  private String addAppTypeIfSet(String input) {
    if (_appType.isEmpty()) {
      return input;
    }
    return input +  " (" + _appType.get().toString() + ")";
  }
  
  
  public void storeStatusSuccess() {
    if (_storable.isEmpty()) { return; }
    store(Constants.STATUS_SUCCESS);
  }
  
  public void storeStatusValidation() {
    if (_storable.isEmpty()) { return; }
    store(Constants.STATUS_VALIDATION);
  }
  
  public void storeStatusParsing() {
    if (_storable.isEmpty()) { return; }
    store(addAppTypeIfSet(Constants.STATUS_PARSING));
  }
  
  public void storeStatusAppBinaryGen() {
    if (_storable.isEmpty()) { return; }
    store(addAppTypeIfSet(Constants.STATUS_APP_BINARY_GENERATION));
  }
  
  public void storeStatusAppImport() {
    if (_storable.isEmpty()) { return; }
    store(addAppTypeIfSet(Constants.STATUS_APP_IMPORT));
  }
  
  
  private void store(String status) {
    _storable.get().setImportStatus(status);
    try {
      new OasImportHistoryStorage().storeOasImportHistory(_storable.get());
    } catch (Exception e) {
      if (_logger.isErrorEnabled()) {
        _logger.error("Error changing oas import history status. " + e.getMessage(), e);
      }
    }
  }
  
}
