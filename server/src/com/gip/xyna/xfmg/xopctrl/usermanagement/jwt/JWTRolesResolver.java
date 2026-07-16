/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package com.gip.xyna.xfmg.xopctrl.usermanagement.jwt;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xopctrl.usermanagement.DomainName;
import com.gip.xyna.xfmg.xopctrl.usermanagement.RolesResolver;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;


/**
 * RolesResolver implementation for JWT domains.
 * Resolves available login roles by executing a configured workflow with the JWT token.
 */
public class JWTRolesResolver implements RolesResolver {

  private final String rolesResolverOrdertype;
  private final RuntimeContext runtimeContext;
  private final String orderContextKey;

  public JWTRolesResolver(String rolesResolverOrdertype, RuntimeContext runtimeContext, String orderContextKey) {
    this.rolesResolverOrdertype = rolesResolverOrdertype;
    this.runtimeContext = runtimeContext;
    this.orderContextKey = orderContextKey;
  }

  @Override
  public Optional<String> getRolesResolverOrdertype() {
    return Optional.ofNullable(rolesResolverOrdertype);
  }

  @Override
  public RuntimeContext getRolesResolverRuntimeContext() {
    return runtimeContext;
  }

  @Override
  public String getRolesResolverOrderContextKey() {
    return orderContextKey;
  }

  @Override
  public List<String> resolveAvailableRoles(String domainName, String credential) {
    List<String> roleNames = new ArrayList<>();

    if (rolesResolverOrdertype == null || rolesResolverOrdertype.isEmpty() || runtimeContext == null || orderContextKey == null || orderContextKey.isEmpty() || credential == null || credential.isEmpty()) {
      return roleNames;
    }

    try {
      DomainName domainNameObj = new DomainName(domainName);
      DestinationKey destKey = new DestinationKey(rolesResolverOrdertype, runtimeContext);
      XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(destKey, domainNameObj);
      XynaOrderServerExtension xose = new XynaOrderServerExtension(xocp);
      xose.setNewOrderContext();
      xose.getOrderContext().set(orderContextKey, credential);

      XynaOrderServerExtension resultOrder =
          XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().startOrderSynchronous(xose);

      addRoleNames(roleNames, resultOrder.getOutputPayload());
    } catch (Exception e) {
      // Intentionally ignored: unresolved roles result in an empty list.
    }

    return roleNames;
  }

  private void addRoleNames(List<String> roleNames, GeneralXynaObject output) {
    if (output == null) {
      return;
    }
    if (output instanceof XynaObjectList<?>) {
      for (Object item : (XynaObjectList<?>) output) {
        addRoleName(roleNames, item);
      }
      return;
    }
    if (output instanceof Container) {
      Container container = (Container) output;
      for (int i = 0; i < container.size(); i++) {
        addRoleName(roleNames, container.get(i));
      }
      return;
    }
    addRoleName(roleNames, output);
  }

  private void addRoleName(List<String> roleNames, Object item) {
    if (!(item instanceof GeneralXynaObject)) {
      return;
    }
    try {
      Object nameField = ((GeneralXynaObject) item).get("name");
      if (nameField instanceof String) {
        String roleName = ((String) nameField).trim();
        if (!roleName.isEmpty()) {
          roleNames.add(roleName);
        }
      }
    } catch (InvalidObjectPathException e) {
      // Ignore items without a 'name' field.
    }
  }

}
