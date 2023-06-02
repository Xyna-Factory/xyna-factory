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

package com.gip.xyna.update.outdatedclasses_5_1_5_1;



import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(primaryKey = TriggerInstanceStorable.COLUMN_ID, tableName = TriggerInstanceStorable.TABLE_NAME)
public class TriggerInstanceStorable extends Storable<TriggerInstanceStorable> {

  private static final long serialVersionUID = 1L;

  private static final TriggerInstanceResultSetReader reader = new TriggerInstanceResultSetReader();

  public static final String TABLE_NAME = "triggerinstances";
  public static final String COLUMN_ID = "id";
  public static final String COLUMN_REVISION = "revision";
  public static final String COLUMN_TRIGGER_INSTANCE_NAME = "triggerinstancename";
  public static final String COLUMN_TRIGGER_NAME = "triggername";
  public static final String COLUMN_START_PARAMETER = "startparameter";
  public static final String COLUMN_DESCPRIPTION = "description";
  
  public static final String COLUMN_ENABLED = "enable";
  public static final String COLUMN_DISABLED_AUTOMATICALLY = "disabledautomatically";
  

  @Column(name = COLUMN_ID, size=100)
  private String id;
  @Column(name = COLUMN_REVISION)
  private Long revision;
  @Column(name = COLUMN_TRIGGER_INSTANCE_NAME, size=50)
  private String triggerInstanceName;
  @Column(name = COLUMN_TRIGGER_NAME, size=50)
  private String triggerName;
  @Column(name = COLUMN_START_PARAMETER, size=500)
  private String startParameter;
  @Column(name = COLUMN_DESCPRIPTION, size=1000)
  private String description;
  @Column(name = COLUMN_ENABLED)
  private boolean enabled;
  @Column(name = COLUMN_DISABLED_AUTOMATICALLY)
  private Boolean disabledautomatically;

  public TriggerInstanceStorable() {
  }

  public TriggerInstanceStorable(String triggerInstanceName, Long revision) {
    this.triggerInstanceName = triggerInstanceName;
    this.revision = revision;
    this.id = generateId(triggerInstanceName, revision);
  }

  public TriggerInstanceStorable(String triggerInstanceName, Long revision, String triggerName, String[] startParameter,
                                 String description, boolean enabled) {
    this.triggerInstanceName = triggerInstanceName;
    this.triggerName = triggerName;
    StringBuilder sb = new StringBuilder();
    char separatorChar = getSeparatorChar(startParameter);
    
    if ( startParameter != null ) {
      for (String s : startParameter) {
        sb.append(s).append(separatorChar);
      }
    }
    
    this.startParameter = sb.toString();
    this.description = description;
    this.enabled = enabled;
    this.revision = revision;
    this.id = generateId(triggerInstanceName, revision);
    if(!enabled) {
      this.disabledautomatically = false;
    }
  }


  private String generateId(String key, Long revision) {
    return key + "#" + revision;
  }
  
  private static final char[] POSSIBLESEPARATORCHARS =
      new char[] {':', '|', '/', ';', '\\', '@', '-', '_', '.', '+', '#', '=', '[', ']', '?', '§', '$', '%', '&', '!'};


  /**
   * suche das erste "freie" separator zeichen, dass nicht bereits in den eigentlichen startparametern benutzt wird
   */
  private static char getSeparatorChar(String[] startParameter) {
    for (char sepChar : POSSIBLESEPARATORCHARS) {
      boolean foundChar = false;
      
      if (startParameter != null) {
        for (String startPara : startParameter) {
          if (startPara.indexOf(sepChar) > -1) {
            foundChar = true;
            break;
          }
        }
      }
      
      if (!foundChar) {
        return sepChar;
      }
    }
    throw new RuntimeException("didn't find any possible separator character for start parameter persistence.");
  }

  @Override
  public Object getPrimaryKey() {
    return id;
  }


  public String getTriggerInstanceName() {
    return triggerInstanceName;
  }


  public String getTriggerName() {
    return triggerName;
  }


  public String getDescription() {
    return description;
  }


  public String getStartParameter() {
    return startParameter;
  }
  
  
  public boolean isEnabled() {
    return enabled;
  }
  
  
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    if(enabled) {
      disabledautomatically = null;
    }
  }


  @Override
  public ResultSetReader<? extends TriggerInstanceStorable> getReader() {
    return reader;
  }


  @Override
  public <U extends TriggerInstanceStorable> void setAllFieldsFromData(U data) {
    TriggerInstanceStorable cast = data;
    triggerInstanceName = cast.triggerInstanceName;
    triggerName = cast.triggerName;
    description = cast.description;
    startParameter = cast.startParameter;
    enabled = cast.enabled;
    id = cast.id;
    revision = cast.revision;
    disabledautomatically = cast.disabledautomatically;
  }


  private static void setAllFieldsFromResultSet(TriggerInstanceStorable target, ResultSet rs) throws SQLException {
    target.triggerInstanceName = rs.getString(COLUMN_TRIGGER_INSTANCE_NAME);
    target.triggerName = rs.getString(COLUMN_TRIGGER_NAME);
    target.description = rs.getString(COLUMN_DESCPRIPTION);
    target.startParameter = rs.getString(COLUMN_START_PARAMETER);
    target.enabled = rs.getBoolean(COLUMN_ENABLED);
    target.id = rs.getString(COLUMN_ID);
    target.revision = rs.getLong(COLUMN_REVISION);
    target.disabledautomatically = rs.getBoolean(COLUMN_DISABLED_AUTOMATICALLY);
  }


  private static class TriggerInstanceResultSetReader implements ResultSetReader<TriggerInstanceStorable> {

    public TriggerInstanceStorable read(ResultSet rs) throws SQLException {
      TriggerInstanceStorable result = new TriggerInstanceStorable();
      setAllFieldsFromResultSet(result, rs);
      return result;
    }

  }

  public static String[] getStartParameterArray(String startParameterString) {
    ArrayList<String> paras = new ArrayList<String>();
    //trennzeichen ist das letzte zeichen.
    //ausnahme: abwärtskompatibilität. früher gab es nur den doppelpunkt. den konnte man aber auch weglassen 
    //wenn man den wert manuell im persistencelayer geändert hat (zb im xml) und es hatte trotzdem funktioniert,
    String splitRegex = startParameterString.substring(startParameterString.length()-1, startParameterString.length());
    boolean validChar = false;
    for (char c : POSSIBLESEPARATORCHARS) {
      if (splitRegex.equals(String.valueOf(c))) {
        validChar = true;
        break;
      }
    }
    if (validChar) {
      splitRegex = "\\" + splitRegex; //kann in regexp ein funktionales zeichen sein, also escapen
    } else {
      splitRegex = ":"; //abwärtskompatibel
    }
    String[] parts = startParameterString.split(splitRegex);
    for (String part : parts) {
      if (part.length() > 0) {
        paras.add(part);
      }
    }
    return paras.toArray(new String[paras.size()]);
  }


  public String[] getStartParameterArray() {
    if (startParameter.length() > 0) {
      return getStartParameterArray(startParameter);
    } else {
      return new String[0];
    }
  }

  
  public String getId() {
    return id;
  }

  
  public void setId(String id) {
    this.id = id;
  }

  
  public Long getRevision() {
    return revision;
  }

  
  public void setTriggerInstanceName(String triggerInstanceName) {
    this.triggerInstanceName = triggerInstanceName;
  }

  
  public void setTriggerName(String triggerName) {
    this.triggerName = triggerName;
  }

  
  public void setStartParameter(String startParameter) {
    this.startParameter = startParameter;
  }

  
  public void setDescription(String description) {
    this.description = description;
  }

  
  public Boolean isDisabledautomatically() {
    return disabledautomatically;
  }

  
  public void setDisabledautomatically(Boolean disabledautomatically) {
    this.disabledautomatically = disabledautomatically;
  }
  
}
