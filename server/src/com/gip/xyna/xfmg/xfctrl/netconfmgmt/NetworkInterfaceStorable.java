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
package com.gip.xyna.xfmg.xfctrl.netconfmgmt;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

@Persistable(primaryKey=NetworkInterfaceStorable.COL_ID, tableName=NetworkInterfaceStorable.TABLE_NAME)
public class NetworkInterfaceStorable extends Storable<NetworkInterfaceStorable> {

  private static final long serialVersionUID = 1L;
  public static final String COL_ID = "id";
  public static final String TABLE_NAME = "networkinterface";
  public static final String COL_INTERFACENAME = "interfacename";
  
  private static final ResultSetReader<NetworkInterfaceStorable> reader = new ResultSetReader<NetworkInterfaceStorable>() {
    
    public NetworkInterfaceStorable read(ResultSet rs) throws SQLException {
      NetworkInterfaceStorable nis = new NetworkInterfaceStorable();
      nis.id = rs.getString(COL_ID);
      nis.interfacename = rs.getString(COL_INTERFACENAME);
      return nis;
    }
  };
  
  @Column(name=COL_ID)
  private String id;
  
  @Column(name=COL_INTERFACENAME)
  private String interfacename;
  
  public NetworkInterfaceStorable() {
    
  }
  
  public NetworkInterfaceStorable(String id, String interfaceName) {
    this.id = id;
    this.interfacename = interfaceName;
  }
  
  @Override
  public ResultSetReader<? extends NetworkInterfaceStorable> getReader() {
    return reader;
  }

  @Override
  public Object getPrimaryKey() {
    return id;
  }
  
  public String getId() {
    return id;
  }
  
  public String getInterfacename() {
    return interfacename;
  }

  @Override
  public <U extends NetworkInterfaceStorable> void setAllFieldsFromData(U data) {
    NetworkInterfaceStorable cast = data;
    id = cast.id;
    interfacename = cast.interfacename;
  }

}
