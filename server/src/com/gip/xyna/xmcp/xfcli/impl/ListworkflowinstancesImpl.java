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
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.TableFormatter;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listworkflowinstances;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstance;



public class ListworkflowinstancesImpl extends XynaCommandImplementation<Listworkflowinstances> {

  private static final int MAX_COUNT = 500;

  public void execute(OutputStream statusOutputStream, Listworkflowinstances payload) throws XynaException {

    long offset = 0;
    int count = MAX_COUNT;

    if (payload.getOffset() != null) {
      try {
        offset = Long.valueOf(payload.getOffset());
      } catch (NumberFormatException e) {
        writeLineToCommandLine(statusOutputStream, "Invalid offset: <" + payload.getOffset() + ">");
        return;
      }
    }

    if (payload.getCount() != null) {
      try {
        count = Integer.valueOf(payload.getCount());
        if (count < 1) {
          writeLineToCommandLine(statusOutputStream, "Invalid number of lines: " + payload.getCount());
          return;
        }
        if (count > MAX_COUNT) {
          writeLineToCommandLine(statusOutputStream, "Number of orders is too large (" + payload.getCount()
              + "), showing " + MAX_COUNT + " only");
          count = MAX_COUNT;
        }
      } catch (NumberFormatException e) {
        writeLineToCommandLine(statusOutputStream, "Invalid number of lines: <" + payload.getCount() + ">");
        return;
      }
    }

    Map<Long, OrderInstance> wfiDb =
        XynaFactory.getInstance().getXynaMultiChannelPortal().getAllRunningProcesses(offset, count);
    Iterator<Entry<Long, OrderInstance>> iter = wfiDb.entrySet().iterator();

    if (wfiDb.size() > 200 && count == Integer.MAX_VALUE) {
      writeLineToCommandLine(statusOutputStream, "Warning: Resultset contains more than 200 entries (only up to "
          + MAX_COUNT + " entries are shown), use an offset and a limited number of lines for more userfriendly output");
      try {
        Thread.sleep(2500);
      } catch (InterruptedException e) {
        // ignore
      }
    }

    if (iter.hasNext()) {
      writeLineToCommandLine(statusOutputStream, "Listing <" + wfiDb.size() + "> known order executions...");
    } else {
      writeLineToCommandLine(statusOutputStream, "No known order executions within the specified range.");
    }

    if (payload.getNewstyle()){

      StringBuilder sb = new StringBuilder();
      OisTableFormatter formatter = new OisTableFormatter(wfiDb.values());
      formatter.writeTableHeader(sb);
      formatter.writeTableRows(sb);
      writeLineToCommandLine(statusOutputStream, sb.toString());

    } else {

      while (iter.hasNext()) {

        Entry<Long, OrderInstance> next = iter.next();
        String output = "Order found for ID " + next.getKey() + ": ";

        if (next.getValue().getOrderType() != null) {
          output += "order type " + next.getValue().getOrderType();
        }
        if (next.getValue().getApplicationName() != null) {
          output += " (" + next.getValue().getApplicationName() + " " + next.getValue().getVersionName() + ")";
        } else {
          if (!next.getValue().getRuntimeContext().equals(RevisionManagement.DEFAULT_WORKSPACE)) {
            output += " (" + next.getValue().getRuntimeContext() + ")";
          }
        }

        if (next.getValue().getStatusAsString() != null) {
          output += ", status " + next.getValue().getStatusAsString();
        }

        output += ".";
        writeLineToCommandLine(statusOutputStream, output);

      }

    }

  }


  private static class OisTableFormatter extends TableFormatter {

    private List<List<String>> rows;
    private List<String> header;
    private List<QueuesTableColumn> columns;
   
    public OisTableFormatter(Collection<OrderInstance> orders) {
      columns = Arrays.asList(QueuesTableColumn.values() );
      generateRowsAndHeader(orders);
    }

    private void generateRowsAndHeader(Collection<OrderInstance> orders) {
      header = new ArrayList<String>();
      for( QueuesTableColumn ac : columns ) {
        header.add( ac.toString() );
      }
      rows = new ArrayList<List<String>>();
      for( OrderInstance ai : orders ) {
        rows.add( generateRow(ai) );
      }
    }

    private List<String> generateRow(OrderInstance ai) {
      List<String> row = new ArrayList<String>();
      for( QueuesTableColumn ac : columns ) {
        row.add( ac.extract(ai) );
      }
      return row;
    }

    public List<String> getHeader() {
      return header;
    }
    
    @Override
    public List<List<String>> getRows() {
      return rows;
    }
    
    public enum QueuesTableColumn {
      ID {
        public String extract(OrderInstance ai) {
          return ai.getId() + "";
        }
      },
      OrderType {
        public String extract(OrderInstance ai) {
          return ai.getOrderType();
        }
      },
      Status {
        public String extract(OrderInstance ai) {
          return ai.getStatusAsString();
        }
      },
      Workspace {
        public String extract(OrderInstance ai) {
          String wsName = ai.getWorkspaceName();
          if (wsName != null) {
            return wsName;
          } else {
            return "";
          }
        }
      },
      Application {
        public String extract(OrderInstance ai) {
          String appName = ai.getApplicationName();
          if (appName != null) {
            return appName;
          } else {
            return "";
          }
        }
      },
      Version {
        public String extract(OrderInstance ai) {
          String vName = ai.getVersionName();
          if (vName != null) {
            return vName;
          } else {
            return "";
          }
        }
      }, StartTime {
        public String extract(OrderInstance ai) {
          return Constants.defaultUTCSimpleDateFormatWithMS().format(new Date(ai.getStartTime()));
        }
      }, LastUpdate {
        public String extract(OrderInstance ai) {
          return Constants.defaultUTCSimpleDateFormatWithMS().format(new Date(ai.getLastUpdate()));
        }
      };

      public abstract String extract(OrderInstance ai);
    }

  }
  
}
