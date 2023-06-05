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

@Persistable(primaryKey = GuiParameter.COL_PARAMETERID, tableName = GuiParameter.TABLENAME)
public class GuiParameter extends Storable<GuiParameter> {

  public static final String TABLENAME = "guiparameter";
  public static final String COL_PARAMETERID = "guiParameterID";
  public static final String COL_NAME = "name";
  public static final String COL_DHCPCONF = "dhcpConf";
  
  @Column(name = COL_PARAMETERID)
  private int guiParameterID;
  
  @Column(name = COL_NAME)
  private String name;
  
  @Column(name = COL_DHCPCONF)
  private String dhcpConf;
  
  public int getGuiParameterID(){
    return guiParameterID;
  }
  
  public String getName(){
    return name;
  }
  
  public String getDhcpConf(){
    return dhcpConf;
  }
  
  public void setGuiParameterID(int guiParameterID){
    this.guiParameterID = guiParameterID;
  }
  
  public void setName(String name){
    this.name = name;
  }
  
  public void setDhcpConf(String dhcpConf){
    this.dhcpConf = dhcpConf;
  }
  
  @Override
  public Object getPrimaryKey() {
    return guiParameterID;
  }
  
  private static class GuiParameterReader implements ResultSetReader<GuiParameter> {

    public GuiParameter read(ResultSet rs) throws SQLException {
      GuiParameter para = new GuiParameter();
      GuiParameter.fillByResultSet(para, rs);
      return para;
    }

  }
  
  private static final GuiParameterReader reader = new GuiParameterReader();

  @Override
  public ResultSetReader<? extends GuiParameter> getReader() {
    return reader;
  }

  @Override
  public <U extends GuiParameter> void setAllFieldsFromData(U data2) {
    GuiParameter data=data2;
    guiParameterID = data.guiParameterID;
    name = data.name;
    dhcpConf = data.dhcpConf;
  }
  
  public static void fillByResultSet(GuiParameter op, ResultSet rs) throws SQLException {
    op.guiParameterID = rs.getInt(COL_PARAMETERID);
    op.name = rs.getString(COL_NAME);
    op.dhcpConf = rs.getString(COL_DHCPCONF);
  }
  

}
