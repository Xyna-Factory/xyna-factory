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

package com.gip.xyna.update.outdatedclasses_5_1_5_1;



import java.sql.ResultSet;
import java.sql.SQLException;

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
  public static final String COLUMN_ENABLED = "enabled";
  public static final String COLUMN_REVISION = "revision";
  public static final String COLUMN_DISABLED_AUTOMATICALLY = "disabledautomatically";

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
  @Column(name = COLUMN_ENABLED)
  private boolean enabled;
  @Column(name = COLUMN_REVISION)
  private Long revision;
  @Column(name = COLUMN_DISABLED_AUTOMATICALLY)
  private Boolean disabledautomatically;
  

  public FilterInstanceStorable() {
  }


  public FilterInstanceStorable(String filterInstanceName, Long revision) {
    this.filterInstanceName = filterInstanceName;
    this.revision = revision;
    this.id = generateId(filterInstanceName, revision);
  }


  public FilterInstanceStorable(String filterInstanceName, Long revision, String filterName, String triggerInstanceName,
                                String description, boolean enabled) {
    this.filterInstanceName = filterInstanceName;
    this.filterName = filterName;
    this.triggerInstanceName = triggerInstanceName;
    this.description = description;
    this.enabled = enabled;
    this.revision = revision;
    this.id = generateId(filterInstanceName, revision);
    if(!enabled) {
      this.disabledautomatically = false;
    }
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


  public String getTriggerInstanceName() {
    return triggerInstanceName;
  }


  public String getDescription() {
    return description;
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
    enabled = cast.enabled;
    id = cast.id;
    revision = cast.revision;
    disabledautomatically = cast.disabledautomatically;
  }


  private static void setAllFieldsFromResultSet(FilterInstanceStorable target, ResultSet rs) throws SQLException {
    target.filterInstanceName = rs.getString(COLUMN_FILTER_INSTANCE_NAME);
    target.filterName = rs.getString(COLUMN_FILTER_NAME);
    target.triggerInstanceName = rs.getString(COLUMN_TRIGGER_INSTANCE_NAME);
    target.description = rs.getString(COLUMN_DESCPRIPTION);
    target.enabled = rs.getBoolean(COLUMN_ENABLED);
    target.id = rs.getString(COLUMN_ID);
    target.revision = rs.getLong(COLUMN_REVISION);
    target.disabledautomatically = rs.getBoolean(COLUMN_DISABLED_AUTOMATICALLY);
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


  /**
   * FIXME m�sste das nicht eigtl enableautomatically heissen?
   */
  public Boolean isDisabledautomatically() {
    return disabledautomatically;
  }


  public void setDisabledautomatically(Boolean disabledautomatically) {
    this.disabledautomatically = disabledautomatically;
  }


}
