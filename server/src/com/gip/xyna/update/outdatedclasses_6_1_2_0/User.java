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
package com.gip.xyna.update.outdatedclasses_6_1_2_0;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.exceptions.XFMG_UserAuthenticationFailedException;
import com.gip.xyna.xfmg.exceptions.XFMG_UserIsLockedException;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.passwordcreation.PasswordCreationUtils;
import com.gip.xyna.xfmg.xopctrl.usermanagement.passwordcreation.PasswordCreationUtils.EncryptionPhase;
import com.gip.xyna.xfmg.xopctrl.usermanagement.selectuser.UserColumns;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.Persistable.StorableProperty;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



@Persistable(primaryKey = User.COL_NAME, tableName = User.TABLENAME, tableProperties = {StorableProperty.PROTECTED})
public class User extends Storable<User> {

  public static final String TABLENAME = "userarchive";  
  public static final String COL_NAME = "name";
  public static final String COL_ROLE = "role";
  public static final String COL_PASSWORD = "password";
  public static final String COL_CREATIONDATE = "creationDate";
  public static final String COL_LOCKEDSTATE = "locked";
  public static final String COL_DOMAINS = "domains";
  public static final String COL_FAILED_LOGINS = "failedLogins";
  

  private static final long serialVersionUID = -7301378884111678454L;

  @Column(name = COL_NAME, size = 50)
  private String name;
  @Column(name = COL_ROLE, size = 50)
  private String role;
  @Column(name = COL_PASSWORD, size = 100)
  private String passwordHash;
  @Column(name = COL_CREATIONDATE)
  private long creationDate;
  @Column(name = COL_LOCKEDSTATE)
  private boolean locked;
  @Column(name = COL_DOMAINS, size = 200)
  private String domains;
  @Column(name = COL_FAILED_LOGINS)
  private int failedLogins = 0; //those have to be persisted now because we don't have a HashMap anymore, we would be fine as long as we're using Memory as default though

  public User() {
    //f�r storable
  }
  
  
  User(String name) {    //f�r usermanagement
    this();
    this.name = name;
  }
  
  
  public User(String name, String role, String password, boolean isPassHashed) {
    this( name, role, password, isPassHashed, null);    
  }
  
  
  public User(String name, String role, String password, boolean isPassHashed, List<String> domains) {
    this(name);    
    this.role = role;
    this.creationDate = System.currentTimeMillis();
    setPassword(password, isPassHashed);
    this.locked = false;
    this.failedLogins = 0;
    if (domains == null || domains.size() == 0) {
      this.domains = UserManagement.PREDEFINED_LOCALDOMAIN_NAME;
    } else {
      this.setDomains(domains);
    }
  }


  public boolean checkPassword(String password, boolean isPasswordHashed) throws XFMG_UserAuthenticationFailedException,
                  XFMG_UserIsLockedException {
    if (locked) {
      throw new XFMG_UserIsLockedException(name);
    }
    if (!isPasswordHashed) {
      password = generateHash(password, EncryptionPhase.LOGIN);
    }
    
    boolean grantAccess = PasswordCreationUtils.checkPassword(password, passwordHash, EncryptionPhase.PERSISTENCE);
    
    if (grantAccess) {
      failedLogins = 0;
      return true;
    } else {
      int allowedTries = getAllowedLoginTries();
      if (allowedTries < 0) {
        throw new XFMG_UserAuthenticationFailedException(name);
      }
      failedLogins++;
      if (failedLogins >= allowedTries) {
        this.locked = true;
        //"Wrong password, user '" + getId() + "' is now locked");
        throw new XFMG_UserAuthenticationFailedException(name);
      } else {
        //"Wrong password: " + (getAllowedLoginTries() - failedLogins) + " tries remaining");
        throw new XFMG_UserAuthenticationFailedException(name);
      }
    }
  }


  public boolean changePassword(String oldPassword, String newPassword, boolean isPasswordHashed, boolean isNewPasswordHashed) throws XFMG_UserAuthenticationFailedException,
                  XFMG_UserIsLockedException {
    if (!locked && checkPassword(oldPassword, isPasswordHashed)) {
      setPassword(newPassword, isNewPasswordHashed);
      return true;
    } else {
      return false;
    }
  }
  
  
  public String getName() {
    return name;
  }

  
  public String getRole() {
    return role;
  }


  public void setRole(String role) {
    this.role = role;
  }


  public boolean isLocked() {
    return locked;
  }


  public void setLocked(boolean locked) {
    if (!locked) {
      this.failedLogins = 0;
    }
    this.locked = locked;
  }
  
  
  public long getCreationDate() {
    return creationDate;
  }


  // A password reset will only work on blocked Users
  public boolean resetPassword(String password) {
    if (locked) {
      setPassword(password, false);
      this.locked = false;
      this.failedLogins = 0;
      return true;
    } else {
      return false;
    }
  }


  public void setPassword(String password, boolean isPassHashed) {
    if (!isPassHashed) {
      password = generateHash(password, EncryptionPhase.LOGIN);
    }
    this.passwordHash = generateHash(password, EncryptionPhase.PERSISTENCE);
  }


  public void clearPassword() {
    this.passwordHash = null;
  }

  
  public String getDomains() {
    return domains;
  }
  
  
  public List<String> getDomainList() {
    if (domains == null || domains.equals("")) {
      return Collections.emptyList();
    }
    String[] domainArray = domains.split(",");
    return Arrays.asList(domainArray);
  }
  
  
  public void setDomains(String domains) { 
    this.domains = domains;
  }
  
  
  public void setDomains(List<String> domainList) { 
    domains = domainListToString(domainList);
  }
  
  public int getFailedLogins() {
    return failedLogins;
  }
  
  
  private final String domainListToString(List<String> domainList) {
    StringBuilder domainBuilder = new StringBuilder();    
    for (int i=0; i<domainList.size(); i++) {
      domainBuilder.append(domainList.get(i));
      if (i+1 < domainList.size()) {
        domainBuilder.append(",");
      }      
    }
    return domainBuilder.toString();
  }
  

  @Override
  public Object getPrimaryKey() {
    return name;
  }


  public static ResultSetReader<User> reader = new ResultSetReader<User>() {

    public User read(ResultSet rs) throws SQLException {
      User u = new User();
      u.name = rs.getString(COL_NAME);
      u.role = rs.getString(COL_ROLE);
      u.passwordHash = rs.getString(COL_PASSWORD);
      u.creationDate = rs.getLong(COL_CREATIONDATE);
      u.locked = rs.getBoolean(COL_LOCKEDSTATE);
      u.failedLogins = rs.getInt(COL_FAILED_LOGINS);
      u.domains = rs.getString(COL_DOMAINS);
      return u;
    }

  };


  @Override
  public ResultSetReader<? extends User> getReader() {
    return reader;
  }


  @Override
  public <U extends User> void setAllFieldsFromData(U data) {
    User cast = data;
    name = cast.name;
    role = cast.role;
    passwordHash = cast.passwordHash;
    creationDate = cast.creationDate;
    locked = cast.locked;
    failedLogins = cast.failedLogins;
    domains = cast.domains;
  }


  public static String generateHash(String password, EncryptionPhase phase) {
    return PasswordCreationUtils.generatePassword(password, phase);
  }


  private int getAllowedLoginTries() {
    String allowedTries = XynaFactory.getInstance().getFactoryManagement().getProperty(XynaProperty.PROPERTYNAME_ALLOWED_ENTRIES);
    if (allowedTries != null) {
      try {
        return Integer.parseInt(allowedTries);
      } catch (Exception e) {
        return -1;
      }
    } else {
      return -1;
    }
  }


  public String getPassword() {
    return passwordHash;
  }
  
  
  public static class DynamicUserReader implements ResultSetReader<User> {

    private List<UserColumns> selectedCols;

    public DynamicUserReader(List<UserColumns> selected) {
      selectedCols = selected;
    }

    public User read(ResultSet rs) throws SQLException {
      User user = new User();
      if (selectedCols.contains(UserColumns.name)) {
        user.name = rs.getString(COL_NAME);
      }
      if (selectedCols.contains(UserColumns.creationDate)) {
        user.creationDate = rs.getLong(COL_CREATIONDATE);
      }
      if (selectedCols.contains(UserColumns.locked)) {
        user.locked = rs.getBoolean(COL_LOCKEDSTATE);
      }
      if (selectedCols.contains(UserColumns.role)) {
        user.role = rs.getString(COL_ROLE);
      }
      if (selectedCols.contains(UserColumns.password)) {
        user.passwordHash = rs.getString(COL_PASSWORD);
      }
      if (selectedCols.contains(UserColumns.domains)) {
        user.domains = rs.getString(COL_DOMAINS);
      }
      if (selectedCols.contains(UserColumns.failedLogins)) {
        user.failedLogins = rs.getInt(COL_FAILED_LOGINS);
      }
   
      return user;
    }
  }

}
