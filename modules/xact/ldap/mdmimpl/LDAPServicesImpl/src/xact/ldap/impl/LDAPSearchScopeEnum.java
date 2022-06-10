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
package xact.ldap.impl;

import xact.ldap.SearchLDAPScope;
import xact.ldap.SearchLDAPScopeBase;
import xact.ldap.SearchLDAPScopeOne;
import xact.ldap.SearchLDAPScopeSubordinateSubtree;
import xact.ldap.SearchLDAPScopeSubtree;

import com.novell.ldap.LDAPConnection;


public enum LDAPSearchScopeEnum {
  BASE(SearchLDAPScopeBase.class, LDAPConnection.SCOPE_BASE),
  ONE(SearchLDAPScopeOne.class, LDAPConnection.SCOPE_ONE),
  SUBTREE(SearchLDAPScopeSubtree.class, LDAPConnection.SCOPE_SUB),
  SUBORDINATESUBTREE(SearchLDAPScopeSubordinateSubtree.class, LDAPConnection.SCOPE_SUBORDINATESUBTREE);
  
  Class<? extends SearchLDAPScope> associatedClass;
  int valueForConnection;
  
  private LDAPSearchScopeEnum(Class<? extends SearchLDAPScope> associatedClass, int valueForConnection) {
    this.associatedClass = associatedClass;
    this.valueForConnection = valueForConnection;
  }
  
  public int getValueForConnection() {
    return valueForConnection;
  }
  
  public static <I extends SearchLDAPScope> LDAPSearchScopeEnum getLDAPSearchScopeByInstance(I instance) {
    for (LDAPSearchScopeEnum value : values()) {
      if (value.associatedClass.isInstance(instance)) {
        return value;
      }
    }
    throw new RuntimeException("Invalid instance for LDAPSearchScope: " + instance.getClass().getCanonicalName());
  }
  
}
