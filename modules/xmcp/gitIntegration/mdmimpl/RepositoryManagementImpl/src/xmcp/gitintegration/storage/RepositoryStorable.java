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
package xmcp.gitintegration.storage;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

@Persistable(primaryKey = RepositoryStorable.COL_REPOPATH, tableName = RepositoryStorable.TABLE_NAME)
public class RepositoryStorable extends Storable<RepositoryStorable>{
  private static final long serialVersionUID = 1L;
  
  public static final String TABLE_NAME = "repositoryconnection";
  public static final String COL_REPOPATH = "repopath";
  public static final String COL_USEAUTH = "useauth";

  @Column(name = COL_REPOPATH)
  private String repopath;
  @Column(name = COL_USEAUTH)
  private boolean userauth;  
  
  public RepositoryStorable() {
    super();
  }
  
  public RepositoryStorable(String repoPath) {
    this();
    this.repopath = repoPath;
  }
  
  public RepositoryStorable(String repoPath, boolean userAuth) {
    this(repoPath);
    this.userauth = userAuth;
  }

  private static RepositoryStorableReader reader = new RepositoryStorableReader();

  @Override
  public ResultSetReader<? extends RepositoryStorable> getReader() {
    return reader;
  }
  
  private static class RepositoryStorableReader implements ResultSetReader<RepositoryStorable> {

    public RepositoryStorable read(ResultSet rs) throws SQLException {
      RepositoryStorable result = new RepositoryStorable();
      result.repopath = rs.getString(COL_REPOPATH);
      result.userauth = rs.getBoolean(COL_USEAUTH);
      return result;
    }
  }
  
  @Override
  public Object getPrimaryKey() {
    return repopath;
  }

  @Override
  public <U extends RepositoryStorable> void setAllFieldsFromData(U data) {
    RepositoryStorable cast = data;
    repopath = cast.repopath;
    userauth = cast.userauth;
  }

  
  public String getRepopath() {
    return repopath;
  }

  
  public void setRepopath(String repopath) {
    this.repopath = repopath;
  }

  
  public boolean getUserauth() {
    return userauth;
  }

  
  public void setUserauth(boolean userauth) {
    this.userauth = userauth;
  }

}
