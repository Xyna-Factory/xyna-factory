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

package xmcp.zeta;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.xnwh.selection.parsing.ArchiveIdentifier;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;



/**
 * TableHelper zur Unterstützung beim Filtern und Sortieren von Tabellen.
 * <br>
 * <b>Initialisierung:</b><br>
 * <code>
 *   TableHelper<AdministrativeVeto, TableInfo> tableHelper = TableHelper.<AdministrativeVeto, TableInfo>init(tableInfo)<br>
 *       .limitConfig(TableInfo::getLimit)<br>
 *       .sortConfig(ti -> {<br>
 *         for (TableColumn tc : ti.getColumns()) {<br>
 *           if(tc.getSort() != null && tc.getSort().length() > 0 && !tc.getSort().equalsIgnoreCase("undefined"))<br>
 *             return new TableHelper.Sort(tc.getPath(), "asc".equalsIgnoreCase(tc.getSort()));<br>
 *         }<br>
 *         return null;<br>
 *       })<br>
 *       .filterConfig(ti -><br> 
 *         ti.getColumns().stream()<br>
 *         .filter(tableColumn -> <br>
 *           !tableColumn.getDisableFilter() && tableColumn.getPath() != null && tableColumn.getFilter() != null<br>
 *         )<br>
 *         .map(tc -> new TableHelper.Filter(tc.getPath(), tc.getFilter()))<br>
 *         .collect(Collectors.toList())<br>
 *       )<br>
 *       .addSelectFunction(TABLE_KEY_NAME, AdministrativeVeto::getName)<br>
 *       .addSelectFunction(TABLE_KEY_DOCUMENTATION, AdministrativeVeto::getDocumentation)<br>
 *       .addTableToDbMapping(TABLE_KEY_NAME, VetoInformationStorable.COL_VETO_NAME)<br>
 *       .addTableToDbMapping(TABLE_KEY_DOCUMENTATION, VetoInformationStorable.COL_DOCUMENTATION);<br>
 * </code>
 * <br>
 * <b>Filtern:</b><br>
 * Der TableHelper bringt bereits mehrere FilterFunctions mit, die automatisch verwendet werden, wenn für den Pfad nichts konfiguriert wurde.<br>
 * Eingebaute FilterFunctions auf Basis des zu filternden Datatyps:
 * <li>String</li>
 * <li>Integer</li>
 * <li>Long</li>
 * <li>Double</li>
 * <li>Boolean</li>
 * Über die Methode <code>addFilterFunction(String path, Function<T, Boolean> filterFunction)</code> können zusätzliche FilterFunctions hinzugefügt werden.
 * <br>
 * <b>Sortieren:</b><br>
 * Ähnlich wie beim Filtern bringt der TableHelper auch schon Funktionen zum Sortieren anhand des Datentyps mit.<br>
 * <li>String</li>
 * <li>Comparable</li>
 * Anwendung: <code>tableHelper.sort(result);</code><br>
 * <br>
 * <b>Anzahl der Listeinträge limitieren:</b><br>
 * <code>result = tableHelper.limit(result);</code>
 *
 * @param <T>
 */
public class TableHelper<T, I> {


  public static class Sort {

    private final String path;
    private final boolean asc;


    public Sort(String path, boolean asc) {
      this.path = path;
      this.asc = asc;
    }
    
    
    public String getPath() {
      return path;
    }
    
    public boolean isAsc() {
      return asc;
    }

  }

  public static class Filter {

    private final String path;
    private final String value;
    private final boolean caseSensitive;
    private final boolean isNumber;

    public Filter(String path, String value) {
      this.path = path;
      this.value = value;
      this.caseSensitive = false;
      this.isNumber = false;
    }

    public Filter(String path, String value, boolean caseSensitive) {
      this.path = path;
      this.value = value;
      this.caseSensitive = caseSensitive;
      this.isNumber = false;
    }

    public Filter(String path, String value, boolean caseSensitive, boolean isNumber) {
      this.path = path;
      this.value = value;
      this.caseSensitive = caseSensitive;
      this.isNumber = isNumber;
    }

    public String getPath() {
      return path;
    }

    public String getValue() {
      return value;
    }

    public boolean getIsNumber() {
      return isNumber;
    }
  }
  
  

  public enum LogicalOperand {
    AND, OR;
  }


  private Function<I, Integer> limitFunction;
  private Function<I, Sort> sortFunction;
  private Function<I, List<Filter>> filterFunction;
  private List<Function<I, List<Filter>>> secondaryFilterFunctions = null;
  private LogicalOperand secondaryFiltersOperand = null;

  private final Map<String, Function<T, ?>> selectFunctions = new HashMap<>(20);
  private final Map<String, Predicate<T>> filterFunctions = new HashMap<>(20);
  private final Map<String, Comparator<T>> comperators = new HashMap<>(20);
  private final Map<String, String> tablePathToDbTableMapping = new HashMap<>(20);
  private final I info;


  private TableHelper(I info) {
    this.info = info;
  }
  
  /**
   * Erzeugt ein Sort-Objekt, wenn das value valide ist.
   * @param path
   * @param value
   * @return Sort oder null
   */
  public static Sort createSortIfValid(String path, String value) {
    if(isValidSortValue(value))
      return new Sort(path, "asc".equalsIgnoreCase(value));
    return null;
  }
  
  /**
   * Checkt ob das Value eine gültige Sortierfunktion ist.
   * @param value
   * @return
   */
  public static boolean isValidSortValue(String value) {
    return value != null && ("asc".equalsIgnoreCase(value) || "dsc".equalsIgnoreCase(value));
  }


  /**
   * Initialisiert einen neuen TableHelper
   * @param <T>
   * @param tableInfo
   * @return
   */
  public static <T, I> TableHelper<T, I> init(I info) {
    return new TableHelper<>(info);
  }


  /**
   * Filterkonfiguration
   * @param filterFunction zur Lieferung einer Liste mit Filter
   * @return
   */
  public TableHelper<T, I> filterConfig(Function<I, List<Filter>> filterFunction) {
    this.filterFunction = filterFunction;
    return this;
  }


  /**
   * Filterkonfiguration für optinale Filter, die zusätzlich zu via filterConfig festgelegtem Filter matchen müssen.
   * 
   * Ein Eintrag kommt dabei durch die gesamte Filterung, falls für ihn gilt:
   * matchesFilterFunction AND (matchesOptionalFilter0 operand matchesOptionalFilter1 operand ...)
   * 
   * Hierbei wird operand mittels des zweiten Parameters dieser Methode festgelegt.
   * @param secondaryFilterFunctions Liste von sekundären Filtern
   * @param operand logischer Operand zwischen den sekundären Filtern - Hinweis: DB-Filterung bei OR wird noch nicht unterstützt, s. Methode filter(...)
   */
  public TableHelper<T, I> secondaryFilterConfig(List<Function<I, List<Filter>>> filterFunctions, LogicalOperand operand) {
    this.secondaryFilterFunctions = filterFunctions;
    this.secondaryFiltersOperand = operand;
    return this;
  }


  /**
   * Limitkonfiguration
   * @param limitFunction Liefert ein Integer zur Limitierung der Ergebnisse. -1 für alle Ergebnisse.
   * @return
   */
  public TableHelper<T, I> limitConfig(Function<I, Integer> limitFunction) {
    this.limitFunction = limitFunction;
    return this;
  }


  /**
   * Sortierkonfiguration
   * @param sortFunction
   * @return
   */
  public TableHelper<T, I> sortConfig(Function<I, Sort> sortFunction) {
    this.sortFunction = sortFunction;
    return this;
  }


  /**
   * 
   * @param tablePath
   * @param colName
   * @return
   */
  public TableHelper<T, I> addTableToDbMapping(String tablePath, String colName) {
    tablePathToDbTableMapping.put(tablePath, colName);
    return this;
  }


  /**
   * Fügt dem TableHelper eine SelectFunction hinzu.<br>
   * Alle Spalten, durch den TableHelper unterstützt werden sollen, benötigen eine selectFunction.
   * @param path Path aus der TableInfo
   * @param selectFunction Function um das Value anhand des Path zu ermitteln
   * @return
   */
  public TableHelper<T, I> addSelectFunction(String path, Function<T, ?> selectFunction) {
    selectFunctions.put(path, selectFunction);
    return this;
  }


  /**
   * Fügt dem TableHelper eine Optionale FilterFunction hinzu.
   * @param path Path aus der TableInfo
   * @param filterFunction Filterfunction zum Filtern des Value.
   * @return
   */
  public TableHelper<T, I> addFilterFunction(String path, Predicate<T> filterFunction) {
    filterFunctions.put(path, filterFunction);
    return this;
  }


  /**
   * Fügt dem TableHelper einen optionalen Comparator hinzu, der bei dem entsprechenden Value verwendet wird.
   * @param path
   * @param comparator
   * @return
   */
  public TableHelper<T, I> addComparator(String path, Comparator<T> comparator) {
    comperators.put(path, comparator);
    return this;
  }


  /**
   * Bereitet den Filter auf, damit er in einer DB-Query als Bedingung verwendet werden kann.
   * @param filter
   * @return
   */
  public static List<String> prepareQueryFilter(String filter) {
    return prepareQueryFilter(filter, false);
  }

  /**
   * Bereitet den Filter auf, damit er in einer DB-Query als Bedingung verwendet werden kann.
   * @param filter
   * @param isNumber
   * @return
   */
  public static List<String> prepareQueryFilter(String filter, boolean isNumber) {
    if (filter == null)
      return Collections.emptyList();

    if(filter.contains("<") && filter.contains(">")) {
      String filterString = filter.replaceAll(" ", "");
      Pattern p = Pattern.compile("([<>][0-9\\,\\.]{1,})");
      Matcher m = p.matcher(filterString);
      List<String> result = new ArrayList<>();
      while(m.find()) {
        result.add(m.group());
      }
      return result;
    } else if(filter.contains("<") || filter.contains(">")) {
      return Arrays.asList(new String[] {filter});
    } else if (filter.contains("*")) {
      filter = filter.replaceAll("\\*", "%");
    } else if (filter.startsWith("'") && filter.endsWith("'") || filter.startsWith("\"") && filter.endsWith("\"")) {
      return Arrays.asList(new String[] {filter});
    } else if (filter.matches("[0-9]+") && isNumber) {
      return Arrays.asList(new String[] {filter});
    }  else {
      filter = "%" + filter + "%";
    }
    return Arrays.asList(new String[] {filter});
  }


  /**
   * Filtert die Einträge anhand der FilterColumn in der TableInfo.
   * @return
   */
  public Predicate<T> filter() {
    return t -> {
      List<Predicate<T>> primaryFilter = getActiveFilter(filterFunction, t);
      boolean result = true;
      for (Predicate<T> predicate : primaryFilter) {
        result = result && predicate.test(t);
        if (!result) {
          return result;
        }
      }

      if (secondaryFiltersOperand == null || secondaryFilterFunctions == null) {
        return result;
      }

      result = (secondaryFiltersOperand == LogicalOperand.AND) ? true : false;
      for (Function<I, List<Filter>> secondaryFilterFunction : secondaryFilterFunctions) {
        List<Predicate<T>> secondaryFilter = getActiveFilter(secondaryFilterFunction, t);
        for (Predicate<T> predicate : secondaryFilter) {
          if (secondaryFiltersOperand == LogicalOperand.AND) {
            result = result && predicate.test(t);
            if (!result) {
              return result;
            }
          } else if (secondaryFiltersOperand == LogicalOperand.OR) {
            result = result || predicate.test(t);
            if (result) {
              return result;
            }
          }
        }
      }

      return result;
    };
  }


  private List<Predicate<T>> getActiveFilter(Function<I, List<Filter>> filterFunction, T t) {
    List<Predicate<T>> activeFilter = new ArrayList<>();
    List<Filter> filters = filterFunction.apply(info);
    if (filters != null) {
      for (Filter filter : filters) {
        if (filterFunctions.containsKey(filter.path)) {
          activeFilter.add(filterFunctions.get(filter.path));
        } else {
          if (selectFunctions.containsKey(filter.path)) {
            if ("*".equals(filter.value)) {
              activeFilter.add(x -> true);
            } else {
              Object value = selectFunctions.get(filter.path).apply(t);
              if (value == null) {
                activeFilter.add(createNullFilter(filter.value, value));
              } else if (value instanceof String) {
                activeFilter.add(createStringFilter(filter, (String) value));
              } else if (value instanceof Integer) {
                if(filter.value.contains("*"))
                  activeFilter.add(createStringFilter(filter, ((Integer) value).toString()));
                else
                  activeFilter.add(createIntegerFilter(filter.value, (Integer) value));
              } else if (value instanceof Long) {
                if(filter.value.contains("*"))
                  activeFilter.add(createStringFilter(filter, ((Long) value).toString()));
                else
                  activeFilter.add(createLongFilter(filter.value, (Long) value));
              } else if (value instanceof Double) {
                if(filter.value.contains("*"))
                  activeFilter.add(createStringFilter(filter, ((Double) value).toString()));
                else
                  activeFilter.add(createDoubleFilter(filter.value, (Double) value));
              } else if (value instanceof Boolean) {
                activeFilter.add(createBooleanFilter(filter.value, (Boolean) value));
              } else {
                activeFilter.add(createObjectFilter(filter, value));
              }
            }
          }
        }
      }
    }

    return activeFilter;
  }


  private Predicate<T> createNullFilter(String filter, Object value) {
    return x -> {
      try {
        return value == null && ("null".equalsIgnoreCase(filter) || "''".equalsIgnoreCase(filter));
      } catch (Exception ex) {
        return false;
      }
    };
  }


  private Predicate<T> createObjectFilter(Filter filter, Object value) {
    return createStringFilter(filter, value.toString());
  }


  private Predicate<T> createBooleanFilter(String filter, Boolean value) {
    return x -> {
      try {
        if (value == null)
          return false;
        Boolean filterBoolean = convertFilterToBoolean(filter);
        return filterBoolean != null && filterBoolean.equals(value);
      } catch (Exception ex) {
        return false;
      }
    };
  }


  private Boolean convertFilterToBoolean(String filter) {
    switch (filter.toLowerCase()) {
      case "1" :
      case "true" :
        return true;
      case "0" :
      case "false" :
        return false;
      default :
        return false;
    }
  }


  private Predicate<T> createIntegerFilter(String filter, Integer value) {
    return x -> {
      try {
        if (value == null)
          return false;
        if(filter.contains("<") && filter.contains(">")) {
          String filterString = filter.replaceAll(" ", "");
          Pattern p = Pattern.compile("([<>][0-9\\,\\.]{1,})");
          Matcher m = p.matcher(filterString);
          while(m.find()) {
            if(!createIntegerFilter(m.group(0), value).test(x))
              return false;
          }
          return true;
        } else if (filter.startsWith(">=")) {
          Integer filterInt = Integer.valueOf(filter.replace(">=", ""));
          return value >= filterInt;
        } else if (filter.startsWith("<=")) {
          Integer filterInt = Integer.valueOf(filter.replace("<=", ""));
          return value <= filterInt;
        } else if (filter.startsWith("<")) {
          Integer filterInt = Integer.valueOf(filter.replace("<", ""));
          return value < filterInt;
        } else if (filter.startsWith(">")) {
          Integer filterInt = Integer.valueOf(filter.replace(">", ""));
          return value > filterInt;
        } else {
          Integer filterInt = Integer.valueOf(filter);
          return value.equals(filterInt);
        }
      } catch (Exception ex) {
        return false;
      }
    };
  }


  private Predicate<T> createLongFilter(String filter, Long value) {
    return x -> {
      try {
        if (value == null)
          return false;
        if(filter.contains("<") && filter.contains(">")) {
          String filterString = filter.replaceAll(" ", "");
          Pattern p = Pattern.compile("([<>][0-9\\,\\.]{1,})");
          Matcher m = p.matcher(filterString);
          while(m.find()) {
            if(!createLongFilter(m.group(0), value).test(x))
              return false;
          }
          return true;
        } else if (filter.startsWith("<")) {
          Long filterInt = Long.valueOf(filter.replace("<", ""));
          return value < filterInt;
        } else if (filter.startsWith(">")) {
          Long filterInt = Long.valueOf(filter.replace(">", ""));
          return value > filterInt;
        } else if (filter.startsWith(">=")) {
          Long filterInt = Long.valueOf(filter.replace(">=", ""));
          return value >= filterInt;
        } else if (filter.startsWith("<=")) {
          Long filterInt = Long.valueOf(filter.replace("<=", ""));
          return value <= filterInt;
        } else {
          Long filterInt = Long.valueOf(filter);
          return value.equals(filterInt);
        }
      } catch (Exception ex) {
        return false;
      }
    };
  }


  private Predicate<T> createDoubleFilter(String filter, Double value) {
    return x -> {
      try {
        if (value == null)
          return false;
        if (filter.startsWith("<")) {
          Double filterInt = Double.valueOf(filter.replace("<", ""));
          return value < filterInt;
        } else if (filter.startsWith(">")) {
          Double filterInt = Double.valueOf(filter.replace(">", ""));
          return value > filterInt;
        } else if (filter.startsWith(">=")) {
          Double filterInt = Double.valueOf(filter.replace(">=", ""));
          return value >= filterInt;
        } else if (filter.startsWith("<=")) {
          Double filterInt = Double.valueOf(filter.replace("<=", ""));
          return value <= filterInt;
        } else {
          Double filterInt = Double.valueOf(filter);
          return value.equals(filterInt);
        }
      } catch (Exception ex) {
        return false;
      }
    };
  }


  private Predicate<T> createStringFilter(Filter filter, String value) {
    return x -> {
      if ("".equals(value)) {
        return "''".equals(filter.value);
      }
      int flags = Pattern.MULTILINE;
      if(!filter.caseSensitive) {
        flags |= Pattern.CASE_INSENSITIVE;
      }
      Pattern p = Pattern.compile(prepareFilter(filter.value), flags);
      return p.matcher(value).find();
    };
  }


  private String prepareFilter(String filter) {
    if (filter.contains("*")) {
      boolean startsWithStart = false;
      if (filter.startsWith("*")) {
        startsWithStart = true;
        filter = filter.replaceFirst("\\*", "");
      }
      String[] split = filter.split("\\*");
      StringBuilder sb = new StringBuilder();
      if(!startsWithStart)
        sb.append("^");
      for(int i = 0; i < split.length; i++) {
        sb.append(Pattern.quote(split[i]));
        if(i < split.length - 1)
          sb.append(".*");
      }
      if(filter.endsWith("*"))
        sb.append(".*");
      sb.append("$");
      filter = sb.toString();
    } else if (filter.startsWith("'") && filter.endsWith("'") || filter.startsWith("\"") && filter.endsWith("\"")) {
      filter = Pattern.quote(filter.substring(1, filter.length()-1));
    } else {
      filter = Pattern.quote(filter);
    }
    return filter;
  }


  public List<T> limit(List<T> result) {
    Integer limit = limitFunction.apply(info);
    if (limit != null && limit != -1)
      return result.subList(0, Math.min(result.size(), limit));
    return result;
  }


  public void sort(SearchRequestBean searchRequestBean) {
    Sort sort = sortFunction.apply(info);
    if (sort != null && tablePathToDbTableMapping.containsKey(sort.path)) {
      searchRequestBean.addOrderBy(tablePathToDbTableMapping.get(sort.path), sort.asc);
    }
  }


  /**
   * Erzeugt eine SearchRequestBean inkl. Filter, Sortierung und Limit.
   * @param archiveIdentifier
   * @return
   */
  public SearchRequestBean createSearchRequest(ArchiveIdentifier archiveIdentifier) {
    Integer limit = -1;
    if(dbLimitAllowed()) {
      limit = limitFunction.apply(info);
    }
    SearchRequestBean searchRequest = new SearchRequestBean(archiveIdentifier, (limit != null) ? limit : -1);
    createSelection(searchRequest);
    filter(searchRequest);
    sort(searchRequest);
    return searchRequest;
  }
  
  private void createSelection(SearchRequestBean searchRequest) {
    StringBuilder selection = new StringBuilder();
    tablePathToDbTableMapping.forEach((tablePath, dbColumnName) -> {
      if(selection.length() > 0)
        selection.append(",");
      selection.append(dbColumnName);
    });
    searchRequest.setSelection(selection.toString());
  }
  
  /**
   * Testet ob es möglich ist das DB-Select zu limitieren.
   * Dies ist nur möglich, wenn alle Gui-Spalten mit aktivem Filter und die Sortierspalte ein Mapping zur DB haben.
   * @return
   */
  private boolean dbLimitAllowed() {
    List<Filter> filters = filterFunction.apply(info);
    if(filters != null) {
      for (Filter filter : filters) {
        if(!tablePathToDbTableMapping.containsKey(filter.path))
          return false;
      }
    }
    Sort sort = sortFunction.apply(info);
    if(sort != null && !tablePathToDbTableMapping.containsKey(sort.path)) {
      return false;
    }
    return true;
  }


  /**
   * Fügt der SearchRequestBean die Filter hinzu.
   * @param searchRequest
   */
  public void filter(SearchRequestBean searchRequest) {
    List<Filter> filters = filterFunction.apply(info);
    filter(searchRequest, filters);

    if (secondaryFilterFunctions == null) {
      return;
    }

    if (secondaryFiltersOperand == LogicalOperand.AND) {
      for (Function<I, List<Filter>> secondaryFilterFunction : secondaryFilterFunctions) {
        filters = secondaryFilterFunction.apply(info);
        filter(searchRequest, filters);
      }
    } else {
      // not supported, yet
    }
  }


  private void filter(SearchRequestBean searchRequest, List<Filter> filters) {
    if (filters == null) {
      return;
    }

    for (Filter filter : filters) {
      if (tablePathToDbTableMapping.containsKey(filter.path)) {
        List<String> parts = prepareQueryFilter(filter.value, filter.isNumber);
        for (String part : parts) {
          searchRequest.addFilterEntry(tablePathToDbTableMapping.get(filter.path), part);
        }
      }
    }
  }


  /**
   * Sortiert die übergebene Liste.
   * @param result
   */
  public void sort(List<T> result) {
    Sort sort = sortFunction.apply(info);
    if (sort != null)
      Collections.sort(result, createComparator(sort));
  }


  @SuppressWarnings("unchecked")
  private Comparator<T> createComparator(Sort sort) {
    if (sort == null) {
      return (o1, o2) -> 0;
    }
    Comparator<T> comparator = comperators.get(sort.path);
    if (comparator == null) {
      comparator = (o1, o2) -> {
        Function<T, ?> selectFunction = selectFunctions.get(sort.path);
        if (selectFunction == null)
          return 0;
        if(o1 == null && o2 == null)
          return 0;
        if (o1 == null)
          return -1;
        if (o2 == null)
          return 1;
        
        Object value1 = selectFunction.apply(o1);
        Object value2 = selectFunction.apply(o2);
        if (value1 == null && value2 == null)
          return 0;
        if (value1 == null)
          return -1;
        if (value2 == null)
          return 1;
        if (value1 instanceof String)
          return compareString((String) value1, (String) value2);
        if (value1 instanceof Comparable)
          return ((Comparable<Object>) value1).compareTo(value2);
        return 0;
      };
    }

    if (!sort.asc)
      comparator = comparator.reversed();
    return comparator;
  }


  private int compareString(String s1, String s2) {
    int res = String.CASE_INSENSITIVE_ORDER.compare(s1, s2);
    return (res != 0) ? res : s1.compareTo(s2);
  }
}
