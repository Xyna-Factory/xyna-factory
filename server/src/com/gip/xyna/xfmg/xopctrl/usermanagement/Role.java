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
package com.gip.xyna.xfmg.xopctrl.usermanagement;



import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gip.xyna.utils.StringUtils;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.Persistable.StorableProperty;



@Persistable(primaryKey = Role.COL_ID, tableName = Role.TABLENAME, tableProperties = StorableProperty.PROTECTED)
public class Role extends Storable<Role> {

  private static final long serialVersionUID = -4376849121863635477L;
  
  private static final String RIGHTS_SEPERATION_MARKER = ",";

  public final static String TABLENAME = "rolearchive";
  public final static String COL_ID = "id";
  public final static String COL_NAME = "name";
  public final static String COL_RIGHTS = "rights";
  public final static String COL_SCOPEDRIGHTS = "scopedrights";
  public final static String COL_DESCRIPTION = "description";
  public final static String COL_ALIAS = "alias";
  public final static String COL_DOMAIN = "domain";


  @Column(name = COL_ID, size = 100)
  private String id;
  @Column(name = COL_NAME, size = 50)
  private String name;
  @Column(name = COL_RIGHTS, size = 15000)
  private String rights;
  @Column(name = COL_SCOPEDRIGHTS, type=ColumnType.BLOBBED_JAVAOBJECT)
  private Set<String> scopedrights;
  @Column(name = COL_DESCRIPTION, size = 2000)
  private String description;
  @Column(name = COL_ALIAS, size = 50)
  private String alias;
  @Column(name = COL_DOMAIN, size = 50)
  private String domain;


  private transient Set<String> rightSet;
  
  /*
   * Internal Role:
   *   - has rights
   *   - has no alias
   *   - is mapped to a Domain that has a DomainType.local
   * External Role:
   *   - has no rights
   *   - has an alias to an internal role
   *   - is mapped to a Domain that has a DomainType != DomainType.local
   */


  public Role() {
    //für storable benötigt
  }


  Role(String fullId) { //vom UserManagement verwendet
    this();
    this.id = fullId;
  }


  public Role(String name, String domain) {
    this(new StringBuilder().append(name).append(domain).toString());
    this.name = name;
    this.domain = domain;
  }


  public void setRights(List<String> rightList) {
    rights = StringUtils.javaListToSeperatedList(rightList, RIGHTS_SEPERATION_MARKER);
    rightSet = null;
  }
  
  public Set<String> getRightsAsSet() {
    if( rightSet == null ) {
      rightSet = new HashSet<String>(getRightsAsList());
    }
    return rightSet;
  }
  
  public List<String> getRightsAsList() {
    if (rights == null || rights.equals("")) {
      return Collections.emptyList();
    }
    String[] rightArray = rights.split(",");
    return Arrays.asList(rightArray);
  }
  

  public String getRights() {
    return rights;
  }


  public boolean grantRight(String right) {
    String trimmedRight = right.trim();
    if( getRightsAsSet().contains(trimmedRight) ) {
      return false;
    } else {
      getRightsAsSet().add(right);
      rights = StringUtils.addToSeperatedList(rights, trimmedRight, RIGHTS_SEPERATION_MARKER, false);
      return true;
    }
  }

  public boolean revokeRight(String right) {
    String trimmedRight = right.trim();
    if( !getRightsAsSet().contains(trimmedRight) ) {
      return false;
    } else {
      getRightsAsSet().remove(right);
      rights = StringUtils.removeFromSeperatedList(rights, trimmedRight, RIGHTS_SEPERATION_MARKER, true);
      return true;
    }
  }

  public boolean hasRight(String right) {
    return getRightsAsSet().contains(right.trim());
  }

  public boolean hasRight(Rights right) {
    return getRightsAsSet().contains(right.name());
  }
  
  public void setScopedRights(Set<String> scopedrights) {
    this.scopedrights = scopedrights;
  }
  
  
  public Set<String> getScopedRights() {
    if (scopedrights == null) {
      return Collections.emptySet();
    } else {
      return scopedrights;
    }
  }
  
  
  public boolean grantScopedRight(String right) {
    if (scopedrights == null) {
      scopedrights = new HashSet<String>();
    }
    return scopedrights.add(right);
  }


  public boolean revokeScopedRight(String right) {
    if (scopedrights == null) {
      return false;
    }
    boolean success = scopedrights.remove(right);
    if (success && scopedrights.size() <= 0) {
      scopedrights = null;
    }
    return success;
  }


  public String getId() {
    return id;
  }


  public String getName() {
    return name;
  }


  public String getDescription() {
    return description;
  }


  public void setDescription(String description) {
    this.description = description;
  }


  public String getAlias() {
    return alias;
  }


  public void setAlias(String alias) {
    this.alias = alias;
  }


  public String getDomain() {
    return domain;
  }


  @Override
  public Object getPrimaryKey() {
    return id;
  }
  
  
  private void fillByResultSet(ResultSet rs) throws SQLException {
    this.name = rs.getString(COL_NAME);
    this.rights = rs.getString(COL_RIGHTS);
    this.domain = rs.getString(COL_DOMAIN);
    this.id = rs.getString(COL_ID);
    this.description = rs.getString(COL_DESCRIPTION);
    this.alias = rs.getString(COL_ALIAS);
    this.scopedrights = (Set<String>) readBlobbedJavaObjectFromResultSet(rs, COL_SCOPEDRIGHTS);
  }


  private static ResultSetReader<Role> reader = new ResultSetReader<Role>() {

    public Role read(ResultSet rs) throws SQLException {
      Role r = new Role();
      r.fillByResultSet(rs);
      return r;
    }

  };


  @Override
  public ResultSetReader<? extends Role> getReader() {
    return reader;
  }


  @Override
  public <U extends Role> void setAllFieldsFromData(U data) {
    Role cast = data;
    name = cast.name;
    rights = cast.rights;
    domain = cast.domain;
    id = cast.id;
    description = cast.description;
    alias = cast.alias;
    scopedrights = cast.scopedrights;
  }

}
