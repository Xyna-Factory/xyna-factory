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
import com.gip.xyna.utils.misc.TableFormatter;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.ClusterNode;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.NodeManagement;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listfactorynodecluster;



public class ListfactorynodeclusterImpl extends XynaCommandImplementation<Listfactorynodecluster> {

  public void execute(OutputStream statusOutputStream, Listfactorynodecluster payload) throws XynaException {
    NodeManagement nodeMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getNodeManagement();
    List<ClusterNode> clusterNodes = nodeMgmt.listClusterNodes();

    Collections.sort(clusterNodes, new ClusterNodeNameComparator() );
    
    StringBuilder sb = new StringBuilder();
    ClusterTableFormatter ctf = new ClusterTableFormatter(clusterNodes);
    ctf.writeTableRows(sb);
    writeToCommandLine(statusOutputStream, sb.toString());
  }
  
  private static class ClusterNodeNameComparator implements Comparator<ClusterNode> {

    public int compare(ClusterNode o1, ClusterNode o2) {
      return o1.getName().compareTo(o2.getName());
    }
    
  }
  
  private static class ClusterTableFormatter extends TableFormatter {

    private List<List<String>> rows;
    private List<String> header;
    
    public ClusterTableFormatter(List<ClusterNode> clusterNodes) {
      setPrettyPrint(false);
      generateRowsAndHeader(clusterNodes);
    }

    private void generateRowsAndHeader(List<ClusterNode> clusterNodes) {
      header = Arrays.asList("name/nodes", "description");
     
      rows = new ArrayList<List<String>>();
      for( ClusterNode cn : clusterNodes ) {
        if( cn.getDescription() != null ) {
          rows.add( Arrays.asList( cn.getName(), cn.getDescription() ) );
        } else {
          rows.add( Arrays.asList( cn.getName(), "" ) );
        }
        for( String node : cn.getFactoryNodes() ) {
          rows.add( Arrays.asList( "  * "+node, "" ) );
        }
      }
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
