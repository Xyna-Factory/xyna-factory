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
package com.gip.xyna.xnwh.persistence.javaserialization;


import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.exceptions.XFMG_UserAuthenticationFailedException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserIsLockedException;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xopctrl.usermanagement.passwordcreation.PasswordCreationUtils;
import com.gip.xyna.xfmg.xopctrl.usermanagement.selectuser.UserColumns;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.Persistable.StorableProperty;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(primaryKey = TestStorable.COL_INTT, tableName = TestStorable.TABLENAME)
public class TestStorable extends Storable<TestStorable> {

  public static final String TABLENAME = "testarchive";  
  public static final String COL_INTT = "intt";
  public static final String COL_LONGG = "longg";
  public static final String COL_STRINGG = "stringg";
  public static final String COL_BLOBB = "blobb";
  

  private static final long serialVersionUID = -7301378884111678454L;

  @Column(name = COL_INTT, size = 50)
  private int intt;
  @Column(name = COL_LONGG, size = 50)
  private long longg;
  @Column(name = COL_STRINGG, size = 50)
  private String stringg;
  @Column(name = COL_BLOBB, type=ColumnType.BLOBBED_JAVAOBJECT)
  private Map<String, String> blobb;
  
  public static boolean throwErrorOnWrite = false;
  public static boolean throwErrorOnRead = false;

  public TestStorable() {
  }
  
  
  public TestStorable(int intt, long longg, String stringg, Map<String, String> blobb) {
    this.intt = intt;
    this.longg = longg;
    this.stringg = stringg;
    this.blobb = blobb;
  }
  


  @Override
  public Object getPrimaryKey() {
    return intt;
  }


  public static ResultSetReader<TestStorable> reader = new ResultSetReader<TestStorable>() {

    public TestStorable read(ResultSet rs) throws SQLException {
      TestStorable u = new TestStorable();
      u.intt = rs.getInt(COL_INTT);
      u.longg = rs.getLong(COL_LONGG);
      u.stringg = rs.getString(COL_STRINGG);
      u.blobb = (Map<String, String>) u.readBlobbedJavaObjectFromResultSet(rs, COL_BLOBB);
      return u;
    }

  };


  @Override
  public ResultSetReader<? extends TestStorable> getReader() {
    return reader;
  }


  @Override
  public <U extends TestStorable> void setAllFieldsFromData(U data2) {
    TestStorable data = data2;
    intt = data.intt;
    longg = data.longg;
    stringg = data.stringg;
    blobb = data.blobb;
  }

  
  private void writeObject(java.io.ObjectOutputStream s) throws IOException {
    if (throwErrorOnWrite) {
      throw new UnsatisfiedLinkError();
    }
    s.defaultWriteObject();
  }
  
  
  private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
    if (throwErrorOnRead) {
      throw new UnsatisfiedLinkError();
    }
    s.defaultReadObject();
  }

}
