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

import java.rmi.RemoteException;

import java.util.*;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;


/**
 * LDAPManager
 * Stellt die eigentliche Kommunikation mit dem LDAP dar.
 */
public class LDAPManager {

  
  /**
   * SearchScope
   */
  public interface SearchScope {
    public static final int SUBTREE  = SearchControls.SUBTREE_SCOPE;
    public static final int OBJECT   = SearchControls.OBJECT_SCOPE;
    public static final int ONELEVEL = SearchControls.ONELEVEL_SCOPE;
  }
  
  
  private static final String CONTEXT_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
  private static final String AUTHENTICATION = "simple";
  private static final String CONNECT_TIMEOUT = "com.sun.jndi.ldap.connect.timeout";

  
  private Hashtable<String, String> env = null;
  
  
  /**
   * Konstruktor mit Properties
   * @param props
   */
  public LDAPManager(Properties props) {
    env = new Hashtable<String, String>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, CONTEXT_FACTORY);
    env.put(Context.PROVIDER_URL, props.getProperty("ldap.server").trim());
    env.put(Context.SECURITY_AUTHENTICATION, props.getProperty("ldap.auth").trim());
    env.put(Context.SECURITY_PRINCIPAL, props.getProperty("ldap.user").trim());
    env.put(Context.SECURITY_CREDENTIALS, props.getProperty("ldap.pass").trim());
    
  }

  /**
   * Konstruktor mit providerURL principal password
   * @param providerURL
   * @param principal
   * @param password
   */
  public LDAPManager(String providerURL, String principal, String password) {
    env = new Hashtable<String, String>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, CONTEXT_FACTORY);
    env.put(Context.PROVIDER_URL, providerURL);
    env.put(Context.SECURITY_AUTHENTICATION, AUTHENTICATION);
    env.put(Context.SECURITY_PRINCIPAL, principal);
    env.put(Context.SECURITY_CREDENTIALS, password);
  }
  
  
  /**
   * Konstruktor mit providerURL principal password und timeout
   * @param providerURL
   * @param principal
   * @param password
   * @param timeout
   */
  public LDAPManager(String providerURL, String principal, String password, String timeout) {
    env = new Hashtable<String, String>();
    env.put(Context.INITIAL_CONTEXT_FACTORY, CONTEXT_FACTORY);
    env.put(Context.PROVIDER_URL, providerURL);
    env.put(Context.SECURITY_AUTHENTICATION, AUTHENTICATION);
    env.put(Context.SECURITY_PRINCIPAL, principal);
    env.put(Context.SECURITY_CREDENTIALS, password);
    env.put(CONNECT_TIMEOUT, timeout);
  }

  
  /**
   * Baut die Verbindung zum LDAP auf.
   * @return
   * @throws NamingException
   */
  private DirContext getDirContext() throws NamingException {
    try {
      InitialDirContext ctx = new InitialDirContext(env);
      return ctx;
    } 
    catch (NamingException e) {
      // FIXME: auf entsprechendes Logging umstellen
      System.out.println("* LDAPManager : INITIAL_CONTEXT_FACTORY : "+env.get(Context.INITIAL_CONTEXT_FACTORY));
      System.out.println("* LDAPManager : PROVIDER_URL            : "+env.get(Context.PROVIDER_URL));
      System.out.println("* LDAPManager : SECURITY_AUTHENTICATION : "+env.get(Context.SECURITY_AUTHENTICATION));
      System.out.println("* LDAPManager : SECURITY_PRINCIPAL      : "+env.get(Context.SECURITY_PRINCIPAL));
      System.out.println("* LDAPManager : SECURITY_CREDENTIALS    : "+env.get(Context.SECURITY_CREDENTIALS));
      System.out.println("* LDAPManager : CONNECT_TIMEOUT         : "+env.get(CONNECT_TIMEOUT));
      NamingException ne=new NamingException("Can't create InitialDirContext to LDAP : "+e.getMessage() );
      ne.setStackTrace(e.getStackTrace());
      throw ne;
    }
  }

  /**
   * Fuegt ein LDAP-Objekt hinzu
   * @param dn Ziel-DN
   * @param attributes Hinzuzufuegende Attribute
   * @throws NamingException
   */
  public void addLDAPObject(String dn,
                            Attributes attributes) throws NamingException {
    DirContext ctx = null;
    try {
      ctx = getDirContext();
      ctx.createSubcontext(dn, attributes);
    } 
    finally {
      if (ctx != null) {
        try {
          ctx.close();
        } 
        catch (Throwable t) {
        }
      }
    }
  }

  /**
   * Aendert ein Objekt
   * @param dn Ziel-DN
   * @param attributes Hinzuzufuegende Attribute
   * @throws NamingException
   */
  public void modifyLDAPObject(String dn,
                            Attributes attributes) throws NamingException {
    DirContext ctx = null;
    try {
      ctx = getDirContext();
      ctx.modifyAttributes(dn, DirContext.REPLACE_ATTRIBUTE,  attributes);
    } 
    finally {
      if (ctx != null) {
        try {
          ctx.close();
        } 
        catch (Throwable t) {
        }
      }
    }
  }

  /**
   * Liefert ein LDAP-Objekt 
   * @param dn Eindeutiger Such-DN
   * @return
   * @throws NamingException
   */
  public Attributes getLDAPObject(String dn) throws NamingException {
    DirContext ctx = null;
    Attributes attributes = null;
    try {
      ctx = getDirContext();
      attributes = ctx.getAttributes(dn);
    } 
    finally {
      if (ctx != null) {
        try {
          ctx.close();
        } 
        catch (Throwable t) {
        }
      }
    }
    return attributes;
  }

  /**
   * Liefert LDAP-Objekte anhand eines Filters
   * @param dn DN, an dem die Suche gestartet wird
   * @param filterAttributes Filter 
   * @return
   * @throws NamingException
   * @throws LDAPException
   */
  public Vector<Attributes> getLDAPObject(String dn, 
                                          int searchScope,
                                          Attributes filterAttributes) throws NamingException,
                                                                      LDAPException {
    DirContext ctx = null;
    Vector<Attributes> retVal = new Vector<Attributes>();
    try {
      ctx = getDirContext();

      SearchControls sc = new SearchControls();
      sc.setCountLimit(100);
      sc.setSearchScope(searchScope);
      String filter = getFilter(filterAttributes);

      NamingEnumeration<SearchResult> ne = ctx.search(dn, filter, sc);
      while (ne.hasMore()) {
        Attributes attributes = ne.next().getAttributes();
        retVal.add(attributes);
      } 
    } 
    finally {
      if (ctx != null) {
        try {
          ctx.close();
        } 
        catch (Throwable t) {
        }
      }
    }
    return retVal;
  }

  /**
   * Loescht ein Objekt aus dem LDAP
   * @param dn Eindeutiger DN
   * @throws NamingException
   */
  public void removeLDAPObject(String dn) throws NamingException {
    DirContext ctx = null;
    try {
      ctx = getDirContext();
      ctx.destroySubcontext(dn);
    } 
    finally {
      if (ctx != null) {
        try {
          ctx.close();
        } 
        catch (Throwable t) {
        }
      }
    }
  }

  /**
   * Loescht LDAP-Objekte nach Filter
   * @param dn Startpunkt der Suche
   * @param filterAttributes Suchfilter
   * @throws NamingException
   * @throws Exception
   * @throws RemoteException
   */
  public void removeLDAPObject(String dn,Attributes filterAttributes) throws NamingException,
                                                                   Exception,
                                                                   RemoteException {
    DirContext ctx = null;
    try {
      ctx = getDirContext();
      SearchControls sc = new SearchControls();
      sc.setCountLimit(100);
      sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
      String filter = getFilter(filterAttributes);

      NamingEnumeration<SearchResult> ne = ctx.search(dn, filter, sc);
      if (ne.hasMore()) {
        SearchResult sr = ne.next();
        if (ne.hasMore()) {
          throw new LDAPException(LDAPException.ErrorCode.MULTI_ELEMENTS_FOUND,filter);
        }
        ctx.destroySubcontext(sr.getNameInNamespace());
      } 
      else {
        throw new LDAPException(LDAPException.ErrorCode.NO_ELEMENT_FOUND,filter);
      }
    } 
    finally {
      if (ctx != null) {
        try {
          ctx.close();
        } 
        catch (Throwable t) {
        }
      }
    }
  }


  /**
   * @param dn Start der Suche
   * @param filterAttributes
   * @param cntlimit
   * @throws NamingException
   */
  public void removeAllLDAPObject(String dn, Attributes filterAttributes, int cntLimit) throws NamingException {
    DirContext ctx = null;
    try {
      ctx = getDirContext();
      SearchControls sc = new SearchControls();
      sc.setCountLimit(cntLimit);
      sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
      String filter = getFilter(filterAttributes);

      NamingEnumeration<SearchResult> ne = ctx.search(dn, filter, sc);
      while (ne.hasMore()) {
        SearchResult sr = ne.next();
        ctx.destroySubcontext(sr.getNameInNamespace());
      }
    } 
    finally {
      if (ctx != null) {
        try {
          ctx.close();
        } 
        catch (Throwable t) {
        }
      }
    }
  }

  /**
   * Baut einen Filter aus Attributen
   * @param filterAttributes
   * @return
   * @throws NamingException
   */
  private static String getFilter(Attributes filterAttributes) throws NamingException {
    String res = "";
    NamingEnumeration ne = filterAttributes.getAll();
    while (ne.hasMore()) {
      Attribute a = (Attribute)ne.nextElement();
      res = res + "(" + a.getID() + "=" + a.get() + ")";
    }
    if (res.length() > 0) {
      res = "(&" + res + ")";
    }
    return res;

  }

  /**
   * Liefert einen Wert aus den Attributen
   * @param attributes 
   * @param name Schluessel zum Suchen
   * @return
   * @throws NamingException
   * @throws LDAPException
   */
  public static String getValue(Attributes attributes,
                                   String name) throws NamingException, LDAPException {
    Attribute att=attributes.get(name);                                     
    if(att==null) throw new LDAPException(LDAPException.ErrorCode.NO_LDAP_ATTRIBUTE, name);
    Object valueObject = att.get();
    if (valueObject != null) {
      return valueObject.toString();
    }
    return "";
  }

  /**
   * Fuegt den Attributen eines hinzu, wenn der Wert nicht null ist
   * @param attributes Achtung: CALL BY REFERENCE
   * @param name Name
   * @param value Wert
   */
  public static void addAttribute(Attributes attributes, 
                                  String name,
                                  Object value) {
    if (value != null) {
      if (value instanceof String) {
        attributes.put(new BasicAttribute(name, value));
      }
      else {
        attributes.put(new BasicAttribute(name+";binary", value));
      }
    }
  }

  /**
   * Fuegt einen Wert hinzu. Gibt es noch keinen wird er angelegt!
   * @param attributes Achtung: CALL BY REFERENCE
   * @param name Name
   * @param value Wert
   */
  public static void addToAttribute(Attributes attributes,
                                    String name,
                                    Object value) {
    if (value !=null && !(value instanceof String)) {
      name +=";binary";
    }
    Attribute att = attributes.get(name);
    if (att == null) {
      attributes.put(new BasicAttribute(name, value));
    } 
    else {
      att.add(value);
    }
  }
  
  /**
   * Liefert eine Komma-getrennte Liste aller Werte zum Attribut
   * @param attributes
   * @param name
   * @return
   * @throws NamingException
   * @throws LDAPException
   */
  public static String getAllValues(Attributes attributes,
                                       String name) throws NamingException, 
                                                       LDAPException {
    Attribute att=attributes.get(name);                     
    if(att==null) {
      throw new LDAPException(LDAPException.ErrorCode.NO_LDAP_ATTRIBUTE, name);
    }
    NamingEnumeration en = att.getAll();
    String res ="";
    int i=0;
    while (en.hasMore()) {
      if (i>0) {
        res += ",";
      }
      res += en.next().toString();
      i++;
    }
    return res;
  }
  
}
