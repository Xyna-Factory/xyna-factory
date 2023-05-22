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
package com.gip.xyna.xdev.map.typegen;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.gip.xyna.utils.db.DBConnectionData;
import com.gip.xyna.utils.db.Parameter;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.utils.db.SQLUtilsLogger;
import com.gip.xyna.xdev.map.TypeMappingEntry;
import com.gip.xyna.xdev.map.typegen.TypeGenerationOptions.HostInXmomPath;
import com.gip.xyna.xdev.map.typegen.exceptions.WSDLParsingException;
import com.gip.xyna.xdev.map.typegen.exceptions.XSDParsingException;
import com.gip.xyna.xdev.map.types.TypeMappingEntryHelper;
import com.gip.xyna.xdev.map.types.TypeMappingEntryHelper.IDGenerator;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.AbstractXynaPropertySource;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomType;


/**
 *
 */
public class TypeInfoGeneratorMain {

  private static final boolean saveToDB = false;
  private static final boolean removeExisting = true;

  public static void main(String[] args) throws FileNotFoundException, XSDParsingException, WSDLParsingException {
    initLoggerAndProperties();
  
    TypeMappingEntryHelper.setIdGenerator( new IDGeneratorImpl() );
    
    String targetId = "bug19119N";
    String basePath = "bug.test";
    
    TypeGenerationOptions tgo = new TypeGenerationOptions();
    tgo.setBasePathForGeneration(basePath);
    //tgo.setUseNamespaceForXmomPath(true);
    tgo.setChangeLabelForAttribute(true);
    tgo.setHostInXmomPath(HostInXmomPath.omitHost);
    tgo.setBaseXmomType(new XmomType("base", "Base","Base"));
    
    List<String> xsds = new ArrayList<String>();
    xsds.add("map/sharedlibs/TypeMapping/src-test/xsds/bug19119.xsd");
    
    
    
    System.out.println( new File(".").getAbsolutePath() );
    
    System.out.println( "Generating types for target \""+targetId+"\" into \""+basePath+"\" from xsds "+xsds);
    
    TypeGeneration tg = new TypeGeneration(tgo);
    System.out.println( "1. parsing XSDs");
    tg.parseXSDs(xsds);
    
    System.out.println( "2. generating dataTypes");
    tg.generateTypes();
    
    System.out.println( "XSD-Daten");
    System.out.println( TypeInfoGeneratorTest.xsdTypesToString(tg) );
    
    System.out.println( "XMOM-Daten");
    System.out.println( TypeInfoGeneratorTest.xmomTypesToString(tg) );
    
    System.out.println( "TypeMappingEntry-Daten");
    List<TypeMappingEntry> tmes = tg.createTypeMappingEntries(targetId);
    for( TypeMappingEntry tme : tmes ) {
      System.out.println(  tme.getKeyv() + "     "+tme.getValue() );
    }
    
    
    if( saveToDB ) {
      DBConnectionData dbd = DBConnectionData.newDBConnectionData().
        user("").password("").url("").build();

      storeTypeMappingEntries(dbd, tmes);
    }
    
  }
  
  /**
   * 
   */
  private static void initLoggerAndProperties() {
    Logger rootLogger = Logger.getRootLogger();
    rootLogger.setLevel(Level.DEBUG);
    rootLogger.removeAllAppenders();
    rootLogger.addAppender(new ConsoleAppender(
               new PatternLayout("XYNA %-5p [%t] (%C:%M:%L) - [%x] %m%n")));
   
    //Properties initialisieren
    XynaPropertyUtils.exchangeXynaPropertySource(new AbstractXynaPropertySource(){
      public String getProperty(String name) {
        return null; //verwendet default
      }
    } );

  }

  private static void storeTypeMappingEntries(DBConnectionData dbd, List<TypeMappingEntry> tmes) {
    if( tmes.size() == 0 ) {
      return; //nichts zu tun
    }
    String targetId = tmes.get(0).getId();
    
    SQLUtils sqlUtils = dbd.createSQLUtils(new SQLUtilsLogger() {
      public void logSQL(String arg0) {
      }
      public void logException(Exception e) {
        e.printStackTrace();
      }
    });
    try {
      String query1 = "SELECT count(*) FROM TypeMapping WHERE id=?";
      Integer exist = sqlUtils.queryInt(query1, new Parameter(targetId) );
      if( exist > 0 ) {
        System.out.println("TargetId "+targetId+" existiert bereits!");
        if( removeExisting ) {
          String delete = "DELETE FROM TypeMapping WHERE id=?";
          sqlUtils.executeDML(delete, new Parameter(targetId) ); 
        } else {
          System.err.println("TargetId "+targetId+" existiert bereits!");
          return;
        }
      }

      String query2 = "SELECT max(pk) FROM TypeMapping";
      Integer max = sqlUtils.queryInt(query2, new Parameter() );
      int pkOffset = max == null ? 1 : max.intValue()+1;
      System.out.println( "pkOffset = "+pkOffset );

      String sql = "Insert INTO TypeMapping(pk, id, keyv, value) VALUES (?,?,?,?)";

      for( TypeMappingEntry tme : tmes ) {
        long pk = tme.getPk()+pkOffset;
        Parameter param = new Parameter( pk, targetId, tme.getKeyv(), tme.getValue() );

        sqlUtils.executeDML(sql, param);
        if( sqlUtils.getLastException() != null ) {
          System.out.println( "Fehler bei "+ pk +" " + tme.getKeyv());
        }
      }
      sqlUtils.commit();
    } finally {
      sqlUtils.closeConnection();
    }
  }


  public static class IDGeneratorImpl implements IDGenerator {
    AtomicLong idgen = new AtomicLong();
    
    public long getId() {
      return idgen.getAndIncrement();
    }
    
  }


}
