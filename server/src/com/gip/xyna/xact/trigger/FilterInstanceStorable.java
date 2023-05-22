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
import java.util.Collections;
import java.util.List;

import com.gip.xyna.utils.collections.lists.StringSerializableList;
import com.gip.xyna.utils.db.types.StringSerializable;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(primaryKey = FilterInstanceStorable.COLUMN_ID, tableName = FilterInstanceStorable.TABLE_NAME)
public class FilterInstanceStorable extends Storable<FilterInstanceStorable> {

  private static final long serialVersionUID = 1L;

  private static final FilterInstanceResultSetReader reader = new FilterInstanceResultSetReader();

  public static final String TABLE_NAME = "filterinstances";
  public static final String COLUMN_ID = "id";
  public static final String COLUMN_FILTER_NAME = "filterename";
  public static final String COLUMN_FILTER_INSTANCE_NAME = "filterinstancename";
  public static final String COLUMN_TRIGGER_INSTANCE_NAME = "triggerinstancename";
  public static final String COLUMN_DESCPRIPTION = "description";
  public static final String COLUMN_REVISION = "revision";
  public static final String COLUMN_STATE = "state";
  public static final String COLUMN_ERROR_CAUSE = "errorcause";
  public static final String COLUMN_OPTIONAL = "optional";
  public static final String COLUMN_CONFIGURATION = "configuration";

  @Column(name = COLUMN_ID, size=100)
  private String id;
  @Column(name = COLUMN_FILTER_INSTANCE_NAME, size=50)
  private String filterInstanceName;
  @Column(name = COLUMN_FILTER_NAME, size=50)
  private String filterName;
  @Column(name = COLUMN_TRIGGER_INSTANCE_NAME, size=50)
  private String triggerInstanceName;
  @Column(name = COLUMN_DESCPRIPTION, size=1000)
  private String description;
  @Column(name = COLUMN_REVISION)
  private Long revision;
  @Column(name = COLUMN_STATE)
  private String state; //kein enum, da sonst sql-Queries Werte nicht finden, falls nach dieser Spalte gefiltert wird
  @Column(name = COLUMN_ERROR_CAUSE, size=4000)
  private String errorCause;
  @Column(name = COLUMN_OPTIONAL)
  private boolean optional;
  @Column(name = COLUMN_CONFIGURATION, size=500)
  private final StringSerializableList<String> configuration = StringSerializableList.autoSeparator(String.class);

  public enum FilterInstanceState implements StringSerializable<FilterInstanceState>{
    ENABLED, DISABLED, ERROR;
    
    public FilterInstanceState deserializeFromString(String string) {
      return FilterInstanceState.valueOf(string);
    }

    public String serializeToString() {
      return toString();
    }
  }
  
  
  public FilterInstanceStorable() {
  }


  public FilterInstanceStorable(String filterInstanceName, Long revision) {
    this.filterInstanceName = filterInstanceName;
    this.revision = revision;
    this.id = generateId(filterInstanceName, revision);
  }
  
  public FilterInstanceStorable(DeployFilterParameter deployFilterParameter) {
    this.filterInstanceName = deployFilterParameter.getInstanceName();
    this.filterName = deployFilterParameter.getFilterName();
    this.triggerInstanceName = deployFilterParameter.getTriggerInstanceName();
    this.description = deployFilterParameter.getDescription();
    this.state = FilterInstanceState.ENABLED.toString();
    this.revision = deployFilterParameter.getRevision();
    this.id = generateId(filterInstanceName, revision);
    this.optional = deployFilterParameter.isOptional();
    if( deployFilterParameter.getConfiguration() != null ) {
      this.configuration.setValues(deployFilterParameter.getConfiguration());
    }
  }
  
  public void setEnabled(boolean enabled) {
    this.state = enabled ? FilterInstanceState.ENABLED.toString() : FilterInstanceState.DISABLED.toString();
  }

  private String generateId(String key, Long revision) {
    return key + "#" + revision;
  }

  @Override
  public Object getPrimaryKey() {
    return id;
  }


  public String getFilterInstanceName() {
    return filterInstanceName;
  }


  public String getFilterName() {
    return filterName;
  }

  /**
   * getter f�r MemoryPersistenceLayer: Tippfehler in ColumnNames
   */
  public String getFiltereName() {
    return filterName;
  }
  
  
  public String getTriggerInstanceName() {
    return triggerInstanceName;
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
    return getStateAsEnum() == FilterInstanceState.ENABLED;
  }
  
  
  public FilterInstanceState getStateAsEnum() {
    if (state == null) {
      return null;
    }
    return FilterInstanceState.valueOf(state);
  }

  public String getState() {
    return state;
  }
  
  
  public String getErrorCause() {
    return errorCause;
  }
  
  
  public Boolean isOptional() {
    return optional;
  }

  @Override
  public ResultSetReader<? extends FilterInstanceStorable> getReader() {
    return reader;
  }


  @Override
  public <U extends FilterInstanceStorable> void setAllFieldsFromData(U data) {
    FilterInstanceStorable cast = data;
    filterInstanceName = cast.filterInstanceName;
    filterName = cast.filterName;
    triggerInstanceName = cast.triggerInstanceName;
    description = cast.description;
    state = cast.state;
    id = cast.id;
    revision = cast.revision;
    errorCause = cast.errorCause;
    optional = cast.optional;
    configuration.setValues(cast.getConfiguration());
  }


  private static void setAllFieldsFromResultSet(FilterInstanceStorable target, ResultSet rs) throws SQLException {
    target.filterInstanceName = rs.getString(COLUMN_FILTER_INSTANCE_NAME);
    target.filterName = rs.getString(COLUMN_FILTER_NAME);
    target.triggerInstanceName = rs.getString(COLUMN_TRIGGER_INSTANCE_NAME);
    target.description = rs.getString(COLUMN_DESCPRIPTION);
    target.state = rs.getString(COLUMN_STATE);
    target.id = rs.getString(COLUMN_ID);
    target.revision = rs.getLong(COLUMN_REVISION);
    target.errorCause = rs.getString(COLUMN_ERROR_CAUSE);
    target.optional = rs.getBoolean(COLUMN_OPTIONAL);
    target.configuration.deserializeFromString(rs.getString(COLUMN_CONFIGURATION));
  }


  private static class FilterInstanceResultSetReader implements ResultSetReader<FilterInstanceStorable> {

    public FilterInstanceStorable read(ResultSet rs) throws SQLException {
      FilterInstanceStorable result = new FilterInstanceStorable();
      setAllFieldsFromResultSet(result, rs);
      return result;
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


  public void setRevision(Long revision) {
    this.revision = revision;
  }


  public void setFilterInstanceName(String filterInstanceName) {
    this.filterInstanceName = filterInstanceName;
  }


  public void setFilterName(String filterName) {
    this.filterName = filterName;
  }


  public void setTriggerInstanceName(String triggerInstanceName) {
    this.triggerInstanceName = triggerInstanceName;
  }


  public void setDescription(String description) {
    this.description = description;
  }

  public void setState(FilterInstanceState state) {
    this.state = state.toString();
    if (state != FilterInstanceState.ERROR) {
      errorCause = null;
    }
  }
  
  public void setError(Throwable t) {
    this.state = FilterInstanceState.ERROR.toString();
    String errorCause = TriggerStorage.getErrorCause(t);
    this.errorCause = errorCause;
  }

  public void setOptional(Boolean optional) {
    this.optional = optional;
  }
  
  public List<String> getConfiguration() {
    return Collections.unmodifiableList(configuration);
  }
}
