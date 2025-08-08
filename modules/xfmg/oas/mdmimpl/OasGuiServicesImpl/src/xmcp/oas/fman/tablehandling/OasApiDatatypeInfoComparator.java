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

package xmcp.oas.fman.tablehandling;

import java.util.Comparator;
import java.util.Optional;

import xmcp.oas.fman.datatypes.OasApiDatatypeInfo;
import xmcp.tables.datatypes.TableColumn;
import xmcp.tables.datatypes.TableInfo;


public class OasApiDatatypeInfoComparator implements Comparator<OasApiDatatypeInfo> {

  public static class SortInput {
    public String path;
    public String direction;
  }
  
  private final String attributeName;
  private final int directionFactor;
  private static final TableHandlingTools tools = new TableHandlingTools();
  
  
  public OasApiDatatypeInfoComparator(TableInfo info) {
    Optional<SortInput> input = determineSortColumnIndex(info);
    if (input.isEmpty()) {
      attributeName = TableHandlingConstants.Paths.API_DATATYPE;
      directionFactor = 1;
      return;
    }
    attributeName = input.get().path;
    if (TableHandlingConstants.SortVariants.DSC.equals(input.get().direction)) {
      directionFactor = -1;
      return;
    }
    directionFactor = 1;
  }

  
  private static Optional<SortInput> determineSortColumnIndex(TableInfo info) {
    if (info == null) { return Optional.empty(); }
    if (info.getColumns() == null) { return Optional.empty(); }
    for (int i = 0; i < info.getColumns().size(); i++) {
      TableColumn col = info.getColumns().get(i);
      Optional<String> sort = tools.getTrimmedOrEmpty(col.getSort());
      if (sort.isEmpty()) { continue; }
      Optional<String> path = tools.getTrimmedOrEmpty(col.getPath());
      if (path.isEmpty()) { continue; }
      SortInput ret = new SortInput();
      ret.path = path.get();
      ret.direction = sort.get();
      return Optional.of(ret);
    }
    return Optional.empty();
  }
  
  
  @Override
  public int compare(OasApiDatatypeInfo info1, OasApiDatatypeInfo info2) {
    Optional<String> val1 = extract(info1);
    if (val1.isEmpty()) { return -1 * directionFactor; }
    Optional<String> val2 = extract(info1);
    if (val2.isEmpty()) { return 1 * directionFactor; }
    
    int res = val1.get().compareTo(val2.get());
    return res * directionFactor;
  }

  
  private Optional<String> extract(OasApiDatatypeInfo info) {
    try {
      Object obj = info.get(attributeName);
      if (!(obj instanceof String)) { return Optional.empty(); }
      return Optional.of((String) obj);
    } catch (Exception e) {
      return Optional.empty();
    }
  }
  
}
