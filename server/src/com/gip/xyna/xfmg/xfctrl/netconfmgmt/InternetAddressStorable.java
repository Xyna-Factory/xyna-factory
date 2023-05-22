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
package com.gip.xyna.xfmg.xfctrl.netconfmgmt;



import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(primaryKey = InternetAddressStorable.COL_ID, tableName = InternetAddressStorable.TABLE_NAME)
public class InternetAddressStorable extends Storable<InternetAddressStorable> {

  private static final long serialVersionUID = 1L;
  public static final String COL_ID = "id";
  public static final String COL_IP = "ip";
  public static final String COL_DOCUMENTATION = "documentation";
  public static final String TABLE_NAME = "internetaddress";
  public static final ResultSetReader<InternetAddressStorable> reader = new ResultSetReader<InternetAddressStorable>() {

    public InternetAddressStorable read(ResultSet rs) throws SQLException {
      InternetAddressStorable ias = new InternetAddressStorable();
      ias.id = rs.getString(COL_ID);
      ias.ip = rs.getString(COL_IP);
      ias.documentation = rs.getString(COL_DOCUMENTATION);
      return ias;
    }
  };

  @Column(name = COL_ID)
  private String id;

  @Column(name = COL_IP)
  private String ip;
  
  @Column(name = COL_DOCUMENTATION)
  private String documentation;


  public InternetAddressStorable() {
  }


  public InternetAddressStorable(String id, String ip, String documentation) {
    this.id = id;
    this.ip = ip;
    this.documentation = documentation;
  }


  @Override
  public ResultSetReader<? extends InternetAddressStorable> getReader() {
    return reader;
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }


  public String getId() {
    return id;
  }


  public String getIp() {
    return ip;
  }

  public String getDocumentation() {
    return documentation;
  }

  @Override
  public <U extends InternetAddressStorable> void setAllFieldsFromData(U data) {
    InternetAddressStorable cast = data;
    id = cast.id;
    ip = cast.ip;
    documentation = cast.documentation;
  }

}
