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

import gip.base.common.*;
import gip.base.db.OBContext;


public class OBListDM {

  @SuppressWarnings("unused")
  public static <E extends OBDTO> OBListDTO<E> convertTkToDTO(OBContext context, OBListDTO<E> retVal, long usedFor) {
    return retVal;
  }
  
  @SuppressWarnings("unused")
  public static OBListDTO<LongDTO> convertTkToDTO(OBContext context, long[] retVal, long usedFor) {
    OBListDTO<LongDTO> list = new OBListDTO<LongDTO>();
    for (int i=0; i<retVal.length; i++) {
      LongDTO l = new LongDTO();
      l.setLong(retVal[i]);
      list.add(l);
    }
    return list;
  }
  
  @SuppressWarnings("unused")
  public static OBListDTO<StringDTO> convertTkToDTO(OBContext context, String[] retVal, long usedFor) {
    OBListDTO<StringDTO> list = new OBListDTO<StringDTO>();
    for (int i=0; i<retVal.length; i++) {
      StringDTO s = new StringDTO();
      s.setString(retVal[i]);
      list.add(s);
    }
    return list;
  }
  
}