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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceManagement;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.storables.OrderInputSourceStorable;
import com.gip.xyna.xmcp.xfcli.ReturnCode;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listorderinputsources;
import com.gip.xyna.xnwh.selection.parsing.ArchiveIdentifier;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SearchResult;



public class ListorderinputsourcesImpl extends XynaCommandImplementation<Listorderinputsources> {

  private static final String[] HEADER = new String[] {OrderInputSourceStorable.COL_ID, OrderInputSourceStorable.COL_NAME,
      OrderInputSourceStorable.COL_TYPE, OrderInputSourceStorable.COL_ORDERTYPE};
  private static final String FORMAT_HEADER = "   %13s  %25s  %15s  %-60s";
  private static final String FORMAT = "   - %13d  %25s  %15s  %-60s";
  private static final String FORMAT_PARAMETER = "      - %15s = %-60s";


  @SuppressWarnings("unchecked")
  public void execute(OutputStream statusOutputStream, Listorderinputsources payload) throws XynaException {
    OrderInputSourceManagement oism =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getOrderInputSourceManagement();
    SearchRequestBean searchRequest = new SearchRequestBean(ArchiveIdentifier.orderInputSource, -1);
    if (payload.getApplicationName() != null) {
      searchRequest.addFilterEntry(OrderInputSourceStorable.COL_APPLICATIONNAME, payload.getApplicationName());
    }
    if (payload.getVersionName() != null) {
      searchRequest.addFilterEntry(OrderInputSourceStorable.COL_VERSIONNAME, payload.getVersionName());
    }
    if (payload.getWorkspaceName() != null) {
      searchRequest.addFilterEntry(OrderInputSourceStorable.COL_WORKSPACENAME, payload.getWorkspaceName());
    }
    SearchResult<?> inputSources = oism.searchInputSources(searchRequest);
    if (inputSources.getCount() == 0) {
      writeLineToCommandLine(statusOutputStream, "No orderinputsources found.");
      writeEndToCommandLine(statusOutputStream, ReturnCode.SUCCESS);
      return;
    }
    Map<RuntimeContext, ArrayList<OrderInputSourceStorable>> group =
        CollectionUtils.group((List<OrderInputSourceStorable>) inputSources.getResult(),
                              new Transformation<OrderInputSourceStorable, RuntimeContext>() {

                                public RuntimeContext transform(OrderInputSourceStorable from) {
                                  if (from.getWorkspaceName() != null) {
                                    return new Workspace(from.getWorkspaceName());
                                  }
                                  return new Application(from.getApplicationName(), from.getVersionName());
                                }

                              });
    List<Entry<RuntimeContext, ArrayList<OrderInputSourceStorable>>> l =
        new ArrayList<Entry<RuntimeContext, ArrayList<OrderInputSourceStorable>>>(group.entrySet());
    Collections.sort(l, new Comparator<Entry<RuntimeContext, ArrayList<OrderInputSourceStorable>>>() {

      public int compare(Entry<RuntimeContext, ArrayList<OrderInputSourceStorable>> o1,
                         Entry<RuntimeContext, ArrayList<OrderInputSourceStorable>> o2) {
        int c = o1.getKey().getName().compareTo(o2.getKey().getName());
        if (c != 0) {
          return c;
        }
        if (o1.getKey() instanceof Application && o2.getKey() instanceof Application) {
          return ((Application) o1.getKey()).getVersionName().compareTo(((Application) o2.getKey()).getVersionName());
        }
        return 0;
      }

    });

    writeLineToCommandLine(statusOutputStream, String.format(FORMAT_HEADER, (Object[]) HEADER));
    for (Entry<RuntimeContext, ArrayList<OrderInputSourceStorable>> e : l) {
      print(statusOutputStream, e.getKey());
      print(statusOutputStream, e.getValue(), payload.getVerbose());
    }
    writeEndToCommandLine(statusOutputStream, ReturnCode.SUCCESS);
  }


  private void print(OutputStream os, ArrayList<OrderInputSourceStorable> l, boolean verbose) {
    Collections.sort(l, new Comparator<OrderInputSourceStorable>() {

      public int compare(OrderInputSourceStorable o1, OrderInputSourceStorable o2) {
        return o1.getName().compareTo(o2.getName());
      }

    });
    for (int i = 0; i < l.size(); i++) {
      OrderInputSourceStorable ois = l.get(i);
      writeLineToCommandLine(os, String.format(FORMAT, ois.getId(), ois.getName(), ois.getType(), ois.getOrderType()));
      if (verbose) {
        for (Entry<String, String> e : ois.getParameters().entrySet()) {
          //TODO plugindescription verwenden, um key besser darzustellen
          writeLineToCommandLine(os, String.format(FORMAT_PARAMETER, e.getKey(), e.getValue()));
        }
      }
    }
  }


  private void print(OutputStream s, RuntimeContext rc) {
    writeLineToCommandLine(s, rc);
  }

}
