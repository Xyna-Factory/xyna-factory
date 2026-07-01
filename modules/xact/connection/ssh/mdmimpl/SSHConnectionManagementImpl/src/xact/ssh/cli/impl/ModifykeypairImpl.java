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
package xact.ssh.cli.impl;

import java.io.OutputStream;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import xact.ssh.cli.generated.Modifykeypair;
import xact.ssh.impl.SSHConnectionManagementRepositoryAccess;


public class ModifykeypairImpl extends XynaCommandImplementation<Modifykeypair> {

  public void execute(OutputStream statusOutputStream, Modifykeypair payload) throws XynaException {
    String identity = "";
    String typeclass = null;
    Integer priority = 0;
    if (payload.getNew_priority() != null) {
        try {
          priority = Integer.parseInt(payload.getNew_priority());
        } catch (NumberFormatException e) { /* ntbd */ }
    }
    if (payload.getNew_identity() != null) {
      identity = payload.getNew_identity();
    }
    if (payload.getNew_typeclass() != null) {
      typeclass = payload.getNew_typeclass();
    }
    SSHConnectionManagementRepositoryAccess.modifyKeyPair(payload.getIdentity(), identity, priority, typeclass);
  }
}
