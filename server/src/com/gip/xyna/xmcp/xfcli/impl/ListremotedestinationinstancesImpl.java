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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.TableFormatter;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteDestinationInstanceInformation;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteDestinationManagement;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listremotedestinationinstances;



public class ListremotedestinationinstancesImpl extends XynaCommandImplementation<Listremotedestinationinstances> {

  public void execute(OutputStream statusOutputStream, Listremotedestinationinstances payload) throws XynaException {
    RemoteDestinationManagement rdMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRemoteDestinationManagement();
    List<RemoteDestinationInstanceInformation> rdis = null;
    rdis = new ArrayList<RemoteDestinationInstanceInformation>(rdMgmt.listRemoteDestinationInstances());
    
    Collections.sort(rdis, new RemoteDestinationInstanceNameComparator() );
    
    StringBuilder output = new StringBuilder();
    RemoteDestinationInstanceTableFormatter rditf = new RemoteDestinationInstanceTableFormatter(rdis);
    rditf.writeTableHeader(output);
    rditf.writeTableRows(output);
    writeToCommandLine(statusOutputStream, output.toString());
  }
  
  private static class RemoteDestinationInstanceNameComparator implements Comparator<RemoteDestinationInstanceInformation> {
    
    public int compare(RemoteDestinationInstanceInformation o1, RemoteDestinationInstanceInformation o2) {
      return o1.getName().compareTo(o2.getName());
    }
    
  }
  
  private static class RemoteDestinationInstanceTableFormatter extends TableFormatter {

    private List<List<String>> rows;
    private List<String> header;
    
    public RemoteDestinationInstanceTableFormatter(List<RemoteDestinationInstanceInformation> rdis) {
      setPrettyPrint(false);
      generateRowsAndHeader(rdis);
    }

    private void generateRowsAndHeader(List<RemoteDestinationInstanceInformation> rdis) {
      header = Arrays.asList("name", "description", "execTimeout", "type", "parameters");
     
      rows = new ArrayList<List<String>>();
      for( RemoteDestinationInstanceInformation rdi : rdis ) {
        List<String> params = StringParameter.paramStringMapToList(rdi.getStartparameter());
        StringBuilder sb = new StringBuilder();
        for( String p : params) {
          sb.append(p).append(" ");
        }
        
        rows.add( Arrays.asList( 
            rdi.getName(), 
            nullAsEmpty(rdi.getDescription()),
            nullAsEmpty(rdi.getExecutionTimeout() == null ? null : rdi.getExecutionTimeout().toSumString()),
            nullAsEmpty(rdi.getTypename()),
            sb.toString()
            ) );
       }
    }
    
    private String nullAsEmpty(String string) {
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
