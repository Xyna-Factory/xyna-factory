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


import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xmcp.xfcli.AllArgs;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCLIConnection.CommandExecution;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableColumnInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.XMOMStorableStructureInformation;

public class PrintStorableCode implements CommandExecution {

  @Override
  public void execute(AllArgs allArgs, CommandLineWriter clw) throws Exception {

    Parameters p = Parameters.parse(allArgs, clw);
    if (p == null) {
      return;
    }
    
    XMOMStorableStructureCache cache = XMOMStorableStructureCache.getInstance(p.revision);
    StorableStructureInformation ssi = cache.getStructuralInformation(p.fqNameRoot);

    // TODO has problems with flattening!
    // even if the target storable is not flattened, the path has to be entered post flattening
    if (p.path != null &&
        !p.path.isEmpty()) {
      String[] parts = p.path.split("\\.");
      for (int i = 0; i < parts.length; i++) {
        StorableColumnInformation sci = follow(ssi, parts[i]);
        if (sci == null) {
          throw new RuntimeException("Path-Part " + parts[i] + " could not be followed!");
        }
        if (!sci.isStorableVariable()) {
          throw new RuntimeException("Path-Part " + parts[i] + " is primitive!");
        }
        ssi = sci.getStorableVariableInformation();
      }
    }
    
    clw.writeLineToCommandLine(ssi.getStorableSource().getCode());
  }
  
  
  StorableColumnInformation follow(StorableStructureInformation ssi, String varName) {
    StorableColumnInformation column = ssi.getColumnInfo(varName);
    if (column == null) {
      for (StorableStructureInformation sub : ssi.getSuperRootStorableInformation().getSubEntriesRecursivly()) {
        column = sub.getColumnInfo(varName);
        if (column != null) {
          break;
        }
      }
    }
    return column;
  }



  private static class Parameters {

    String fqNameRoot;
    long revision;
    String path;
    

    private Parameters() {
    }


    public static Parameters parse(AllArgs allArgs, CommandLineWriter clw) {
      Parameters p = new Parameters();
      try {
        switch (allArgs.getArgCount()) {
          case 3 :
            p.path = allArgs.getArg(2);
          case 2 :
            p.revision = Long.valueOf(allArgs.getArg(1));
            p.fqNameRoot = allArgs.getArg(0);
            break;
          default :
            writeUsage(clw);
            return null;
        }
        return p;
      } catch (Exception e) {
        CentralFactoryLogging.getLogger(CheckClassloader.class).debug("Failed to parse parameters", e);
        writeUsage(clw);
        return null;
      }
    }


    private static void writeUsage(CommandLineWriter clw) {
      clw.writeLineToCommandLine("parameters are <fqName of root XMOMStorable: String>, <revision: long>, [<path to object: String>,]\n");
    }
  }

}
