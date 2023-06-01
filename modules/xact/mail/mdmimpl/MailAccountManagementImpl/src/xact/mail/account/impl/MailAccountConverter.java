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
package xact.mail.account.impl;

import java.util.ArrayList;
import java.util.List;

import xact.mail.account.AccountProtocolParameter;
import xact.mail.account.MailAccountData;
import xact.mail.account.MailAccountData.AccountProtocol;
import xact.mail.account.MailAccountData.KnownAccountProtocol;
import xact.mail.account.MailAccountData.KnownTransportProtocol;
import xact.mail.account.MailAccountParameter;
import xact.mail.account.MailAccountProperty;
import xact.mail.account.TransportProtocolParameter;
import xact.mail.enums.IMAP;
import xact.mail.enums.POP3;
import xact.mail.enums.Protocol;
import xact.mail.enums.SSL;
import xact.mail.enums.STARTTLS;
import xact.mail.enums.Security;

/**
 * Konversion xact.mail.account.MailAccountData in XMOM.Objekt xact.mail.account.MailAccountParameter und umgekehrt
 * FIXME Verdopplung: Auch in MailAdapterImpl
 */
public class MailAccountConverter {

  private MailAccountConverter() {
  }
  
  public static  MailAccountData convertFromXmom(MailAccountParameter mailAccount) {
    MailAccountData.Builder madBuilder = new MailAccountData.Builder();
    
    madBuilder.name(mailAccount.getName());
    madBuilder.host(mailAccount.getHost());
    madBuilder.user(mailAccount.getUser()).password(mailAccount.getPassword());
    
    madBuilder.keyStore(mailAccount.getKeyStore());
    madBuilder.trustStore(mailAccount.getTrustStore());
    
    AccountProtocolParameter accParams = mailAccount.getAccountProtocol();
    KnownAccountProtocol accProt = convertAccountProtocolFromXmom(accParams.getProtocol());
    madBuilder.accountProtocol(accProt);
    xact.mail.account.MailAccountData.Security accSec = convertSecurityFromXmom(accParams.getSecurity());
    madBuilder.accountSecurity(accSec);
    
    TransportProtocolParameter transParams = mailAccount.getTransportProtocol();
    xact.mail.account.MailAccountData.Security transSec = convertSecurityFromXmom(transParams.getSecurity());
    madBuilder.transportSecurity(transSec);
    
    if (accParams.getPort() == null ||
        accParams.getPort() <= 0) {
      switch (accProt) {
        case IMAP :
          switch (accSec) {
            case NONE :
            case STARTTLS :
              madBuilder.accountPort(143);
              break;
            case SSL:
              madBuilder.accountPort(993);
              break;
            default :
              break;
          }
          break;
        case POP3 :
          switch (accSec) {
            case NONE :
            case STARTTLS :
              madBuilder.accountPort(110);
              break;
            case SSL:
              madBuilder.accountPort(995);
              break;
            default :
              break;
          }
          break;
        default :
          break;
      }
    } else {
      madBuilder.accountPort(accParams.getPort());
    }
    
    if (transParams.getPort() == null) {
      switch (transSec) {
        case NONE :
          madBuilder.transportPort(25);
          break;
        case SSL:
        case STARTTLS :
          madBuilder.transportPort(465);
          break;
        default :
          break;
      }
    } else if (transParams.getPort() < 0) {
      madBuilder.transportProtocol(KnownTransportProtocol.NONE);
    } else {
      madBuilder.transportProtocol(KnownTransportProtocol.SMTP);
      madBuilder.transportPort(transParams.getPort());
    }
    
    madBuilder.properties(convertMailAccountPropertysFromXmom(mailAccount.getAdditionalProperties()));
    
    if( mailAccount.getAdditionalProperties() != null ) {
      for( MailAccountProperty map : mailAccount.getAdditionalProperties() ) {
        madBuilder.addProperty( convertMailAccountPropertyFromXmom(map) );
      }
    }
    return madBuilder.build();
  }

  private static List<MailAccountData.MailAccountProperty> convertMailAccountPropertysFromXmom(List<? extends MailAccountProperty> properties) {
    if( properties == null || properties.isEmpty() ) {
      return new ArrayList<>(0);
    }
    List<MailAccountData.MailAccountProperty> maps = new ArrayList<>();
    for( MailAccountProperty map : properties ) {
      maps.add( new MailAccountData.MailAccountProperty( map.getKey(), map.getValue(),  map.getDocumentation() ) );
     }
    return maps;
  }

  private static MailAccountData.MailAccountProperty convertMailAccountPropertyFromXmom(MailAccountProperty mapXmom) {
    return new MailAccountData.MailAccountProperty(mapXmom.getKey(), mapXmom.getValue(), " ");
  }

  private static KnownAccountProtocol convertAccountProtocolFromXmom(Protocol protocol) {
    if (protocol instanceof IMAP) {
      return KnownAccountProtocol.IMAP;
    } else if (protocol instanceof POP3) {
      return KnownAccountProtocol.POP3;
    } else if (protocol == null) {
      return KnownAccountProtocol.NONE;
    } else {
      throw new IllegalArgumentException("Unexpected protocol "+protocol);
    }
  }
  
  private static xact.mail.account.MailAccountData.Security convertSecurityFromXmom(Security security) {
    if (security instanceof SSL) {
      return xact.mail.account.MailAccountData.Security.SSL;
    } else if (security instanceof STARTTLS) {
      return xact.mail.account.MailAccountData.Security.STARTTLS;
    } else if (security == null) {
      return xact.mail.account.MailAccountData.Security.NONE;
    } else {
      throw new IllegalArgumentException("Unexpected security "+security);
    }
  }
  
  public static MailAccountParameter convertToXmom(MailAccountData mad) {
    MailAccountParameter.Builder builder = new MailAccountParameter().buildMailAccountParameter();
    builder.name(mad.getName());
    builder.host(mad.getHost());
    builder.user(mad.getUser());
    builder.password(mad.getPassword());
    
    builder.keyStore(mad.getKeyStore());
    builder.trustStore(mad.getTrustStore());
    
    AccountProtocolParameter.Builder accBuilder = new AccountProtocolParameter().buildAccountProtocolParameter();
    accBuilder.port(mad.getAccountPort());
    accBuilder.protocol(convertAccountProtocolToXmom(mad.getAccountProtocol()));
    accBuilder.security(convertSecurityToXmom(mad.getAccountSecurity()));
    builder.accountProtocol(accBuilder.instance());
    
    TransportProtocolParameter.Builder transBuilder = new TransportProtocolParameter().buildTransportProtocolParameter();
    transBuilder.port(mad.getTransportPort());
    transBuilder.security(convertSecurityToXmom(mad.getTransportSecurity()));
    builder.transportProtocol(transBuilder.instance());

    builder.additionalProperties(convertMailAccountPropertysToXmom(mad.getProperties()) );
    return builder.instance();
  }

  private static List<MailAccountProperty> convertMailAccountPropertysToXmom(
      List<MailAccountData.MailAccountProperty> properties) {
    if( properties == null || properties.isEmpty() ) {
      return new ArrayList<>(0);
    }
    List<MailAccountProperty> maps = new ArrayList<>();
    for( MailAccountData.MailAccountProperty map : properties ) {
      maps.add(new MailAccountProperty().buildMailAccountProperty().
          key(map.getKey()).
          value(map.getValue()).
          documentation(map.getDocumentation()).
          instance());
    }
    return maps;
  }

  private static Protocol convertAccountProtocolToXmom(AccountProtocol type) {
    if( type == null ) {
      return null;
    }
    if (type instanceof KnownAccountProtocol) {
      switch( (KnownAccountProtocol)type ) {
        case IMAP:
          return new IMAP();
        case POP3:
          return new POP3();
        case NONE:
          return null;
        default:
          throw new IllegalArgumentException("Unexpected Protocol "+type);
      }
    } else {
      return null;
    }
  }
  
  
  private static Security convertSecurityToXmom(xact.mail.account.MailAccountData.Security security) {
    if( security == null ) {
      return null;
    }
    switch(security) {
      case SSL:
        return new SSL();
      case STARTTLS:
        return new STARTTLS();
      case NONE:
        return null;
      default:
        throw new IllegalArgumentException("Unexpected Security "+security);
    }
  }
  
  
}
