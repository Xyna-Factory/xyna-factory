/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package xact.sftp.cli.impl;

import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import java.io.OutputStream;
import com.gip.xyna.utils.exceptions.XynaException;
import xact.sftp.cli.generated.Addonetimecredentials;
import xact.sftp.impl.SFTPTriggerAccessServiceOperationImpl;



public class AddonetimecredentialsImpl extends XynaCommandImplementation<Addonetimecredentials> {

  public void execute(OutputStream statusOutputStream, Addonetimecredentials payload) throws XynaException {
    SFTPTriggerAccessServiceOperationImpl.addOneTimeCredentials(payload.getUsername(), payload.getPassword(), payload.getIp(), payload.getPort());
    writeLineToCommandLine(statusOutputStream, "One time crediantials added succesfully.");
  }

}
