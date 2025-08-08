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
package xmcp.gitintegration.impl;



import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(primaryKey = RepositoryConnectionStorable.COL_WORKSPACENAME, tableName = RepositoryConnectionStorable.TABLE_NAME)
public class RepositoryConnectionStorable extends Storable<RepositoryConnectionStorable> {

  private static final long serialVersionUID = -1L;

  public static final String TABLE_NAME = "repositoryconnectionstorable";

  public static final String COL_WORKSPACENAME = "workspacename";
  public static final String COL_PATH = "path";
  public static final String COL_SUBPATH = "subpath";
  public static final String COL_SAVEDINREPO = "savedinrepo";
  @Deprecated
  public static final String COL_SPLITTED = "splitted";
  public static final String COL_SPLITTYPE = "splittype";

  @Column(name = COL_WORKSPACENAME)
  private String workspacename;

  @Column(name = COL_PATH)
  private String path;

  @Column(name = COL_SUBPATH)
  private String subpath;

  @Column(name = COL_SAVEDINREPO)
  private boolean savedinrepo;

  @Deprecated
  @Column(name = COL_SPLITTED)
  private boolean split;
  
  @Column(name = COL_SPLITTYPE)
  private String splittype;


  public RepositoryConnectionStorable() {
    super();
  }


  public RepositoryConnectionStorable(String workspacename) {
    this();
    this.workspacename = workspacename;
  }


  public RepositoryConnectionStorable(String workspacename, String path, String subpath, boolean savedinrepo, String splitted) {
    this(workspacename);
    this.path = path;
    this.subpath = subpath;
    this.savedinrepo = savedinrepo;
    this.splittype = splitted;
  }


  @Override
  public String getPrimaryKey() {
    return workspacename;
  }


  private static FactoryContentDifferencesStorableReader reader = new FactoryContentDifferencesStorableReader();


  @Override
  public ResultSetReader<? extends RepositoryConnectionStorable> getReader() {
    return reader;
  }


  private static class FactoryContentDifferencesStorableReader implements ResultSetReader<RepositoryConnectionStorable> {

    @Override
    public RepositoryConnectionStorable read(ResultSet rs) throws SQLException {
      RepositoryConnectionStorable result = new RepositoryConnectionStorable();
      result.workspacename = rs.getString(COL_WORKSPACENAME);
      result.path = rs.getString(COL_PATH);
      result.subpath = rs.getString(COL_SUBPATH);
      result.savedinrepo = rs.getBoolean(COL_SAVEDINREPO);
      result.splittype = rs.getString(COL_SPLITTYPE);
      if(result.splittype == null) {
        boolean old = rs.getBoolean(COL_SPLITTED);
        result.splittype = old ? WorkspaceConfigSplit.BYTYPE.getId() : WorkspaceConfigSplit.NONE.getId();
      }
      return result;
    }
  }


  @Override
  public <U extends RepositoryConnectionStorable> void setAllFieldsFromData(U data) {
    RepositoryConnectionStorable cast = data;
    workspacename = cast.workspacename;
    path = cast.path;
    subpath = cast.subpath;
    savedinrepo = cast.savedinrepo;
    splittype = cast.splittype;
  }


  public String getWorkspacename() {
    return workspacename;
  }


  public void setWorkspacename(String workspacename) {
    this.workspacename = workspacename;
  }


  public String getPath() {
    return path;
  }


  public void setPath(String path) {
    this.path = path;
  }


  public String getSubpath() {
    return subpath;
  }


  public void setSubpath(String subpath) {
    this.subpath = subpath;
  }


  public void setSavedinrepo(boolean savedinrepo) {
    this.savedinrepo = savedinrepo;
  }


  public boolean getSavedinrepo() {
    return savedinrepo;
  }


  @Deprecated
  public void setSplitted(boolean splitted) {
    this.split = splitted;
  }


  @Deprecated
  public boolean getSplitted() {
    return split;
  }

  public void setSplittype(String splitted) {
    this.splittype = splitted;
  }


  public String getSplittype() {
    return splittype;
  }
}
