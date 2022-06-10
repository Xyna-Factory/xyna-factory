/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.xdnc.dhcpv6.db.storables;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

@Persistable(primaryKey = PoolType.COL_POOLTYPEID, tableName = PoolType.TABLENAME)
public class PoolType extends Storable<PoolType> {
  
  public static final String TABLENAME = "pooltype";
  public static final String COL_POOLTYPEID = "poolTypeID";
  public static final String COL_NAME = "name";
  public static final String COL_CLASSIDS = "classIDs";
  public static final String COL_NEGATION = "negation";
  public static final String COL_ATTRIBUTES = "attributes";
  public static final String COL_FIXEDATTRIBUTES = "fixedAttributes";

  @Column(name = COL_POOLTYPEID)
  private int poolTypeID;
  
  @Column(name = COL_CLASSIDS)
  private String classIDs;
  
  @Column(name = COL_NAME)
  private String name;
  
  @Column(name = COL_NEGATION)
  private String negation;
  
  @Column(name = COL_ATTRIBUTES)
  private String attributes;
  
  @Column(name = COL_FIXEDATTRIBUTES)
  private String fixedAttributes;
  
public PoolType(){
  }
  
  public PoolType(int poolTypeID, String name, String classIDs, String negation, String attributes, String fixedAttributes){
    this.poolTypeID = poolTypeID;
    this.name = name;
    this.classIDs = classIDs;
    this.negation = negation;
    this.attributes = attributes;
    this.fixedAttributes = fixedAttributes;
  }
  
  public int getPoolTypeID(){
    return poolTypeID;
  }
  
  public String getName(){
    return name;
  }
  
  public String getNegation(){
    return negation;
  }
  
  public String getAttributes(){
    return attributes;
  }
  
  public String getFixedAttributes(){
    return fixedAttributes;
  }
  
  public String getClassIDs(){
    return classIDs;
  }
  
  
  
  @Override
  public Object getPrimaryKey() {
    return poolTypeID;
  }
  
  private static class PoolTypeReader implements ResultSetReader<PoolType> {

    public PoolType read(ResultSet rs) throws SQLException {
      PoolType pt = new PoolType();
      PoolType.fillByResultSet(pt, rs);
      return pt;
    }
  }
  
  public static void fillByResultSet(PoolType pt, ResultSet rs) throws SQLException {
    pt.poolTypeID = rs.getInt(COL_POOLTYPEID);
    pt.name = rs.getString(COL_NAME);
    pt.classIDs = rs.getString(COL_CLASSIDS);
    pt.negation = rs.getString(COL_NEGATION);
    pt.attributes = rs.getString(COL_ATTRIBUTES);
    pt.fixedAttributes = rs.getString(COL_FIXEDATTRIBUTES);
  }
  
  private static final PoolTypeReader reader = new PoolTypeReader();
  
  @Override
  public ResultSetReader<? extends PoolType> getReader() {
    return reader;
  }
  

  @Override
  public <U extends PoolType> void setAllFieldsFromData(U data2) {
    PoolType data=data2;
    poolTypeID = data.poolTypeID;
    name = data.name;
    classIDs = data.classIDs;
    negation = data.negation;
    attributes = data.attributes;
    fixedAttributes = data.fixedAttributes;
    
  }

}
