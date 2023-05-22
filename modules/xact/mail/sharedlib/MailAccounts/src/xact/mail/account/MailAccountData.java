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
package xact.mail.account;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import xact.mail.Receiver;
import xact.mail.account.MailAccountData.KnownTransportProtocol;
import xact.mail.internal.POP3Impl;
import xact.mail.internal.IMAPImpl;
import xact.mail.internal.MailServer;

public class MailAccountData implements Serializable {

  private static final long serialVersionUID = 1L;
  
  
  public interface Protocol {
    
    public String getProtocolIdentifier();
    
  }
  
  public interface TransportProtocol extends Protocol { };
  
  public interface AccountProtocol extends Protocol { 
    
    public Receiver getReceiver(MailServer mailServer);
    
  };
  
  public enum KnownTransportProtocol implements TransportProtocol {
    NONE("none"),
    SMTP("smtp");
    
    private final String protocolIdentifier; // used for javax.mail
    
    private KnownTransportProtocol(String protocolIdentifier) {
      this.protocolIdentifier = protocolIdentifier;
    }
    
    public String getProtocolIdentifier() {
      return protocolIdentifier;
    }
    
    public static KnownTransportProtocol fromString(String value) {
      if (value != null &&
          !value.isEmpty()) {
        for (KnownTransportProtocol accp : values()) {
          if (accp.protocolIdentifier.equalsIgnoreCase(value)) {
            return accp;
          }
        }
      }
      return NONE;
    }
  }
  
  public enum KnownAccountProtocol implements AccountProtocol {
    NONE("none") {
      public Receiver getReceiver(MailServer mailServer) {
        throw new UnsupportedOperationException("AccountProtocol not specified!");
      }
    },
    IMAP("imap") {
      public Receiver getReceiver(MailServer mailServer) {
        return new IMAPImpl(mailServer);
      }
    },
    POP3("pop3") {
      public Receiver getReceiver(MailServer mailServer) {
        return new POP3Impl(mailServer);
      }
    };

    private final String protocolIdentifier; // used for javax.mail
    
    private KnownAccountProtocol(String protocolIdentifier) {
      this.protocolIdentifier = protocolIdentifier;
    }
    
    
    public String getProtocolIdentifier() {
      return protocolIdentifier;
    }

    public static KnownAccountProtocol fromString(String value) {
      if (value != null &&
          !value.isEmpty()) {
        for (KnownAccountProtocol accp : values()) {
          if (accp.protocolIdentifier.equalsIgnoreCase(value)) {
            return accp;
          }
        }
      }
      return NONE;
    }

    public abstract Receiver getReceiver(MailServer mailServer);
  }
  
  
  public enum Security {
    NONE("none"),
    SSL("SSL"),
    STARTTLS("STARTTLS");
    
    private final String name;
    
    private Security(String name) {
      this.name = name;
    }
    
    public String getName() {
      return name;
    }

    public static Security fromString(String value) {
      if (value != null &&
          !value.isEmpty()) {
        for (Security security : values()) {
          if (security.name.equalsIgnoreCase(value)) {
            return security;
          }
        }
      }
      return NONE;
    }
  }
  
  private String name;
  private String host;
  private String address;
  private String user;
  private String password;
  private AccountProtocol accountProtocol;
  private TransportProtocol transportProtocol;
  private String keyStore;
  private String trustStore;
  private int accountPort;
  private int transportPort;
  private Security accountSecurity;
  private Security transportSecurity;
  private List<MailAccountProperty> properties; 
  
  private MailAccountData() {
  }
  
  private MailAccountData(MailAccountData mad, Collection<MailAccountProperty> maps) {
    this.name = mad.name;
    this.host = mad.host;
    this.address = mad.address;
    this.user = mad.user;
    this.password = mad.password;
    this.accountProtocol = mad.accountProtocol;
    this.transportProtocol = mad.transportProtocol;
    this.keyStore = mad.keyStore;
    this.trustStore = mad.trustStore;
    this.accountPort = mad.accountPort;
    this.transportPort = mad.transportPort;
    this.accountSecurity = mad.accountSecurity;
    this.transportSecurity = mad.transportSecurity;
    this.properties = Collections.unmodifiableList(new ArrayList<>(maps));
  }
  
  public String getName() {
    return name;
  }
  public String getHost() {
    return host;
  }
  public String getAddress() {
    return address;
  }
  public String getUser() {
    return user;
  }
  public String getPassword() {
    return password;
  }
  public AccountProtocol getAccountProtocol() {
    return accountProtocol;
  }
  public TransportProtocol getTransportProtocol() {
    return transportProtocol;
  }
  public String getKeyStore() {
    return keyStore;
  }
  public String getTrustStore() {
    return trustStore;
  }
  public int getAccountPort() {
    return accountPort;
  }
  public int getTransportPort() {
    return transportPort;
  }
  public Security getAccountSecurity() {
    return accountSecurity;
  }
  public Security getTransportSecurity() {
    return transportSecurity;
  }
  public List<MailAccountProperty> getProperties() {
    return properties;
  }
  
  public static class Builder {
    private final MailAccountData instance;
    private final TreeMap<String,MailAccountProperty> properties;
    
    public Builder() {
      this.instance = new MailAccountData();
      this.properties = new TreeMap<>();
    }
    public Builder(MailAccountData mad) {
      this.instance = mad;
      this.properties = new TreeMap<>();
      for( MailAccountProperty map : mad.getProperties() ) {
        properties.put( map.getKey(), map );
      }
    }

    public MailAccountData build() {
      if (instance.accountProtocol == null) {
        accountProtocol(KnownAccountProtocol.NONE);
      }
      if (instance.transportProtocol == null) {
        transportProtocol(KnownTransportProtocol.NONE);
      }
      if (instance.accountSecurity == null) {
        accountSecurity(Security.NONE);
      }
      if (instance.transportSecurity == null) {
        transportSecurity(Security.NONE);
      }
      deriveDefaultAccountPort();
      deriveDefaultTransportPort();
      return new MailAccountData(instance, properties.values() );
    }
    
    private void deriveDefaultTransportPort() {
      if (instance.transportPort == 0 &&
          instance.transportProtocol instanceof KnownTransportProtocol) {
        switch (instance.transportSecurity) {
          case NONE :
            transportPort(25);
            break;
          case SSL:
          case STARTTLS :
            transportPort(465);
            break;
          default :
            break;
        }
      } else if (instance.transportPort < 0) {
        transportProtocol(KnownTransportProtocol.NONE);
      } else {
        transportProtocol(KnownTransportProtocol.SMTP);
      }
    }
    private void deriveDefaultAccountPort() {
      if (instance.accountPort == 0 &&
          instance.accountProtocol instanceof KnownAccountProtocol) {
        switch ((KnownAccountProtocol)instance.accountProtocol) {
          case IMAP :
            switch (instance.accountSecurity) {
              case NONE :
              case STARTTLS :
                accountPort(143);
                break;
              case SSL:
                accountPort(993);
                break;
              default :
                break;
            }
            break;
          case POP3 :
            switch (instance.accountSecurity) {
              case NONE :
              case STARTTLS :
                accountPort(110);
                break;
              case SSL:
                accountPort(995);
                break;
              default :
                break;
            }
            break;
          default :
            break;
        }
      }
    }
    
    public Builder name( String name) {
      instance.name = name;
      return this;
    }
    public Builder host( String host) {
      instance.host = host;
      return this;
    }
    public Builder address( String address) {
      instance.address = address;
      return this;
    }
    public Builder user( String user) {
      instance.user = user;
      return this;
    }
    public Builder password( String password) {
      instance.password = password;
      return this;
    }
    public Builder accountProtocol( AccountProtocol accountProtocol) {
      instance.accountProtocol = accountProtocol;
      return this;
    }
    public Builder transportProtocol( TransportProtocol transportProtocol) {
      instance.transportProtocol = transportProtocol;
      return this;
    }
    public Builder keyStore( String keyStore) {
      instance.keyStore = keyStore;
      return this;
    }
    public Builder trustStore( String trustStore) {
      instance.trustStore = trustStore;
      return this;
    }
    public Builder accountPort( int accountPort) {
      instance.accountPort = accountPort;
      return this;
    }
    public Builder transportPort( int transportPort) {
      instance.transportPort = transportPort;
      return this;
    }
    public Builder accountSecurity( Security accountSecurity) {
      instance.accountSecurity = accountSecurity;
      return this;
    }
    public Builder transportSecurity( Security transportSecurity) {
      instance.transportSecurity = transportSecurity;
      return this;
    }
    public Builder addProperty(MailAccountProperty map) {
      properties.put( map.getKey(), map);
      return this;
    }
    public Builder addProperty(String key, String value, String documentation ) {
      properties.put( key, new MailAccountProperty(key, value, documentation) );
      return this;
    }
    public Builder removeProperty(String key) {
      properties.remove(key);
      return this;
    }
    
    public Builder properties(List<MailAccountProperty> maps) {
      for( MailAccountProperty map : maps ) {
        properties.put( map.getKey(), map);
      }
      return this;
    }

  }
  
  public static class MailAccountProperty implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    public MailAccountProperty(String key, String value, String documentation) {
      this.key = key;
      this.value = value;
      this.documentation = documentation;
    }
    private String key;
    private String value;
    private String documentation;
    public String getKey() {
      return key;
    }
    public String getValue() {
      return value;
    }
    public String getDocumentation() {
      return documentation;
    }
  }

  
}
