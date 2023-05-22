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
package gip.base.db.demultiplexing;

import java.util.HashMap;

import gip.base.common.HashMapDTO;
import gip.base.db.OBContext;


public class HashMapDM {


  /**
   * @param context
   * @param retVal
   * @param usedFor
   * @return HashMap
   */
  @SuppressWarnings("rawtypes")
  public static HashMapDTO convertTkToDTO(OBContext context, HashMap retVal, long usedFor) {
    HashMapDTO dto = new HashMapDTO();
    dto.setHashMap(retVal);
    return dto;
  }

  
}


