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

package com.gip.xyna.xprc.xsched;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xfmg.xclusteringservices.XynaClusteringServicesManagement;
import com.gip.xyna.xnwh.persistence.ClusteredStorable;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.IndexType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xprc.xsched.CapacityManagement.State;



@Persistable(tableName = CapacityStorable.TABLE_NAME, primaryKey = CapacityStorable.COL_ID)
public class CapacityStorable extends ClusteredStorable<CapacityStorable> {

  private static final long serialVersionUID = 1L;

  public static final String TABLE_NAME = "capacities";

  public static final String COL_ID = "id";
  public static final String COL_NAME = "name";
  public static final String COL_CARDINALITY = "cardinality";
  public static final String COL_STATE = "statestring";
  

  public static final ResultSetReader<CapacityStorable> reader = new CapacityStorableReader();


  @Column(name = COL_ID, index = IndexType.PRIMARY)
  private long id;

  // Index multiple weil eigentlich name+binding zusammen das Objekt eindeutig identifizieren. Separater
  // unique PK ist eigentlich ein Workaround dafür, dass wir keine PKs über mehrere Spalten unterstützen.
  @Column(name = COL_NAME, index = IndexType.MULTIPLE)
  private String name;

  @Column(name = COL_CARDINALITY)
  private int cardinality;

  @Column(name = COL_STATE)
  private String stateString;
  
  public CapacityStorable() {
    super(XynaClusteringServicesManagement.DEFAULT_BINDING_NO_CLUSTER);
  }


  public CapacityStorable(int binding) {
    super(binding);
  }


  public String getName() {
    return name;
  }


  public void setName(String name) {
    this.name = name;
  }


  public CapacityStorable(long id, int binding) {
    super(binding);
    this.id = id;
  }


  public int getCardinality() {
    return cardinality;
  }


  @Override
  public ResultSetReader<? extends CapacityStorable> getReader() {
    return reader;
  }


  @Override
  public Long getPrimaryKey() {
    return id;
  }


  @Override
  public <U extends CapacityStorable> void setAllFieldsFromData(U data) {
    super.setBinding(data.getBinding());
    CapacityStorable cast = data;
    this.id = cast.id;
    this.name = cast.name;
    this.cardinality = cast.cardinality;
    this.stateString = cast.stateString;
    
    if (this.stateString == null) {
      //logger.warn("Was unable to read State - assuming it is ACTIVE");
      this.stateString = State.ACTIVE.toString();
    }
  }


  @SuppressWarnings("unchecked")
  private static void fillByResultset(CapacityStorable cs, ResultSet rs) throws SQLException {
    ClusteredStorable.fillByResultSet(cs, rs);
    cs.id = rs.getLong(COL_ID);
    cs.name = rs.getString(COL_NAME);
    cs.cardinality = rs.getInt(COL_CARDINALITY);
    cs.stateString = rs.getString(COL_STATE);
    
    if (cs.stateString == null) {
      //logger.warn("Was unable to read State - assuming it is ACTIVE");
      cs.stateString = State.ACTIVE.toString();
    }
  }


  private static class CapacityStorableReader implements ResultSetReader<CapacityStorable> {

    public CapacityStorable read(ResultSet rs) throws SQLException {
      CapacityStorable result = new CapacityStorable();
      fillByResultset(result, rs);
      return result;
    }

  }

  
  public String getStateString() {
    return stateString;
  }


  public State getState() {
    if (stateString == null) {
      //logger.warn("Was unable to read State - assuming it is ACTIVE");
      stateString = State.ACTIVE.toString();
      return State.ACTIVE;
    } else {
      return State.valueOf(stateString);
    }
  }


  public long getId() {
    return id;
  }


  public void setCardinality(int cardinality) {
    this.cardinality = cardinality;
  }


  public void setState(State state) {
    this.stateString = state.toString();
  }

}
