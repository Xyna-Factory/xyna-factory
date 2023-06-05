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

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.utils.collections.Pair;


public class XSDGenerator {

  private String filename;
  private List<Pair<String,String>> namespaces = new ArrayList<Pair<String,String>>();    
  private List<String> rows = new ArrayList<String>();
  private String elementFormDefault = "qualified";
  private String attributeFormDefault = "unqualified";
  
  public XSDGenerator(String filename) {
    this.filename = filename;
  }

  public XSDGenerator rows(String ... rows) {
    for( String row : rows ) {
      this.rows.add(row);
    }
    return this;
  }

  public XSDGenerator namespace(String prefix, String namespace) {
    namespaces.add( Pair.of(prefix,namespace) );
    return this;
  }
  
  public String toXSD() {
    StringBuilder xsd = new StringBuilder();
    xsd.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    xsd.append("<xsd:schema");
    for( Pair<String,String> pairNS : namespaces ) {
      if( pairNS.getFirst() == null ) {
        xsd.append(" targetNamespace");
      } else {
        xsd.append(" xmlns:").append(pairNS.getFirst());
      }
      xsd.append("=\"").append(pairNS.getSecond()).append("\"");
    }
    xsd.append(" elementFormDefault=\"").append(elementFormDefault).append("\"").
        append(" attributeFormDefault=\"").append(attributeFormDefault).append("\"").
        append(" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"").
        append(" xmlns=\"http://www.gip.com\"").
        append(">\n");
    for( String row : rows ) {
      xsd.append(row).append("\n");
    }
    xsd.append( "</xsd:schema>\n");
    return xsd.toString();
  }
  
  public String save() throws IOException {
    String xsdString = toXSD();
    System.out.println( xsdString );
    try {
      FileWriter fw = new FileWriter(filename);
      fw.append(xsdString);
      fw.close();
     
    } catch (IOException e) {
      e.printStackTrace();
      throw e;
    }
    return filename;
  }

  public XSDGenerator elementFormDefault(String elementFormDefault) {
    this.elementFormDefault = elementFormDefault;
    return this;
  }

  public XSDGenerator attributeFormDefault(String attributeFormDefault) {
    this.attributeFormDefault = attributeFormDefault;
    return this;
  }

}
