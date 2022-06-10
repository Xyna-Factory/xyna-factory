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
package com.gip.xyna.utils.console;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 *
 */
public abstract class Colorizer {
  
  

  public abstract String getColor(String id);
  
  public abstract String normal();
  
  
  public static class ColorCycle extends Colorizer {
    
    private List<AnsiTextColor> cols = Arrays.asList( AnsiTextColor.values() ) ;
    private int currentColor = 1;
    private Map<String,String> colors = new HashMap<String,String>();

    public String getColor(String id) {
      String col = colors.get(id);
      if( col == null ) {
        col = cols.get(currentColor%cols.size()).getEscapeSequence();
        ++currentColor;
        colors.put(id, col);
      }
      return col;
    }

    public String normal() {
      return AnsiTextColor.normal.getEscapeSequence();
    }

  }
  
  public static class BlackOnly extends Colorizer {
    

    public String getColor(String id) {
      return "";
    }

    public String normal() {
      return "";
    }

  }
  
  

  public static Colorizer colorize(boolean colorize) {
    if( colorize ) {
      return new ColorCycle();
    } else {
      return new BlackOnly();
    }
  }
  

}
