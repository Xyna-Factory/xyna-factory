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
package com.gip.xyna.xfmg.xods.ordertypemanagement;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(primaryKey = OrdertypeInformation.COL_ID, tableName = OrdertypeInformation.TABLE_NAME)
public class OrdertypeInformation extends Storable<OrdertypeInformation> {

  private static final long serialVersionUID = -5584665252091344617L;
  
  public final static String TABLE_NAME = "ordertype";
  public final static String COL_ID = "id";
  public final static String COL_ORDERTYPE_NAME = "ordertypeName";
  public final static String COL_DOCUMENTATION = "documentation";
  public final static String COL_REVISION = "revision";
  
  @Column(name = COL_ID, size = 200)
  private String id;
  @Column(name = COL_ORDERTYPE_NAME, size = 200)
  private String ordertypeName;
  @Column(name = COL_DOCUMENTATION, size = 2000)
  private String documentation;
  @Column(name = COL_REVISION)
  private long revision;


  public OrdertypeInformation() {
  }


  public OrdertypeInformation(String ordertypename, long revision) {
    this.id = genId(ordertypename, revision);
    this.revision = revision;
    this.ordertypeName = ordertypename;
  }


  private static String genId(String ot, long rev) {
    return ot + "#" + rev;
  }


  public OrdertypeInformation(OrdertypeParameter ordertypeParameter, long revision) {
    this(ordertypeParameter.getOrdertypeName(), revision);
    this.documentation = ordertypeParameter.getDocumentation();
  }
  
  
  public String getOrdertypeName() {
    return ordertypeName;
  }
  
  
  public String getDocumentation() {
    return documentation;
  }
  
  public long getRevision() {
    return revision;
  }
  
  public void setOrdertypeName(String ordertypeName) {
    this.ordertypeName = ordertypeName;
  }
  
  
  public void setDocumentation(String documentation) {
    this.documentation = documentation;
  }
  
  private static OrdertypeInformationReader reader = new OrdertypeInformationReader();

  @Override
  public ResultSetReader<OrdertypeInformation> getReader() {
    return reader;
  }
  
  private static class OrdertypeInformationReader implements ResultSetReader<OrdertypeInformation> {

    public OrdertypeInformation read(ResultSet rs) throws SQLException {
      OrdertypeInformation oti = new OrdertypeInformation();
      oti.ordertypeName = rs.getString(COL_ORDERTYPE_NAME);
      oti.documentation = rs.getString(COL_DOCUMENTATION);
      oti.revision = rs.getInt(COL_REVISION);
      oti.id = rs.getString(COL_ID);
      return oti;
    }
    
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }

  
  public String getId() {
    return id;
  }


  @Override
  public <U extends OrdertypeInformation> void setAllFieldsFromData(U data) {
    OrdertypeInformation cast = data;
    ordertypeName = cast.ordertypeName;
    documentation = cast.documentation;
    revision = cast.revision;
    id = cast.id;
  }


  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof OrdertypeInformation)) {
      return false;
    }
    OrdertypeInformation other = (OrdertypeInformation) obj;
    return this.ordertypeName.equals(other.ordertypeName) && revision == other.revision;
  }
  
  
  @Override
  public int hashCode() {
    return this.ordertypeName.hashCode() ^ (31 * new Long(revision).hashCode());
  }

}
