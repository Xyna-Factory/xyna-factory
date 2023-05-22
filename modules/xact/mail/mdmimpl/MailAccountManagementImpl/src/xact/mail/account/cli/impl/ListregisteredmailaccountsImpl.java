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
package xact.mail.account.cli.impl;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.TableFormatter;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;

import xact.mail.account.MailAccountData;
import xact.mail.account.MailAccountStorage;
import xact.mail.account.MailAccountData.KnownAccountProtocol;
import xact.mail.account.MailAccountData.KnownTransportProtocol;
import xact.mail.account.MailAccountData.MailAccountProperty;
import xact.mail.account.MailAccountData.Security;
import xact.mail.account.cli.generated.Listregisteredmailaccounts;



public class ListregisteredmailaccountsImpl extends XynaCommandImplementation<Listregisteredmailaccounts> {

  public void execute(OutputStream statusOutputStream, Listregisteredmailaccounts payload) throws XynaException {
    Collection<MailAccountData> mailAccounts = MailAccountStorage.getInstance().getMailAccounts();
    
    StringBuilder output = new StringBuilder();
    MailAccountDataTableFormatter madtf = new MailAccountDataTableFormatter(mailAccounts);
    madtf.writeTableHeader(output);
    madtf.writeTableRows(output);
    
    writeLineToCommandLine(statusOutputStream, output.toString());
    if( payload.getProperties() ) {
      output = new StringBuilder();
      MailAccountPropertyTableFormatter maptf = new MailAccountPropertyTableFormatter(mailAccounts);
      maptf.writeTableHeader(output);
      maptf.writeTableRows(output);
      writeLineToCommandLine(statusOutputStream, output.toString());
    }
    
  }

  private static class MailAccountDataTableFormatter extends TableFormatter {

    private List<List<String>> rows;
    private List<String> header;
    private List<MailAccountDataColumn> columns;


    public MailAccountDataTableFormatter(Collection<MailAccountData> mailAccounts) {
      columns = Arrays.asList(MailAccountDataColumn.values() );
      generateRowsAndHeader(mailAccounts);
    }

    private void generateRowsAndHeader(Collection<MailAccountData> mailAccounts) {
      header = new ArrayList<>();
      for( MailAccountDataColumn madc : columns ) {
        header.add( madc.getDisplayName() );
      }
      rows = new ArrayList<>();
      for( MailAccountData mad : mailAccounts ) {
        rows.add( generateRow(mad) );
      }
    }

    private List<String> generateRow(MailAccountData mad) {
      List<String> row = new ArrayList<>();
      for( MailAccountDataColumn madc : columns ) {
        row.add( madc.extract(mad) );
      }
      return row;
    }
    
    @Override
    public List<List<String>> getRows() {
      return rows;
    }
    
    @Override
    public List<String> getHeader() {
      return header;
    }

    private enum MailAccountDataColumn {
      
      NAME("Name") {
        public String extract(MailAccountData mad) {
          return mad.getName();
        }
      },
      HOST("Host") {
        public String extract(MailAccountData mad) {
          return mad.getHost();
        }
      },
      ADDRESS("Address") {
        public String extract(MailAccountData mad) {
          return mad.getAddress();
        }
      },
      USER("User") {
        public String extract(MailAccountData mad) {
          return nullToEmtpy(mad.getUser());
        }
      },
      TYPE("Type") {
        public String extract(MailAccountData mad) {
          return String.valueOf(deriveMailType(mad));
        }
      },
      KEYSTORE("Keystore") {
        public String extract(MailAccountData mad) {
          return nullToEmtpy(mad.getKeyStore());
        }
      },
      TRUSTSTORE("Truststore") {
        public String extract(MailAccountData mad) {
          return nullToEmtpy(mad.getTrustStore());
        }
      },
      ;
      
      private String displayName;

      private MailAccountDataColumn(String displayName) {
        this.displayName = displayName;
      }
      public String getDisplayName() {
        return displayName;
      }
      public abstract String extract(MailAccountData mad);
    }
    
  }
  
  private static String deriveMailType(MailAccountData mad) {
    StringBuilder typeBuilder = new StringBuilder();
    if (mad.getAccountProtocol() == KnownAccountProtocol.NONE &&
        mad.getTransportProtocol() == KnownTransportProtocol.NONE) {
      typeBuilder.append("Internal"); 
    }
    
    if (mad.getTransportProtocol() != KnownTransportProtocol.NONE) {
      typeBuilder.append(mad.getTransportProtocol().getProtocolIdentifier());
      appendSecurity(typeBuilder,mad.getTransportSecurity());
      if (mad.getAccountProtocol() == KnownAccountProtocol.NONE) {
        typeBuilder.append(" (send only)");
      } else {
        typeBuilder.append(" ");
      }
    }
    
    if (mad.getAccountProtocol() != KnownAccountProtocol.NONE) {
      typeBuilder.append(mad.getAccountProtocol().getProtocolIdentifier());
      appendSecurity(typeBuilder,mad.getAccountSecurity());
      if (mad.getTransportProtocol() == KnownTransportProtocol.NONE) {
        typeBuilder.append(" (receive only)");
      }
    }
    
    return typeBuilder.toString();
  }
  
  private static void appendSecurity(StringBuilder builder, Security security) {
    switch (security) {
      case NONE :
        break;
      case SSL:
        builder.append("s");
        break;
      case STARTTLS:
        builder.append("+"+Security.STARTTLS.getName());
        break;
      default :
        break;
    }
  }
  
  private static class MailAccountPropertyTableFormatter extends TableFormatter {

    private List<List<String>> rows;
    private List<String> header;


    public MailAccountPropertyTableFormatter(Collection<MailAccountData> mailAccounts) {
      generateRowsAndHeader(mailAccounts);
    }

    private void generateRowsAndHeader(Collection<MailAccountData> mailAccounts) {
      header = Arrays.asList( "MailAccount", "Property name", "Property value", "Documentation");
      rows = new ArrayList<>();
      for( MailAccountData mad : mailAccounts ) {
        for( MailAccountProperty map : mad.getProperties() ) {
          rows.add( generateRow( mad.getName(), map) );
        }
      }
    }

    private List<String> generateRow(String name, MailAccountProperty map) {
      return Arrays.asList( name, map.getKey(), nullToEmtpy(map.getValue()), nullToEmtpy(map.getDocumentation()) );
    }
    
    @Override
    public List<List<String>> getRows() {
      return rows;
    }
    
    @Override
    public List<String> getHeader() {
      return header;
    }
  }

  public static String nullToEmtpy(String value) {
    if( value == null ) {
      return "";
    }
    return value;
  }
  
}

