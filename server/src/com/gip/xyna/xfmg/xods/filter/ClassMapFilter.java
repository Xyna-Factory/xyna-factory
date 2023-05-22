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
package com.gip.xyna.xfmg.xods.filter;

import java.util.List;

import com.gip.xyna.xfmg.xods.filter.ClassMapFilters.TerminalStreamOperation;

public class ClassMapFilter<E> {
  
  private final List<FilterElement<E>> elements;
  private final TerminalStreamOperation terminal;
  private final String clazz;
  
  protected ClassMapFilter(String clazz, TerminalStreamOperation terminal, List<FilterElement<E>> elements) {
    this.clazz = clazz;
    this.terminal = terminal;
    this.elements = elements;
  }
  
  public boolean accept(E element) {
    return terminal.evaluate(elements, element);
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class(")
      .append(clazz)
      .append(").");
    for (FilterElement<E> filterElement : elements) {
      sb.append(filterElement.toString())
        .append('.');
    }
    sb.append(terminal.toString())
      .append("()");
    return sb.toString();
  }
  
  
  public static class FilterElement<E> {
    
    private final StringMapper<E> mapper;
    private final List<Filter> filters;
    
    protected FilterElement(StringMapper<E> mapper, List<Filter> filters) {
      this.mapper = mapper;
      this.filters = filters;
    }
    
    
    public StringMapper<E> getMapper() {
      return mapper;
    }
    
    
    public List<Filter> getFilters() {
      return filters;
    }
    
    
    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("map(")
        .append(mapper.getIdentifier())
        .append(')');
      for (Filter filter : filters) {
        sb.append(".filter(")
          .append(filter.toString())
          .append(')');
      }
      return sb.toString();
    }
    
    
  }
  
}
