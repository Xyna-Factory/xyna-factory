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

package com.gip.xyna.xact.trigger;



import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import com.gip.xyna.utils.collections.lists.StringSerializableList;
import com.gip.xyna.utils.db.types.StringSerializable;
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
  
  public static final String COLUMN_STATE = "state";
  public static final String COLUMN_ERROR_CAUSE = "errorcause";
  
  
  
  public enum TriggerInstanceState implements StringSerializable<TriggerInstanceState>{
    ENABLED, DISABLED, ERROR;
    
    public TriggerInstanceState deserializeFromString(String string) {
      return TriggerInstanceState.valueOf(string);
    }

    public String serializeToString() {
      return toString();
    }
  }
  
  
  @Column(name = COLUMN_ID, size=100)
  private String id;
  @Column(name = COLUMN_REVISION)
  private Long revision;
  @Column(name = COLUMN_TRIGGER_INSTANCE_NAME, size=50)
  private String triggerInstanceName;
  @Column(name = COLUMN_TRIGGER_NAME, size=50)
  private String triggerName;
  @Column(name = COLUMN_START_PARAMETER, size=500)
  private final StringSerializableList<String> startParameter = StringSerializableList.autoSeparator(String.class, ":|/;\\@-_.+#=[]?�$%&!", ':');
  // hier wird � noch als Separator verwendet!
  //suche das erste "freie" separator zeichen, dass nicht bereits in den eigentlichen startparametern benutzt wird
  //ausnahme: abw�rtskompatibilit�t. fr�her gab es nur den doppelpunkt. den konnte man aber auch weglassen 
  //wenn man den wert manuell im persistencelayer ge�ndert hat (zb im xml) und es hatte trotzdem funktioniert,

  @Column(name = COLUMN_DESCPRIPTION, size=1000)
  private String description;
  @Column(name = COLUMN_STATE)
  private String state; //kein enum, da sonst sql-Queries Werte nicht finden, falls nach dieser Spalte gefiltert wird
  @Column(name = COLUMN_ERROR_CAUSE, size=4000)
  private String errorCause;

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
    this.startParameter.setValues(Arrays.asList(startParameter));
    this.description = description;
    this.state = enabled ? TriggerInstanceState.ENABLED.toString() : TriggerInstanceState.DISABLED.toString();
    this.revision = revision;
    this.id = generateId(triggerInstanceName, revision);
  }


  private String generateId(String key, Long revision) {
    return key + "#" + revision;
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
  
  /**
   * @deprecated use getState instead
   * @return
   */
  @Deprecated
  public boolean isEnabled() {
    return getStateAsEnum() == TriggerInstanceState.ENABLED;
  }
  
  public String getState() {
    return state;
  }

  public TriggerInstanceState getStateAsEnum() {
    if (state == null) {
      return null;
    }
    return TriggerInstanceState.valueOf(state);
  }
  
  
  public void setState(TriggerInstanceState state) {
    this.state = state.toString();
    if (state != TriggerInstanceState.ERROR) {
      errorCause = null;
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
    startParameter.setValues(cast.startParameter);
    state = cast.state;
    id = cast.id;
    revision = cast.revision;
    errorCause = cast.errorCause;
  }


  private static void setAllFieldsFromResultSet(TriggerInstanceStorable target, ResultSet rs) throws SQLException {
    target.triggerInstanceName = rs.getString(COLUMN_TRIGGER_INSTANCE_NAME);
    target.triggerName = rs.getString(COLUMN_TRIGGER_NAME);
    target.description = rs.getString(COLUMN_DESCPRIPTION);
    target.startParameter.deserializeFromString(rs.getString(COLUMN_START_PARAMETER));
    target.state = rs.getString(COLUMN_STATE);
    target.id = rs.getString(COLUMN_ID);
    target.revision = rs.getLong(COLUMN_REVISION);
    target.errorCause = rs.getString(COLUMN_ERROR_CAUSE);
  }


  private static class TriggerInstanceResultSetReader implements ResultSetReader<TriggerInstanceStorable> {

    public TriggerInstanceStorable read(ResultSet rs) throws SQLException {
      TriggerInstanceStorable result = new TriggerInstanceStorable();
      setAllFieldsFromResultSet(result, rs);
      return result;
    }

  }
  
  public void setStartParameter(String startParameter) {
    this.startParameter.deserializeFromString(startParameter);
  }
  public String getStartParameter() {
    return startParameter.serializeToString();
  }

  public List<String> getStartParameters() {
    return startParameter;
  }

  public static String[] getStartParameterArray(String startParameterString) {
    TriggerInstanceStorable tis = new TriggerInstanceStorable();
    tis.startParameter.deserializeFromString(startParameterString);
    return tis.getStartParameterArray(); 
  }
  
  public String[] getStartParameterArray() {
    return startParameter.toArray(new String[startParameter.size()]);
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

  
  public void setDescription(String description) {
    this.description = description;
  }

  
  public String getErrorCause() {
    return errorCause;
  }
  
  public void setErrorCause(String errorCause) {
    this.errorCause = errorCause;
  }

  public void setError(Throwable t) {
    this.state = TriggerInstanceState.ERROR.toString();
    String errorCause = TriggerStorage.getErrorCause(t);
    this.errorCause = errorCause;
  }

}
