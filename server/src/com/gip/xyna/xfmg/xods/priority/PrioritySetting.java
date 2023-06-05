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
package com.gip.xyna.xfmg.xods.priority;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;


@Persistable(primaryKey= PrioritySetting.COL_ID, tableName = PrioritySetting.TABLENAME)
public class PrioritySetting extends Storable<PrioritySetting> {

  private static final long serialVersionUID = 1512304800432506091L;
  
  public static final String TABLENAME = "prioritysettings";
  public static final String COL_ID = "id";
  public static final String COL_ORDERTYPE = "orderType";
  public static final String COL_PRIORITY = "priority";
  public static final String COL_REVISION = "revision";
  
  @Column(name=COL_ID)
  private String id;
  @Column(name=COL_ORDERTYPE)
  private String orderType;
  @Column(name=COL_PRIORITY)
  private int priority;
  @Column(name=COL_REVISION)
  private Long revision;
  
  
  public PrioritySetting() {
  }
  
  
  public PrioritySetting(String orderType, Long revision) {
    this.id = orderType + "#" + revision;
    this.orderType = orderType;
    this.priority = -1;
    this.revision = revision;
  }
  
                         
  public PrioritySetting(String orderType, int priority, Long revision) {
    this(orderType, revision);
    this.priority = priority;
  }
  

  @Override
  public ResultSetReader<? extends PrioritySetting> getReader() {
    return new ResultSetReader<PrioritySetting>() {

      public PrioritySetting read(ResultSet rs)
          throws SQLException {
        
        PrioritySetting result = new PrioritySetting();
        result.id = rs.getString(COL_ID);
        result.orderType = rs.getString(COL_ORDERTYPE);
        result.priority = rs.getInt(COL_PRIORITY);
        result.revision = rs.getLong(COL_REVISION);
        
        return result;
      }
    };
  }

  
  @Override
  public Object getPrimaryKey() {
    return id;
  }

  
  @Override
  public <U extends PrioritySetting> void setAllFieldsFromData(U data) {
    PrioritySetting cast = data;
    orderType = cast.orderType;
    priority = cast.priority;
    id = cast.id;
    revision = cast.revision;
  }


  
  public String getOrderType() {
    return orderType;
  }


  
  public int getPriority() {
    return priority;
  }


  
  public void setOrderType(String orderType) {
    this.orderType = orderType;
  }


  
  public void setPriority(int priority) {
    this.priority = priority;
  }


  
  public String getId() {
    return id;
  }


  
  public void setId(String id) {
    this.id = id;
  }


  
  public Long getRevision() {
    return revision;
  }


  
  public void setRevision(Long revision) {
    this.revision = revision;
  }
  

}
