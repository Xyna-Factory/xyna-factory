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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import xmcp.gitintegration.cli.generated.Listcommits;
import xmcp.gitintegration.impl.RepositoryInteraction;
import xmcp.gitintegration.repository.Commit;



public class ListcommitsImpl extends XynaCommandImplementation<Listcommits> {

  private static final String CO = "%s (UTC) %s %s %s %s\n";
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");


  public void execute(OutputStream statusOutputStream, Listcommits payload) throws XynaException {
    try {
      List<Commit> commits =
          new RepositoryInteraction().listCommits(payload.getRepository(), payload.getBranch(), Integer.parseInt(payload.getLength()));
      for (Commit commit : commits) {
        String time = DATE_FORMAT.format(new Date(commit.getCommitTime()));
        writeToCommandLine(statusOutputStream, String.format(CO, time, commit.getCommitHash(), commit.getAuthorName(),
                                                             commit.getAuthorEmail(), commit.getComment().trim()));
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
