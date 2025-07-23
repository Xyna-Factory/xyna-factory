/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
import java.util.Collections;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.TableFormatter;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.ActiveMQConnectData;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.OracleAQConnectData;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.IQueue;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.WebSphereMQConnectData;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listqueues;


public class ListqueuesImpl extends XynaCommandImplementation<Listqueues> {

  public void execute(OutputStream statusOutputStream, Listqueues payload) throws XynaException {

    Collection<? extends IQueue> queues = XynaFactory.getInstance().getFactoryManagement().listQueues();

    StringBuilder builder = new StringBuilder();
    if (payload.getOldstyle()) {
      addListHeaderOLDSTYLE(builder);
      int length = queues.size();
      int index = 0;
      for (IQueue queue : queues) {
        index++;
        addListItemOLDSTYLE(queue, builder, index < length);
      }
    } else {
      if (queues.size() > 0) {
        builder.append("Found ").append(queues.size());
        if (queues.size() == 1) {
          builder.append(" queue.\n\n");
        } else {
          builder.append(" queues.\n\n");
        }
        QueuesTableFormatter qtf = new QueuesTableFormatter(queues);
        qtf.writeTableHeader(builder);
        qtf.writeTableRows(builder);
      } else {
        builder.append("No queues registered.");
      }
    }

    writeToCommandLine(statusOutputStream, builder.toString());

  }


  public void addListItemOLDSTYLE(IQueue queue, StringBuilder s, boolean withLineBreak) {
    s.append(queue.getUniqueName()).append(", ");
    s.append(queue.getExternalName()).append(", ");
    s.append(queue.getQueueType().toString()).append(", ");
    s.append(queue.getConnectData().toString());
    if (withLineBreak) {
      s.append("\n");
    }
  }


  public static String addListHeaderOLDSTYLE(StringBuilder s) {
    s.append("Registered Queues  (");
    s.append("Unique Name, ");
    s.append("External Name, ");
    s.append("Queue Type, ");
    s.append("Connect Data ");
    s.append(") \n");
    return s.toString();
  }


  private static class QueuesTableFormatter extends TableFormatter {

    private List<List<String>> rows;
    private List<String> header;
    private List<QueuesTableColumn> columns;
   
    public QueuesTableFormatter(Collection<? extends IQueue> queues) {
      columns = //Arrays.asList(QueuesTableColumn.values() );
          QueuesTableColumn.ALL_WITHOUT_PASSWORD;
      generateRowsAndHeader(queues);
    }

    private void generateRowsAndHeader(Collection<? extends IQueue> queues) {
      header = new ArrayList<String>();
      for( QueuesTableColumn ac : columns ) {
        header.add( ac.toString() );
      }
      rows = new ArrayList<List<String>>();
      for( IQueue ai : queues ) {
        rows.add( generateRow(ai) );
      }
    }

    private List<String> generateRow(IQueue ai) {
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
      UniqueXynaQueueName {
        public String extract(IQueue ai) {
          return ai.getUniqueName();
        }
      },
      ExternalQueueName {
        public String extract(IQueue ai) {
          return ai.getExternalName();
        }
      },
      Type {
        public String extract(IQueue ai) {
          return ai.getQueueType().toString();
        }
      },
      Hostname {
        public String extract(IQueue ai) {
          if (ai.getConnectData() instanceof WebSphereMQConnectData) {
            return ((WebSphereMQConnectData) ai.getConnectData()).getHostname();
          } else if (ai.getConnectData() instanceof ActiveMQConnectData) {
            return ((ActiveMQConnectData) ai.getConnectData()).getHostname();
          } else {
            return "-";
          }
        }
      },
      Port {
        public String extract(IQueue ai) {
          if (ai.getConnectData() instanceof WebSphereMQConnectData) {
            return ((WebSphereMQConnectData) ai.getConnectData()).getPort() + "";
          } else if (ai.getConnectData() instanceof ActiveMQConnectData) {
            return ((ActiveMQConnectData) ai.getConnectData()).getPort() + "";
          } else {
            return "-";
          }
        }
      },
      QueueManager {
        public String extract(IQueue ai) {
          if (ai.getConnectData() instanceof WebSphereMQConnectData) {
            return ((WebSphereMQConnectData) ai.getConnectData()).getQueueManager();
          } else {
            return "-";
          }
        }
      }, Channel {
        public String extract(IQueue ai) {
          if (ai.getConnectData() instanceof WebSphereMQConnectData) {
            return ((WebSphereMQConnectData) ai.getConnectData()).getChannel();
          } else {
            return "-";
          }
        }
      }, ConnectString {
        public String extract(IQueue ai) {
          if (ai.getConnectData() instanceof OracleAQConnectData) {
            return ((OracleAQConnectData) ai.getConnectData()).getJdbcUrl();
          } else {
            return "-";
          }
        }
      }, UserName {
        public String extract(IQueue ai) {
          if (ai.getConnectData() instanceof OracleAQConnectData) {
            return ((OracleAQConnectData) ai.getConnectData()).getUserName();
          } else {
            return "-";
          }
        }
      }, Password {
        public String extract(IQueue ai) {
          if (ai.getConnectData() instanceof OracleAQConnectData) {
            return ((OracleAQConnectData) ai.getConnectData()).getPassword();
          } else {
            return "-";
          }
        }
      };

      
      public static final List<QueuesTableColumn> ALL_WITHOUT_PASSWORD = Collections.unmodifiableList(
          Arrays.asList(UniqueXynaQueueName, ExternalQueueName, Type, Hostname, Port, 
              QueueManager, Channel, ConnectString, UserName) );
      
      public abstract String extract(IQueue ai);
    }

  }

}
