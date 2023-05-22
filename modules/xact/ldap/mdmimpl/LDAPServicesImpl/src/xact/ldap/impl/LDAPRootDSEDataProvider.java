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

import java.util.HashSet;
import java.util.Set;

import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchResults;
import com.novell.ldap.LDAPAttribute;

public class LDAPRootDSEDataProvider {
  
  private final static String ATTRIBUTE_NAME_SUPPORTEDCONTROLS = "supportedControl";
  private final static String ATTRIBUTE_NAME_ALTSERVER = "altServer";
  private final static String ATTRIBUTE_NAME_NAMINGCONTEXTS = "namingContexts";
  private final static String ATTRIBUTE_NAME_SUPPORTEDEXTENSIONS = "supportedExtension";
  private final static String ATTRIBUTE_NAME_SUPPORTEDFEATURES = "supportedFeatures";
  private final static String ATTRIBUTE_NAME_SUPPORTEDLDAPVERSION = "supportedLDAPVersion";
  private final static String ATTRIBUTE_NAME_SUPPORTEDSASLMECHANISMS = "supportedSASLMechanisms";
  private final static String ATTRIBUTE_NAME_SUBSCHEMASUBENTRY= "subschemaSubentry";
  
  private static Set<String> supportedControls;
  private static Set<String> namingContexts;
  private static Set<String> alternativeServers;
  private static Set<String> supportedExtensions;
  private static Set<String> supportedSASLMechanisms;
  private static Set<String> subschemaSubentry; // do we get this?
  private static String supportedLDAPVersion;
  

  
  public static boolean isControlSupported(String oid, LDAPConnection connection) throws LDAPRootDSEDiscoveryFailed {
    if (supportedControls == null) {
      synchronized (LDAPRootDSEDataProvider.class) {
        if (supportedControls == null) {
          discoverLDAPRootDSE(connection);
        }
      }
    }
    return supportedControls.contains(oid);
  }
  
  
  private static void discoverLDAPRootDSE(LDAPConnection connection) throws LDAPRootDSEDiscoveryFailed {
    String[] allLDAPv3RootDSEAttributes = new String[] {ATTRIBUTE_NAME_SUPPORTEDCONTROLS, ATTRIBUTE_NAME_ALTSERVER, ATTRIBUTE_NAME_NAMINGCONTEXTS,
                    ATTRIBUTE_NAME_SUPPORTEDEXTENSIONS, ATTRIBUTE_NAME_SUPPORTEDFEATURES, ATTRIBUTE_NAME_SUPPORTEDLDAPVERSION,
                    ATTRIBUTE_NAME_SUBSCHEMASUBENTRY, ATTRIBUTE_NAME_SUPPORTEDSASLMECHANISMS};
    try {
      LDAPSearchResults result = connection.search("", LDAPConnection.SCOPE_BASE, "(objectClass=*)", allLDAPv3RootDSEAttributes, false);
      LDAPEntry rootDSE = result.next();
      if (rootDSE == null) {
        throw new LDAPRootDSEDiscoveryFailed();
      } else {
        initializeAll(rootDSE);
      }
    } catch (LDAPException e) {
      throw new LDAPRootDSEDiscoveryFailed(LDAPExceptionEnum.transformLDAPExceptionToAppropriateXynaException(e));
    }
  }
  
  
  private static void initializeAll(LDAPEntry rootDSE) {
    supportedControls = getAttributeAsSet(rootDSE, ATTRIBUTE_NAME_SUPPORTEDCONTROLS);
    namingContexts = getAttributeAsSet(rootDSE, ATTRIBUTE_NAME_NAMINGCONTEXTS);
    alternativeServers = getAttributeAsSet(rootDSE, ATTRIBUTE_NAME_ALTSERVER);
    supportedExtensions = getAttributeAsSet(rootDSE, ATTRIBUTE_NAME_SUPPORTEDEXTENSIONS);
    supportedSASLMechanisms = getAttributeAsSet(rootDSE, ATTRIBUTE_NAME_SUPPORTEDSASLMECHANISMS);
    subschemaSubentry = getAttributeAsSet(rootDSE, ATTRIBUTE_NAME_SUBSCHEMASUBENTRY);
    supportedLDAPVersion = getAttributeValue(rootDSE, ATTRIBUTE_NAME_SUPPORTEDLDAPVERSION);
  }
  
  
  private static Set<String> getAttributeAsSet(LDAPEntry entry, String attributeName) {
    LDAPAttribute attribute = entry.getAttribute(attributeName);
    Set<String> attributeSet = new HashSet<String>();
    for (String attributeValue : attribute.getStringValueArray()) {
      attributeSet.add(attributeValue);
    }
    return attributeSet;
  }
  
  private static String getAttributeValue(LDAPEntry entry, String attributeName) {
    LDAPAttribute attribute = entry.getAttribute(attributeName);
    return attribute.getStringValue();
  }
  
  
  public static class LDAPRootDSEDiscoveryFailed extends Exception {
    
    private static final long serialVersionUID = 1581184372008022768L;

    public LDAPRootDSEDiscoveryFailed() {
      super("Failed to discover RootDSE");
    }
    
    public LDAPRootDSEDiscoveryFailed(Throwable t) {
      super("Failed to discover RootDSE", t);
    }
    
  }
  
  
}
