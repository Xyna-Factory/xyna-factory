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
package xact.ldap.impl;

import xact.ldap.SearchFilterDepthScope;
import xact.ldap.SearchRecursiveScope;
import xact.ldap.SearchResultScope;
import xact.ldap.SearchRootScope;

public enum SearchResultScopeEnum {
  ROOT(SearchRootScope.class),
  FILTEDEPTH(SearchFilterDepthScope.class),
  RECURSIVE(SearchRecursiveScope.class);
  
  Class<? extends SearchResultScope> associatedClass;
  
  private SearchResultScopeEnum(Class<? extends SearchResultScope> associatedClass) {
    this.associatedClass = associatedClass;
  }
  
  
  public static <I extends SearchResultScope> SearchResultScopeEnum getLDAPSearchScopeByInstance(I instance) {
    for (SearchResultScopeEnum value : values()) {
      if (value.associatedClass.isInstance(instance)) {
        return value;
      }
    }
    throw new RuntimeException("Invalid instance for LDAPSearchScope: " + instance.getClass().getCanonicalName());
  }
  
}
