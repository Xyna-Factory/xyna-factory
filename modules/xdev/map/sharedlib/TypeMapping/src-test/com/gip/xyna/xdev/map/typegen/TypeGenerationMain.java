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
package com.gip.xyna.xdev.map.typegen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.AbstractXynaPropertySource;


/**
 * Versuch, TypeGeneration direkt aus dem Jar aufzurufen. Kann leider nicht klappen, da 
 * IDGeneration und ODS nicht funktionieren. Deployment etc. könnte noch über RMI abgewickelt 
 * werden, das Speichern der TypeMappings allerdings nicht...
 */
public class TypeGenerationMain {
  
  public static void main(String[] args) throws Exception {
    
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
    
    if( args == null || args.length <3 ) {
      System.err.println( "Missing parameters <targetId> <basePath> <xsd>... ");
      System.err.println( "Example newProject xact.np projectBase.xsd projectMain.xsd" );
      return;
    }
    
    String targetId = args[0];
    String basePath = args[1];
    
    List<String> xsds = new ArrayList<String>();
    for( int p=2; p <args.length; ++p ) {
      xsds.add( args[p] );
    }
    
    System.out.println( "Generating types for target \""+targetId+"\" into \""+basePath+"\" from xsds "+xsds);
    
    TypeGenerationOptions tgo = new TypeGenerationOptions();
    tgo.setBasePathForGeneration(basePath);
    tgo.setUseNamespaceForXmomPath(true);
    tgo.setChangeLabelForAttribute(true);
      
    TypeGeneration tg = new TypeGeneration(tgo);
    System.out.println( "1. parsing XSDs");
    tg.parseXSDs(xsds);
    
    System.out.println( "2. generating dataTypes");
    tg.generateTypes();
    
    System.out.println( "3. saving and deploying dataTypes");
    tg.saveAndDeployDataTypes();
    
    System.out.println( "4. storing TypeMapping");
    tg.storeTypeMappings(null,targetId);
    
    System.out.println( "Generated dataTypes are "); 
    List<String> generatedTypes = tg.getDataTypes();
    Collections.sort(generatedTypes);
    System.out.println( generatedTypes );
  }

}
