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
package com.gip.xyna.xfmg.xfctrl.datamodel.xsd;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.db.DBConnectionData;
import com.gip.xyna.utils.db.Parameter;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.utils.db.SQLUtilsLogger;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.xfmg.exceptions.XFMG_NoSuchRevision;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.generation.TypeGeneration;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.generation.exceptions.WSDLParsingException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.generation.exceptions.XSDParsingException;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.DataTypeXmlHelper;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.TypeInfo;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.TypeMappingEntryHelper;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.TypeMappingEntryHelper.IDGenerator;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.DataModel;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.AbstractXynaPropertySource;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Datatype;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomGenerator.XmomGeneratorBuilder;


/**
 *
 */
public class TypeInfoGeneratorMain {

  private static final boolean removeExisting = true;

  public static void main(String[] args) throws FileNotFoundException, XSDParsingException, WSDLParsingException, StringParameterParsingException, XFMG_NoSuchRevision, XPRC_XmlParsingException, Ex_FileAccessException, ClassNotFoundException {
    initLoggerAndProperties();
  
    XmomGeneratorBuilder.useDefaultRevisionForTests = true;
    TypeMappingEntryHelper.setIdGenerator( new IDGeneratorImpl() );
    
    //String targetId = "fahrradTest1";
    String targetId = "cli";
    String basePath = "maptest";
    
    ArrayList<String> params = new ArrayList<String>();
    params.add("name="+targetId);
    params.add("workspaces=");
    //params.add("basePath=base");
    params.add("baseTypeName=xdnc.model.xsd.XSDBaseModel");
    params.add("labelCustomization=suffixForAttribute= (attribute)");
    
    Map<String, Object> map = StringParameter.parse(params).with(ImportParameter.importParameters);
    ImportParameter importParameter = ImportParameter.parameterForImport("XSD", map);
    
    DataModel dataModel = DataModelUtils.getDataModel(importParameter, "XSD");
    
    
    List<String> xsds = new ArrayList<String>();
    //xsds.add("map/sharedlibs/TypeMapping/src-test/Basic.xsd");
    //xsds.add("/tmp/Genros.xsd");
    //xsds.add("/tmp/mappingTest2");
    
    
    xsds = TypeInfoGeneratorTest.buildXSD( 
        "<xsd:complexType name=\"AnyTest_Type\">",
        "  <xsd:sequence>",
        "    <xsd:element name=\"valueA\" type=\"xsd:string\"/>",
        "    <xsd:element name=\"valueB\" type=\"xsd:anyType\"/>",
        "  </xsd:sequence>",
        "</xsd:complexType>",
        "<xsd:complexType name=\"valueB\">",
        "  <xsd:sequence>",
        "    <xsd:element name=\"el\" type=\"xsd:string\"/>",
        "  </xsd:sequence>",
        "</xsd:complexType>",
        "<xsd:element name=\"anyTest\" type=\"AnyTest_Type\" />",
        "<xsd:element name=\"vTest\" type=\"valueB\" />"
           );
    
    System.out.println( new File(".").getAbsolutePath() );
    
    System.out.println( "Generating types for target \""+targetId+"\" into \""+basePath+"\" from xsds "+xsds);
    
    TypeGeneration tg = new TypeGeneration(importParameter);
    System.out.println( "1. parsing XSDs");
    tg.parseXSDs(xsds);
    
    System.out.println( "2. generating dataTypes");
    tg.generateTypes();
    
    System.out.println( "XSD-Daten");
    System.out.println( TypeInfoGeneratorTest.xsdTypesToString(tg) );
    
    System.out.println( "XMOM-Daten"); 
    listToSysout( InformationUtils.xmomTypesToString(tg) );
    
    System.out.println( "TypeMappingEntry-Daten");
    List<TypeMappingEntry> tmes = null;

    TypeMappingEntryHelper tmeh = new TypeMappingEntryHelper(targetId);
    tmes = new ArrayList<TypeMappingEntry>();
    for( TypeInfo typeInfo : tg.getTypeInfos() ) {
      System.err.println(typeInfo  );
      String name =  "AnyTest_Type"; // "cliAdapterRequest"; //"routerParameterType";
      if( typeInfo.getName().getName().equals(name) ) {
         tmes = tmeh.toTypeMappingEntries(typeInfo);
        for( TypeMappingEntry tme : tmes ) {
          System.out.println(  tme.getKeyv() + "     "+tme.getValue() );
        }
      }
      //cliAdapterRequest
      //tmes.addAll( tmeh.toTypeMappingEntries(typeInfo) );
    }
    
    listToSysout( InformationUtils.xmomTypesToTree(tg) );
    
    DataTypeXmlHelper dtxh = new DataTypeXmlHelper(importParameter);
    int i=0;
    for( TypeInfo t : tg.getTypeInfos() ) {
      ++i;
      if( i< 3 ) {
        Datatype dt = dtxh.toDatatype(t, dataModel);
        System.out.println( dt.toXML() );
        
        TypeInfo tn = dtxh.toTypeInfo(dt);
        System.err.println( InformationUtils.xmomTypeToString(tn) );
        System.err.println( InformationUtils.xmomTypeToString(tn) );
      }
    }
    
    

  }
  

  private static void listToSysout(List<String> list) {
    for( String s : list ) {
      System.out.println(s);
    }
    
  }

  /**
   * 
   */
  private static void initLoggerAndProperties() {
    LoggingUtils.init(true);
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
