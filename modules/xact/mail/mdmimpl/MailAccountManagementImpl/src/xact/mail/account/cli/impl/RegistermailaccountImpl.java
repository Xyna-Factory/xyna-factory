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

import xact.mail.account.MailAccountData;
import xact.mail.account.MailAccountData.KnownAccountProtocol;
import xact.mail.account.MailAccountData.KnownTransportProtocol;
import xact.mail.account.MailAccountData.Security;
import xact.mail.account.MailAccountStorage;
import xact.mail.account.MailAccountStorageException;
import xact.mail.account.cli.generated.Registermailaccount;



public class RegistermailaccountImpl extends XynaCommandImplementation<Registermailaccount> {

  public void execute(OutputStream statusOutputStream, Registermailaccount payload) throws XynaException {
    
    KnownAccountProtocol kap = KnownAccountProtocol.fromString(payload.getType());
    KnownTransportProtocol ktp = KnownTransportProtocol.fromString(payload.getTransportType());
    Security accSec = Security.fromString(payload.getAccountSecurity());
    Security transSec = Security.fromString(payload.getTransportSecurity());
    
    MailAccountData.Builder madb = new MailAccountData.Builder()
        .name(payload.getName())
        .host(payload.getHost())
        .address(payload.getAddress())
        .user(payload.getUser())
        .password(payload.getPassword())
        .keyStore(payload.getKeyStore())
        .trustStore(payload.getTrustStore())
        .accountProtocol(kap)
        .transportProtocol(ktp)
        .accountSecurity(accSec)
        .transportSecurity(transSec);
    
    if (payload.getAccountPort() == null ||
        payload.getAccountPort().isEmpty()) {
      switch (kap) {
        case IMAP :
          switch (accSec) {
            case NONE :
            case STARTTLS :
              madb.accountPort(143);
              break;
            case SSL:
              madb.accountPort(993);
              break;
            default :
              break;
          }
          break;
        case POP3 :
          switch (accSec) {
            case NONE :
            case STARTTLS :
              madb.accountPort(110);
              break;
            case SSL:
              madb.accountPort(995);
              break;
            default :
              break;
          }
          break;
        default :
          break;
      }
    } else {
      madb.accountPort(Integer.parseInt(payload.getAccountPort()));
    }
    
    if (payload.getTransportPort() == null ||
        payload.getTransportPort().isEmpty()) {
      switch (ktp) {
        case SMTP :
          switch (transSec) {
            case NONE :
              madb.transportPort(25);
              break;
            case SSL:
            case STARTTLS :
              madb.transportPort(465);
              break;
            default :
              break;
          }
          break;
        default :
          break;
      }
    } else {
      madb.transportPort(Integer.parseInt(payload.getTransportPort()));
    }
    
    MailAccountData mad = madb.build();
    
    try {
      if (payload.getReplace()) {
        MailAccountStorage.getInstance().replaceMailAccount(mad);
      } else {
        MailAccountStorage.getInstance().addNewMailAccount(mad);
      } 
    } catch (MailAccountStorageException e) {
      ((CommandLineWriter)statusOutputStream).writeLineToCommandLine(e.getMessage());
    }
  }


}
