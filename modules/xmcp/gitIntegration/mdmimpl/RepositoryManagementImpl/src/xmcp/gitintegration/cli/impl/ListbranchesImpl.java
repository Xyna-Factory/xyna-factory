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
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import xmcp.gitintegration.cli.generated.Listbranches;
import xmcp.gitintegration.impl.RepositoryInteraction;
import xmcp.gitintegration.repository.Branch;
import xmcp.gitintegration.repository.BranchData;



public class ListbranchesImpl extends XynaCommandImplementation<Listbranches> {

  public void execute(OutputStream statusOutputStream, Listbranches payload) throws XynaException {
    try {
      BranchData result = new RepositoryInteraction().listBranches(payload.getRepository());
      writeToCommandLine(statusOutputStream, "There are " + result.getBranches().size() + " branches:\n");
      writeToCommandLine(statusOutputStream, "Current Branch: " + formatBranch(result.getCurrentBranch()));
      for(Branch branch : result.getBranches()) {
        writeToCommandLine(statusOutputStream, formatBranch(branch));
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  private String formatBranch(Branch branch) {
    if(branch == null) {
      return "null\n";
    }
    String target = branch.getTarget() != null && !branch.getTarget().isEmpty()? " " + branch.getTarget() : "";
    return branch.getName() + target + " @ " + branch.getCommitHash() + "\n";
  }

}
