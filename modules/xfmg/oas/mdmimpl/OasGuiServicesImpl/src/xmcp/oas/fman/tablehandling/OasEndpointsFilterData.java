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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import xmcp.oas.fman.tablehandling.TableHandlingConstants.Paths;
import xmcp.tables.datatypes.TableColumn;
import xmcp.tables.datatypes.TableInfo;


public class OasEndpointsFilterData {
  
  private static final TableHandlingTools tools = new TableHandlingTools();
  private final Optional<Pattern> generatedRtcPattern;
  private final Optional<Pattern> implementationRtcPattern;
  private final Optional<Pattern> apiDatatypePattern;
  private final Optional<Pattern> implementationDatatypePattern;
  private final Optional<Pattern> statusPattern;
  
  
  public OasEndpointsFilterData(TableInfo info) {
    Map<String, String> map = new HashMap<>();
    initMap(map, info);
    generatedRtcPattern = buildPattern(map, Paths.GENERATED_RTC);
    implementationRtcPattern = buildPattern(map, Paths.IMPLEMENTATION_RTC);
    apiDatatypePattern = buildPattern(map, Paths.API_DATATYPE);
    implementationDatatypePattern = buildPattern(map, Paths.IMPLEMENTATION_DATATYPE);
    statusPattern = buildPattern(map, Paths.STATUS);
  }
  
  
  public boolean matchesGeneratedRtcFilter(String value) {
    return matches(generatedRtcPattern, value);
  }

  
  public boolean matchesImplementationRtcFilter(String value) {
    return matches(implementationRtcPattern, value);
  }

  
  public boolean matchesApiDatatypeFilter(String value) {
    return matches(apiDatatypePattern, value);
  }

  
  public boolean matchesImplementationDatatypeFilter(String value) {
    return matches(implementationDatatypePattern, value);
  }

  
  public boolean matchesStatusFilter(String value) {
    return matches(statusPattern, value);
  }
  
  
  private boolean matches(Optional<Pattern> pattern, String value) {
    if (pattern.isEmpty()) {
      return true;
    }
    Optional<String> optvalue = tools.getTrimmedOrEmpty(value);
    if (optvalue.isEmpty()) {
      return pattern.isEmpty();
    }
    return pattern.get().matcher(optvalue.get()).matches();
  }


  private static Optional<Pattern> buildPattern(Map<String, String> map, String key) {
    String filter = map.get(key);
    if (filter == null) {
      return Optional.empty();
    }
    if (!filter.startsWith("*")) {
      filter = "*" + filter;
    }
    if (!filter.endsWith("*")) {
      filter = filter + "*";
    }
    filter = filter.replace("*", ".*");
    Pattern pattern = Pattern.compile(filter);
    return Optional.ofNullable(pattern);
  }
  
  
  private static void initMap(Map<String, String> map, TableInfo info) {
    if (info == null) { return; }
    if (info.getColumns() == null) { return; }
    for (TableColumn col : info.getColumns()) {
      Optional<String> path = tools.getTrimmedOrEmpty(col.getPath());
      if (path.isEmpty()) { continue; }
      Optional<String> filter = tools.getTrimmedOrEmpty(col.getFilter());
      if (filter.isEmpty()) { continue; }
      map.put(path.get(), filter.get());
    }
  }
  
}
