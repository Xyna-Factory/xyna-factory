/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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

import gip.base.common.DoubleDTO;
import gip.base.db.OBContext;


public class DoubleDM {

  
  /**
   * @param context
   * @param retVal
   * @param usedFor
   * @return DoubleDTO
   */
  public static DoubleDTO convertTkToDTO(OBContext context, double retVal, long usedFor) {
    DoubleDTO dto = new DoubleDTO();
    dto.setDouble(retVal);
    return dto;
  }
  
  
}


