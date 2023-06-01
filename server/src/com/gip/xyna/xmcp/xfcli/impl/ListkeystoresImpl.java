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

import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.TableFormatter;
import com.gip.xyna.xfmg.xfctrl.keymgmt.KeyManagement;
import com.gip.xyna.xfmg.xfctrl.keymgmt.KeyStore;
import com.gip.xyna.xfmg.xfctrl.keymgmt.KeyStoreInformation;
import com.gip.xyna.xfmg.xfctrl.keymgmt.KeyStoreType;
import com.gip.xyna.xmcp.xfcli.generated.Listkeystores;



public class ListkeystoresImpl extends XynaCommandImplementation<Listkeystores> {

  public void execute(OutputStream statusOutputStream, Listkeystores payload) throws XynaException {
    KeyManagement keyMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getKeyManagement();
    
    StringBuilder output = new StringBuilder();
    KeyStoreInformationFormatter ksif = new KeyStoreInformationFormatter(keyMgmt.listKeyStores());
    ksif.writeTableHeader(output);
    ksif.writeTableRows(output);
    writeToCommandLine(statusOutputStream, output.toString());
  }
  
  
  private static class KeyStoreInformationFormatter extends TableFormatter {

    private final List<List<String>> rows;
    
    KeyStoreInformationFormatter(Collection<KeyStoreInformation> info) {
      rows = new ArrayList<List<String>>();
      for (KeyStoreInformation ks : info) {
        List<String> row = new ArrayList<String>();
        row.add(ks.getName());
        row.add(ks.getType());
        rows.add(row);
      }
    }
    
    @Override
    public List<List<String>> getRows() {
      return rows;
    }

    @Override
    public List<String> getHeader() {
      return Arrays.asList("name", "type");
    }
    
  }

}
