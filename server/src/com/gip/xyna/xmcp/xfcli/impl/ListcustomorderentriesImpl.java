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
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.TableFormatter;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RevisionOrderControl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RevisionOrderControl.CustomOrderEntryInformation;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listcustomorderentries;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;



public class ListcustomorderentriesImpl extends XynaCommandImplementation<Listcustomorderentries> {

  public void execute(OutputStream statusOutputStream, Listcustomorderentries payload) throws XynaException {
    Collection<CustomOrderEntryInformation> customOrderEntryTypes = RevisionOrderControl.getAllCustomOrderEntryTypes();

    if (customOrderEntryTypes != null && customOrderEntryTypes.size() > 0) {
      OrderEntryTableFormatter formatter = new OrderEntryTableFormatter(customOrderEntryTypes);
      StringBuilder sb = new StringBuilder();
      formatter.writeTableRows(sb);
      writeToCommandLine(statusOutputStream, formatter.writeTableHeader());
      writeLineToCommandLine(statusOutputStream, sb.toString());
    } else {
      writeToCommandLine(statusOutputStream, "No custom order entry types defined");
    }
  }


  private static class OrderEntryTableFormatter extends TableFormatter {

    private List<List<String>> rows;
    private List<String> header;
    private List<OrderEntryColumn> columns;


    public OrderEntryTableFormatter(Collection<CustomOrderEntryInformation> customOrderEntries) {
      columns = Arrays.asList(OrderEntryColumn.values());
      generateRowsAndHeader(customOrderEntries);
    }


    private void generateRowsAndHeader(Collection<CustomOrderEntryInformation> customOrderEntries) {
      header = new ArrayList<String>();
      for (OrderEntryColumn oc : columns) {
        header.add(oc.toString());
      }
      rows = new ArrayList<List<String>>();
      for (CustomOrderEntryInformation ci : customOrderEntries) {
        rows.add(generateRow(ci));
      }
    }


    private List<String> generateRow(CustomOrderEntryInformation ci) {
      List<String> row = new ArrayList<String>();
      for (OrderEntryColumn oc : columns) {
        row.add(oc.extract(ci));
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


    public enum OrderEntryColumn {
      Name {

        public String extract(CustomOrderEntryInformation coi) {
          return coi.getName();
        }
      },
      Description {

        public String extract(CustomOrderEntryInformation coi) {
          return coi.getDescription();
        }
      },
      DefiningRuntimeContext {

        public String extract(CustomOrderEntryInformation coi) {
          RuntimeContext rtc;
          String result;
          try {
            RevisionManagement revisionManagement;
            revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
            rtc = revisionManagement.getRuntimeContext(coi.getDefiningRevision());
            result = rtc.getGUIRepresentation();
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
            result = "<error>";
          }

          return String.valueOf(result);
        }
      };

      public abstract String extract(CustomOrderEntryInformation coi);
    }
  }

}
