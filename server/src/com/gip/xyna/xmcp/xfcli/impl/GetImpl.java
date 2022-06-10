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
package com.gip.xyna.xmcp.xfcli.impl;



import java.io.OutputStream;
import java.util.EnumSet;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyWithDefaultValue;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Get;



public class GetImpl extends XynaCommandImplementation<Get> {

  public void execute(OutputStream statusOutputStream, Get payload) throws XynaException {
    if (XynaFactory.getInstance().getFactoryManagement() == null
        || XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS() == null
        || XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration() == null) {
      throw new RuntimeException("Command may not be executed right now.");
    }
    Configuration configuration = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration();
    
    XynaPropertyWithDefaultValue property = configuration.getPropertyWithDefaultValue(payload.getKey());
    
    if( property == null) {
      String output = "Property '" + payload.getKey() + "' is not set.\n";
      writeToCommandLine(statusOutputStream, output);
      return;
    }
    
    if( !payload.getShowDoc() && !payload.getVerbose() ) {
      //altes Verhalten für Abwärtskompatibilität
      if ( property.getValue() != null ) {
        writeToCommandLine(statusOutputStream, "Value of property '", payload.getKey(), "': ", property.getValue(), "\n");
      } else {
        String output = "Property '" + payload.getKey() + "' is not set.\n";
        writeToCommandLine(statusOutputStream, output);
      }
    } else {
      StringBuilder output = new StringBuilder();
      Mode mode = Mode.Verbose;
      if( payload.getVerbose() ) {
        mode = Mode.ExtraVerbose;
      }
      boolean prettyPrint = payload.getPrettyPrint();
      EnumSet<DocumentationLanguage> showDoc = getShowDoc(payload.getShowDoc(), payload.getLang() );

      appendProperty(output, mode, property, showDoc, null, prettyPrint);
      writeToCommandLine(statusOutputStream, output.toString());
    }
  }

  private EnumSet<DocumentationLanguage> getShowDoc(boolean showdoc, String lang) {
    if( showdoc ) {
      if( "EN".equalsIgnoreCase(lang) ) {
        return EnumSet.of(DocumentationLanguage.EN);
      } else if( "DE".equalsIgnoreCase(lang) ) {
        return EnumSet.of(DocumentationLanguage.DE);
      } else if( "ALL".equalsIgnoreCase(lang) ) {
        return EnumSet.allOf(DocumentationLanguage.class);
      } else {
        return EnumSet.of(DocumentationLanguage.EN); //Default
      }
    } else {
      return EnumSet.noneOf(DocumentationLanguage.class);
    }
  }

  /**
   * TODO analoge Methode in ListPropertiesImpl
   * @param output
   * @param mode
   * @param property
   * @param showDoc
   * @param prettyPrint
   */
  private void appendProperty(StringBuilder output, Mode mode, XynaPropertyWithDefaultValue property,
                              EnumSet<DocumentationLanguage> showDoc, 
                              Object applicationNames, boolean prettyPrint) {
    String sep = prettyPrint ? "\n\t" : " ";
    if( prettyPrint ) {
      output.append(property.getName());
    } else {
      output.append("Name: ").append(property.getName());
    }
    if ( mode != Mode.Normal ) {
      output.append(sep).append("Value: ");
      if(property.getValue() == null) {
        output.append("not defined");
      } else {
        output.append("'").append(property.getValue()).append("'");
      }
    }
    if( mode == Mode.ExtraVerbose ) {
      if(property.getDefValue() != null) {
        output.append(sep).append("Default value: ");
        output.append("'").append(property.getDefValue()).append("'");          
      }
    }
    output.append(sep);
    appendDocumentation(output, showDoc, property, prettyPrint);
    output.append(sep);
    //appendReader(output, property, applicationNames);
  }
  
  private void appendDocumentation(StringBuilder output, EnumSet<DocumentationLanguage> showDoc,
                                   XynaPropertyWithDefaultValue property, boolean prettyPrint) {
    //Documentation mit ausgeben (falls nicht vorhanden, wird die defaultDocumentation ausgegeben)
    if( showDoc.size() == 0 ) {
      return;
    }
    String sep = "Documentation: ";
    for (DocumentationLanguage lang: showDoc ) {
      output.append(sep);
      sep = prettyPrint ? "\n\t               " : " ";
      String doc = property.getDocumentation().get(lang);
      if (doc != null && doc.length() > 0) {
        output.append(lang.toString()).append(": '").append(doc).append("'");
        continue;
      }
      doc = property.getDefDocumentation().get(lang);
      if (doc != null) {
        output.append(lang.toString()).append(": '").append(doc).append("'");
      } else {
        if( prettyPrint ) {
          output.append(lang.toString()).append(": ''");
        }
      }
    }
  }

  private static enum Mode {
    Normal, Verbose, ExtraVerbose;
  }

  
}
