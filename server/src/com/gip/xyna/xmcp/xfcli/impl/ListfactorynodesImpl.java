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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.TableFormatter;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.FactoryNodeInformation;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.NodeManagement;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listfactorynodes;



public class ListfactorynodesImpl extends XynaCommandImplementation<Listfactorynodes> {

  private static final Logger logger = CentralFactoryLogging.getLogger(ListfactorynodesImpl.class);

  public void execute(OutputStream statusOutputStream, Listfactorynodes payload) throws XynaException {
    NodeManagement nodeMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getNodeManagement();
    
    List<FactoryNodeInformation> factoryNodes = nodeMgmt.listFactoryNodes( payload.getVerbose() );
    
    Collections.sort(factoryNodes, new FactoryNodeNameComparator() );
    
    StringBuilder output = new StringBuilder();
    FactoryNodeTableFormatter fntf = new FactoryNodeTableFormatter(factoryNodes, payload.getVerbose());
    fntf.writeTableHeader(output);
    fntf.writeTableRows(output);
    writeToCommandLine(statusOutputStream, output.toString());
  }
  
  private static class FactoryNodeNameComparator implements Comparator<FactoryNodeInformation> {

    public int compare(FactoryNodeInformation o1, FactoryNodeInformation o2) {
      return o1.getName().compareTo(o2.getName());
    }
    
  }
  
  private static class FactoryNodeTableFormatter extends TableFormatter {

    private List<List<String>> rows;
    private List<String> header;
    
    public FactoryNodeTableFormatter(List<FactoryNodeInformation> factoryNodes, boolean verbose) {
      setPrettyPrint(false);
      generateRowsAndHeader(factoryNodes, verbose);
    }

    private void generateRowsAndHeader(List<FactoryNodeInformation> factoryNodes, boolean verbose) {
      
      header = Arrays.asList("name", "instanceId", "type", "status", "description");
      if( verbose ) {
        header = combine(header, Arrays.asList("wait Con.", "wait Res."));
      }
      rows = new ArrayList<List<String>>();
      
      for( FactoryNodeInformation fni : factoryNodes ) {
        List<String> row = Arrays.asList( 
            fni.getName(), 
            String.valueOf(fni.getInstanceId()), 
            fni.getRemoteAccessType(),
            fni.getStatus().toString(),
            nullToEmpty(fni.getDescription()) 
            );
        if( verbose ) {
          Throwable exception = fni.getConnectException();
          String ex = "";
          if (exception != null) {
            StringWriter sw = new StringWriter();
            exception.printStackTrace(new PrintWriter(sw));
            ex = "\n" + sw.toString();
          }
          row = combine(row, Arrays.asList(
              String.valueOf(fni.getWaitingForConnectivity()), 
              String.valueOf(fni.getWaitingForResult() + ex) ));
          
        }
        rows.add(row);
      }
    }
    
    private List<String> combine(List<String> l1, List<String> l2) {
      List<String> result = new ArrayList<String>(l1.size()+l2.size());
      result.addAll(l1);
      result.addAll(l2);
      return result;
    }

    private String nullToEmpty(String string) {
      if( string == null ) {
        return "";
      }
      return string;
    }

    public List<String> getHeader() {
      return header;
    }
    
    @Override
    public List<List<String>> getRows() {
      return rows;
    }
    
  }

}
