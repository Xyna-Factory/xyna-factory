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
package com.gip.xyna.xfmg.xods.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.xfmg.xods.filter.ClassMapFilter.FilterElement;
import com.gip.xyna.xfmg.xods.filter.ClassMapFilters.TerminalStreamOperation;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;


public class ClassMapFilterParser {

  // class(GenerationBase).map(fqXmlName).filter(RegExp(bg.test.*)).filter(RegExp(bg.testi.*)).map(isFactoryComponent).filter(WhiteList(true)).map(isFactoryComponent).filter(BlackList(false)).noneMatch()
  private final static Pattern CLASS_EXTRACTOR = Pattern.compile("^class\\(([a-zA-Z]+)\\)\\.");
  private final static Pattern TERMINAL_EXTRACTOR = Pattern.compile("\\.(" + TerminalStreamOperation.allMatch.toString() + "|" +
                                                                             TerminalStreamOperation.anyMatch.toString() + "|" +
                                                                             TerminalStreamOperation.noneMatch.toString() + ")\\(\\)$");
  private final static Pattern MAP_SPLITTER = Pattern.compile("map\\(.+?\\)(?=(\\.map\\(|$))");
  private final static Pattern MAP_AND_FILTER_SPLITTER = Pattern.compile("(map|filter)\\((.+?)\\)(?!\\))");
  
  private boolean parsingSuccess = false;
  
  private String clazz;
  private TerminalStreamOperation terminalOperation;
  private Filter filter;
  private List<Filter> filters;
  private StringMapper<?> mapper;
  private FilterElement<?> filterElement;
  private List<FilterElement<?>> filterElements;
  
  
  public ClassMapFilter<?> construct() {
    if (parsingSuccess) {
      return new ClassMapFilter(clazz, terminalOperation, filterElements);
    } else {
      throw new IllegalStateException("Parsing not finished succesfully!");
    }
  }

  
  public boolean accept(String streamFilter) {
    Matcher classMatcher = CLASS_EXTRACTOR.matcher(streamFilter);
    Matcher terminalMatcher = TERMINAL_EXTRACTOR.matcher(streamFilter);
    if (classMatcher.find() && terminalMatcher.find()) {
      if (acceptClass(classMatcher.group(1)) && 
          acceptTerminalOperation(terminalMatcher.group(1))) {
        String mapAndFilters = streamFilter.substring(classMatcher.end());
        mapAndFilters = mapAndFilters.substring(0, terminalMatcher.start() - classMatcher.end());
        Matcher mapMatcher = MAP_SPLITTER.matcher(mapAndFilters);
        this.filterElements = new ArrayList<FilterElement<?>>();
        while (mapMatcher.find()) {
          if (acceptMapAndFilters(mapMatcher.group())) {
            filterElements.add(this.filterElement);
          } else {
            return false;
          }
        }
        parsingSuccess = true;
        return true;
      } else {
        return false;
      }      
    } else {
      return false;
    }
  }

  
  protected boolean acceptTerminalOperation(String operation) {
    terminalOperation = TerminalStreamOperation.valueOf(TerminalStreamOperation.class, operation);
    return true;
  }


  protected boolean acceptClass(String clazz) {
    if (ClassMapFilters.getInstance().isClassKnown(clazz)) {
      this.clazz = clazz;
      return true;
    } else {
      return false;      
    }
  }

  
  protected boolean acceptMapAndFilters(String mapAndFilters) {
    List<String> mapAndFilterList = new ArrayList<String>();
    String escapedMapAndFilters = escape(mapAndFilters, '"');
    Matcher mapAndFilterMatcher = MAP_AND_FILTER_SPLITTER.matcher(escapedMapAndFilters);
    while (mapAndFilterMatcher.find()) {
      mapAndFilterList.add(mapAndFilters.substring(mapAndFilterMatcher.start(2), mapAndFilterMatcher.end(2)).replaceAll("(?<!\")\"", "").replaceAll("\"\"", "\""));
    }
    if (mapAndFilterList.size() < 2) {
      return false;
    } else {
      if (acceptMapper(mapAndFilterList.get(0)) &&
          acceptFilters(mapAndFilterList.subList(1, mapAndFilterList.size()))) {
        this.filterElement = new FilterElement(this.mapper, this.filters);
        return true;
      } else {
        return false;
      }
    }
  }
  
  
  protected boolean acceptFilters(List<String> filters) {
    this.filters = new ArrayList<Filter>();
    for (String filter : filters) {
      if (acceptFilter(filter)) {
        this.filters.add(this.filter);
      } else {
        return false;
      }
    }
    return true;
  }


  protected boolean acceptMapper(String mapper) {
    if (ClassMapFilters.getInstance().isMapperKnown(clazz, mapper)) {
      this.mapper = ClassMapFilters.getInstance().getMapper(clazz, mapper);
      return true;
    } else {
      return false;
    }
  }
  
  
  protected boolean acceptFilter(String filter) {
    String filtername = filter.substring(0 , filter.indexOf('('));
    String filterParams = filter.substring(filter.indexOf('(') + 1, filter.length() - 1);
    String escapedFilterParams = escape(filterParams, '"');
    String[] splitEscapedFilterParams = escapedFilterParams.split(",");
    String[] splitFilterParams = new String[splitEscapedFilterParams.length];
    int fromIndex = 0;
    for (int i = 0; i < splitEscapedFilterParams.length; i++) {
      splitFilterParams[i] = filterParams.substring(fromIndex, fromIndex + splitEscapedFilterParams[i].length());
      splitFilterParams[i] = splitFilterParams[i].trim().replaceAll("(?<!\")\"", "").replaceAll("\"\"", "\"");
      fromIndex += splitEscapedFilterParams[i].length() + 1;
    }
    this.filter = ClassMapFilters.getInstance().instantiateFilter(filtername, splitFilterParams);
    return true;
  }


  protected static String escape(String unescaped, char escapeSign) {
    StringBuilder escaped = new StringBuilder();
    boolean doEscape = false;
    int lastIndex = 0;
    int escapeIndex = unescaped.indexOf(escapeSign);
    while (escapeIndex >= 0) {
      if (doEscape) {
        char[] escape = new char[escapeIndex - lastIndex +1];
        Arrays.fill(escape, '_');
        escaped.append(new String(escape));
        doEscape = false;
      } else {
        escaped.append(unescaped.substring(lastIndex == 0 ? 0 : lastIndex + 1, escapeIndex));
        doEscape = true;
      }
      lastIndex = escapeIndex;
      escapeIndex = unescaped.indexOf(escapeSign, escapeIndex + 1);
    }
    escaped.append(unescaped.substring(lastIndex == 0 ? 0 : lastIndex + 1));
    return escaped.toString();
  }
  
  
  public static void main(String[] args) {
    ClassMapFilters.getInstance().registerMapper("GenerationBase", new StringMapper<GenerationBase>() {
      public String map(GenerationBase instance) {
        return instance.getOriginalFqName();
      }
      public String getIdentifier() {
        return "fqXmlName";
      }
    });
    ClassMapFilters.getInstance().registerMapper("GenerationBase", new StringMapper<GenerationBase>() {
      public String map(GenerationBase instance) {
        return String.valueOf(instance.isXynaFactoryComponent());
      }
      public String getIdentifier() {
        return "isFactoryComponent";
      }
    });
    
    String filterDefinition = "stream(GenerationBase).map(fqXmlName).filter(RegExp(bg.test.*)).filter(RegExp(bg.testi.*)).map(isFactoryComponent).filter(WhiteList(true)).map(isFactoryComponent).filter(BlackList(false)).noneMatch()"; 
    System.out.println("Definition:");
    System.out.println(filterDefinition);
    ClassMapFilter<?> fs = build(filterDefinition);
    System.out.println("Creation:");
    System.out.println(fs);
    System.out.println("ReCreation:");
    System.out.println(build(fs.toString()));
    
    
  }


  @SuppressWarnings("unchecked")
  public static <E> ClassMapFilter<E> build(String filterstreamDefintion) {
    ClassMapFilterParser fsp = new ClassMapFilterParser();
    if (fsp.accept(filterstreamDefintion)) {
      return (ClassMapFilter<E>) fsp.construct();
    } else {
      return null;
    }
  }
  
}
