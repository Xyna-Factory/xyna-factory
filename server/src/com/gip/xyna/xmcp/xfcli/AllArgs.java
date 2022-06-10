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
package com.gip.xyna.xmcp.xfcli;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * AllArgs kapselt Kommando und Parameter.
 * Es werden verschiedene Methoden angeboten, um einfach auf die Argumente zugreifen zu können.
 *
 */
public class AllArgs {

  private String command;
  private String lowerCaseCommand;
  private List<String> args;
  private List<String> additionals;
  
  public AllArgs() {}
  
  @Override
  public String toString() {
    return "AllArgs("+command+","+args+","+additionals+")";
  }

  public void setCommand(String command) {
    this.command = command;
    this.lowerCaseCommand = command.toLowerCase();
  }
  
  public void setArgs(List<String> args) {
    this.args = new ArrayList<String>(args);
  }

  public void addArg(String arg) {
    if( args == null ) {
      args = new ArrayList<String>();
    }
    args.add(arg);
  }

  public void addAdditional(String additional) {
    if( additionals == null ) {
      additionals = new ArrayList<String>();
    }
    additionals.add(additional);
  }

  public String getCommand() {
    return command;
  }
  
  public String getLowerCaseCommand() {
    return lowerCaseCommand;
  }
  
  public String[] getArgsAsArray(int beginIndex) {
    if( args == null || args.size() <= beginIndex ) {
      return new String[]{};
    }
    if( beginIndex == 0 ) {
      return (String[])args.toArray(new String[args.size()]);
    }
    return (String[])args.subList(beginIndex,args.size()).toArray(new String[args.size()-beginIndex]);
  }


  public List<String> getArgs() {
    return args;
  }
  
  public String getFirstArgOrDefault(String defVal) {
    if( args == null ) {
      return defVal;
    }
    if( args.size() == 0 ) {
      return defVal;
    }
    return args.get(0);
  }

  public String getArg(int i) {
    return args.get(i);
  }

  public int getArgCount() {
    if( args == null ) {
      return 0;
    }
    return args.size();
  }


  public boolean containsArg(String string) {
    if( args == null ) {
      return false;
    }
    return args.contains(string);
  }

  /**
   * Einfaches Parsen der Argument in eine Map: 
   * alle Parameter, die mit - beginnen, werden Key
   * alle nachfolgenden Parameter ohne "-" werden zu einen String-Value mit Leerzeichen getrennt
   * wenn erster Parameter kein "-" hat ist Key=null
   * @return
   */
  public Map<String, String> parseArgsToMap() {
    if( args == null ) {
      return Collections.emptyMap();
    }
    Map<String, String> map = new HashMap<String, String>();
    String lastKey = null;
    for( int i=0; i<args.size(); ++i ) {
      String kv = args.get(i);
      if( kv.startsWith("-") ) {
        //Key
        lastKey = kv;
        map.put(lastKey, null);
      } else {
        //Value
        String last = map.get(lastKey);
        if( last != null ) {
          map.put(lastKey, last+" "+kv);
        } else {
          map.put(lastKey, kv);
        }
      }
    }
    return map;
  }

  public List<String> getAdditionals() {
    return additionals;
  }

/*
  public static void main(String[] args) {
    AllArgs aa = new AllArgs();
    aa.addArg("bla");
    aa.addArg("-blubb");
    aa.addArg("2");
    aa.addArg("6");
    aa.addArg("-quark");
    aa.addArg("honig");
    aa.addArg("-baum");
    aa.addArg("-wald");

    System.out.println(aa.parseArgsToMap() );
  }*/
  
}
