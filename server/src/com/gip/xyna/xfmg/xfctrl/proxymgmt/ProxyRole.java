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
package com.gip.xyna.xfmg.xfctrl.proxymgmt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.xfctrl.proxymgmt.right.ProxyRight;
import com.gip.xyna.xfmg.xopctrl.usermanagement.RightScope;
import com.gip.xyna.xfmg.xopctrl.usermanagement.RightScopeBuilder;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.ScopedRightCache;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Action;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.ScopedRight;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;


/**
 * Ergänzt Role um wichtige Methoden für ProxyManagement und kann dabei auch mehrere Rollen zusammenfassen.
 *
 */
public class ProxyRole {

  private static final Logger logger = CentralFactoryLogging.getLogger(ProxyRole.class);

  
  private final String name;
  private final EnumSet<Rights> rights;
  private final EnumSet<ScopedRight> scopedRights;
  private final ScopedRightCache scopedRightCache;
  private final GenerationData generationData;
 
  public ProxyRole(String name, EnumSet<Rights> rights, EnumSet<ScopedRight> scopedRights, 
      ScopedRightCache scopedRightCache, GenerationData generationData) {
    this.name = name;
    this.rights = rights;
    this.scopedRights = scopedRights;
    this.scopedRightCache = scopedRightCache;
    this.generationData = generationData;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ProxyRole(").append(name).append(",gen=").append(generationData);
    return sb.toString();
  }
  
  public String getRightsCoveredBy(ProxyRight proxyRight, Action action) {
    return proxyRight.filterRightStrings( this, action);
  }

  public String getName() {
    return name;
  }

  public boolean hasRight(Rights right) {
    if( right == null ) {
      return false;
    }
    
    return rights.contains(right);
  }

  public boolean hasRight(ScopedRight scopedRight, Action action) {
    if( scopedRight == null ) {
      return false;
    }
    if( ! scopedRights.contains(scopedRight) ) {
      return false;
    }
    if( action == null || action == Action.none ) {
      return true;
    } else { //feinere Filterung!
      return scopedRightCache.hasRightCoveredBy(scopedRight, action);
    }
  }
  
  public boolean hasRight(String scopedRight) {
    return scopedRightCache.hasRight(scopedRight);
  }
  
  public static ProxyRoleBuilder newProxyRole(String proxyName) {
    return new ProxyRoleBuilder(proxyName);
  }
  
  public GenerationData getGenerationData() {
    return generationData;
  }
  
  public List<String> getRightsCovering(ScopedRight scopedRight, Action action) {
    return scopedRightCache.getRightsCovering(scopedRight, action);
  }
  
  public RightStringList getRightStrings() {
    return new RightStringList(this);
  }
  
  
  
  public static class GenerationData {
    private List<String> roles;
    private List<String> unrecognized;
    private List<String> additional;
    private List<String> all;
    private boolean acceptPublic = true;
    private boolean deprecated;
    
    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("GenerationData(");
      sb.append("roles=").append(roles);
      sb.append(", additional=").append(additional);
      if( ! acceptPublic ) {
        sb.append(", withoutPublic");
      }
      if( deprecated ) {
        sb.append(", withDeprecated");
      }
      sb.append(")");
      return sb.toString();
    }
    
    public boolean acceptPublic() {
      return acceptPublic;
    }
    
    public boolean deprecated() {
      return deprecated;
    }
    
    public List<String> getRoles() {
      return roles;
    }
    
    public List<String> getUnrecognized() {
      return unrecognized;
    }
    
    public List<String> getAdditional() {
      return additional;
    }

    public List<String> getAll() {
      return all;
    }
  }
  
  
  public static class ProxyRoleBuilder {

    private String name;
    private HashSet<String> rights = new HashSet<String>();
    private HashSet<String> scopedRights = new HashSet<String>();
    private HashSet<String> additional = new HashSet<String>();
    private HashSet<String> roles = new HashSet<String>();
    private HashSet<String> unrecognized = new HashSet<String>();
    private GenerationData generationData = new GenerationData();
    
    public ProxyRoleBuilder(String name) {
      this.name = name;
    }

    public ProxyRoleBuilder addRole(Role role) {
      rights.addAll( role.getRightsAsSet() );
      scopedRights.addAll( role.getScopedRights() );
      roles.add( role.getName() );
      return this;
    }

    public ProxyRoleBuilder addRight(String right) {
      additional.add(right);
      if( right.indexOf(':') > 0 ) {
        scopedRights.add(right);
      } else {
        rights.add(right);
      }
      return this;
    }

    public ProxyRoleBuilder withoutPublic() {
      generationData.acceptPublic = false;
      return this;
    }
    
    public ProxyRoleBuilder withDeprecated() {
      generationData.deprecated = true;
      return this;
    }
    
    
    
    public ProxyRole buildProxyRole() {
      return buildProxyRole(createDefaultRightScopeMap());
    }
    
    public ProxyRole buildProxyRole(UserManagement userManagement) throws PersistenceLayerException {
      return buildProxyRole(userManagement.getRightScopeMap());
    }
    
    private ProxyRole buildProxyRole(Map<String, RightScope> rightScopeMap) {
      
      fillGenerationData();
      
      EnumSet<Rights> ers = getEnumRights();
      EnumSet<ScopedRight> esrs = getEnumScopedRights();
      
      ScopedRightCache scopedRightCache = new ScopedRightCache(name, scopedRights, rightScopeMap);
      
      return new ProxyRole(name, ers, esrs, scopedRightCache, generationData);
    }

    private Map<String, RightScope> createDefaultRightScopeMap() {
      Map<String, RightScope> rightScopeMap = new HashMap<String, RightScope>();
      RightScopeBuilder rsb = new RightScopeBuilder();
      add(rightScopeMap, rsb, "base.fileaccess:[read, write, insert, delete, *]:/.*/");
      add(rightScopeMap, rsb, "xfmg.xfctrl.ApplicationDefinitionManagement:[write, insert, *]:/.*/:/.*/");
      add(rightScopeMap, rsb, "xfmg.xfctrl.ApplicationManagement:[list, start, stop, deploy, remove, migrate, *]:/.*/:/.*/");
      add(rightScopeMap, rsb, "xfmg.xfctrl.WorkspaceManagement:[list, *]:/.*/");
      add(rightScopeMap, rsb, "xfmg.xfctrl.XynaProperties:[read, write, insert, delete, *]:/.*/");
      add(rightScopeMap, rsb, "xfmg.xfctrl.administrativeVetos:[read, write, insert, delete, *]:/\\*/");
      add(rightScopeMap, rsb, "xfmg.xfctrl.capacities:[read, write, insert, delete, *]:/\\*/");
      add(rightScopeMap, rsb, "xfmg.xfctrl.cronLikeOrders:[read, write, insert, delete, *]:/\\*/:/\\*/:/\\*/");
      add(rightScopeMap, rsb, "xfmg.xfctrl.dataModels:[read, write, insert, delete, *]:/\\*/");
      add(rightScopeMap, rsb, "xfmg.xfctrl.deploymentItems:[read, *]:/\\*/:/\\*/:/\\*/");
      add(rightScopeMap, rsb, "xfmg.xfctrl.deploymentMarker:[write, insert, delete, *]");
      add(rightScopeMap, rsb, "xfmg.xfctrl.orderInputSources:[read, write, insert, delete, generate, *]:/\\*/:/\\*/:/\\*/");
      add(rightScopeMap, rsb, "xfmg.xfctrl.orderTypes:[read, write, insert, delete, *]:/\\*/:/\\*/:/\\*/");
      add(rightScopeMap, rsb, "xfmg.xfctrl.timeControlledOrders:[read, write, insert, enable, disable, kill, *]:/.*/:/.*/:/.*/");
      add(rightScopeMap, rsb, "xfmg.xfctrl.TriggerManagement:[read, write, insert, delete, *]:/.*/");
      add(rightScopeMap, rsb, "xfmg.xfctrl.FilterManagement:[read, write, insert, delete, *]:/.*/");
      add(rightScopeMap, rsb, "xnwh.persistence.Storables:[read, write, insert, delete, *]:*:*");
      add(rightScopeMap, rsb, "xprc.xpce.StartOrder:/.*/:/.*/:/.*/");
      return rightScopeMap;
    }
    private void add(Map<String, RightScope> rightScopeMap, RightScopeBuilder rsb, String definition) {
      RightScope rs = rsb.buildRightScope(definition);
      if( rs == null ) {
        logger.warn( "Could not build RightScope for \""+definition+"\"");
      } else {
        rightScopeMap.put( rs.getName(), rs);
      }
    }

    private void fillGenerationData() {
      generationData.additional = new ArrayList<String>(additional);
      Collections.sort(generationData.additional);
      
      generationData.roles = new ArrayList<String>(roles);
      Collections.sort(generationData.roles);
      
      generationData.unrecognized = new ArrayList<String>(unrecognized);
      Collections.sort(generationData.unrecognized);
      
      HashSet<String> all = new HashSet<String>();
      all.addAll(additional);
      all.addAll(rights);
      all.addAll(scopedRights);
      generationData.all = new ArrayList<String>(all);
      Collections.sort(generationData.all);
    }

    private EnumSet<Rights> getEnumRights() {
      EnumSet<Rights> ers = EnumSet.noneOf(Rights.class);
      for( String r : rights ) {
        if( r != null ) {
          try {
            ers.add( Rights.valueOf(r) );
          } catch( IllegalArgumentException e ) {
            unrecognized.add(r);
          }
        }
      }
      return ers;
    }

    private EnumSet<ScopedRight> getEnumScopedRights() {
      EnumSet<ScopedRight> esrs = EnumSet.noneOf(ScopedRight.class);
      for( String r : scopedRights ) {
        int idx = r.indexOf(':');
        if( idx == -1 ) {
          unrecognized.add(r);
        } else {
          String key = r.substring(0,idx);
          boolean found = false;
          for( ScopedRight sr : ScopedRight.values() ) {
            if( sr.getKey().equals(key) ) {
              esrs.add(sr);
              found = true;
              break;
            }
          }
          if( ! found ) {
            unrecognized.add(r);
          }
        }
      }
      return esrs;
    }
    
    
  }
  
  
  public static class RightStringList {
    private ProxyRole proxyRole;
    private List<String> rs;
    
    public RightStringList(ProxyRole proxyRole) {
      this.proxyRole = proxyRole;
      this.rs = new ArrayList<String>();
    }

    public String asString() {
      switch( rs.size() ) {
        case 0: 
          return ""; //Sollte nicht auftreten
        case 1: 
          return rs.get(0);
        default:
          return rs.toString();
      }
    }

    public RightStringList add(ScopedRight scopedRight, Action action) {
      if( scopedRight != null ) {
        rs.addAll( proxyRole.getRightsCovering(scopedRight, action) );
      }
      return this;
    }

    public RightStringList add(Rights right) {
      if( proxyRole.hasRight(right) ) {
        rs.add( right.name() );
      }
      return this;
    }
    
  }

  
}
