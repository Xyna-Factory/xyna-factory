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
package xact.ssh.server;



import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(tableName = ClientPasswordStorable.TABLE_NAME, primaryKey = ClientPasswordStorable.COL_ID)
public class ClientPasswordStorable extends Storable<ClientPasswordStorable> {

  private static final long serialVersionUID = 9077661986329920786L;

  public final static String TABLE_NAME = "sftpclientpassword";
  public final static String COL_ID = "id";
  public final static String COL_USERNAME = "username";
  public final static String COL_PASSWORD = "password";

  @Column(name = COL_ID)
  private long id;


  // @Column(name = COL_NAME, index = IndexType.MULTIPLE)
  @Column(name = COL_USERNAME)
  private String username;

  @Column(name = COL_PASSWORD)
  private String password;


  public ClientPasswordStorable() {
  }


  public ClientPasswordStorable(String username, String password) {
    this.setUsername(username);
    this.setPassword(password);
    try {
      this.id = IDGenerator.getInstance().getUniqueId();
    }
    catch (XynaException e) {
      throw new RuntimeException("Could not generate unique id", e);
    }
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }


  public long getId() {
    return id;
  }


  public void setIdentifier(long id) {
    this.id = id;
  }


  public String getUsername() {
    return username;
  }


  public void setUsername(String username) {
    this.username = username;
  }


  public String getPassword() {
    return password;
  }


  public void setPassword(String password) {
    this.password = password;
  }
 


  public static final ResultSetReader<ClientPasswordStorable> reader = new FileContentStorableResultSetReader();


  @Override
  public ResultSetReader<? extends ClientPasswordStorable> getReader() {
    return reader;
  }


  @Override
  public <U extends ClientPasswordStorable> void setAllFieldsFromData(U data) {
    ClientPasswordStorable data2 = (ClientPasswordStorable)data;
    this.id = data2.id;
    this.username = data2.username;
    this.password = data2.password;
  }




  private static class FileContentStorableResultSetReader implements ResultSetReader<ClientPasswordStorable> {

    public ClientPasswordStorable read(ResultSet rs) throws SQLException {
      ClientPasswordStorable result = new ClientPasswordStorable();
      result.id = rs.getLong(COL_ID);
      result.username = rs.getString(COL_USERNAME);
      result.password = rs.getString(COL_PASSWORD);
      return result;
    }
  }
}
