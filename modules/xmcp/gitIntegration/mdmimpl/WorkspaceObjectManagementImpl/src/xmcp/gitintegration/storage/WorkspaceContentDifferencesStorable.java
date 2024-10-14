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
package xmcp.gitintegration.storage;



import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(primaryKey = WorkspaceContentDifferencesStorable.COL_LISTID, tableName = WorkspaceContentDifferencesStorable.TABLE_NAME)
public class WorkspaceContentDifferencesStorable extends Storable<WorkspaceContentDifferencesStorable> {

  private static final long serialVersionUID = -1L;

  public static final String TABLE_NAME = "workspacecontentdifferencesstorable";

  public static final String COL_LISTID = "listid";
  public static final String COL_WORKSPACENAME = "workspacename";


  @Column(name = COL_LISTID)
  private Long listid;
  @Column(name = COL_WORKSPACENAME)
  private String workspacename;


  public WorkspaceContentDifferencesStorable() {
    super();
  }


  public WorkspaceContentDifferencesStorable(Long listid) {
    this.listid = listid;
  }


  @Override
  public Long getPrimaryKey() {
    return listid;
  }


  private static WorkspaceContentDifferencesStorableReader reader = new WorkspaceContentDifferencesStorableReader();


  @Override
  public ResultSetReader<? extends WorkspaceContentDifferencesStorable> getReader() {
    return reader;
  }


  private static class WorkspaceContentDifferencesStorableReader implements ResultSetReader<WorkspaceContentDifferencesStorable> {

    @Override
    public WorkspaceContentDifferencesStorable read(ResultSet rs) throws SQLException {
      WorkspaceContentDifferencesStorable result = new WorkspaceContentDifferencesStorable();
      result.listid = rs.getLong(COL_LISTID);
      result.workspacename = rs.getString(COL_WORKSPACENAME);
      return result;
    }
  }


  @Override
  public <U extends WorkspaceContentDifferencesStorable> void setAllFieldsFromData(U data) {
    WorkspaceContentDifferencesStorable cast = data;
    listid = cast.listid;
    workspacename = cast.workspacename;
  }


  public Long getListid() {
    return listid;
  }


  public void setListid(Long listid) {
    this.listid = listid;
  }


  public String getWorkspacename() {
    return workspacename;
  }


  public void setWorkspacename(String workspacename) {
    this.workspacename = workspacename;
  }
}
