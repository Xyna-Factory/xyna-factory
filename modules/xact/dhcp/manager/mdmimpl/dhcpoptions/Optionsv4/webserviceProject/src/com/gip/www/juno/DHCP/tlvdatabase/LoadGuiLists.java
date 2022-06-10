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

package com.gip.www.juno.DHCP.tlvdatabase;



import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import org.apache.log4j.Logger;

import com.gip.juno.ws.tools.SQLUtilsCache;
import com.gip.juno.ws.tools.SQLUtilsContainer;
import com.gip.www.juno.DHCP.WS.Optionsv4.Optionsv4BindingImpl;
import com.gip.xyna.utils.db.ResultSetReader;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.xdnc.dhcp.db.storables.Condition;
import com.gip.xyna.xdnc.dhcp.db.storables.DeviceClass;
import com.gip.xyna.xdnc.dhcp.db.storables.GuiAttribute;
import com.gip.xyna.xdnc.dhcp.db.storables.GuiFixedAttribute;
import com.gip.xyna.xdnc.dhcp.db.storables.GuiParameter;
import com.gip.xyna.xdnc.dhcp.db.storables.PoolType;



public class LoadGuiLists {

  static Logger logger = Logger.getLogger(Optionsv4BindingImpl.class);


  private SQLUtilsContainer sqlUtilsContainer = null;


  private static ResultSetReader<GuiAttribute> guiattributereader = new ResultSetReader<GuiAttribute>() {

    public GuiAttribute read(ResultSet rs) throws SQLException {

      int guiattributeid = rs.getInt("guiAttributeID");
      String name = rs.getString("name");
      String dhcpConf = rs.getString("dhcpConf");
      String xdhcpConf = rs.getString("xdhcpConf");
      String werteBereich = rs.getString("werteBereich");
      String optionEncoding = rs.getString("optionEncoding");

      werteBereich = werteBereich.replace("\\","\\\\");
      
      
      GuiAttribute g = new GuiAttribute();
      g.setGuiAttributeID(guiattributeid);
      g.setName(name);
      g.setOptionEncoding(optionEncoding);
      g.setDhcpConf(dhcpConf);
      g.setXdhcpConf(xdhcpConf);
      g.setWerteBereich(werteBereich);
      return g;
    }
  };

  private static ResultSetReader<GuiFixedAttribute> guifixedattributereader = new ResultSetReader<GuiFixedAttribute>() {

    public GuiFixedAttribute read(ResultSet rs) throws SQLException {

      int guifixedattributeid = rs.getInt("guiFixedAttributeID");
      String name = rs.getString("name");
      String dhcpConf = rs.getString("dhcpConf");
      String xdhcpConf = rs.getString("xdhcpConf");
      String optionEncoding = rs.getString("optionEncoding");
      String value = rs.getString("value");
      String valueRange = rs.getString("valueRange");


      GuiFixedAttribute g = new GuiFixedAttribute();
      g.setFixedAttributeID(guifixedattributeid);
      g.setName(name);
      g.setOptionEncoding(optionEncoding);
      g.setDhcpConf(dhcpConf);
      g.setXdhcpConf(xdhcpConf);
      g.setValue(value);
      g.setValueRange(valueRange);
      return g;
    }
  };

  private static ResultSetReader<GuiParameter> guiparameterreader = new ResultSetReader<GuiParameter>() {

    public GuiParameter read(ResultSet rs) throws SQLException {

      int guiparameterid = rs.getInt("guiParameterID");
      String name = rs.getString("name");
      String dhcpConf = rs.getString("dhcpConf");
      String xdhcpConf = rs.getString("xdhcpConf");


      GuiParameter g = new GuiParameter();
      g.setGuiParameterID(guiparameterid);
      g.setName(name);
      g.setDhcpConf(dhcpConf);
      g.setXdhcpConf(xdhcpConf);
      return g;
    }
  };

  private static ResultSetReader<DeviceClass> devicereader = new ResultSetReader<DeviceClass>() {

    public DeviceClass read(ResultSet rs) throws SQLException {

      // int classId = rs.getInt("classID");
      // String name = rs.getString("name");
      // String attributes = rs.getString("attributes");
      // String fixedAttributes = rs.getString("fixedAttributes");
      // String conditional = rs.getString("conditional");
      // int priority = rs.getInt("priority");

      DeviceClass d = new DeviceClass();
      DeviceClass.fillByResultSet(d, rs);
      return d;
    }
  };

  private static ResultSetReader<Condition> conditionreader = new ResultSetReader<Condition>() {

    public Condition read(ResultSet rs) throws SQLException {

      Condition c = new Condition();
      Condition.fillByResultSet(c, rs);
      return c;
    }
  };

  private static ResultSetReader<PoolType> pooltypereader = new ResultSetReader<PoolType>() {

    public PoolType read(ResultSet rs) throws SQLException {

      PoolType p = new PoolType();
      PoolType.fillByResultSet(p, rs);
      return p;
    }
  };


  public void setUp() throws Exception {

    try {
      sqlUtilsContainer = SQLUtilsCache.getForManagement("dhcp", logger);
    }
    catch (Exception e) {
      //logger.info("Error initializing database access to dhcp", e);
      throw new Exception("Error initializing database access to dhcp", e);
    }
  }

  public void undeploy() throws Exception {
    try {
      SQLUtilsCache.release(sqlUtilsContainer, logger);
    }
    catch (Exception e) {
      // logger.info("", e);
      throw e;
    }
  }


  public void createListOfGuiAttributeEntries(Collection<GuiAttribute> liste) throws Exception {
    try {
      // erst loeschen, dann schreiben
      SQLUtils sqlUtils = sqlUtilsContainer.getSQLUtils();

      String query = "DELETE from guiattribute";

      sqlUtils.executeDML(query, new com.gip.xyna.utils.db.Parameter());

      sqlUtils.commit();

      // liste schreiben
      for (GuiAttribute g : liste) {
        query = "INSERT INTO `guiattribute` (`guiAttributeID`, `name`, `optionEncoding`, `dhcpConf`, `xdhcpConf`, `werteBereich`) VALUES ("+g.getGuiAttributeID()+", '"+g.getName()+"', '"+g.getOptionEncoding()+"', '"+g.getDhcpConf()+"', '"+g.getXdhcpConf()+"', '"+g.getWerteBereich()+"')";

        sqlUtils.executeDML(query, new com.gip.xyna.utils.db.Parameter());

        sqlUtils.commit();

      }
    }
    catch (Exception e) {
      //logger.info("Error deleting and writing guiattribute", e);
      throw new Exception("Error deleting and writing guiattribute", e);
    }
  }


  public void createListOfGuiFixedAttributeEntries(Collection<GuiFixedAttribute> liste)
                  throws Exception {
    try {
      // erst loeschen, dann schreiben
      SQLUtils sqlUtils = sqlUtilsContainer.getSQLUtils();

      String query = "DELETE from guifixedattribute";

      sqlUtils.executeDML(query, new com.gip.xyna.utils.db.Parameter());

      sqlUtils.commit();

      // liste schreiben
      for (GuiFixedAttribute g : liste) {
        query = "INSERT INTO `guifixedattribute` (`guiFixedAttributeID`, `name`, `optionEncoding`, `dhcpConf`, `xdhcpConf`, `value`, `valueRange`) VALUES ("+g.getGuiFixedAttributeID()+", '"+g.getName()+"', '"+g.getOptionEncoding()+"', '"+g.getDhcpConf()+"', '"+g.getXdhcpConf()+"', '"+g.getValue()+"', '"+g.getValueRange()+"')";

        sqlUtils.executeDML(query, new com.gip.xyna.utils.db.Parameter());

        sqlUtils.commit();

      }
    }
    catch (Exception e) {
      //logger.info("Error deleting and writing guifixedattribute", e);
      throw new Exception("Error deleting and writing guifixedattribute",e);
    }
  
  }


  public void createListOfGuiParameterEntries(Collection<GuiParameter> liste) throws Exception {
    try {
      // erst loeschen, dann schreiben
      SQLUtils sqlUtils = sqlUtilsContainer.getSQLUtils();

      String query = "DELETE from guiparameter";

      sqlUtils.executeDML(query, new com.gip.xyna.utils.db.Parameter());

      sqlUtils.commit();

      // liste schreiben
      for (GuiParameter g : liste) {
        query = "INSERT INTO `guiparameter` (`guiParameterID`, `name`, `dhcpConf`, `xdhcpConf`) VALUES ("+g.getGuiParameterID()+", '"+g.getName()+"', '"+g.getDhcpConf()+"', '"+g.getXdhcpConf()+"')";

        sqlUtils.executeDML(query, new com.gip.xyna.utils.db.Parameter());

        sqlUtils.commit();

      }
    }
    catch (Exception e) {
      //logger.info("Error deleting and writing guiparameter", e);
      throw new Exception("Error deleting and writing guiparameter", e);
    }
    }


  public Collection<GuiAttribute> loadGuiAttribute() throws Exception {
    // Zugriff auf Datenbank optionsv4 um Decoder zu initialisieren
    Collection<GuiAttribute> guiattribute = null;
    try {
      SQLUtils sqlUtils = sqlUtilsContainer.getSQLUtils();

      String query = "SELECT * from guiattribute";

      guiattribute = sqlUtils.query(query, new com.gip.xyna.utils.db.Parameter(), guiattributereader);

      sqlUtils.commit();

      return guiattribute;
    }
    catch (Exception e) {
      //logger.info("Error reading guiattribute", e);
      throw new Exception("Error reading guiattribute", e);
    }
    
  }


  public Collection<GuiFixedAttribute> loadGuiFixedAttribute() throws Exception {
    Collection<GuiFixedAttribute> guifixedattribute = null;
    try {
      SQLUtils sqlUtils = sqlUtilsContainer.getSQLUtils();

      String query = "SELECT * from guifixedattribute";

      guifixedattribute = sqlUtils.query(query, new com.gip.xyna.utils.db.Parameter(), guifixedattributereader);

      sqlUtils.commit();

      return guifixedattribute;
    }
    catch (Exception e) {
      //logger.info("Error reading guifixedattribute", e);
      throw new Exception("Error reading guifixedattribute", e);
    }
  }


  public Collection<GuiParameter> loadGuiParameter() throws Exception {
    Collection<GuiParameter> guiparameter = null;
    try {
      SQLUtils sqlUtils = sqlUtilsContainer.getSQLUtils();

      String query = "SELECT * from guiparameter";

      guiparameter = sqlUtils.query(query, new com.gip.xyna.utils.db.Parameter(), guiparameterreader);

      sqlUtils.commit();

      return guiparameter;
    }
    catch (Exception e) {
      //logger.info("Error reading guiattribute", e);
      throw new Exception("Error reading guiattribute", e);
    }
   
  }


  public Collection<DeviceClass> loadDeviceClass() throws Exception {
    Collection<DeviceClass> deviceclass = null;
    try {
      SQLUtils sqlUtils = sqlUtilsContainer.getSQLUtils();

      String query = "SELECT * from class";

      deviceclass = sqlUtils.query(query, new com.gip.xyna.utils.db.Parameter(), devicereader);

      sqlUtils.commit();

      return deviceclass;
    }
    catch (Exception e) {
      //logger.info("Error reading class", e);
      throw new Exception("Error reading class", e);
    }
  }


  public Collection<Condition> loadCondition() throws Exception {
    Collection<Condition> condition = null;
    try {
      SQLUtils sqlUtils = sqlUtilsContainer.getSQLUtils();

      String query = "SELECT * from classcondition";

      condition = sqlUtils.query(query, new com.gip.xyna.utils.db.Parameter(), conditionreader);

      sqlUtils.commit();

      return condition;
    }
    catch (Exception e) {
      //logger.info("Error reading classcondition", e);
      throw new Exception("Error reading classcondition", e);
    }
    
  }


  public Collection<PoolType> loadPoolType() throws Exception {
    Collection<PoolType> pooltype = null;
    try {
      SQLUtils sqlUtils = sqlUtilsContainer.getSQLUtils();

      String query = "SELECT * from pooltype";

      pooltype = sqlUtils.query(query, new com.gip.xyna.utils.db.Parameter(), pooltypereader);

      sqlUtils.commit();

      return pooltype;
    }
    catch (Exception e) {
      //logger.info("Error reading pooltype", e);
      throw new Exception("Error reading pooltype", e);
    }
    
  }


}
