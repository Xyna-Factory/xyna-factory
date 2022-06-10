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
package com.gip.xyna.utils.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.sql.DataSource;

/**
 * Class for managing connections to a datasource.
 */
public class DBConnector {

   public static void closeConnection(Connection con) throws SQLException {
      if ((null != con) && !con.isClosed()) {
        setClientInfo(con,"");
        con.close();
      }
   }

   /**
   * @deprecated use getConnection with clientInfo-String
   * @param datasource
   * @return
   * @throws Exception
   */
    public static Connection getConnection(String datasource) throws Exception {
      String additionalInfo= getCaller();      
      return getConnection(datasource, "XU.DBC "+datasource+" "+additionalInfo);  
    }

  private static String getCaller() {
    StackTraceElement ste[]=new Throwable("").getStackTrace();
    for(int i=0;i<ste.length;i++){
      if (ste[i].getClassName().indexOf("com.gip.xyna.utils.db")<0){
        return ste[i].getFileName()+"["+ste[i].getLineNumber()+"]";        
      }
     }
     int i=ste.length-1;
     return ste[i].getFileName()+"["+ste[i].getLineNumber()+"]";//wird i.A. oben verlassen
  }
   
   
   
   public static Connection getConnection(String datasource, String clientInfo) throws Exception {
      Connection conn = null;
      DataSource ds = null;
      InitialContext ic = new InitialContext();
      ds = (DataSource) ic.lookup(datasource);
      conn = ds.getConnection();
      if (conn.isClosed()) {
         // TODO: throw XynaException
         throw new Exception("connection ist schon geschlossen");
      }
      setClientInfo(conn, clientInfo);
      return conn;
   }
   
   public static void setClientInfo(Connection con, String clientInfo) {
      PreparedStatement ps=null;
      try{
        ps=con.prepareStatement("{call DBMS_APPLICATION_INFO.SET_CLIENT_INFO(?)}");
        ps.setString(1,clientInfo);
        ps.execute();
      } catch(Throwable t){
        try{ps.close();}catch(Throwable t2 ){}
      }
      
     
   }
}
