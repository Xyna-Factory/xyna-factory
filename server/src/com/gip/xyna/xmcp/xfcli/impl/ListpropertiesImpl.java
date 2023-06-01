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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyNode;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyWithDefaultValue;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listproperties;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;



public class ListpropertiesImpl extends XynaCommandImplementation<Listproperties> {

  private static enum Mode {
    Normal, Verbose, ExtraVerbose;
  }
  
  public void execute(OutputStream statusOutputStream, Listproperties arg) throws XynaException {

    StringBuilder output = new StringBuilder();

    Collection<XynaPropertyWithDefaultValue> properties = factory.getXynaMultiChannelPortalPortal().getPropertiesWithDefaultValuesReadOnly();
    Mode mode = Mode.Normal;
    if( arg.getExtraverbose() ) {
      mode = Mode.ExtraVerbose;
    } else {
      if( arg.getVerbose() ) {
        mode = Mode.Verbose;
      }
    }
    
    if(!arg.getExtraverbose()) {
      Set<XynaPropertyWithDefaultValue> toBeRemoved = new HashSet<XynaPropertyWithDefaultValue>();
      for(XynaPropertyWithDefaultValue entry : properties) {
        if(entry.getValue() == null) {
          toBeRemoved.add(entry);
        }
      }
      properties.removeAll(toBeRemoved);
    }
    if (properties == null || properties.size() == 0) {
      writeToCommandLine(statusOutputStream, "No XynaProperties registered for server\n");
      return;
    }

    writeToCommandLine(statusOutputStream, "Listing information for " + properties.size() + " XynaProperties...\n");
    
    List<Long> allRevisions = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                    .getRevisionManagement().getAllRevisions();
    Map<Long, RuntimeContext> runtimeContexts = new HashMap<Long, RuntimeContext>();
    for(Long revision : allRevisions) {
      try {
        RuntimeContext rc = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                        .getRevisionManagement().getRuntimeContext(revision);
        runtimeContexts.put(revision, rc);
      } catch(XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        // ignore
      }
    }
    
    // Properties sortieren
    List<XynaPropertyWithDefaultValue> sortedProps = new ArrayList<XynaPropertyWithDefaultValue>(properties);
    Collections.sort(sortedProps, new Comparator<XynaPropertyWithDefaultValue>() {

      public int compare(XynaPropertyWithDefaultValue o1, XynaPropertyWithDefaultValue o2) {
        return o1.getName().compareTo(o2.getName());
      }
    });
    
    EnumSet<DocumentationLanguage> showDoc = getShowDoc(arg.getShowDocumentation(), arg.getLang() );
    
    for (XynaPropertyWithDefaultValue property : sortedProps) {
      appendProperty( output, mode, property, showDoc, runtimeContexts, arg.getPrettyPrint() );
      output.append("\n");      
    }
    writeToCommandLine(statusOutputStream, output.toString());

  }
  
  //FIXME duplicate code mindestens auch in GetImpl

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

  private void appendProperty(StringBuilder output, Mode mode, XynaPropertyWithDefaultValue property,
                              EnumSet<DocumentationLanguage> showDoc, 
                              Map<Long, RuntimeContext> runtimeContexts, boolean prettyPrint) {
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
    appendReader(output, property, runtimeContexts);
  }
  
  private void appendReader(StringBuilder output, XynaPropertyWithDefaultValue property,
                            Map<Long, RuntimeContext> runtimeContexts) {
    boolean isThereSomeReader = false;
    for (Long revision : runtimeContexts.keySet()) {
      Set<DependencyNode> readers = factory.getFactoryManagementPortal().getXynaFactoryControl()
                      .getDependencyRegister()
                      .getDependencies(property.getName(), DependencySourceType.XYNAPROPERTY, revision);
      if (readers == null || readers.size() == 0) {
        continue;
      }
      if(!isThereSomeReader) {
        output.append("Reader:");
      }
      
      RuntimeContext runtimeContext = runtimeContexts.get(revision);
      for (DependencyNode reader : readers) {
        if(isThereSomeReader) {
          output.append(",");
        }
        isThereSomeReader = true;
        output.append(" ").append(reader.getType().getName());
        output.append(" '").append(reader.getUniqueName()).append("'");
        
        if(runtimeContext instanceof Application) {
          output.append(" (").append(runtimeContext.getName()).append(" ").append(((Application) runtimeContext).getVersionName()).append(")");
        } else if (!runtimeContext.equals(RevisionManagement.DEFAULT_WORKSPACE)) {
          output.append(" (").append(runtimeContext).append(")");
        }
      }
    }
    
    if(!isThereSomeReader) {
      output.append("UNUSED");
    }
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

}
