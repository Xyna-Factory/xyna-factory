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

package xmcp.zeta.storage.generic.filter.elems;

import java.util.List;
import java.util.Optional;

import xmcp.zeta.storage.generic.filter.parser.FilterInputParser;
import xmcp.zeta.storage.generic.filter.shared.JsonWriter;
import xmcp.zeta.storage.generic.filter.shared.SqlWhereClauseData;


public interface FilterElement {

  public boolean isFinished();

  public void parse(FilterInputParser parser);
  
  public void writeJson(JsonWriter json);
  
  public void writeSql(String colname, SqlWhereClauseData sql);
  
  public Optional<FilterElement> getChild(int index);
  
  public String getInfoString();
  
  
  public default String writeTreeInfo() {
    StringBuilder str = new StringBuilder();
    writeTreeInfoElem("/", str);
    return str.toString();
  }
  
  
  public default void writeTreeInfoElem(String branch, StringBuilder str) {
    str.append(branch + " : " + this.getInfoString()).append("\n");
    int i = 0;
    while (true) {
      Optional<FilterElement> child = this.getChild(i);
      if (child.isEmpty()) { break; }
      child.get().writeTreeInfoElem(branch + i + "/", str);
      i++;
    }
  }
  
}
