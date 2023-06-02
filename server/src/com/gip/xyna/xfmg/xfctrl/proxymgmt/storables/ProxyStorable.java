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
package com.gip.xyna.xfmg.xfctrl.proxymgmt.storables;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.gip.xyna.utils.collections.CSVStringList;
import com.gip.xyna.xfmg.xfctrl.RMIManagement.RMIParameter;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.ProxyRole;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.ProxyRole.GenerationData;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.ProxyRole.ProxyRoleBuilder;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;

@Persistable(primaryKey = Role.COL_NAME, tableName = ProxyStorable.TABLENAME)
public class ProxyStorable extends Storable<ProxyStorable> {

  
  private static final long serialVersionUID = 1L;
  
  public final static String TABLENAME = "rmiproxy";
  
  public final static String COL_NAME = "name";
  public final static String COL_ROLES = "roles";
  public final static String COL_RIGHTS = "rights";
  public final static String COL_WITHOUT_PUBLIC = "withoutPublic";
  public final static String COL_WITH_DEPRECATED = "withDeprecated";
  public final static String COL_RMI_NAME = "rmiName";
  public final static String COL_REGISTRY_HOST = "registryHost";
  public final static String COL_REGISTRY_PORT = "registryPort";
  public final static String COL_COMMUNICATION_PORT = "communicationPort";
  public final static String COL_DESCRIPTION = "description";
  
  @Column(name = COL_NAME, size = 50)
  private String name;
  @Column(name = COL_ROLES, size = 4000)
  private CSVStringList roles;
  @Column(name = COL_RIGHTS, size = 4000)
  private CSVStringList rights;
  @Column(name = COL_WITHOUT_PUBLIC)
  private boolean withoutPublic;
  @Column(name = COL_WITH_DEPRECATED)
  private boolean withDeprecated;
  @Column(name = COL_COMMUNICATION_PORT)
  private int communicationPort;
  @Column(name = COL_REGISTRY_PORT)
  private int registryPort;
  @Column(name = COL_DESCRIPTION, size = 4000)
  private String description;
  @Column(name = COL_REGISTRY_HOST, size = 60)
  private String registryHost;
  @Column(name = COL_RMI_NAME, size = 60)
  private String rmiName;
    
  public ProxyStorable() {
  }
  
  public ProxyStorable fillFromProxyRole(ProxyRole proxyRole) {
    this.name = proxyRole.getName();
    GenerationData genData = proxyRole.getGenerationData();
    this.roles = new CSVStringList(genData.getRoles());
    this.rights = new CSVStringList(genData.getAdditional());
    
    this.withoutPublic = ! genData.acceptPublic();
    this.withDeprecated = genData.deprecated();
    return this;
  }

  public ProxyStorable fillFromRMIParameter(RMIParameter parameter) {
    this.rmiName = parameter.getRmiName();
    this.registryHost = parameter.getRegistryHost();
    this.registryPort = parameter.getRegistryPort();
    this.communicationPort = parameter.getCommunicationPort();
    return this;
  }
  
  public ProxyRole toProxyRole(UserManagement userManagement) throws PersistenceLayerException {
    ProxyRoleBuilder prb = ProxyRole.newProxyRole(name);
    for( String role : roles ) {
      prb.addRole(userManagement.getRole(role));
    }
    for( String right : rights ) {
      prb.addRight(right);
    }
    if( withoutPublic ) {
      prb.withoutPublic();
    }
    if( withDeprecated ) {
      prb.withDeprecated();
    }
    return prb.buildProxyRole(userManagement);
  }
  
  public RMIParameter toRMIParameter() {
    return new RMIParameter(rmiName,registryHost,registryPort,communicationPort);
  }
  

  @Override
  public ResultSetReader<? extends ProxyStorable> getReader() {
    return reader;
  }


  private static ResultSetReader<ProxyStorable> reader = new ResultSetReader<ProxyStorable>() {

    public ProxyStorable read(ResultSet rs) throws SQLException {
      ProxyStorable proxy = new ProxyStorable();
      proxy.name = rs.getString(COL_NAME);
      proxy.roles = CSVStringList.valueOf(rs.getString(COL_ROLES));
      proxy.rights = CSVStringList.valueOf(rs.getString(COL_RIGHTS));
      proxy.withoutPublic = rs.getBoolean(COL_WITHOUT_PUBLIC);
      proxy.withDeprecated = rs.getBoolean(COL_WITH_DEPRECATED);
      proxy.rmiName = rs.getString(COL_RMI_NAME);
      proxy.registryHost = rs.getString(COL_REGISTRY_HOST);
      proxy.registryPort = rs.getInt(COL_REGISTRY_PORT);
      proxy.communicationPort = rs.getInt(COL_COMMUNICATION_PORT);
      proxy.description = rs.getString(COL_DESCRIPTION);
      return proxy;
    }

  };

  @Override
  public String getPrimaryKey() {
    return name;
  }

  @Override
  public <U extends ProxyStorable> void setAllFieldsFromData(U data) {
    ProxyStorable proxy = (ProxyStorable)data;
    this.name = proxy.name;
    this.roles = proxy.roles;
    this.rights = proxy.rights;
    this.withoutPublic = proxy.withoutPublic;
    this.withDeprecated = proxy.withDeprecated;
    this.rmiName = proxy.rmiName;
    this.registryHost = proxy.registryHost;
    this.registryPort = proxy.registryPort;
    this.communicationPort = proxy.communicationPort;
    this.description = proxy.description;
  }

  public String getName() {
    return name;
  }
  
  public CSVStringList getRoles() {
    return roles;
  }
  
  public CSVStringList getRights() {
    return rights;
  }
  
  public boolean isWithDeprecated() {
    return withDeprecated;
  }
  
  public boolean isWithoutPublic() {
    return withoutPublic;
  }

  public String getDescription() {
    return description;
  }
  public void setDescription(String description) {
    this.description = description;
  }

  
  
}
