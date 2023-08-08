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



@Persistable(primaryKey = RepositoryUserStorable.COL_ID, tableName = RepositoryUserStorable.TABLE_NAME)
public class RepositoryUserStorable extends Storable<RepositoryUserStorable> {

  private static final long serialVersionUID = 1L;

  public static final String TABLE_NAME = "repositoryuser";
  public static final String COL_ID = "id";
  public static final String COL_FACTORY_USERNAME = "factoryusername";
  public static final String COL_REPO_USERNAME = "repousername";
  public static final String COL_REPOSITORY = "repopath";
  public static final String COL_TIMESTAMP = "createdtimestamp";
  public static final String COL_MAIL = "mail";

  @Column(name = COL_ID)
  private String id;
  @Column(name = COL_FACTORY_USERNAME)
  private String factoryusername;
  @Column(name = COL_REPO_USERNAME)
  private String repousername;
  @Column(name = COL_REPOSITORY)
  private String repopath;
  @Column(name = COL_TIMESTAMP)
  private long createdtimestamp;
  @Column(name = COL_MAIL)
  private String mail;


  public RepositoryUserStorable() {
    super();
  }


  public RepositoryUserStorable(String factoryUsername, String repoUsername, String repoPath, long createdTimestamp, String mail) {
    this();
    this.factoryusername = factoryUsername;
    this.repousername = repoUsername;
    this.repopath = repoPath;
    this.createdtimestamp = createdTimestamp;
    this.mail = mail;
    this.id = createIdentifier(this.factoryusername, this.repopath);
  }


  public static String createIdentifier(String username, String repopath) {
    return username + "_" + repopath;
  }


  public static RepositoryUserStorableReader reader = new RepositoryUserStorableReader();


  @Override
  public ResultSetReader<? extends RepositoryUserStorable> getReader() {
    return reader;
  }


  private static class RepositoryUserStorableReader implements ResultSetReader<RepositoryUserStorable> {

    public RepositoryUserStorable read(ResultSet rs) throws SQLException {
      RepositoryUserStorable result = new RepositoryUserStorable();
      result.id = rs.getString(COL_ID);
      result.factoryusername = rs.getString(COL_FACTORY_USERNAME);
      result.repousername = rs.getString(COL_REPO_USERNAME);
      result.repopath = rs.getString(COL_REPOSITORY);
      result.createdtimestamp = rs.getLong(COL_TIMESTAMP);
      result.mail = rs.getString(COL_MAIL);
      return result;
    }
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }


  @Override
  public <U extends RepositoryUserStorable> void setAllFieldsFromData(U data) {
    RepositoryUserStorable cast = data;
    id = cast.id;
    factoryusername = cast.factoryusername;
    repousername = cast.repousername;
    repopath = cast.repopath;
    createdtimestamp = cast.createdtimestamp;
    mail = cast.mail;
  }


  public String getId() {
    return id;
  }


  public void setId(String id) {
    this.id = id;
  }


  public String getFactoryusername() {
    return factoryusername;
  }


  public void setFactoryusername(String factoryusername) {
    this.factoryusername = factoryusername;
  }


  public String getRepousername() {
    return repousername;
  }


  public void setRepousername(String repousername) {
    this.repousername = repousername;
  }


  public String getRepopath() {
    return repopath;
  }


  public void setRepopath(String repopath) {
    this.repopath = repopath;
  }


  public long getCreatedtimestamp() {
    return createdtimestamp;
  }


  public void setCreatedtimestamp(long createdtimestamp) {
    this.createdtimestamp = createdtimestamp;
  }


  public String getMail() {
    return mail;
  }


  public void setMail(String mail) {
    this.mail = mail;
  }
}
