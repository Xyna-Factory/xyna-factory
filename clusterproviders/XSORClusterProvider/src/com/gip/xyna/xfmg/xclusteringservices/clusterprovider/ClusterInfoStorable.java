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
package com.gip.xyna.xfmg.xclusteringservices.clusterprovider;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

@Persistable(primaryKey=ClusterInfoStorable.COL_PK, tableName=ClusterInfoStorable.TABLE_NAME)
public class ClusterInfoStorable extends Storable<ClusterInfoStorable> {

  private static final long serialVersionUID = 1L;
  public static final String TABLE_NAME="clusterinfo";
  public static final String COL_PK = "pk";
  public static final String COL_MODTIME = "modificationtime";
  public static final String COL_RELEASETIME = "releasetime";
  
  final static ResultSetReader<ClusterInfoStorable> reader = new ResultSetReader<ClusterInfoStorable>() {

    @Override
    public ClusterInfoStorable read(ResultSet rs) throws SQLException {
      ClusterInfoStorable s = new ClusterInfoStorable();
      s.modificationTime = rs.getLong(COL_MODTIME);
      s.releaseTime = rs.getLong(COL_RELEASETIME);
      s.primaryKey = rs.getString(COL_PK);
      return s;
    }
    
  };
  
  @Column(name=COL_PK)
  private String primaryKey;
  
  @Column(name=COL_MODTIME)
  private long modificationTime;
  
  @Column(name=COL_RELEASETIME)
  private long releaseTime;
  
  public ClusterInfoStorable() {
    
  }
  
  public ClusterInfoStorable(Storable<?> s, long releaseTime, long modificationTime) {
    //unsch�n, aber ausreichend
    //alternative w�re eine spalte f�r den PK und eine f�r den tablename, was dazu f�hrt, dass man einen zweispaltigen in der tabelle hat.
    //das unterst�tzen wir in den persistencelayers derzeit nicht.
    primaryKey = getPkForStorablePk(s);
    this.releaseTime = releaseTime;
    this.modificationTime = modificationTime;
  }

  static String getPkForStorablePk(Storable<?> s) {
    return s.getTableName() + "!" + getPkAsString(s.getPrimaryKey());
  }
  
  private static String getPkAsString(Object primaryKey) {
    if (primaryKey.getClass().isArray()) {
      if (primaryKey.getClass() == byte[].class) {
        return Arrays.toString((byte[])primaryKey);
      }
      throw new RuntimeException("unsupported");
    } else {
      return String.valueOf(primaryKey);
    }
  }

  @Override
  public Object getPrimaryKey() {
    return primaryKey;
  }
  
  public String getPk() {
    return primaryKey;
  }

  public long getReleaseTime() {
    return releaseTime;    
  }
  
  public long getModificationTime() {
    return modificationTime;
  }
  
  @Override
  public ResultSetReader<? extends ClusterInfoStorable> getReader() {
    return reader;
  }

  @Override
  public <U extends ClusterInfoStorable> void setAllFieldsFromData(U data2) {
    ClusterInfoStorable data = data2;
    primaryKey = data.primaryKey;
    modificationTime = data.modificationTime;
    releaseTime=data.releaseTime;
  }

}
