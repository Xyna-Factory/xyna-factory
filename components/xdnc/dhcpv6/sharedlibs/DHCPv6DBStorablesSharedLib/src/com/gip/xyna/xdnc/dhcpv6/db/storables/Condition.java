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
import java.util.List;

import xdnc.dhcp.Node;


import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

@Persistable(primaryKey = Condition.COL_CONDITIONID, tableName = Condition.TABLENAME)
public class Condition extends Storable<Condition>{

  public static final String TABLENAME = "classcondition";
  public static final String COL_CONDITIONID = "conditionID";
  public static final String COL_PARAMETER = "parameter";
  public static final String COL_OPERATOR = "operator";
  public static final String COL_NAME = "name";
  public static final String COL_VALUE = "value";
  
  @Column(name = COL_CONDITIONID)
  private int conditionID;
  
  @Column(name = COL_PARAMETER)
  private String parameter;
  
  @Column(name = COL_OPERATOR)
  private String operator;
  
  @Column(name = COL_NAME)
  private String name;
  
  @Column(name = COL_VALUE)
  private String value;
  
  public Condition(){
    
  }
  
  public Condition(int conditioId, String parameter, String operator, String name, String value){
    this.conditionID = conditioId;
    this.parameter = parameter;
    this.operator = operator;
    this.name = name;
    this.value = value;
  }
  
  public int getConditionID(){
    return conditionID;
  }
  
  public String getParameter(){
    return parameter;
  }
  
  public String getOperator(){
    return operator;
  }
  
  public String getName(){
    return name;
  }
  
  public String getValue(){
    return value;
  }
  
  private static class ConditionReader implements ResultSetReader<Condition> {

    public Condition read(ResultSet rs) throws SQLException {
      Condition cond = new Condition();
      Condition.fillByResultSet(cond, rs);
      return cond;
    }
  }
  
  public static void fillByResultSet(Condition cond, ResultSet rs) throws SQLException {
    cond.conditionID = rs.getInt(COL_CONDITIONID);
    cond.name = rs.getString(COL_NAME);
    cond.parameter = rs.getString(COL_PARAMETER);
    cond.operator = rs.getString(COL_OPERATOR);
    cond.value = rs.getString(COL_VALUE);
  }
  
  private static final ConditionReader reader = new ConditionReader();
  
  @Override
  public ResultSetReader<? extends Condition> getReader() {
    return reader;
  }

  @Override
  public Object getPrimaryKey() {
    return conditionID;
  }

  @Override
  public <U extends Condition> void setAllFieldsFromData(U data2) {
    Condition data = data2;
    conditionID = data.conditionID;
    parameter = data.parameter;
    operator = data.operator;
    name = data.name;
    value = data.value;
  }
  
  public static boolean evaluate(String condition, List<? extends Node> inputoptions){
    //TODO
    if (condition.equals("1")) return true;
    else return false;
  }

}
