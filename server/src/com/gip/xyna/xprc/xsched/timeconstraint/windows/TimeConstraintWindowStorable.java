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
package com.gip.xyna.xprc.xsched.timeconstraint.windows;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;


@Persistable(tableName = TimeConstraintWindowStorable.TABLE_NAME, primaryKey = TimeConstraintWindowStorable.COL_ID)
public class TimeConstraintWindowStorable extends Storable<TimeConstraintWindowStorable> {

  private static final long serialVersionUID = 1L;

  public static final String TABLE_NAME = "timeconstraintwindows";

  public static final String COL_ID = "id";
  public static final String COL_NAME = "name";
  public static final String COL_DESCRIPTION = "description";
  public static final String COL_SUB_COUNT = "subWindowCount";
  public static final String COL_SUB_ID = "subId";
  public static final String COL_DEFINITION = "definition";
  

  public static final ResultSetReader<TimeConstraintWindowStorable> reader = new TimeConstraintWindowStorableReader();


  @Column(name = COL_ID, index = IndexType.PRIMARY)
  private long id;

  // Index multiple weil eigentlich name+subId zusammen das Objekt eindeutig identifizieren. Separater
  // unique PK ist eigentlich ein Workaround dafür, dass wir keine PKs über mehrere Spalten unterstützen.
  @Column(name = COL_NAME, index = IndexType.MULTIPLE)
  private String name;

  @Column(name = COL_DESCRIPTION, size=4000)
  private String description;

  @Column(name = COL_SUB_COUNT)
  private int subWindowCount;
  
  @Column(name = COL_SUB_ID)
  private int subId;
  
  @Column(name = COL_DEFINITION, size=4000)
  private TimeWindowDefinition timeWindowDefinition;

  @Override
  public ResultSetReader<? extends TimeConstraintWindowStorable> getReader() {
    return reader;
  }

  @Override
  public Long getPrimaryKey() {
    return id;
  }
  
  public TimeConstraintWindowStorable() {
  }
  
  /**
   * @return leeres storable mit neuer unique id
   */
  public static TimeConstraintWindowStorable create() {
    TimeConstraintWindowStorable t = new TimeConstraintWindowStorable();
    t.id = XynaFactory.getInstance().getIDGenerator().getUniqueId();
    return t;
  }
  
  public TimeWindowDefinition getDefinition() {
    return timeWindowDefinition;
  }
  
  public void setDefinition(TimeWindowDefinition definition) {
    this.timeWindowDefinition = definition;
  }

  @Override
  public <U extends TimeConstraintWindowStorable> void setAllFieldsFromData(U data) {
    TimeConstraintWindowStorable cast = data;
    id = cast.id;
    name = cast.name;
    description = cast.description;
    subWindowCount = cast.subWindowCount;
    subId = cast.subId;
    timeWindowDefinition = cast.timeWindowDefinition;
  }
 
  private static void fillByResultset(TimeConstraintWindowStorable tcws, ResultSet rs) throws SQLException {
    tcws.id = rs.getLong(COL_ID);
    tcws.name = rs.getString(COL_NAME);
    tcws.description = rs.getString(COL_DESCRIPTION);
    tcws.subWindowCount = rs.getInt(COL_SUB_COUNT);
    tcws.subId = rs.getInt(COL_SUB_ID);
    tcws.timeWindowDefinition = TimeWindowDefinition.valueOf( rs.getString(COL_DEFINITION));
  }

  private static class TimeConstraintWindowStorableReader implements ResultSetReader<TimeConstraintWindowStorable> {

    public TimeConstraintWindowStorable read(ResultSet rs) throws SQLException {
      TimeConstraintWindowStorable result = new TimeConstraintWindowStorable();
      fillByResultset(result, rs);
      return result;
    }

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

  
  public String getDescription() {
    return description;
  }

  
  public void setDescription(String description) {
    this.description = description;
  }

  
  public int getSubWindowCount() {
    return subWindowCount;
  }

  
  public void setSubWindowCount(int subWindowCount) {
    this.subWindowCount = subWindowCount;
  }

  
  public int getSubId() {
    return subId;
  }

  
  public void setSubId(int subId) {
    this.subId = subId;
  }
  
}
