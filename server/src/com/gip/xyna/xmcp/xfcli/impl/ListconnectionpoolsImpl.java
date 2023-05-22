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
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.TableFormatter;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listconnectionpools;
import com.gip.xyna.xnwh.pools.ConnectionPoolInformation;



public class ListconnectionpoolsImpl extends XynaCommandImplementation<Listconnectionpools> {

  public void execute(OutputStream statusOutputStream, Listconnectionpools payload) throws XynaException {
    
    List<ConnectionPoolInformation> pools = XynaFactory.getInstance().getXynaNetworkWarehouse().getConnectionPoolManagement().listConnectionPoolInformation();
    
    if( payload.getAsTable() ) {
      StringBuilder output = new StringBuilder();
      PoolTableFormatter ptf = new PoolTableFormatter(pools);
      ptf.writeTableHeader(output);
      ptf.writeTableRows(output);
      writeToCommandLine(statusOutputStream, output.toString());
    } else {
      String format = " %-" + 40 + "s %-" + 15 + "s  %-" + 12 + "s %-" + 4 + "s / %-" + 4 + "s %-" + 10 + "s";
      writeLineToCommandLine(statusOutputStream, String.format(format, "Name", "Pooltype", "State", "used", "size", "waiting"));
      for (ConnectionPoolInformation cpi : pools) {
        writeLineToCommandLine(statusOutputStream, String.format(format, cpi.getName(), cpi.getPooltype(), cpi.getState(), cpi.getUsed(), cpi.getSize() + (cpi.isDynamic() ? "+" : ""), cpi.getWaitingThreadInformation().length));
      }
    }
    
    
  }

  
  private static class PoolTableFormatter extends TableFormatter {
    private List<List<String>> rows;
    private List<String> header;
    private List<PoolColumn> columns;
  
    public PoolTableFormatter(List<ConnectionPoolInformation> cpis) {
      columns = Arrays.asList(PoolColumn.values() );
      generateRowsAndHeader(cpis);
    }

    private void generateRowsAndHeader(List<ConnectionPoolInformation> cpis) {
      header = new ArrayList<String>();
      for( PoolColumn pc : columns ) {
        header.add( pc.toString() );
      }
      rows = new ArrayList<List<String>>();
      for( ConnectionPoolInformation cpi : cpis ) {
        rows.add( generateRow(cpi) );
      }
    }

    private List<String> generateRow(ConnectionPoolInformation cpi) {
      List<String> row = new ArrayList<String>();
      for( PoolColumn pc : columns ) {
        try {
          row.add( pc.extract(cpi) );
        } catch( NullPointerException e ) {
          row.add("null");
        }
      }
      return row;
    }

    public List<String> getHeader() {
      return header;
    }
    
    public List<List<String>> getRows() {
      return rows;
    }
    
    public enum PoolColumn {
      Name {
        public String extract(ConnectionPoolInformation cpi) {
          return cpi.getName();
        }
      },
      Type {
        public String extract(ConnectionPoolInformation cpi) {
          return cpi.getPooltype();
        }
      },
      State {
        public String extract(ConnectionPoolInformation cpi) {
          return cpi.getState();
        }
      },
      Used {
        public String extract(ConnectionPoolInformation cpi) {
          return String.valueOf(cpi.getUsed());
        }
      },
      Size {
        public String extract(ConnectionPoolInformation cpi) {
          return String.valueOf(cpi.getSize()) + (cpi.isDynamic() ? "+" : "");
        }
      },
      Waiting {
        public String extract(ConnectionPoolInformation cpi) {
          return String.valueOf(cpi.getWaitingThreadInformation().length);
        }
      },
      User {
        public String extract(ConnectionPoolInformation cpi) {
          return cpi.getPoolDefinition().getUser();
        }
      },
      Params {
        public String extract(ConnectionPoolInformation cpi) {
          return String.valueOf(cpi.getPoolDefinition().getParams());
        }
      },
      Retries {
        public String extract(ConnectionPoolInformation cpi) {
          return String.valueOf(cpi.getPoolDefinition().getRetries());
        }
      },
      ConnectString {
        public String extract(ConnectionPoolInformation cpi) {
          return cpi.getPoolDefinition().getConnectstring();
        }
      },
      ValidationInterval {
        public String extract(ConnectionPoolInformation cpi) {
          return String.valueOf(cpi.getPoolDefinition().getValidationinterval());
        }
      },
      
      ;
      public abstract String extract(ConnectionPoolInformation cpi);

    }
  }

}
