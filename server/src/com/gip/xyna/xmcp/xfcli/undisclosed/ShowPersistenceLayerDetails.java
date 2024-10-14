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
package com.gip.xyna.xmcp.xfcli.undisclosed;

import java.io.IOException;
import java.io.Reader;

import com.gip.xyna.xmcp.xfcli.AllArgs;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCLIConnection.CommandExecution;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.PersistenceLayerInstanceBean;


/**
 *
 */
public class ShowPersistenceLayerDetails implements CommandExecution {

  public void execute(AllArgs allArgs, CommandLineWriter clw) throws IOException {
    if (allArgs.getArgCount() == 0 ) {
      return;
    }
    PersistenceLayerInstanceBean[] instances = ODSImpl.getInstance().getPersistenceLayerInstances();
    for (PersistenceLayerInstanceBean instance : instances) {
      if (allArgs.getArg(0).equals(String.valueOf(instance.getPersistenceLayerInstanceID()))) {
        
        String[] args = allArgs.getArgsAsArray(1);
        Reader reader = instance.getPersistenceLayerInstance().getExtendedInformation(args);
        clw.writeReaderToCommandLine(reader);
        break;
      }
    }

  }

}
