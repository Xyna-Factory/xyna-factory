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
package com.gip.xyna.xfmg.xfctrl.datamodel.tr069;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelResult;
import com.gip.xyna.xfmg.xfctrl.datamodelmgmt.DataModelResult.MessageGroup;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Datatype;


/**
 *
 */
public class DataModelTypeImplTest {

  /**
   * @throws StringParameterParsingException 
   * @throws MalformedURLException 
   * @throws Ex_FileAccessException 
   * @throws XPRC_XmlParsingException 
   */
  public static void main(String[] args) throws StringParameterParsingException, XPRC_XmlParsingException, Ex_FileAccessException {
   
    Logger rootLogger = Logger.getRootLogger();
    rootLogger.setLevel(Level.DEBUG);
    rootLogger.removeAllAppenders();
    rootLogger.addAppender(new ConsoleAppender(
               new PatternLayout("XYNA %-5p [%t] (%C:%M:%L) - [%x] %m%n")));

    //Logger.getLogger(TR069Reader.class).setLevel(Level.TRACE);
    
    DataModelTypeImpl dmti = new DataModelTypeImpl();
    
    List<String> params = Arrays.<String>asList(
       //"modelRestrictions=VoiceService:1.0",
       //"modelRestrictions=STBService:1.0",
       //"modelRestrictions=Device:1.2",
       "modelRestrictions=InternetGatewayDevice:1.6",
       "information=ObjectModelTree,Documents"
        );
    Map<String, Object> importParamMap = 
        StringParameter.parse(params).with(ImportParameter.importParameters);
                                       
                                       
    ImportParameter parameter = new ImportParameter(importParamMap, "TR069");
    
    //
    
    DataModelResult dmr = new DataModelResult();
    
    String aPath = "/tmp/";
    
    TR069Import tr069Import = null;
    try {
      tr069Import = dmti.parseModulesAndSelectDataModels(dmr, parameter, 
                                                         Arrays.asList(aPath);
    } catch( Exception e ) {
      e.printStackTrace();
      printResult( dmr, true );
      return;
    }
    boolean proceedImport = true;
    
    if( ! parameter.getInformations().isEmpty() ) {
      InformationProcessor ip = new InformationProcessor(tr069Import, parameter);
      ip.process(dmr);
      proceedImport = ip.proceedImport();
    }
    
    
    printResult( dmr, true );
    
    if( ! proceedImport ) {
      return;
    }
    tr069Import.createModelDataTypes();

    System.out.println("\n");
    
    for( TR069ModelImport tmi : tr069Import.getTR069ModelImports() ) {
      System.err.println( tmi.getDataModel().toString().replaceAll(",","\n") );
      System.err.println();
      
      for( Datatype dt : tmi.getDataTypes() ) {
        System.err.println( dt.toXML() );
        break;
      }
      
    }
       
 
  }

  /**
   * @param dmr
   */
  private static void printResult(DataModelResult result, boolean printAllDatatypes) {
    if( result.hasSingleMessages() ) {
      System.out.println( result.singleMessagesToString("\n") );
    }
    if( result.hasMessageGroups() ) {
      for( MessageGroup mg :  result.getMessageGroups() ) {
        if( !printAllDatatypes ) {
          if( mg.getHeader().equals("Already existing data types") ) {
            continue;
          }
        }
        System.out.println( mg.toSingleString(":\n", "\n") );
      }
    }
  }


}
