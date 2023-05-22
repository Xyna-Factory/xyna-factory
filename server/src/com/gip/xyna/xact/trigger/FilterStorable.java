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

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.gip.xyna.utils.StringUtils;
import com.gip.xyna.utils.db.types.StringSerializable;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(primaryKey = FilterStorable.COLUMN_ID, tableName = FilterStorable.TABLE_NAME)
public class FilterStorable extends Storable<FilterStorable> {

  private static final long serialVersionUID = 1L;

  private static final FilterResultSetReader reader = new FilterResultSetReader();

  public static final String TABLE_NAME = "filters";
  
  public static final String COLUMN_FILTER_NAME = "filtername";
  public static final String COLUMN_ID = "id";
  public static final String COLUMN_REVISION = "revision";
  public static final String COLUMN_JAR_FILES = "jarfiles";
  public static final String COLUMN_FQ_FILTER_CLASSNAME = "fqfilterclassname";
  public static final String COLUMN_TRIGGER_NAME = "triggername";
  public static final String COLUMN_SHARED_LIBS = "sharedlibs";
  public static final String COLUMN_DESCPRIPTION = "description";
  public static final String COLUMN_STATE = "state";
  public static final String COLUMN_ERROR_CAUSE = "errorcause";

  @Column(name = COLUMN_ID, size=100)
  private String id;
  @Column(name = COLUMN_REVISION)
  private Long revision;
  @Column(name = COLUMN_FILTER_NAME, size=50)
  private String filterName;
  @Column(name = COLUMN_FQ_FILTER_CLASSNAME, size=100)
  private String fqFilterClassName;
  @Column(name = COLUMN_JAR_FILES, size=2000)
  private String jarFiles;
  @Column(name = COLUMN_TRIGGER_NAME, size=50)
  private String triggerName;
  @Column(name = COLUMN_SHARED_LIBS, size=500)
  private String sharedLibs;
  @Column(name = COLUMN_DESCPRIPTION, size=1000)
  private String description;
  @Column(name = COLUMN_STATE)
  private String state; //kein enum, da sonst sql-Queries Werte nicht finden, falls nach dieser Spalte gefiltert wird
  @Column(name = COLUMN_ERROR_CAUSE, size=4000)
  private String errorCause;

  public enum FilterState implements StringSerializable<FilterState>{
    OK, EMPTY, ERROR;
    
    public FilterState deserializeFromString(String string) {
      return FilterState.valueOf(string);
    }

    public String serializeToString() {
      return toString();
    }
  }

  public FilterStorable() {
  }


  public FilterStorable(String filterName, Long revision) {
    this.filterName = filterName;
    this.revision = revision;
    this.id = generateId(filterName, revision);
  }


  public FilterStorable(String filterName, Long revision, String[] jarFiles, String fqFilterClassName, String triggerName,
                        String[] sharedLibs, String description, FilterState state) {

    this.filterName = filterName;
    this.fqFilterClassName = fqFilterClassName;
    this.jarFiles = StringUtils.joinStringArray(jarFiles, ":");
    this.triggerName = triggerName;
    this.sharedLibs = StringUtils.joinStringArray(sharedLibs, ":");
    this.description = description;
    this.revision = revision;
    this.id = generateId(filterName, revision);
    this.state = state.toString();
  }


  private String generateId(String key, Long revision) {
    return key + "#" + revision;
  }

 
  public String getFilterName() {
    return filterName;
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }


  public String getJarFiles() {
    return jarFiles;
  }
  
  public File[] getJarFilesAsArray() {
    ArrayList<File> jars = new ArrayList<File>();
    if (jarFiles != null && jarFiles.length() > 0) {
      for (String s : jarFiles.split(":")) {
        jars.add(new File(RevisionManagement.getPathForRevision(PathType.FILTER, revision) + Constants.fileSeparator
            + s));
      }
    }
    
    return jars.toArray(new File[0]);
  }

  public String getTriggerName() {
    return triggerName;
  }


  public String getSharedLibs() {
    return sharedLibs;
  }

  
  public String[] getSharedLibsArray() {
    if ( sharedLibs.length() > 0 ) {
      return sharedLibs.split(":");
    } else {
      return new String[0];
    }
  }
  

  public String getDescription() {
    return description;
  }

  
  public String getState() {
    return state;
  }

  public FilterState getStateAsEnum() {
    if (state == null) {
      return null;
    }
    return FilterState.valueOf(state);
  }
  
  
  public String getErrorCause() {
    return errorCause;
  }
  
  
  @Override
  public ResultSetReader<? extends FilterStorable> getReader() {
    return reader;
  }


  @Override
  public <U extends FilterStorable> void setAllFieldsFromData(U data) {
    FilterStorable cast = data;
    filterName = cast.filterName;
    jarFiles = cast.jarFiles;
    fqFilterClassName = cast.fqFilterClassName;
    triggerName = cast.triggerName;
    sharedLibs = cast.sharedLibs;
    description = cast.description;
    id = cast.id;
    revision = cast.revision;
    state = cast.state;
    errorCause = cast.errorCause;
  }


  private static void setAllFieldsFromResultSet(FilterStorable target, ResultSet rs) throws SQLException {
    target.filterName = rs.getString(COLUMN_FILTER_NAME);
    target.jarFiles = rs.getString(COLUMN_JAR_FILES);
    target.fqFilterClassName = rs.getString(COLUMN_FQ_FILTER_CLASSNAME);
    target.triggerName = rs.getString(COLUMN_TRIGGER_NAME);
    target.sharedLibs = rs.getString(COLUMN_SHARED_LIBS);
    target.description = rs.getString(COLUMN_DESCPRIPTION);
    target.id = rs.getString(COLUMN_ID);
    target.revision = rs.getLong(COLUMN_REVISION);
    target.state = rs.getString(COLUMN_STATE);
    target.errorCause = rs.getString(COLUMN_ERROR_CAUSE);
  }


  private static class FilterResultSetReader implements ResultSetReader<FilterStorable> {

    public FilterStorable read(ResultSet rs) throws SQLException {
      FilterStorable result = new FilterStorable();
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


  
  public void setJarFiles(String jarFiles) {
    this.jarFiles = jarFiles;
  }


  
  public void setSharedLibs(String sharedLibs) {
    this.sharedLibs = sharedLibs;
  }


  
  public void setFilterName(String filterName) {
    this.filterName = filterName;
  }


  
  public void setTriggerName(String triggerName) {
    this.triggerName = triggerName;
  }


  
  public void setDescription(String description) {
    this.description = description;
  }


  
  public String getFqFilterClassName() {
    return fqFilterClassName;
  }


  
  public void setFqFilterClassName(String fqFilterClassName) {
    this.fqFilterClassName = fqFilterClassName;
  }

  
  public void setState(FilterState state) {
    this.state = state.toString();
    if (state != FilterState.ERROR) {
      errorCause = null;
    }
  }
  
  public void setErrorCause(String errorCause) {
    this.errorCause = errorCause;
  }
  
  public void setError(Throwable t) {
    this.state = FilterState.ERROR.toString();
    String errorCause = TriggerStorage.getErrorCause(t);
    this.errorCause = errorCause;
  }
}
