/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.xnwh.persistence.xmom;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.XMOMStorableStructureInformation;


public class DistinctLimitSingleColumnReader extends SingleColumnReader {

  Set<Object> alreadyReturned = new HashSet<Object>();
  final int maxObjects;
  
  DistinctLimitSingleColumnReader(XMOMStorableStructureInformation rootInfo, int maxObjects) {
    super(rootInfo);
    this.maxObjects = maxObjects;
  }
  
  @Override
  public Object read(ResultSet rs) throws SQLException {
    Object value = super.read(rs);
    if (alreadyReturned.size() < maxObjects || maxObjects == -1) {
      if (!alreadyReturned.add(value)) {
        return null;
      } else {
        return value;
      }
    } else {
      return null;
    }
  }

}
