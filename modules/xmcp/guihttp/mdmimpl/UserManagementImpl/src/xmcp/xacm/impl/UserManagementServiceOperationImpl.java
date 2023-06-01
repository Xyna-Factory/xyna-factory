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
package xmcp.xacm.impl;


import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.exceptions.XFMG_PredefinedXynaObjectException;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;

import xmcp.tables.datatypes.TableColumn;
import xmcp.tables.datatypes.TableInfo;
import xmcp.xacm.UserManagementServiceOperation;
import xmcp.xacm.domainmanagement.datatypes.Domain;
import xmcp.xacm.rolemanagement.datatypes.RoleName;
import xmcp.xacm.usermanagement.datatypes.User;
import xmcp.xacm.usermanagement.datatypes.Username;
import xmcp.xacm.usermanagement.exceptions.DeleteUserException;
import xmcp.xacm.usermanagement.exceptions.GetRolesException;
import xmcp.xacm.usermanagement.exceptions.ListDomainsException;
import xmcp.xacm.usermanagement.exceptions.LoadUsersException;
import xmcp.zeta.TableHelper;


public class UserManagementServiceOperationImpl implements ExtendedDeploymentTask, UserManagementServiceOperation {
  
  private static final XynaFactoryManagement factoryManagement =  (XynaFactoryManagement) XynaFactory.getInstance().getFactoryManagement();
  
  private static final String LIST_ENTRY_KEY_USER = "user";
  private static final String LIST_ENTRY_KEY_ROLE = "role";
  private static final String LIST_ENTRY_KEY_LOCKED = "locked";
  private static final String LIST_ENTRY_KEY_DOMAINS = "domains";
  private static final String LIST_ENTRY_KEY_CREATION_DATE = "creationDate";
  
  @Override
  public List<? extends RoleName> getRoles() throws GetRolesException {
    try {
      Collection<Role> roles = factoryManagement.getRoles();
      return roles.stream()
          .map(r -> new RoleName(r.getName()))
          .sorted(Comparator.comparing(RoleName::getRoleName, String.CASE_INSENSITIVE_ORDER))
          .collect(Collectors.toList());
    } catch (PersistenceLayerException e) {
      throw new GetRolesException(e.getMessage(), e);
    }
  }

  public List<? extends User> getUsers(TableInfo tableInfo) throws LoadUsersException {
    TableHelper<User, TableInfo> tableHelper =
        TableHelper.<User, TableInfo> init(tableInfo)
            .limitConfig(TableInfo::getLimit)
            .sortConfig(ti -> {
              for (TableColumn tc : ti.getColumns()) {
                TableHelper.Sort sort = TableHelper.createSortIfValid(tc.getPath(), tc.getSort());
                if(sort != null)
                  return sort;
              }
              return null;
            })
            .filterConfig(ti -> ti.getColumns().stream()
                .filter(tableColumn -> !tableColumn.getDisableFilter() && tableColumn.getPath() != null && tableColumn.getFilter() != null && tableColumn.getFilter().length() > 0)
                .map(tc -> new TableHelper.Filter(tc.getPath(), tc.getFilter())).collect(Collectors.toList()))
            .addSelectFunction(LIST_ENTRY_KEY_USER, User::getUser)
            .addSelectFunction(LIST_ENTRY_KEY_ROLE, User::getRole)
            .addSelectFunction(LIST_ENTRY_KEY_LOCKED, User::getLocked)
            .addSelectFunction(LIST_ENTRY_KEY_DOMAINS, User::getDomains)
            .addSelectFunction(LIST_ENTRY_KEY_CREATION_DATE, User::getCreationDate);

    try {
      Collection<com.gip.xyna.xfmg.xopctrl.usermanagement.User> users = factoryManagement.getUser();
      List<User> result = users.stream().map(u -> {
        User user = new User();
        user.setCreationDate(u.getCreationDate());
        user.setDomains(u.getDomains());
        user.setLocked(u.isLocked());
        user.setRole(u.getRole());
        user.setUser(u.getName());
        return user;
      }).filter(tableHelper.filter()).collect(Collectors.toList());

      tableHelper.sort(result);
      return tableHelper.limit(result);
    } catch (PersistenceLayerException e) {
      throw new LoadUsersException(e.getMessage(), e);
    }
  }
  
  @Override
  public void deleteUser(Username username) throws DeleteUserException {
    try {
      com.gip.xyna.xfmg.xopctrl.usermanagement.User user = factoryManagement.getUser(username.getName());
      if(user == null) {
        throw new DeleteUserException("User not found");
      }
      factoryManagement.deleteUser(username.getName());
    } catch (PersistenceLayerException | XFMG_PredefinedXynaObjectException e) {
      throw new DeleteUserException(e.getMessage(), e);
    }
  }
  
  @Override
  public List<? extends Domain> getDomains() throws ListDomainsException {
    try {
      Collection<com.gip.xyna.xfmg.xopctrl.usermanagement.Domain> domains = factoryManagement.getDomains();
      return domains.stream().map(d -> {
        Domain r = new Domain();
        r.setDescription(d.getDescription());
        r.setDomainType(d.getDomainType());
        r.setMaxRetries(d.getMaxRetries());
        r.setName(d.getName());
        return r;
      }).collect(Collectors.toList());
    } catch (PersistenceLayerException e) {
      throw new ListDomainsException(e.getMessage(), e);
    }
  }
  
  public void onDeployment() throws XynaException {
    // nothing
  }

  public void onUndeployment() throws XynaException {
    // nothing
  }

  public Long getOnUnDeploymentTimeout() {
    return null;
  }

  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    return null;
  }

}
