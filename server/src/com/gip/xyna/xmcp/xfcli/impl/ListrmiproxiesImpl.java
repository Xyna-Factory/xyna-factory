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
import com.gip.xyna.xfmg.xfctrl.RMIManagement.RMIParameter;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.ProxyInformation;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.ProxyManagement;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.storables.ProxyStorable;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listrmiproxies;



public class ListrmiproxiesImpl extends XynaCommandImplementation<Listrmiproxies> {

  public void execute(OutputStream statusOutputStream, Listrmiproxies payload) throws XynaException {
    ProxyManagement pm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getProxyManagement();
    
    List<ProxyInformation> proxies =  pm.listProxies();
    
    Collections.sort(proxies, new ProxyInformationComparator() );
    
    StringBuilder output = new StringBuilder();
    ProxyInformationTableFormatter pitf = new ProxyInformationTableFormatter(proxies, payload.getVerbose());
    pitf.writeTableHeader(output);
    pitf.writeTableRows(output);
    writeToCommandLine(statusOutputStream, output.toString());
  }
  
  private static class ProxyInformationComparator implements Comparator<ProxyInformation> {

    public int compare(ProxyInformation o1, ProxyInformation o2) {
      return o1.getName().compareTo(o2.getName());
    }
    
  }
  
  private static class ProxyInformationTableFormatter extends TableFormatter {

    private List<List<String>> rows;
    private List<String> header;
    
    public ProxyInformationTableFormatter(List<ProxyInformation> proxies, boolean verbose) {
      setPrettyPrint(true);
      generateRowsAndHeader(proxies, verbose);
    }

    private void generateRowsAndHeader(List<ProxyInformation> proxies, boolean verbose) {
      if( verbose ) {
        header = Arrays.asList("name", "roles", "rights", "add. methods", "total rights", "total methods",  "registryHost", "registryPort", "url", "port", "description");
      } else {
        header = Arrays.asList("name", "url", "port", "description");
      }
      rows = new ArrayList<List<String>>();
      for( ProxyInformation pi : proxies ) {
        rows.add( createRow( pi, verbose) );
      }
    }

    private List<String> createRow(ProxyInformation pi, boolean verbose) {
      List<String> row  = new ArrayList<String>( header.size() );
      ProxyStorable prs = pi.getProxy();
      RMIParameter rp = prs.toRMIParameter();
      
      row.add( pi.getName() );
      if( verbose ) {
        row.add( listToString(prs.getRoles()) );
        row.add( listToString(prs.getRights()) );
        row.add( getRemark(prs) );
        row.add( String.valueOf(pi.getNumberOfRights()) );
        row.add( String.valueOf(pi.getNumberOfProxyMethods()) );
        row.add( rp.getRegistryHost() ); 
        row.add( String.valueOf( rp.getRegistryPort() ) ); 
      }
      row.add( nullToEmpty( getUrl(pi) ) );
      row.add( getComPort(pi,rp) );
      row.add( nullToEmpty( pi.getDescription() ) );
      return row;
    }

    private String getUrl(ProxyInformation pi) {
      if( pi.getRMIParameter() == null ) {
        return null;
      }
      return pi.getRMIParameter().getUrl();
    }

    private String getComPort(ProxyInformation pi, RMIParameter rpStored) {
      int cpS = rpStored.getCommunicationPort();
      if( pi.getRMIParameter() == null ) {
        //Proxy ist nicht aktiv
        return String.valueOf( cpS );
      } else {
        int cpA = pi.getRMIParameter().getCommunicationPort();
        if( cpA == cpS ) {
          return String.valueOf( cpA );
        } else {
          return String.valueOf( cpA )+"*";
        }
      }
    }

    private String getRemark(ProxyStorable prs) {
      StringBuilder remark = new StringBuilder();
      if( ! prs.isWithoutPublic() ) {
        remark.append("public");
      }
      if( prs.isWithDeprecated() ) {
        if( remark.length() != 0 ) {
          remark.append(", ");
        }
        remark.append("deprecated");
      }
      return remark.toString();
    }

    private String listToString(List<String> list) {
      if( list == null ) {
        return "";
      }
      switch( list.size() ) {
        case 0: return "";
        case 1: return list.get(0);
        default:
          StringBuilder sb = new StringBuilder();
          String sep = "";
          for( String s : list ) {
            sb.append(sep).append(s);
            sep = " ";
          }
          return sb.toString();
      }
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
