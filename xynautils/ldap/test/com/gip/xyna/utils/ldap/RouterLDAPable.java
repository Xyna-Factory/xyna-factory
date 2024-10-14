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
package com.gip.xyna.utils.ldap;

import java.io.ByteArrayInputStream;
import java.security.cert.*;
import java.util.Hashtable;
import java.util.Vector;

import com.gip.xyna.utils.ldap.LDAPable;



/**
 * RouterLDAPable Repraesentation eines Routers
 */
public class RouterLDAPable implements LDAPable {
  
  public String routerName = "";
  public String ldapId = "";
  public Vector<X509Certificate> certs = new Vector<X509Certificate>();
  public String serialNumber = "";
  // to be continued
  
  public RouterLDAPable() {
  }
  
  /**
   * @see com.gip.xyna.utils.ldap.LDAPable#getDN()
   */
  public String getDN() {
    String retVal="";
    if (ldapId!=null){
      if (routerName.length()>0) {
        retVal = "cn="+routerName+",";
      }
      retVal += "dc=CDU,ou="+ldapId;
    }
    return retVal;
  }
  
  /**
   * @see com.gip.xyna.utils.ldap.LDAPable#getInstance()
   */
  public LDAPable getInstance() {
    // Wir nehmen mal an, alle Router gehoeren zum selben Netz
    RouterLDAPable router = new RouterLDAPable();
    router.ldapId=this.ldapId;
    return router;
  }
  
  /**
   * @see com.gip.xyna.utils.ldap.LDAPable#getLDAPAttributes()
   */
  public Hashtable getLDAPAttributes() {
    Hashtable retVal = new Hashtable();
    if (serialNumber!=null && serialNumber.length()>0)  retVal.put("serialNumber", serialNumber);
    if (routerName!=null && routerName.length()>0)      retVal.put("cn", routerName);
    retVal.put("objectClass", new String[] {"top","cep"}); // immer
    if (certs.size()>0) {
      Vector<byte[]> l_certs = new Vector<byte[]>();
      try {
        for (int i = 0; i < certs.size(); i++) {
          l_certs.add(certs.elementAt(i).getEncoded());
        }
        // Hier dummys
        // certs.add(CertUtils.getDummyCRT1().getEncoded());
        // certs.add(CertUtils.getDummyCRT2().getEncoded());
        retVal.put("userCertificate",l_certs);
      }
      catch (CertificateEncodingException e) {
        e.printStackTrace();
      }
    }
    return retVal;
  }
  
  /**
   * @see com.gip.xyna.utils.ldap.LDAPable#setValue(java.lang.String, java.lang.String)
   */
  public void setValue(String key, String value) {
    if (key.equalsIgnoreCase("cn")) {
      routerName = value;
      // System.out.println("Setze RouterName auf "+value);
    }
    else if (key.equalsIgnoreCase("serialNumber")) {
      serialNumber = value;
      // System.out.println("Setze SerialNumber auf "+value);
    }
    else {
      // unbekannt oder uninteressant
      // System.out.println("RouterLDAPable.setValue("+key+","+value+")");
    }
  }
  
  /**
   * @see com.gip.xyna.utils.ldap.LDAPable#setValue(java.lang.String, java.lang.Object)
   */
  public void setValue(String key, byte[] value) {
    if (key.toLowerCase().startsWith("usercertificate")) {
      try {
        //System.out.println("TestLDAPable.setValue("+key+",Zertifikat)");
        CertificateFactory cF;
        cF = CertificateFactory.getInstance("X509");
        ByteArrayInputStream bAIS = new ByteArrayInputStream(value);
        X509Certificate cert = (X509Certificate) cF.generateCertificate(bAIS);
        bAIS.close();
        certs.add(cert);
//        System.out.println(CertUtils.certToString(cert));
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
    else {
      System.out.println("TestLDAPable.set(Binary)Value("+key+")"); 
    }
  }

  /**
   * Zum Debuggen
   * @see java.lang.Object#toString()
   */
  public String toString() {
    String retVal = "Router:";
    retVal += "\nldapId:       " + ldapId;
    retVal += "\nrouterName:   " + routerName;
    retVal += "\nserialNumber: " + serialNumber;
    for (int i = 0; i < certs.size(); i++) {
      retVal += "\n" +CertUtils.certToString(certs.elementAt(i));
    }
    return retVal;
  }

}


