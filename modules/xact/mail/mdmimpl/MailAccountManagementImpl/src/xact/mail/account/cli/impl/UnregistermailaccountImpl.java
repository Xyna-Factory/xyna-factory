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
package xact.mail.account.cli.impl;

import java.io.OutputStream;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;

import xact.mail.account.MailAccountStorage;
import xact.mail.account.MailAccountStorageException;
import xact.mail.account.cli.generated.Unregistermailaccount;



public class UnregistermailaccountImpl extends XynaCommandImplementation<Unregistermailaccount> {

  public void execute(OutputStream statusOutputStream, Unregistermailaccount payload) throws XynaException {
    try {
      MailAccountStorage.getInstance().removeMailAccount(payload.getName());
    } catch( MailAccountStorageException e ) {
      ((CommandLineWriter)statusOutputStream).writeLineToCommandLine(e.getMessage());
    }
  }

}
