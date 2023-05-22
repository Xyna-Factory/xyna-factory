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
package com.gip.xyna.utils.ldap;

import java.util.Hashtable;

import com.gip.xyna.utils.ldap.LDAPable;



/**
 * NetLDAPable: Netze im LDAP
 */
public class NetLDAPable implements LDAPable {
  
  // Das merkt sich normalerweise die Klasse
  public String ldapId="";
  
  // Q&D: Durchzaehlen
  static int no=0;
  
  public NetLDAPable() {
    no++;
  }
  
  /**
   * @see com.gip.xyna.utils.ldap.LDAPable#getDN()
   */
  public String getDN() {
    if (ldapId!=null && ldapId.length()>0) {
      // ldapId bekannt, dann setzen
      return "ou="+ldapId;
    }
    // Sonst leer
    return "";
  }
  
  /**
   * @see com.gip.xyna.utils.ldap.LDAPable#getInstance()
   */
  public LDAPable getInstance() {
    // Einfach ein neues Element
    return new NetLDAPable();
  }
  
  /**
   * @see com.gip.xyna.utils.ldap.LDAPable#getLDAPAttributes()
   */
  public Hashtable getLDAPAttributes() {
    Hashtable retVal = new Hashtable();
    // Fuer die Suche: wir suchen "ou"s!
    retVal.put("objectClass", "organizationalUnit");
    if (ldapId!=null && ldapId.length()>0) {
      // ldapId? Steht wenn dann in ou drin
      retVal.put("ou", ldapId);
    }
    // weitere Attrs gibts nicht
    return retVal;
  }
  
  /**
   * @see com.gip.xyna.utils.ldap.LDAPable#setValue(java.lang.String, java.lang.String)
   */
  public void setValue(String key, String value) {
    if (key.equalsIgnoreCase("ou")) {
      ldapId = value;
      //System.out.println(no+":Setze LDAP-Id auf "+value);
    }
    else {
      // zum Debuggen
      System.out.println(no+":TestLDAPable.setValue("+key+","+value+")");
    }
  }
  
  /**
   * @see com.gip.xyna.utils.ldap.LDAPable#setValue(java.lang.String, java.lang.Object)
   */
  public void setValue(String key, byte[] value) {
    // sollte es hier nicht geben, da es hier keine Zertifikate gibt.
    System.out.println(no+"TestLDAPable.setValue("+key+","+String.valueOf(value)+")");
  }
  
  /**
   * Zum Debuggen
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return "Ich bin das Netz mit der ldapId " + ldapId + ".";
  }
  
}


