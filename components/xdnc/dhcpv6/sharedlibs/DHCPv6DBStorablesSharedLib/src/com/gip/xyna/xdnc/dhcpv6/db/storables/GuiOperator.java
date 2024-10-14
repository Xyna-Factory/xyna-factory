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
package com.gip.xyna.xdnc.dhcpv6.db.storables;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

@Persistable(primaryKey = GuiOperator.COL_OPERATORID, tableName = GuiOperator.TABLENAME)
public class GuiOperator extends Storable<GuiOperator> {

  public static final String TABLENAME = "guioperator";
  public static final String COL_OPERATORID = "guiOperatorID";
  public static final String COL_NAME = "name";
  public static final String COL_DHCPCONF = "dhcpConf";
  
  @Column(name = COL_OPERATORID)
  private int guiOperatorID;
  
  @Column(name = COL_NAME)
  private String name;
  
  @Column(name = COL_DHCPCONF)
  private String dhcpConf;
  
  public int getGuiOperatorID(){
    return guiOperatorID;
  }
  
  public String getName(){
    return name;
  }
  
  public String getDhcpConf(){
    return dhcpConf;
  }
  
  public void setGuiOperatorID(int guiOperatorID){
    this.guiOperatorID = guiOperatorID;
  }
  
  public void setName(String name){
    this.name = name;
  }
  
  public void setDhcpConf(String dhcpConf){
    this.dhcpConf = dhcpConf;
  }
  
  @Override
  public Object getPrimaryKey() {
    return guiOperatorID;
  }

  private static class GuiOperatorReader implements ResultSetReader<GuiOperator> {

    public GuiOperator read(ResultSet rs) throws SQLException {
      GuiOperator op = new GuiOperator();
      GuiOperator.fillByResultSet(op, rs);
      return op;
    }

  }
  
  private static final GuiOperatorReader reader = new GuiOperatorReader();
  
  @Override
  public ResultSetReader<? extends GuiOperator> getReader() {
    return reader;
  }

  @Override
  public <U extends GuiOperator> void setAllFieldsFromData(U data2) {
    GuiOperator data=data2;
    guiOperatorID = data.guiOperatorID;
    name = data.name;
    dhcpConf = data.dhcpConf;    
  }
  
  public static void fillByResultSet(GuiOperator op, ResultSet rs) throws SQLException {
    op.guiOperatorID = rs.getInt(COL_OPERATORID);
    op.name = rs.getString(COL_NAME);
    op.dhcpConf = rs.getString(COL_DHCPCONF);
  }

}
