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
package com.gip.xyna.utils.db.failover;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import junit.framework.TestCase;

import com.gip.xyna.utils.db.DBConnectionData;

public class FailoverTest extends TestCase {

  private static final String URL_NORMAL = "jdbc:mysql://127.0.0.1/testdb";
  private static final String URL_FAILOVER = "jdbc:mysql://127.0.0.2/testdb";
  
  
  private DBConnectionData createBaseConnectionData() {
    DBConnectionData cd = DBConnectionData.newDBConnectionData().
    user("test").
    password("testPwd").
    url(URL_NORMAL).
    build();
    return cd;
  }
  
  
  public void testFailoverNone() {
    FailoverDBConnectionData fcd = FailoverDBConnectionData.newFailoverDBConnectionData().
    dbConnectionData(createBaseConnectionData()).
    failoverSource("none").
    failoverUrl(URL_FAILOVER).
    build(); 
    
    Failover failover = fcd.createNewFailover();
    
    assertEquals( failover.isFailover(), false );
  }
  
  public void testFailoverSpecial() {
    
    FailoverSources.addFailover("special", new FailoverSourceSpecial("") );
    
    FailoverDBConnectionData fcd = FailoverDBConnectionData.newFailoverDBConnectionData().
    dbConnectionData(createBaseConnectionData()).
    failoverSource("special").
    failoverSourceParam("a").
    failoverUrl(URL_FAILOVER).
    build(); 
    
    Failover failover = fcd.createNewFailover();
    
    assertEquals( failover.isFailover(), false );
    FailoverSourceSpecial.failoverA = true;
    assertEquals( failover.isFailover(), true );
    
  }
  private static class FailoverSourceSpecial implements FailoverSource {
    public static boolean failoverA = false;
    public static boolean failoverB = false;
    private String type;
    
    public FailoverSourceSpecial(String type) {
      this.type = type;
    }

    public boolean isFailover() {
      if( "a".equals(type) ) return failoverA;
      if( "b".equals(type) ) return failoverB;
      return false;
    }

    public FailoverSource newInstance(String failoverParam) {
      return new FailoverSourceSpecial(failoverParam);
    }
    
  }
  
  
  
  public void testFailoverFile() {
    File file = new File("failoverSource.dat");
    try {
   
      FailoverDBConnectionData fcd = FailoverDBConnectionData.newFailoverDBConnectionData().
      dbConnectionData(createBaseConnectionData()).
      failoverSource("file").
      failoverSourceParam(file.getName()).
      failoverUrl(URL_FAILOVER).
      build(); 

      Failover failover = fcd.createNewFailover();
      assertEquals( false , failover.isFailover() );

      writeIntoFile( file, "1" );
      assertEquals( false, failover.isFailover() );

      writeIntoFile( file, "2" );
      assertEquals( true, failover.isFailover() );

      writeIntoFile( file, "1" );
      assertEquals( false, failover.isFailover() );

    } finally {
      file.delete();
    }
  }
  private void writeIntoFile(File file, String string) {
    FileWriter fw;
    try {
      fw = new FileWriter(file);
      fw.write(string);
      fw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void testFailoverData() {
    File file = new File("failoverSource.dat");
    try {
      FailoverDBConnectionData fcd = FailoverDBConnectionData.newFailoverDBConnectionData().
      dbConnectionData(createBaseConnectionData()).
      failoverSource("file").
      failoverSourceParam(file.getName()).
      failoverUrl(URL_FAILOVER).
      build(); 

      Failover failover = fcd.createNewFailover();
      
      writeIntoFile( file, "1" );
      assertEquals( URL_NORMAL, failover.getConnectionData().getUrl() );

      writeIntoFile( file, "2" );
      assertEquals( URL_FAILOVER, failover.getConnectionData().getUrl() );

    } finally {
      file.delete();
    }
  }

  //TODO weitere Test zu:
  //failover.createSQLUtils(sqlUtilsLogger);
  //failover.checkAndRecreate(sqlUtils, sqlUtilsLogger);
  //failover.recreateSQLUtils(sqlUtils, sqlUtilsLogger);

  
}
