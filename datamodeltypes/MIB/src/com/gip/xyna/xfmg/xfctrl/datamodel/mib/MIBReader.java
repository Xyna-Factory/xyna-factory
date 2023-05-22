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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jsmiparser.parser.SmiDefaultParser;
import org.jsmiparser.smi.SmiMib;
import org.jsmiparser.smi.SmiModule;
import org.jsmiparser.smi.SmiOidNode;
import org.jsmiparser.smi.SmiOidValue;
import org.jsmiparser.smi.SmiSymbol;
import org.jsmiparser.util.problem.DefaultProblemEventHandler;
import org.jsmiparser.util.problem.DefaultProblemReporterFactory;
import org.jsmiparser.util.problem.ProblemEventHandler;
import org.jsmiparser.util.problem.ProblemReporterFactory;


/**
 *
 */
public class MIBReader {

  private List<URL> urls = new ArrayList<URL>();
  private SmiMib mib;
  private StringBuilderStream out;
  private StringBuilderStream err;
  private OIDMap<SmiModule> moduleMap;
  
  private static List<String> EXPECTED_MISSING_MODULE_IDENTITY = 
      Arrays.asList("JSMI_INTERNAL_MIB", "SNMPv2-SMI");
  
  public void importFiles(List<String> files) throws MalformedURLException {
    for( String filename : files ) {
      importRecursively( new File(filename) );
    }
  }

  private void importRecursively(File file) throws MalformedURLException {
    if( file.isDirectory() ) {
      for( File f : file.listFiles(new MibFilter() ) ) {
        importRecursively(f);
      }
    } else {
      urls.add(file.toURI().toURL());
    }
  }
  
  private static class MibFilter implements FileFilter {

    public boolean accept(File pathname) {
      if( pathname.isDirectory() ) {
        return true;
      }
      String name = pathname.getName().toLowerCase();
      if( name.endsWith(".mib") ) {
        return true;
      }
      if( name.endsWith(".txt") ) {
        return true;
      }
      if( name.endsWith(".my") ) {
        return true;
      }
      return false;
    }
    
  }
  

  public void parse() {
    SmiDefaultParser parser = createParser();
    parser.getFileParserPhase().setInputUrls(urls);
    mib = parser.parse();
  }
  
  private SmiDefaultParser createParser() {
    out = new StringBuilderStream();
    err = new StringBuilderStream();
    
    ProblemEventHandler peh = new DefaultProblemEventHandler( new PrintStream(out), new PrintStream(err) );
    ProblemReporterFactory prf = new DefaultProblemReporterFactory(getClass().getClassLoader(), peh);
    
    SmiDefaultParser parser = new SmiDefaultParser(prf);
    return parser;
  }
  
  private static class StringBuilderStream extends OutputStream {
    private StringBuilder sb = new StringBuilder();

    @Override
    public void write(int b) throws IOException {
      sb.append((char) b);
    }
    
    @Override
    public String toString() {
      return sb.toString();
    }
    
    public int length() {
      return sb.length();
    }
    
    public StringBuilder getStringBuilder() {
      return sb;
    }
    
  }

  public String getOutMessage() {
    return out.toString();
  }

  public String getErrMessage() {
    return err.toString();
  }
  
  public void fillModuleMap() {
    moduleMap = new OIDMap<SmiModule>();
    for( SmiModule module : mib.getModules() ) {
      SmiOidValue mi = getModuleIdentity(module);
      if( mi != null ) {
        SmiOidNode node = mi.getNode();
        if( node != null && node.getOid() != null ) {
          moduleMap.put( node.getOidStr(), module);
        } else {
          err.getStringBuilder().append("Module has no Parent: "+ module.getId()+"\n" );
        }
      } else {
        if( ! EXPECTED_MISSING_MODULE_IDENTITY.contains(module.getId()) ) {
          out.getStringBuilder().append( "Module has no ModuleIdentity: "+ module.getId()+"\n");
        }
      }
    }
    moduleMap.sort();
  }

  public OIDMap<SmiModule> getModuleMap() {
    return moduleMap;
  }
  
  public SmiMib getMib() {
    return mib;
  }

  
  
  
  
  
  /**
   * @return
   */
  public List<String> listMibObjects() {
    List<String> mibObjects = new ArrayList<String>();
    
    mibObjects.add("All Modules =" + mib.getModules());
    mibObjects.add("All ObjectTypes =" +  mib.getObjectTypes().getAll());
    mibObjects.add("All OidValues =" +  mib.getOidValues().getAll());
    mibObjects.add("All Scalars =" +  mib.getScalars().getAll());
    mibObjects.add("All Symbols =" +  mib.getSymbols().getAll());
    mibObjects.add("All Tables =" +  mib.getTables().getAll());
    mibObjects.add("All Columns =" + mib.getColumns().getAll());
    mibObjects.add("All Rows =" + mib.getRows().getAll());
    mibObjects.add("All TextualConventions =" +  mib.getTextualConventions().getAll());
    mibObjects.add("All Types =" +  mib.getTypes().getAll());
    mibObjects.add("All Variables =" +  mib.getVariables().getAll());
    
    
    return mibObjects;
  }

  public List<String> listMibObjectsForModule(String moduleName) {
    List<String> mibObjects = new ArrayList<String>();
    SmiModule module = mib.findModule(moduleName);
    
    if( module == null ) {
      mibObjects.add( "No module \""+moduleName+"\" found.");
      mibObjects.add( "All Modules =" + mib.getModules());
      return mibObjects;
    }
    

    mibObjects.add("Module ObjectTypes =" +  module.getObjectTypes());
    mibObjects.add("Module OidValues =" +  module.getOidValues());
    mibObjects.add("Module Scalars =" +  module.getScalars());
    mibObjects.add("Module Symbols =" +  module.getSymbols());
    mibObjects.add("Module Tables =" +  module.getTables());
    mibObjects.add("Module Columns =" + module.getColumns());
    mibObjects.add("Module Rows =" + module.getRows());
    mibObjects.add("Module TextualConventions =" +  module.getTextualConventions());
    mibObjects.add("Module Types =" +  module.getTypes());
    mibObjects.add("Module Variables =" +  module.getVariables());
    
    
    return mibObjects;
  }
  
  
  public List<String> getMIBInformations() {
    List<String> mibInfo = new ArrayList<String>();
    for( OID oid : moduleMap.keyList() ) {
      SmiModule module = moduleMap.get(oid);
      SmiOidValue mi = getModuleIdentity(module);
      SmiOidNode node = mi.getNode();
      int version = module.getModuleIdentity() == null ? 0 : module.getModuleIdentity().getRevisions().size();
      mibInfo.add( oid +" "+ MIBTools.getPath(node) +" "+ module.getId()+ " v"+ version );
    }
    return mibInfo;
  }


  
  public SmiOidNode findNodeByOid(String oid) {
    return MIBTools.findByOid( mib, oid);
  }

  public List<SmiOidNode> findNodesByOid(List<String> oids) {
    List<SmiOidNode> oidRestrictionNodes = new ArrayList<SmiOidNode>();
    for( String oid : oids ) {
      SmiOidNode node = MIBTools.findByOid( mib, oid);
      if( node == null ) {
        //FIXME wie Fehler ausgeben?
      } else {
        oidRestrictionNodes.add(node);
      }
    }
    // mib.findByOidPrefix(oid)
    return oidRestrictionNodes;
   
  }

  private SmiOidValue getModuleIdentity(SmiModule module) {
    List<SmiSymbol> symbols = (List<SmiSymbol>) module.getSymbols();
    return (SmiOidValue) symbols.get(0); //Module Identity ist der erste Eintrag nach den Imports
  }

}
