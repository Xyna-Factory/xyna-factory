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

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.xnwh.exceptions.XNWH_EncryptionException;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.securestorage.SecureStorage;

import xact.mail.account.MailAccountData.Builder;
import xact.mail.account.MailAccountData.KnownAccountProtocol;
import xact.mail.account.MailAccountData.KnownTransportProtocol;
import xact.mail.account.MailAccountData.Security;

@Persistable(primaryKey = MailAccountDataStorable.COL_NAME, tableName = MailAccountDataStorable.TABLE_NAME)
public class MailAccountDataStorable extends Storable<MailAccountDataStorable> {
  
  public static final String TABLE_NAME = "mailaccounts";
  
  public static final String COL_NAME = "name";
  public static final String COL_HOST = "host";
  public static final String COL_ADDRESS = "address";
  public static final String COL_USER = "user";
  public static final String COL_USER_CRED = "password";
  public static final String COL_TYPE = "type";
  public static final String COL_KEYSTORE = "keyStore";
  public static final String COL_TRUSTSTORE = "truststore";
  public static final String COL_TRANSPORT_PORT = "transportport";
  public static final String COL_ACCOUNT_PORT = "accountport";
  public static final String COL_TRANSPORT_SECURITY = "transportsecurity";
  public static final String COL_ACCOUNT_SECURITY = "accountsecurity";

  private static final long serialVersionUID = -1L;


  @Column(name = COL_NAME, size = 100)
  private String name;
  @Column(name = COL_HOST)
  private String host;
  @Column(name = COL_ADDRESS)
  private String address;
  @Column(name = COL_USER)
  private String user;
  @Column(name = COL_USER_CRED)
  private String password;
  @Column(name = COL_TYPE)
  private String type;
  @Column(name = COL_KEYSTORE, size = 100)
  private String keyStore;
  @Column(name = COL_TRUSTSTORE, size = 100)
  private String truststore;
  @Column(name = COL_TRANSPORT_PORT)
  private int transportport;
  @Column(name = COL_ACCOUNT_PORT)
  private int accountport;
  @Column(name = COL_TRANSPORT_SECURITY)
  private String transportsecurity;
  @Column(name = COL_ACCOUNT_SECURITY)
  private String accountsecurity;
  
  
  public MailAccountDataStorable() {
    super();
  }

  public MailAccountDataStorable(String name) {
    this.name = name;
  }

  @Override
  public String getPrimaryKey() {
    return name;
  }

  private static MailAccountDataStorableReader reader = new MailAccountDataStorableReader();
  
  @Override
  public ResultSetReader<? extends MailAccountDataStorable> getReader() {
    return reader;
  }

  private static class MailAccountDataStorableReader implements ResultSetReader<MailAccountDataStorable> {

    public MailAccountDataStorable read(ResultSet rs) throws SQLException {
      MailAccountDataStorable mads = new MailAccountDataStorable();
      mads.name = rs.getString(COL_NAME);
      mads.host = rs.getString(COL_HOST);
      mads.address = rs.getString(COL_ADDRESS);
      mads.user = rs.getString(COL_USER);
      mads.password = rs.getString(COL_USER_CRED);
      mads.type = rs.getString(COL_TYPE);
      mads.keyStore = rs.getString(COL_KEYSTORE);
      mads.truststore = rs.getString(COL_TRUSTSTORE);
      mads.transportport = rs.getInt(COL_TRANSPORT_PORT);
      mads.accountport = rs.getInt(COL_ACCOUNT_PORT);
      mads.transportsecurity = rs.getString(COL_TRANSPORT_SECURITY);
      mads.accountsecurity = rs.getString(COL_ACCOUNT_SECURITY);
      return mads;
    }
    
  }
  
  @Override
  public <U extends MailAccountDataStorable> void setAllFieldsFromData(U data) {
    MailAccountDataStorable cast = data;
    name = cast.name;
    host = cast.host;
    address = cast.address;
    user = cast.user;
    password = cast.password;
    type = cast.type;
    keyStore = cast.keyStore;
    truststore = cast.truststore;
    transportport = cast.transportport;
    accountport = cast.accountport;
    transportsecurity = cast.transportsecurity;
    accountsecurity = cast.accountsecurity;
  }

  public void fill(Builder builder) {
    builder.name(name);
    builder.host(host);
    builder.address(address);
    builder.user(user);
    builder.password( getPassword() );
    builder.accountProtocol(KnownAccountProtocol.fromString(type));
    builder.transportProtocol(accountport >= 0 ? KnownTransportProtocol.SMTP : KnownTransportProtocol.NONE);
    builder.keyStore(keyStore);
    builder.trustStore(truststore);
    builder.accountPort(accountport);
    builder.transportPort(transportport);
    builder.transportSecurity(Security.fromString(transportsecurity));
    builder.accountSecurity(Security.fromString(accountsecurity));
  }

  public String getName() {
    return name;
  }

  public static MailAccountDataStorable of(MailAccountData mad) {
    MailAccountDataStorable mads = new MailAccountDataStorable();
    mads.name = mad.getName();
    mads.host = mad.getHost();
    mads.address = mad.getAddress();
    mads.user = mad.getUser();
    mads.setPassword(mad.getPassword());
    mads.type = mad.getAccountProtocol().getProtocolIdentifier();
    mads.keyStore = mad.getKeyStore();
    mads.truststore = mad.getKeyStore();
    mads.transportport = mad.getTransportPort();
    mads.accountport = mad.getAccountPort();
    mads.transportsecurity = mad.getTransportSecurity().name();
    mads.accountsecurity = mad.getAccountSecurity().name();
    return mads;
  }

  private void setPassword(String password) {
    if( password == null ) {
      this.password = null;
      return;
    }
    try {
      this.password = SecureStorage.staticEncrypt(name+user, password);
    } catch (XNWH_EncryptionException e) {
      logger.warn("Failed to encrypt password", e);
    }
  }
  private String getPassword() {
    if( password == null ) {
      return null;
    }
    try {
      return SecureStorage.staticDecrypt(name+user, password);
    } catch (XNWH_EncryptionException e) {
      logger.warn("Failed to decrypt password", e);
      return null;
    }
  }
}
