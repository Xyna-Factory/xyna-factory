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
package xmcp.gitintegration.cli.impl;

import java.io.OutputStream;
import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;

import xmcp.gitintegration.FactoryContent;
import xmcp.gitintegration.FactoryContentDifference;
import xmcp.gitintegration.FactoryContentDifferences;
import xmcp.gitintegration.cli.generated.Comparefactoryxml;
import xmcp.gitintegration.impl.CliOutputCreator;
import xmcp.gitintegration.impl.FactoryContentComparator;
import xmcp.gitintegration.impl.FactoryContentCreator;



public class ComparefactoryxmlImpl extends XynaCommandImplementation<Comparefactoryxml> {

  public void execute(OutputStream statusOutputStream, Comparefactoryxml payload) throws XynaException {
    FactoryContentCreator creator = new FactoryContentCreator();
    
    FactoryContent xmlConfig = creator.readFactoryContent();
    FactoryContent currentConfig = creator.createFactoryContent();
    FactoryContentComparator comparator = new FactoryContentComparator();
    FactoryContentDifferences differences = comparator.compareFactoryContent(currentConfig, xmlConfig, true);
    
    List<? extends FactoryContentDifference> diffs = differences.getDifferences();
    if (!diffs.isEmpty()) {
      writeToCommandLine(statusOutputStream, "List has been saved. Id: " + differences.getListId() + ".\n");
      writeToCommandLine(statusOutputStream, "Use resolvefactoryxml to update factory\n");
      writeToCommandLine(statusOutputStream, "Use listfactorydiffs to show open lists\n");
    }
    String differenceString = diffs.size() == 1 ? "is one difference " : "are " + diffs.size() + " differences ";
    writeToCommandLine(statusOutputStream, "There " + differenceString + " between factory.xml and factory state.\n");
    CliOutputCreator outputCreator = new CliOutputCreator();
    String output = outputCreator.createOutput(diffs);
    writeToCommandLine(statusOutputStream, output);
  }

}
