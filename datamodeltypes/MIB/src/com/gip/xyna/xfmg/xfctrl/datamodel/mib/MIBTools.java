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
package com.gip.xyna.xfmg.xfctrl.datamodel.mib;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jsmiparser.smi.SmiMib;
import org.jsmiparser.smi.SmiModule;
import org.jsmiparser.smi.SmiOidNode;
import org.jsmiparser.smi.SmiOidValue;
import org.jsmiparser.smi.SmiVariable;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;


/**
 *
 */
public class MIBTools {
  
  private static final Pattern whiteSpaces = Pattern.compile("^(\\s*)(.*)$");
  private static final Pattern linebreaks = Pattern.compile("\r\n|\n|\r");

  private static Logger logger = CentralFactoryLogging.getLogger(MIBTools.class);
  
  public static String getPath(SmiOidNode node) {
    
    Map<String,String> baseOidNames = new HashMap<String,String>();
    baseOidNames.put( "1", "iso");
    baseOidNames.put( "1.3.6.1.4.1", "SNMPv2-SMI::enterprises");
    
    
    List<SmiOidNode> parents = new ArrayList<SmiOidNode>();
    parents.add( node );
    SmiOidNode parent = node.getParent();
    while( parent != null && parent.getOid() != null ) {
      if( baseOidNames.containsKey( parent.getOidStr() ) ) {
        break;
      }
      parents.add(parent);
      parent = parent.getParent();
    }
    StringBuilder sb = new StringBuilder();
    sb.append( baseOidNames.get( parent.getOidStr() ));
    for( int p=parents.size()-1; p>= 0; --p ) {
      sb.append(".").append(getName(parents.get(p)));
    }
    
    return sb.toString();
  }

  public static String getName(SmiOidNode node) {
    String name = null;
    for( SmiOidValue v : node.getValues() ) {
      name = v.getId();
      if( name != null ) { //TODO: Was wenn unterschiedlich?
        break;
      }
    }
    return name;
  }

  public static SmiOidNode findByOid(SmiMib mib, String oid) {
    String[] oidParts = oid.split("\\.");
    SmiOidNode node = null;
    if( oidParts[0].contains("::") ) {
      //SNMPv2-SMI::enterprises.28747.1
      int idx = oidParts[0].indexOf("::");
      String moduleName = oidParts[0].substring(0, idx);
      String name = oidParts[0].substring(idx+2);
      
      SmiModule module = mib.findModule(moduleName);
      /*
      if( module.getModuleIdentity() != null ) {
        node = module.getModuleIdentity().getNode();
        if( name.length() != 0 ) {
          if( ! getName(node).equals(name) ) {
            node = getChildByIndexOrName( node, name);
          }
        }
      } else {*/
        SmiOidValue ov = module.findOidValue(name);
        if( ov == null ) {
          return null; //Parent-Node nicht gefunden
        }
        node = ov.getNode();
      //}
    } else {
      //1.3.6.1.4.1.28747.1
      node = getChildByIndexOrName( mib.getRootNode(), oidParts[0]);
    }
    for( int i=1; i<oidParts.length; i++ ) {
      node = getChildByIndexOrName( node, oidParts[i] );
      if (node == null) {
        return null; //Child existiert nicht
      }
    }
    return node;
  }

  private static SmiOidNode getChildByIndexOrName(SmiOidNode parent, String indexOrName) {
    try {
      return parent.findChild( Integer.parseInt(indexOrName) );
    } catch( NumberFormatException e ) {
      return getChildByName( parent, indexOrName ); //TODO optimieren
    }
  }

  private static SmiOidNode getChildByName(SmiOidNode parent, String name) {
    for( SmiOidNode child : parent.getChildren() ) {
      if( name.equals(getName(child)) ) {
        return child;
      }
    }
    return null;
  }
  

  public static String getOid(SmiMib mib, String oid) {
    SmiOidNode node = findByOid( mib, oid);
    if( node != null ) {
      return node.getOidStr();
    } else {
      return null;
    }
  }

  public static String stripLeadingWhitespaces(String description) {
    int idx = description.indexOf('\n');
    if (idx < 0) {
      //Kein Umbruch, daher nichts zu tun
      return description;
    }
    description = description.trim();

    String[] parts = linebreaks.split(description);

    //minimum führender whitespaces bestimmen. tabs als 8 leerzeichen behandeln
    boolean first = true;
    int minimumWhiteSpaces = Integer.MAX_VALUE;    
    for (String part : parts) {
      Matcher m = whiteSpaces.matcher(part);
      if (!m.matches()) {
        throw new RuntimeException();
      }
      String leadingWhiteSpaces = m.group(1);
      String remainder = m.group(2);
      if (!first && remainder.length() == 0) {
        //leere zeilen ignorieren
        continue;
      }
      int whiteSpaceCnt = 0;
      for (int i = 0; i < leadingWhiteSpaces.length(); i++) {
        char c = leadingWhiteSpaces.charAt(i);
        if (c == ' ') {
          whiteSpaceCnt++;
        } else if (c == '\t') {
          whiteSpaceCnt += 8;
        } else {
          logger.warn("unexpected whitespace: [" + (int) c + "] = " + c);
        }
      }
      if (first) {
        first = false;
      } else {
        minimumWhiteSpaces = Math.min(minimumWhiteSpaces, whiteSpaceCnt);
      }
    }

    if (minimumWhiteSpaces > 0 && minimumWhiteSpaces != Integer.MAX_VALUE) {
      /*
       * gemeinsam führende whitespaces entfernen, aber tabs nicht durch leerzeichen ersetzen.
       * wenn man das noch machen würde, würde das entfernen der einrückung bei mischung von
       * tabs und leerzeichen noch besser funktionieren. 
       */
      StringBuilder sb = new StringBuilder();
      for (int line = 0; line < parts.length; line++) {
        String part = parts[line];
        Matcher m = whiteSpaces.matcher(part);
        m.matches();
        String leadingWhiteSpaces = m.group(1);
        String remainder = m.group(2);
        if (remainder.length() > 0) {
          int cutWhiteSpaces = 0;
          int charIdx = 0;
          while (cutWhiteSpaces < minimumWhiteSpaces && charIdx < leadingWhiteSpaces.length()) {
            char c = leadingWhiteSpaces.charAt(charIdx++);
            if (c == ' ') {
              cutWhiteSpaces++;
            } else {
              cutWhiteSpaces += 8;
            }
          }
          sb.append(leadingWhiteSpaces.substring(charIdx));
          sb.append(remainder);
        }
        if (line != parts.length - 1) {
          sb.append("\n");
        }
      }
      return sb.toString();
    }
    return description;
  }


  public static Pair<String, Boolean> getOid(SmiOidValue val, SmiOidNode parentOidNode, String parentOidPrefix) {
    String oid;
    boolean root = false;
    if (val.getOidStr().startsWith(parentOidNode.getOidStr())) {
      oid = val.getOidStr().substring(parentOidNode.getOidStr().length()+1);
      if( parentOidPrefix != null ) {
        oid = parentOidPrefix+"."+oid;
      }
    } else {
      root = true;
      oid = val.getOidStr();
    }
    
    //Skalare OIDs ben�tigten eine 0 am Ende
    if (val instanceof SmiVariable && ((SmiVariable)val).isScalar()) {
      oid = oid + ".0";
    }
    
    return Pair.of(oid, root);
  }
}
