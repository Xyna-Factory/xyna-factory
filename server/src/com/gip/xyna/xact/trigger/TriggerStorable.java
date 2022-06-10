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

package com.gip.xyna.xact.trigger;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import com.gip.xyna.utils.db.types.StringSerializable;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;


@Persistable(primaryKey = TriggerStorable.COLUMN_ID, tableName = TriggerStorable.TABLE_NAME)
public class TriggerStorable extends Storable<TriggerStorable> {

  private static final long serialVersionUID = 1L;

  private static final TriggerResultSetReader reader = new TriggerResultSetReader();

  public static final String TABLE_NAME = "triggers";
  public static final String COLUMN_ID = "id";
  public static final String COLUMN_REVISION = "revision";
  public static final String COLUMN_TRIGGER_NAME = "triggername";
  public static final String COLUMN_JAR_FILES = "jarfiles";
  public static final String COLUMN_FQ_TRIGGER_CLASSNAME = "fqtriggerclassname";
  public static final String COLUMN_SHARED_LIBS = "sharedlibs";
  public static final String COLUMN_DESCPRIPTION = "description";
  public static final String COLUMN_START_PARAMETER_DOCU = "startparameterdocumentation";
  public static final String COLUMN_STATE = "state";
  public static final String COLUMN_ERROR_CAUSE = "errorcause";

  public enum TriggerState implements StringSerializable<TriggerState>{
    OK, EMPTY, ERROR;
    
    public TriggerState deserializeFromString(String string) {
      return TriggerState.valueOf(string);
    }

    public String serializeToString() {
      return toString();
    }
  }
  
  @Column(name = COLUMN_ID, size=100)
  private String id;
  @Column(name = COLUMN_REVISION)
  private Long revision;
  @Column(name = COLUMN_TRIGGER_NAME, size=50)
  private String triggerName;
  @Column(name = COLUMN_FQ_TRIGGER_CLASSNAME, size=100)
  private String fqTriggerClassName;
  @Column(name = COLUMN_JAR_FILES, size=2000)
  private String jarFiles;
  @Column(name = COLUMN_SHARED_LIBS, size=500)
  private String sharedLibs;
  @Column(name = COLUMN_STATE)
  private String state; //kein enum, da sonst sql-Queries Werte nicht finden, falls nach dieser Spalte gefiltert wird
  @Column(name = COLUMN_ERROR_CAUSE, size=4000)
  private String errorCause;
  
  /**
   * @deprecated ist in trigger impl definiert
   * */ 
  @Deprecated
  @Column(name = COLUMN_DESCPRIPTION, size=1000)
  private String description;
  
  /**
   * @deprecated ist in trigger impl definiert
   * */ 
  @Deprecated
  @Column(name = COLUMN_START_PARAMETER_DOCU, size=2000)
  private String startParameterDocumentation;


  public TriggerStorable() {
  }


  public TriggerStorable(String triggerName, Long revision) {
    this.triggerName = triggerName;
    this.revision = revision;
    this.id = generateId(triggerName, revision);
  }


  public TriggerStorable(String triggerName, Long revision, String[] jarFiles, String fqTriggerClassName, String[] sharedLibs, TriggerState state) {

    this.triggerName = triggerName;
    this.fqTriggerClassName = fqTriggerClassName;

    StringBuilder sb = new StringBuilder();
    for (String s : jarFiles) {
      sb.append(s).append(":");
    }
    this.jarFiles = sb.toString();

    sb = new StringBuilder();
    for (String s : sharedLibs) {
      sb.append(s).append(":");
    }
    this.sharedLibs = sb.toString();
    this.revision = revision;
    this.id = generateId(triggerName, revision);
    
    this.state = state.toString();
  }

  private String generateId(String key, Long revision) {
    return key + "#" + revision;
  }  
  
  
  public String getTriggerName() {
    return triggerName;
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
      for (String jarFileName : jarFiles.split(":")) {
        jars.add(new File(RevisionManagement.getPathForRevision(PathType.TRIGGER, revision) 
                              + Constants.fileSeparator + jarFileName));
      }
    }
    
    return jars.toArray(new File[0]);
  }


  public String getFqTriggerClassName() {
    return fqTriggerClassName;
  }


  public String getSharedLibs() {
    return sharedLibs;
  }

  public String getState() {
    return state;
  }

  public TriggerState getStateAsEnum() {
    if (state == null) {
      return null;
    }
    return TriggerState.valueOf(state);
  }

  public String getErrorCause() {
    return errorCause;
  }
  
  @Deprecated
  public String getDescription() {
    return description;
  }

  @Deprecated
  public String getStartParameterDocumentation() {
    return startParameterDocumentation;
  }


  @Override
  public ResultSetReader<? extends TriggerStorable> getReader() {
    return reader;
  }


  @Override
  public <U extends TriggerStorable> void setAllFieldsFromData(U data) {
    TriggerStorable cast = data;
    triggerName = cast.triggerName;
    jarFiles = cast.jarFiles;
    fqTriggerClassName = cast.fqTriggerClassName;
    sharedLibs = cast.sharedLibs;
    description = cast.description;
    startParameterDocumentation = cast.startParameterDocumentation;
    id = cast.id;
    revision = cast.revision;
    state = cast.state;
    errorCause = cast.errorCause;
  }


  private static void setAllFieldsFromResultSet(TriggerStorable target, ResultSet rs) throws SQLException {
    target.triggerName = rs.getString(COLUMN_TRIGGER_NAME);
    target.jarFiles = rs.getString(COLUMN_JAR_FILES);
    target.fqTriggerClassName = rs.getString(COLUMN_FQ_TRIGGER_CLASSNAME);
    target.sharedLibs = rs.getString(COLUMN_SHARED_LIBS);
    target.description = rs.getString(COLUMN_DESCPRIPTION);
    target.startParameterDocumentation = rs.getString(COLUMN_START_PARAMETER_DOCU);
    target.id = rs.getString(COLUMN_ID);
    target.revision = rs.getLong(COLUMN_REVISION);
    target.state = rs.getString(COLUMN_STATE);
    target.errorCause = rs.getString(COLUMN_ERROR_CAUSE);
  }


  private static class TriggerResultSetReader implements ResultSetReader<TriggerStorable> {

    public TriggerStorable read(ResultSet rs) throws SQLException {
      TriggerStorable result = new TriggerStorable();
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


  
  public void setTriggerName(String triggerName) {
    this.triggerName = triggerName;
  }


  
  public void setFqTriggerClassName(String fqTriggerClassName) {
    this.fqTriggerClassName = fqTriggerClassName;
  }

  public void setState(TriggerState state) {
    this.state = state.toString();
    if (state != TriggerState.ERROR) {
      errorCause = null;
    }
  }

  public void setErrorCause(String errorCause) {
    this.errorCause = errorCause;
  }

  public void setError(Throwable t) {
    this.state = TriggerState.ERROR.toString();
    String errorCause = TriggerStorage.getErrorCause(t);
    this.errorCause = errorCause;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }


  
  public void setStartParameterDocumentation(String startParameterDocumentation) {
    this.startParameterDocumentation = startParameterDocumentation;
  }


  public String[] getSharedLibsAsArray() {
    String[] arr = new String[0];
    if (sharedLibs != null && sharedLibs.contains(":")) {
      arr = sharedLibs.split(":");
    }
    return arr;
  }

}
