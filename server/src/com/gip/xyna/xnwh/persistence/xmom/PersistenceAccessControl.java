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
package com.gip.xyna.xnwh.persistence.xmom;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.exceptions.XFMG_ACCESS_VIOLATION;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.ScopedRightCache;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableColumnInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.VarDefinitionSite;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.XMOMStorableStructureInformation;
import com.gip.xyna.xprc.XynaOrderServerExtension;


public class PersistenceAccessControl {
  
  static interface PersistenceAccessContext {
    public void checkAccess(AccessType type, StorableColumnInformation column);
    public void checkAccess(AccessType type, Collection<StorableColumnInformation> columns);
    public void checkAccess(AccessType type, XMOMStorableStructureInformation storable);
  }
  
  
  public static class ScopedRightBasedAccessContext implements PersistenceAccessContext {
  
    private final ScopedRightCache rightCache;
  
    ScopedRightBasedAccessContext(ScopedRightCache rightCache) {
      this.rightCache = rightCache;      
    }

    private boolean hasAccess(AccessType type, String storableType, String variable) {
      String[] scopeParts = new String[3];
      scopeParts[0] = type.getStringRepresentation();
      scopeParts[1] = storableType;
      scopeParts[2] = variable;
      return rightCache.hasRight(XMOMPersistenceManagement.PERSISTENCE_RIGHT_SCOPE_KEY, scopeParts);
    }
    
    private boolean hasAccess(AccessType type, String storableType) {
      return hasAccess(type, storableType, "*"); 
    }
    
    public void checkAccess(AccessType type, StorableColumnInformation column) {
      if (!hasAccess(type, column)) {
        buildAndThrow(type, column.getParentStorableInfo(), column);
      }
    }
    
    
    private boolean hasAccess(AccessType type, StorableColumnInformation column) {
      Pair<XMOMStorableStructureInformation, String> parentAndPath = getFirstXMOMStorableParentAndPath(column);
      return hasAccess(type, parentAndPath.getFirst().getFqXmlName(), parentAndPath.getSecond());
    }

    public void checkAccess(AccessType type, Collection<StorableColumnInformation> columns) {
      Set<XMOMStorableStructureInformation> rootStorables = new HashSet<XMOMStorableStructureInformation>();
      for (StorableColumnInformation column : columns) {
        rootStorables.add(getFirstXMOMStorableParent(column));
      }
      boolean fullAccessToAllParticipatingTypes = true;
      for (XMOMStorableStructureInformation rootStorable : rootStorables) {
        if (!hasAccess(type, rootStorable.getFqXmlName())) {
          fullAccessToAllParticipatingTypes = false;
        }
      }
      if (!fullAccessToAllParticipatingTypes) {
        for (StorableColumnInformation column : columns) {
          if (column.getDefinitionSite() != VarDefinitionSite.STORABLE) {
            if (!hasAccess(type, column)) {
              buildAndThrow(type, column.getParentStorableInfo(), column);
            }
          }
        }
      }
    }
    
    
    public void checkAccess(AccessType type, XMOMStorableStructureInformation storable) {
      if (!hasAccess(type, storable.getFqXmlName())) {
        buildAndThrow(type, storable);
      }
    }
    
    private XMOMStorableStructureInformation getFirstXMOMStorableParent(StorableColumnInformation column) {
      return getFirstXMOMStorableParentAndPath(column).getFirst();
    }
    
    private Pair<XMOMStorableStructureInformation, String> getFirstXMOMStorableParentAndPath(StorableColumnInformation column) {
      StorableStructureInformation currentParent = column.getParentStorableInfo();
      Stack<StorableColumnInformation> accessStack = new Stack<XMOMStorableStructureCache.StorableColumnInformation>();
      while (!(currentParent instanceof XMOMStorableStructureInformation)) {
        if (currentParent.getPossessingColumn() == null) {
          throw new IllegalArgumentException("How should that happen!");
        } else {
          accessStack.push(currentParent.getPossessingColumn());
          currentParent = currentParent.getPossessingColumn().getParentStorableInfo();
        }
      }
      StringBuilder sb = new StringBuilder();
      for (StorableColumnInformation accessColumn : accessStack) {
        sb.append(accessColumn.getVariableName()).append('.');
      }
      sb.append(column.getVariableName());
      return Pair.of((XMOMStorableStructureInformation) currentParent, sb.toString());
    }
    
    
    private void buildAndThrow(AccessType type, StorableStructureInformation info) {
      buildAndThrow(type, info, null);
    }
    
    private void buildAndThrow(AccessType type, StorableStructureInformation info, StorableColumnInformation column) {
      StringBuilder actionBuilder = new StringBuilder();
      actionBuilder.append(XMOMPersistenceManagement.PERSISTENCE_RIGHT_SCOPE_KEY)
                   .append('.').append(type.getStringRepresentation()).append('(')
                   .append(info.getFqXmlName());
      if (column != null) {
        actionBuilder.append(", ").append(column.getVariableName());
      }
      actionBuilder.append(')');
      XFMG_ACCESS_VIOLATION accessViolation = new XFMG_ACCESS_VIOLATION(actionBuilder.toString(), rightCache.getRoleName());
      throw new RuntimeException(accessViolation);
    }
    
  }
  
  
  
  private static class AllAccessContext implements PersistenceAccessContext {

    public void checkAccess(AccessType type, StorableColumnInformation column) { }
    public void checkAccess(AccessType type, Collection<StorableColumnInformation> columns) { }
    public void checkAccess(AccessType type, XMOMStorableStructureInformation storable) { };
    
  }
  
  private final static AllAccessContext ALLACCESSCONTEXT_INSTANCE = new AllAccessContext();
  
  
  static enum AccessType {
    INSERT("insert"), UPDATE("write"), DELETE("delete"), READ("read"), ALL("*");
    
    private final String stringRepresentation;
    
    AccessType(String stringRepresentation) {
      this.stringRepresentation = stringRepresentation;
    }
    
    public String getStringRepresentation() {
      return stringRepresentation;
    }
  }
  
  
  public static PersistenceAccessContext allAccess() {
    return ALLACCESSCONTEXT_INSTANCE;
  }
  
  
  public static PersistenceAccessContext getAccessContext(XynaOrderServerExtension correlatedOrder) {
    if (correlatedOrder != null &&
        correlatedOrder.getCreationRole() != null) {
      Role role = correlatedOrder.getCreationRole();

      try {
        ScopedRightCache rightCache = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement().getRoleRightScope(role);
        if (rightCache.hasRight(XMOMPersistenceManagement.PERSISTENCE_RIGHT_SCOPE_ALL_ACCESS)) {
          return ALLACCESSCONTEXT_INSTANCE;
        } else {
          return new ScopedRightBasedAccessContext(rightCache);
        }
      } catch (PersistenceLayerException e) {
        throw new RuntimeException(e);
      }
    } else {
      return ALLACCESSCONTEXT_INSTANCE;
    }
  }
}
