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

import java.util.Properties;
import java.util.Vector;

import com.gip.xyna.utils.ldap.LDAPWrapper;
import com.gip.xyna.utils.ldap.LDAPable;



/**
 * LDAPTest
 */
public class LDAPTest {

  /**
   * @param args
   */
  public static void main(String[] args) {
    Properties props = new Properties();
    props.put("ldap.user","cn=admin,dc=ldap-test,dc=gip,dc=local");
    props.put("ldap.pass","admin");
    props.put("ldap.server","ldap://giplin229:389");
    props.put("ldap.rootDN","dc=ldap-test,dc=gip,dc=local");

    RouterLDAPable example = new RouterLDAPable();
    example.routerName = "ffmarcornmsdatenal1";
    example.ldapId = "1275";

    LDAPWrapper ldap = new LDAPWrapper(props);
    try {
      example = (RouterLDAPable) ldap.find(example.getDN(), example);
      System.out.println("LDAPTest.main :"+example.toString());
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    
    try {
      ldap.set(example.getDN(),example,true);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    
//    try {
//      example = ldap.find(example.getDN(), example);
//    }
//    catch (Exception e) {
//      e.printStackTrace();
//    }
    
    NetLDAPable ouFilter = new NetLDAPable();
    try {
      Vector<LDAPable> nets = ldap.findAll(ouFilter.getDN(), LDAPWrapper.SearchScope.SUBTREE, ouFilter);
      for (int i = 0; i < nets.size(); i++) {
        System.out.println("LDAPTest.main: "+nets.elementAt(i).toString());
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }
}


