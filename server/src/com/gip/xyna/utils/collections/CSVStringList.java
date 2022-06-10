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
package com.gip.xyna.utils.collections;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import com.gip.xyna.utils.db.types.StringSerializable;
import com.gip.xyna.utils.misc.CsvUtils;


public class CSVStringList extends WrappedList<String> implements Serializable, StringSerializable<CSVStringList> {

  private static final long serialVersionUID = 1L;

  public CSVStringList() {
    super( new ArrayList<String>() );
  }
  
  public CSVStringList(Collection<String> c) {
    super( new ArrayList<String>(c) );
  }

  
  public CSVStringList deserializeFromString(String csv) {
    return valueOf(csv);
    
  }

  public static CSVStringList valueOf(String csv) {
    CSVStringList list = new CSVStringList();
    for( String v : CsvUtils.iterate(csv) ) {
      list.add(v);
    }
    return list;
  }

  public String serializeToString() {
    return CsvUtils.toCSV(wrapped);
  }

}
