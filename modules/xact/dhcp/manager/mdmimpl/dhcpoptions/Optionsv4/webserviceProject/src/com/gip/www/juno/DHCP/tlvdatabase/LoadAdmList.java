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
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.juno.ws.tools.SQLUtilsCache;
import com.gip.juno.ws.tools.SQLUtilsContainer;
import com.gip.www.juno.DHCP.WS.Optionsv4.Optionsv4BindingImpl;
import com.gip.xyna.utils.db.ResultSetReader;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.xact.trigger.tlvencoding.util.StringToMapUtil;



public class LoadAdmList {

  static Logger logger= Logger.getLogger(Optionsv4BindingImpl.class);

  
  private SQLUtilsContainer sqlUtilsContainer = null;

  
  private static ResultSetReader<DHCPEncoding> optionsV4reader = new ResultSetReader<DHCPEncoding>() {

    public DHCPEncoding read(ResultSet rs) throws SQLException {

      String tmp = rs.getString("valuedatatypeargumentsstring");
      try {
      tmp = tmp.replace("{", "");
      tmp = tmp.replace("}", "");
      tmp = tmp.replace(" ", "");
      } catch (Exception e) {
        throw new SQLException("Entry in column valuedatatypeargumentsstring may not be null",e);
      }

      // logger.info("Reading Encoding");
      // logger.info(rs.getInt("id"));
      // logger.info(rs.getInt("parentid"));
      // logger.info(rs.getString("typename"));
      // logger.info(rs.getLong("typeencoding"));
      // logger.info(rs.getInt("enterprisenr"));
      // logger.info(rs.getString("valuedatatypename"));

      int id = rs.getInt("id");
      Integer parentid = rs.getInt("parentid");
      if (parentid == 0)
        parentid = null;
      String typename = rs.getString("typename");
      Long typeencoding = rs.getLong("typeencoding");
      Integer enterprisenr = rs.getInt("enterprisenr");
      if (enterprisenr == 0)
        enterprisenr = null;
      String valuedatatypename = rs.getString("valuedatatypename");
      
      boolean readOnly = rs.getBoolean("readonly");
      String statusflag = rs.getString("status");
      String guiname = rs.getString("guiname");
      String guiattribute = rs.getString("guiattribute");
      String guifixedattribute = rs.getString("guifixedattribute");
      String guiparameter = rs.getString("guiparameter");
      int guiattributeid = rs.getInt("guiattributeid");
      int guifixedattributeid = rs.getInt("guifixedattributeid");
      int guiparameterid = rs.getInt("guiparameterid");
      String guiattributewertebereich = rs.getString("guiattributewertebereich");
      String guifixedattributevalue = rs.getString("guifixedattributevalue");

      try {
      guiattributewertebereich = guiattributewertebereich.replace("\\","\\\\");
      } catch (Exception e) {
        if (guiattributewertebereich == null){
          guiattributewertebereich = "";
        } else {
          throw new SQLException("Troubles formatting guiattributewertebereich",e);
        }
        
      }

      DHCPEncoding e = new DHCPEncoding(id, parentid, typename, typeencoding, enterprisenr, valuedatatypename,
                                            StringToMapUtil.toMap(tmp),readOnly,statusflag,guiname,guiattribute,guifixedattribute,guiparameter,
                                            guiattributeid,guifixedattributeid,guiparameterid,guiattributewertebereich,guifixedattributevalue);
      return e;
    }

  };


  public void setUp() throws Exception {
    try {
      sqlUtilsContainer = SQLUtilsCache.getForManagement("xynadhcp", logger);
    }
    catch (Exception e) {
      //logger.info("Error initializing database access to xynadhcp", e);
      throw new Exception("Error initializing database access to xynadhcp",e);
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

  

  public void createListOfDHCPEncodingEntry(List<DHCPEncoding> liste) throws Exception {
    try {
      
      // erst loeschen, dann schreiben
      SQLUtils sqlUtils = sqlUtilsContainer.getSQLUtils();

      String query = "DELETE from optionsv4";

      sqlUtils.executeDML(query, new com.gip.xyna.utils.db.Parameter());
      //sqlUtils.query(query, new com.gip.xyna.utils.db.Parameter(), optionsV4reader);
      
      sqlUtils.commit();
      
      // liste schreiben
      for(DHCPEncoding e:liste)
      {
        query = e.getMySQLInsert();

        //sqlUtils.query(query, new com.gip.xyna.utils.db.Parameter(), optionsV4reader);
        sqlUtils.executeDML(query, new com.gip.xyna.utils.db.Parameter());
        
        sqlUtils.commit();
        
      }
    }
    catch (Exception e) {
      //logger.info("Error deleting and writing optionsv4", e);
      throw new Exception("Error deleting and writing optionsv4",e);
    }

  }


  public Collection<DHCPEncoding> loadDHCPEntries() throws Exception {
    Collection<DHCPEncoding> optionsv4 = null;
    try {
      SQLUtils sqlUtils = sqlUtilsContainer.getSQLUtils();

      String query = "SELECT * from optionsv4";

      optionsv4 = sqlUtils.query(query, new com.gip.xyna.utils.db.Parameter(), optionsV4reader);
      
      sqlUtils.commit();
      
      return optionsv4;
    }
    catch (Exception e) {
      //logger.info("Error reading optionsv4", e);
      throw new Exception("Error reading optionsv4",e);

    }

  }


}
