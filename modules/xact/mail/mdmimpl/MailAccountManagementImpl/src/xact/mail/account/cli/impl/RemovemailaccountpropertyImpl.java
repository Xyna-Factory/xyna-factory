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
package xact.mail.account.cli.impl;

import java.io.OutputStream;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;

import xact.mail.account.MailAccountData.MailAccountProperty;
import xact.mail.account.MailAccountStorage;
import xact.mail.account.MailAccountStorageException;
import xact.mail.account.cli.generated.Removemailaccountproperty;



public class RemovemailaccountpropertyImpl extends XynaCommandImplementation<Removemailaccountproperty> {

  public void execute(OutputStream statusOutputStream, Removemailaccountproperty payload) throws XynaException {
    MailAccountProperty map = new MailAccountProperty(payload.getKey(), null, null );
    try {
      MailAccountStorage.getInstance().removeMailAccountProperty(payload.getName(), map);
    } catch( MailAccountStorageException e ) {
      ((CommandLineWriter)statusOutputStream).writeLineToCommandLine(e.getMessage());
    }
  }

}
