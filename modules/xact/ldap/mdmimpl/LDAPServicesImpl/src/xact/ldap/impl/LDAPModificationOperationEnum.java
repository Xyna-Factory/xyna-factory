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
package xact.ldap.impl;

import xact.ldap.LDAPModificationAdd;
import xact.ldap.LDAPModificationDelete;
import xact.ldap.LDAPModificationOperation;
import xact.ldap.LDAPModificationReplace;



public enum LDAPModificationOperationEnum {
  ADD(LDAPModificationAdd.class, com.novell.ldap.LDAPModification.ADD),
  DELETE(LDAPModificationDelete.class, com.novell.ldap.LDAPModification.DELETE),
  REPLACE(LDAPModificationReplace.class, com.novell.ldap.LDAPModification.REPLACE);
  
  Class<? extends LDAPModificationOperation> associatedClass;
  int valueForConnection;
  
  private LDAPModificationOperationEnum(Class<? extends LDAPModificationOperation> associatedClass, int valueForConnection) {
    this.associatedClass = associatedClass;
    this.valueForConnection = valueForConnection;
  }
  
  public int getValueForModification() {
    return valueForConnection;
  }
  
  public static <I extends LDAPModificationOperation> LDAPModificationOperationEnum getLDAPSearchScopeByInstance(I instance) {
    for (LDAPModificationOperationEnum value : values()) {
      if (value.associatedClass.isInstance(instance)) {
        return value;
      }
    }
    throw new RuntimeException("Invalid instance for LDAPSearchScope: " + instance.getClass().getCanonicalName());
  }
}
