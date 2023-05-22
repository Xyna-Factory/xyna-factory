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

import com.gip.xyna.utils.db.UnsupportingResultSet;
import com.gip.xyna.utils.db.types.StringSerializable;
import com.gip.xyna.xnwh.persistence.Storable;



public abstract class BaseResultSet extends UnsupportingResultSet {

  protected boolean wasNull = false;

  protected abstract void ensureIdx() throws SQLException;

  protected abstract Storable getCurrentData();

  public long nvl(Long l) {
    if (l == null) {
      wasNull = true;
      return 0;
    }
    return l;
  }


  public int nvl(Integer i) {
    if (i == null) {
      wasNull = true;
      return 0;
    }
    return i;
  }


  public boolean nvl(Boolean b) {
    if (b == null) {
      wasNull = true;
      return false;
    }
    return b;
  }


  public double nvl(Double d) {
    if (d == null) {
      wasNull = true;
      return 0;
    }
    return d;
  }


  public float nvl(Float f) {
    if (f == null) {
      wasNull = true;
      return 0;
    }
    return f;
  }


  public byte nvl(Byte b) {
    if (b == null) {
      wasNull = true;
      return 0;
    }
    return b;
  }


  public Object nvl(Object o) {
    if (o == null) {
      wasNull = true;
    }
    return o;
  }


  public String nvl(String s) {
    if (s == null) {
      wasNull = true;
    }
    return s;
  }


  //Spezialisierungen f�r nvlString: Ausgeben eines beliebigen Objects als String
  public String nvlString(Object o) {
    if (o == null) {
      wasNull = true;
      return null;
    }
    if( o instanceof StringSerializable ) {
      return ((StringSerializable<?>)o).serializeToString();
    }
    return String.valueOf(o);
  }
  public String nvlString(String s) {
    if (s == null) {
      wasNull = true;
      return null;
    }
    return s;
  }

  @Override
  public boolean wasNull() throws SQLException {
    return wasNull;
  }

}
