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
package com.gip.xyna.xdnc.dhcpv6.db.storables;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

@Persistable(primaryKey = GuiFixedAttribute.COL_ATTRIBUTEID, tableName = GuiFixedAttribute.TABLENAME)
public class GuiFixedAttribute extends Storable<GuiFixedAttribute> {

  public static final String TABLENAME = "guifixedattribute";
  public static final String COL_ATTRIBUTEID = "guiFixedAttributeID";
  public static final String COL_NAME = "name";
  public static final String COL_DHCPCONF = "dhcpConf";
  public static final String COL_WERT = "value";
  public static final String COL_WERTEBEREICH = "valueRange";
  public static final String COL_OPTIONENCODING = "optionEncoding";
  
  
  @Column(name = COL_ATTRIBUTEID)
  private int guiFixedAttributeID;
  
  @Column(name = COL_NAME)
  private String name;
  
  @Column(name = COL_DHCPCONF)
  private String dhcpConf;
  
  @Column(name = COL_WERT)
  private String value;
  
  @Column(name = COL_WERTEBEREICH)
  private String valueRange;
  
  @Column(name = COL_OPTIONENCODING)
  private String optionEncoding;
  
  public int getGuiFixedAttributeID(){
    return guiFixedAttributeID;
  }
  
  public String getName(){
    return name;
  }
  
  public String getDhcpConf(){
    return dhcpConf;
  }
  
  public String getValue(){
    return value;
  }
  
  public String getValueRange(){
    return valueRange;
  }
  
  public String getOptionEncoding(){
    return optionEncoding;
  }
  
  @Override
  public Object getPrimaryKey() {
    return guiFixedAttributeID;
  }
  
  
  public void setFixedAttributeID(int guiFixedAttributeID){
    this.guiFixedAttributeID = guiFixedAttributeID;
  }
  
  public void setName(String name){
    this.name = name;
  }
  
  public void setDhcpConf(String dhcpConf){
    this.dhcpConf = dhcpConf;
  }
  
  public void setValue(String value){
    this.value = value;
  }

  public void setValueRange(String valueRange){
    this.valueRange = valueRange;
  }
  
  public void setOptionEncoding(String optionEncoding){
    this.optionEncoding = optionEncoding;
  }
  

  private static class GuiFixedAttributeReader implements ResultSetReader<GuiFixedAttribute> {

    public GuiFixedAttribute read(ResultSet rs) throws SQLException {
      GuiFixedAttribute ga = new GuiFixedAttribute();
      GuiFixedAttribute.fillByResultSet(ga, rs);
      return ga;
    }

  }
  
  private static final GuiFixedAttributeReader reader = new GuiFixedAttributeReader();
  
  @Override
  public ResultSetReader<? extends GuiFixedAttribute> getReader() {
    return reader;
  }

  @Override
  public <U extends GuiFixedAttribute> void setAllFieldsFromData(U data2) {
    GuiFixedAttribute data = data2;
    guiFixedAttributeID = data.guiFixedAttributeID;
    name = data.name;
    dhcpConf = data.dhcpConf; 
    value = data.value;
    valueRange = data.valueRange;
    optionEncoding = data.optionEncoding;
  }
  
  public static void fillByResultSet(GuiFixedAttribute ga, ResultSet rs) throws SQLException {
    ga.guiFixedAttributeID = rs.getInt(COL_ATTRIBUTEID);
    ga.name = rs.getString(COL_NAME);
    ga.dhcpConf = rs.getString(COL_DHCPCONF);
    ga.value = rs.getString(COL_WERT);
    ga.valueRange = rs.getString(COL_WERTEBEREICH);
    ga.optionEncoding = rs.getString(COL_OPTIONENCODING);
  }

}
