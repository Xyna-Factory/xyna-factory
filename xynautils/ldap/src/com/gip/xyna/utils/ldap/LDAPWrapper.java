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

import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttributes;



/**
 * LDAPWrapper
 */
public class LDAPWrapper {
  
  
  public interface SearchScope extends LDAPManager.SearchScope{ }
  
  private Properties _ldapProperties = null; 

  private String rootDN=null;
 
  
  /**
   * @param props
   */
  public LDAPWrapper(Properties props) {
    this._ldapProperties = props;
    rootDN=props.getProperty("ldap.rootDN");
    if (rootDN!=null) {
      rootDN = rootDN.trim();
      if (rootDN.length()>0 && rootDN.startsWith(",")) {
        rootDN=rootDN.substring(1);
      }
    }
  }
  
  // ----------------------------------------------------------------------------------
  // ------ Lesende Funktionen --------------------------------------------------------
  // ----------------------------------------------------------------------------------

  /**
   * Liefert genau ein Element 
   * @param dn Eindeutiger Pfad zum Element (ohne root)
   */
  public LDAPable find(String dn, LDAPable example) throws Exception {
    dn=handleDN(dn);
    // FIXME: auf entsprechendes Logging umstellen
    System.out.println("* LDAPWrapper : find() : "+dn);

    LDAPManager ldapManager = new LDAPManager(_ldapProperties);
    Attributes attrs=ldapManager.getLDAPObject(dn);
    convertFromAtributes(attrs, example);
    
    return example;
  }

  /**
   * Liefert alle Objekte 
   * @param dn Einduetiger Pfad, ab dem die Suche startet (ohne root)
   * @param searchScope siehe Search-Scope-Konstanten
   * @param example Filterobjekt und Vorlage fuer die Liste
   * @return
   * @throws Exception
   */
  public Vector<LDAPable> findAll(String dn, int searchScope, LDAPable example) throws Exception {
    dn=handleDN(dn);
    // FIXME: auf entsprechendes Logging umstellen
    System.out.println("* LDAPWrapper : findAll() : "+dn);
    
    LDAPManager ldapManager = new LDAPManager(_ldapProperties);
    Vector<LDAPable> retVal = new Vector<LDAPable>();
    Attributes filterAttributes = convertToAttributes(example);
    Vector<Attributes> retAttributes = ldapManager.getLDAPObject(dn, searchScope, filterAttributes);
    
    for (int i = 0; i < retAttributes.size(); i++) {
      LDAPable nextElement = example.getInstance();
      convertFromAtributes(retAttributes.elementAt(i), nextElement);
      retVal.add(nextElement);
    }
    
    return retVal;
  }
  
  
  
  // ----------------------------------------------------------------------------------
  // ------ Schreibende Funktionen ----------------------------------------------------
  // ----------------------------------------------------------------------------------
  
  /**
   * Speichert ein Objekt im LDAP
   * @param dn Eindeutiger Pfad zum Element (ohne root)
   * @param example Das Objekt, dass gespeichert werden soll
   * @param update true fuer update, false fuer insert Achtung: update NULLt vorhandene Attribute nicht! 
   */
  public void set(String dn, LDAPable example, boolean update) throws Exception {
    dn=handleDN(dn);
    // FIXME: auf entsprechendes Logging umstellen
    System.out.println("* LDAPWrapper : set("+update+") : "+dn);
    
    LDAPManager ldapManager = new LDAPManager(_ldapProperties);
    if (update) {
      ldapManager.getLDAPObject(dn);
    }

    Attributes attrs = convertToAttributes(example);
    if (update) {
      ldapManager.modifyLDAPObject(dn, attrs);
    }
    else {
      ldapManager.addLDAPObject(dn, attrs);
    }
  }

  
  /**
   * Loescht einen Eintrag im LDAP
   * @param dn Eindeutiger Pfad zum Element (ohne root)
   * @throws Exception
   */
  public void delete(String dn) throws Exception {
    dn=handleDN(dn);
    // FIXME: auf entsprechendes Logging umstellen
    System.out.println("* LDAPWrapper : delete() : "+dn);
    
    LDAPManager ldapManager = new LDAPManager(_ldapProperties);
    ldapManager.removeLDAPObject(dn);
  }
  
  /**
   * Loescht mehrere Eintraege im LDAP
   * @param dn Einduetiger Pfad, ab dem die Suche startet (ohne root)
   * @param searchScope siehe Search-Scope-Konstanten
   * @param example Filterobjekt und Vorlage fuer die Liste
   * @throws Exception
   */
  public void findAndDelete(String dn, int searchScope, LDAPable example) throws Exception {
    Vector<LDAPable> toDelete = findAll(dn, searchScope, example);

    for (int i = 0; i < toDelete.size(); i++) {
      delete(toDelete.elementAt(i).getDN());
    }
  }
  
  // ----------------------------------------------------------------------------------
  // ------ Converter-Funktionen ------------------------------------------------------
  // ----------------------------------------------------------------------------------
  
  /**
   * Macht aus einem Objekt Attribute
   * @param example Das Objekt
   * @return Umgewandelt in Attribute
   */
  private Attributes convertToAttributes(LDAPable example) {
    Attributes attrs= new BasicAttributes();
    Enumeration en = example.getLDAPAttributes().keys();
    while (en.hasMoreElements()) {
      String key = (String) en.nextElement();
      Object oVal = example.getLDAPAttributes().get(key);
      if (oVal instanceof String ||
          oVal instanceof byte[]) {
        // Ein Element
        LDAPManager.addAttribute(attrs, key, oVal);
      }
      else if (oVal instanceof String[]) {
        // Mehrere Elemente
        String[] valArray = (String[]) oVal;
        for (int i = 0; i < valArray.length; i++) {
          LDAPManager.addToAttribute(attrs, key, valArray[i]);
        }
      }
      else if (oVal instanceof Vector) {
        // Mehrere Elemente
        Vector valArray = (Vector) oVal;
        for (int i = 0; i < valArray.size(); i++) {
          LDAPManager.addToAttribute(attrs, key, valArray.elementAt(i));
        }
      }
      // Liste to be continued!!!
      // z.B. int?
    }
    return attrs;
  }

  /**
   * Macht aus Attributen ein Objekt
   * @param attrs Die Attribute, die der LDAP liefert
   * @param example umgewandelt in ein Objekt (CALL BY REFERENCE!)
   */
  private static void convertFromAtributes(Attributes attrs, LDAPable example) {
    NamingEnumeration<String> en = attrs.getIDs();
    while (en.hasMoreElements()) {
      try {
        String key = en.nextElement();
        Attribute att = attrs.get(key);
        NamingEnumeration e=att.getAll();
        while (e.hasMore()) {
          Object o=e.next();
          String value="";
          if (o instanceof String) {
            value = (String) o;
            example.setValue(key, value);
          }
          else if (o instanceof byte[]) {
            String newkey = key;
            if (newkey.indexOf(";binary")>=0) {
              newkey = newkey.substring(0,key.indexOf(";binary"));
            }
            example.setValue(newkey, (byte[]) o);
          }
          else {
            System.out.println("XLDAP.find :"+o.getClass().getName());
          }
        }
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
  
  // ----------------------------------------------------------------------------------
  // ------ Sonstige Hilfs-Funktionen -------------------------------------------------
  // ----------------------------------------------------------------------------------

  /**
   * Fuegt evtl. den root-Pfad an
   * @param dn Lokaler dn
   * @return globale dn
   */
  private String handleDN(String dn) {
    if (rootDN==null || rootDN.length()==0) {
      return dn;
    }
    if (dn.length()==0) {
      return rootDN;
    }
    return dn+","+rootDN;
  }
  
  
  
  // ----------------------------------------------------------------------------------
  // ------ Test-Funktionen -----------------------------------------------------------
  // ----------------------------------------------------------------------------------
  
  /**
   * FOR DEBUGGING.
   * schreibt die Attribute raus
   * @param attrs Attribute
   * @throws Exception 
   */
  public void printAttributes(Attributes attrs) throws Exception {
    if (attrs == null) {
      System.out.println("No attributes found !!!");
    }
    else {
      try {
        NamingEnumeration enumeration=attrs.getAll();
        while( enumeration.hasMore()) {
          Attribute attribute=(Attribute) enumeration.next();
          System.out.println("ATTRIBUTE :" + attribute.getID());
          NamingEnumeration e=attribute.getAll();
          while (e.hasMore()) {
            System.out.println("  = " + e.next());
          }
        }
      }
      catch (NamingException e) {
        e.printStackTrace();
      }
    }
  }

  
}
  


