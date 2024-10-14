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

@Persistable(primaryKey = GuiAttribute.COL_ATTRIBUTEID, tableName = GuiAttribute.TABLENAME)
public class GuiAttribute extends Storable<GuiAttribute> {

  public static final String TABLENAME = "guiattribute";
  public static final String COL_ATTRIBUTEID = "guiAttributeID";
  public static final String COL_NAME = "name";
  public static final String COL_DHCPCONF = "dhcpConf";
  public static final String COL_WERTEBEREICH = "werteBereich";
  public static final String COL_OPTIONENCODING = "optionEncoding";
  
  @Column(name = COL_ATTRIBUTEID)
  private int guiAttributeID;
  
  @Column(name = COL_NAME)
  private String name;
  
  @Column(name = COL_DHCPCONF)
  private String dhcpConf;
  
  @Column(name = COL_WERTEBEREICH)
  private String werteBereich;
  
  @Column(name = COL_OPTIONENCODING)
  private String optionEncoding;
  
  public int getGuiAttributeID(){
    return guiAttributeID;
  }
  
  public String getName(){
    return name;
  }
  
  public String getDhcpConf(){
    return dhcpConf;
  }
  
  public String getWerteBereich(){
    return werteBereich;
  }
  
  public String getOptionEncoding(){
    return optionEncoding;
  }
  
  public void setGuiAttributeID(int guiAttributeID){
    this.guiAttributeID = guiAttributeID;
  }
  
  public void setName(String name){
    this.name = name;
  }
  
  public void setDhcpConf(String dhcpConf){
    this.dhcpConf = dhcpConf;
  }
  
  public void setWerteBereich(String werteBereich){
    this.werteBereich = werteBereich;
  }
  
  public void setOptionEncoding(String optionEncoding){
    this.optionEncoding = optionEncoding;
  }
  
  @Override
  public Object getPrimaryKey() {
    return guiAttributeID;
  }
  
  private static class GuiAttributeReader implements ResultSetReader<GuiAttribute> {

    public GuiAttribute read(ResultSet rs) throws SQLException {
      GuiAttribute ga = new GuiAttribute();
      GuiAttribute.fillByResultSet(ga, rs);
      return ga;
    }

  }
  
  private static final GuiAttributeReader reader = new GuiAttributeReader();

  @Override
  public ResultSetReader<? extends GuiAttribute> getReader() {
    return reader;
  }

  @Override
  public <U extends GuiAttribute> void setAllFieldsFromData(U data2) {
    GuiAttribute data=data2;
    guiAttributeID = data.guiAttributeID;
    name = data.name;
    dhcpConf = data.dhcpConf; 
    werteBereich = data.werteBereich;
    optionEncoding = data.optionEncoding;
  }
  
  public static void fillByResultSet(GuiAttribute ga, ResultSet rs) throws SQLException {
    ga.guiAttributeID = rs.getInt(COL_ATTRIBUTEID);
    ga.name = rs.getString(COL_NAME);
    ga.dhcpConf = rs.getString(COL_DHCPCONF);
    ga.werteBereich = rs.getString(COL_WERTEBEREICH);
    ga.optionEncoding = rs.getString(COL_OPTIONENCODING);
  }

}
