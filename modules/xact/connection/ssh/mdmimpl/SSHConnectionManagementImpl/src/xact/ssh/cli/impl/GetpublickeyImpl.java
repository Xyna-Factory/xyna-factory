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

import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.utils.exceptions.XynaException;
import java.io.OutputStream;
import java.util.List;

import xact.ssh.EncryptionType;
import xact.ssh.cli.generated.Getpublickey;
import xact.ssh.impl.SSHConnectionManagementRepositoryAccess;



public class GetpublickeyImpl extends XynaCommandImplementation<Getpublickey> {

  public void execute(OutputStream statusOutputStream, Getpublickey payload) throws XynaException {
    EncryptionType type = null;
    if (payload.getEncryptiontype() != null) {
      type = EncryptionType.getByStringRepresentation(payload.getEncryptiontype());
    }
    List<String> keys = SSHConnectionManagementRepositoryAccess.getPublicKey(type);
    writeLineToCommandLine(statusOutputStream, "Found " + keys.size() + " identities:");
    for (String key : keys) {
      writeLineToCommandLine(statusOutputStream, key);
    }
  }

}
