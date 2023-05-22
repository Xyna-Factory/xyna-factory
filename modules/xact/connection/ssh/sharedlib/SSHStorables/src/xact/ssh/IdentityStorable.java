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
package xact.ssh;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;


@Persistable(tableName = IdentityStorable.TABLE_NAME, primaryKey = IdentityStorable.COL_ID)
public class IdentityStorable extends Storable<IdentityStorable> {

  private static final long serialVersionUID = -6705705543747584456L;
  
  public final static String TABLE_NAME = "identityrepository";
  public final static String COL_ID = "id";
  public final static String COL_NAME = "name";
  public final static String COL_TYPE = "type";
  public final static String COL_PUBLICKEY = "publickey";
  public final static String COL_PRIVATEKEY = "privatekey";
  
  @Column(name = COL_ID)
  private long id;
  
  @Column(name = COL_NAME)
  private String name;
  
  @Column(name = COL_TYPE)
  private String type;
  
  @Column(name = COL_PUBLICKEY, type = ColumnType.BLOBBED_JAVAOBJECT)
  private byte[] publickey;
  
  @Column(name = COL_PRIVATEKEY, type = ColumnType.BLOBBED_JAVAOBJECT)
  private byte[] privatekey;
  
  
  public IdentityStorable() {
    
  }
  
  
  public IdentityStorable(String name, String type, byte[] publickey, byte[] privatekey) {
    this();
    this.name = name;
    this.type = type;
    this.publickey = publickey;
    this.privatekey = privatekey;
    try {
      this.id = IDGenerator.getInstance().getUniqueId();
    } catch (XynaException e) {
      throw new RuntimeException("Could not generate unique id",e);
    }
  }
  
  @Override
  public Object getPrimaryKey() {
    return id;
  }


  public long getId() {
    return id;
  }

  
  public void setId(long id) {
    this.id = id;
  }

  
  public String getName() {
    return name;
  }

  
  public void setName(String name) {
    this.name = name;
  }
  
  
  public String getType() {
    return type;
  }

  
  public void setType(String type) {
    this.type = type;
  }

  
  public byte[] getPublickey() {
    return publickey;
  }

  
  public void setPublickey(byte[] publickey) {
    this.publickey = publickey;
  }

  
  public byte[] getPrivatekey() {
    return privatekey;
  }

  
  public void setPrivatekey(byte[] privatekey) {
    this.privatekey = privatekey;
  }

  
  /*public CheckResult check(IdentityStorable identity) {
    if (name.equals(identity.name) && type.equals(identity.type)) {
      if (Arrays.equals(publickey, identity.publickey) &&
          Arrays.equals(privatekey, identity.privatekey) ) {
        return CheckResult.OK;
      } else {
        return CheckResult.CHANGED;
      }
    } else {
      return CheckResult.NOT_INCLUDED;
    }
  }*/


  public static final ResultSetReader<IdentityStorable> reader = new IdentityStorableResultSetReader();
  
  @Override
  public ResultSetReader<? extends IdentityStorable> getReader() {
    return reader;
  }

  @Override
  public <U extends IdentityStorable> void setAllFieldsFromData(U data) {
    IdentityStorable cast = data;
    this.id = cast.id;
    this.name = cast.name;
    this.type = cast.type;
    this.publickey = cast.publickey;
    this.privatekey = cast.privatekey;
  }


  private static class IdentityStorableResultSetReader implements ResultSetReader<IdentityStorable> {

    public IdentityStorable read(ResultSet rs) throws SQLException {
      IdentityStorable result = new IdentityStorable();
      result.id = rs.getLong(COL_ID);
      result.name = rs.getString(COL_NAME);
      result.type = rs.getString(COL_TYPE);
      result.publickey = (byte[]) result.readBlobbedJavaObjectFromResultSet(rs, COL_PUBLICKEY, Long.toString(result.id));
      result.privatekey = (byte[]) result.readBlobbedJavaObjectFromResultSet(rs, COL_PRIVATEKEY, Long.toString(result.id));
      return result;
    }

  }
  
  

}
