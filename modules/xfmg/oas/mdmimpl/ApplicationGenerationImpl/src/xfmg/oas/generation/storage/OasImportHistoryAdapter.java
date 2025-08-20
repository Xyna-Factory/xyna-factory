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


package xfmg.oas.generation.storage;

import xmcp.oas.fman.storables.OAS_ImportHistory;


public class OasImportHistoryAdapter {

  public OAS_ImportHistory adapt(OasImportHistoryStorable input) {
    OAS_ImportHistory ret = new OAS_ImportHistory();
    ret.setUniqueIdentifier(input.getUniqueIdentifier());
    ret.setType(input.getType());
    ret.setDate0(input.getDate());
    ret.setFileName(input.getFileName());
    ret.setSpecificationFile(input.getSpecificationFile());
    ret.setImportStatus(input.getImportStatus());
    ret.setErrorMessage(input.getErrorMessage());
    return ret;
  }
  
  
  public OasImportHistoryStorable adapt(OAS_ImportHistory input) {
    OasImportHistoryStorable ret = new OasImportHistoryStorable();
    ret.setUniqueIdentifier(input.getUniqueIdentifier());
    ret.setType(input.getType());
    ret.setDate(input.getDate0());
    ret.setFileName(input.getFileName());
    ret.setSpecificationFile(input.getSpecificationFile());
    ret.setImportStatus(input.getImportStatus());
    ret.setErrorMessage(input.getErrorMessage());
    return ret;
  }
  
}
