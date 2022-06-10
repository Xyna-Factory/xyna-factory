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
package com.gip.xyna.xprc.xsched.timeconstraint.windows;

import java.io.Serializable;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.utils.db.types.StringSerializable;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.SimplePeriodicTimeWindow.SimplePeriodicTimeWindowDefinition;


/**
 * Definition eines {@link TimeWindow}s. 
 * Diese Basisklasse ist abstrakt und immutable. Konkrete TimeWindowDefinition-Klassen sollten immutable sein.
 */
public abstract class TimeWindowDefinition implements Serializable, StringSerializable<TimeWindowDefinition> {
  private static final long serialVersionUID = 1L;

  private static HashMap<String,TimeWindowDefinition> definitionTypes = new HashMap<String,TimeWindowDefinition>();
  protected static void addTimeWindowDefinitionType( TimeWindowDefinition type) {
    definitionTypes.put( type.getType(), type );
  }
  static {
    //TODO leider funktioniert keine Lazy-Initialisierung von den abgeleiteten Typen aus
    addTimeWindowDefinitionType(new SimplePeriodicTimeWindowDefinition("",0));
    addTimeWindowDefinitionType(new RestrictionBasedTimeWindow.RestrictionBasedTimeWindowDefinition("", "", false));
  }


  private static final String P_DEFINITION = "(\\w+)\\(.*\\)";
  private static final Pattern PATTERN_DEFINITION = Pattern.compile(P_DEFINITION);
  
  
 
  public abstract TimeWindow constructTimeWindow();
  
  public abstract TimeWindowDefinition deserializeFromString(String string);

  public abstract String serializeToString();
  public abstract String getType();
  
  
  public static TimeWindowDefinition valueOf(String string) {
    if( string == null ) {
      return null;
    }
    Matcher m = PATTERN_DEFINITION.matcher(string);
    if( ! m.matches() ) {
      throw new IllegalArgumentException("\""+string +"\" does not match pattern \""+P_DEFINITION+"\"");
    }
    
    TimeWindowDefinition type = definitionTypes.get(m.group(1));
    if( type == null ) {
      throw new IllegalArgumentException("Unknown TimeWindowDefinition-Type \""+m.group(1)+"\", known: "+definitionTypes.keySet() );
    }
    
    return type.deserializeFromString(string);
  }
  
  
}
