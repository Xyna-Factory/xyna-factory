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

package com.gip.xyna.xnwh.persistence.memory;

import java.sql.SQLException;
import java.util.List;

import com.gip.xyna.xnwh.persistence.Storable;




public class MemoryBaseResultCountSet extends IMemoryBaseResultSet {

  private boolean askedNext = false;
  private int count = -1;


  protected void setCount(int count) {
    this.count = count;
  }


  @Override
  public int getInt(int columnIndex) throws SQLException {
    if (columnIndex == 1) {
      if (count == -1) {
        throw new IllegalStateException("Count has not been evaluated yet");
      }
      return count;
    } else {
      return super.getInt(columnIndex);
    }
  }

  @Override
  public long getLong(int columnIndex) throws SQLException {
    if (columnIndex == 1) {
      if (count == -1) {
        throw new IllegalStateException("Count has not been evaluated yet");
      }
      return count;
    } else {
      return super.getLong(columnIndex);
    }
  }


  @Override
  public void unlockReadLocks() {
    // do nothing, no locks
  }


  @Override
  public boolean next() {
    if (!askedNext) {
      askedNext = true;
      return true;
    }
    else
      return false;
  }


  @Override
  public List<MemoryRowLock> getWriteLocks() {
    return null;
  }


  @Override
  public int size() {
    return 1;
  }


  @Override
  protected void ensureIdx() throws SQLException {
    
  }


  @Override
  protected Storable getCurrentData() {
    return null;
  }

}
