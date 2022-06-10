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
package com.gip.xyna.xfmg.xods.filter;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;


public abstract class Filter {
  
  protected String[] args;
  
  Filter() {
    // for newInstance
  }
  
  public final void initialize(String... args) {
    this.args = args;
    initializeInternally(args);
  }
  
  protected abstract void initializeInternally(String... args);
  
  public abstract String getIdentifier();
  
  public abstract boolean accept(String value);
  
  public final String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getIdentifier())
      .append('(');
    for (int i = 0; i < args.length; i++) {
      sb.append('"')
        .append(args[i].replaceAll("\"", "\"\""))
        .append('"');
      if (i+1 < args.length) {
        sb.append(",");
      }
    }
    sb.append(')');
    return sb.toString();
  }
  
  
  public static class BlackListFilter extends SetBasedFilter {
    
    public final static String IDENTIFIER = "BlackList";
    
    public BlackListFilter() {
      super(true);
    }

    @Override
    public String getIdentifier() {
      return IDENTIFIER;
    }

  }
  
  
  public static class WhiteListFilter extends SetBasedFilter {
    
    public final static String IDENTIFIER = "WhiteList";
    
    public WhiteListFilter() {
      super(false);
    }

    @Override
    public String getIdentifier() {
      return IDENTIFIER;
    }

  }
  
  
  public static class RegExpFilter extends Filter {
    
    public final static String IDENTIFIER = "RegExp";
    
    private Pattern pattern;
    
    public boolean accept(String value) {
      return pattern.matcher(value).matches();
    }

    public Class<String> getFilteringClass() {
      return String.class;
    }

    @Override
    protected void initializeInternally(String... args) {
      pattern = Pattern.compile(args[0]);
    }

    @Override
    public String getIdentifier() {
      return IDENTIFIER;
    }
    
  }
  
  
  private static abstract class SetBasedFilter extends Filter {
    
    private Set<String> values;
    private final boolean invert;
    
    
    public SetBasedFilter(boolean invert) {
      this.invert = invert;
    }

    public boolean accept(String value) {
      if (values.contains(value)) {
        return !invert;
      } else {
        return invert;
      }
    }
    
    @Override
    protected void initializeInternally(String... args) {
      this.values = new HashSet<String>();
      for (String string : args) {
        this.values.add(string);
      }
    }
    
  }
  
  
  
}
