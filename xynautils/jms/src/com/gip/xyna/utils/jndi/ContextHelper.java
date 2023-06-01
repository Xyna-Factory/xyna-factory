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
package com.gip.xyna.utils.jndi;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.LinkRef;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.apache.log4j.Logger;

/**
 * klasse zum einfacheren und sicheren erstellen von initialcontext.<br>
 * falls man den context des eigenen containers haben möchte, benötigt man diese helperklasse
 * nicht, sondern kann einfach <code>new InitialContext();</code> benutzen.<br>
 * benutzungsbeispiel:<br>
 * <code>
 *    ContextHelper ch = new ContextHelper();<br>
      ch.setContextFactory(ContextHelper.ContextFactoryType.REMOTE_CLIENT);<br>
      ch.setOC4JUserName("oc4jadmin");<br>
      ch.setOC4JPassword("o8lberich");<br>
      ch.setProviderUrl("gipsun173", 6003, "oc4j_soa");<br>
      InitialContext ctx = ch.getInitialContext();<br>
 * </code>
 * <p>
 * hier kann man auch was nachlesen:
 * http://download.oracle.com/docs/cd/B32110_01/web.1013/b28958/jndi.htm#BGBICBCG
 */
public class ContextHelper {
  public ContextHelper() {
  }

  /**
   * enumeration für die unterschiedlichen standard context factory types (oracle). <br>
   * OAS_CLIENT = oracle.j2ee.naming.ApplicationClientInitialContextFactory zu benutzen, wenn die
   * klasse im OAS lebt.<br>
   * REMOTE_CLIENT = oracle.j2ee.rmi.RMIInitialContextFactory zu benutzen, wenn die klasse von ausserhalb
   * des OAS aufgerufen wird.
   */
  public enum ContextFactoryType {

    OAS_CLIENT("oracle.j2ee.naming.ApplicationClientInitialContextFactory"),
    REMOTE_CLIENT("oracle.j2ee.rmi.RMIInitialContextFactory"),
    ;

    private String cf;

    ContextFactoryType(String cf) {
      this.cf = cf;
    }

    public String getContextFactory() {
      return cf;
    }
  }

  private Hashtable<String, String> env = new Hashtable<String, String>();

  /**
   * setzt (oder löscht bei null) den value für key Context.SECURITY_PRINCIPAL
   * @param user
   */
  public void setOC4JUserName(String user) {
    addEntry(Context.SECURITY_PRINCIPAL, user);
  }

  /**
   * setzt (oder löscht bei null) den value für key Context.SECURITY_CREDENTIALS
   * @param password
   */
  public void setOC4JPassword(String password) {
    addEntry(Context.SECURITY_CREDENTIALS, password);
  }

  /**
   * setzt (oder löscht bei null) den value für key Context.INITIAL_CONTEXT_FACTORY
   * @param cf
   */
  public void setContextFactory(ContextFactoryType cf) {
    setContextFactory(cf.getContextFactory());
  }

  /**
   * setzt (oder löscht bei null) den value für key Context.INITIAL_CONTEXT_FACTORY
   * @param cf
   */
  public void setContextFactory(String cf) {
    addEntry(Context.INITIAL_CONTEXT_FACTORY, cf);
  }

  /**
   * setzt (oder löscht bei null) den value für key Context.PROVIDER_URL
   * @param providerUrl
   */
  public void setProviderUrl(String providerUrl) {
    addEntry(Context.PROVIDER_URL, providerUrl);
  }

  /**
   * setzt (oder löscht bei null) den value für key Context.PROVIDER_URL im format <br>
   * opmn:ormi://hostname:opmnport:oc4jname
   * @param hostname
   * @param opmnPort
   * @param oc4jName
   */
  public void setProviderUrl(String hostname, int opmnPort, String oc4jName) {
    setProviderUrl(buildProviderUrl(hostname, opmnPort, oc4jName));
  }

  /**
   * alles auf einmal setzen
   * @param username
   * @param password
   * @param contextfactory
   * @param providerurl
   * @see #setProviderUrl(String)
   * @see #setOC4JUserName(String)
   * @see #setOC4JPassword(String)
   * @see #setContextFactory(String)
   */
  public void setValuesForRemoteOC4J(String username, String password, String contextfactory,
    String providerurl) {
    setOC4JUserName(username);
    setOC4JPassword(password);
    setProviderUrl(providerurl);
    setContextFactory(contextfactory);
  }

  /**
   * hilfsmethode zum bauen der providerurl
   * @param hostname
   * @param opmnPort
   * @param oc4jName
   * @return
   */
  public static String buildProviderUrl(String hostname, int opmnPort, String oc4jName) {
    return "opmn:ormi://" + hostname + ":" + opmnPort + ":" + oc4jName;
  }

  /**
   * möglichkeit zusätzliche parameter zu setzen
   * @param key
   * @param value
   */
  public void setKeyValuePair(String key, String value) {
    addEntry(key, value);
  }

  /**
   * holt den initialcontext mit den vorher gesetzten parametern
   * @return
   * @throws NamingException
   */
  public InitialContext getInitialContext() throws NamingException {
    return new InitialContext(env);
  }

  private void addEntry(String key, String value) {
    if (value != null) {
      env.put(key, value);
    } else {
      env.remove(key);
    }
  }

  /**
   * loggt den Context Baum
   * @param indent zusätzliche einrückung
   * @param ctx der zu debuggende context
   * @param start start-knoten (zb. java:comp/env)
   * @param logger hier wird hingelogt
   * @throws NamingException
   */
  public static void showTree(String indent, Context ctx, String start,
    Logger logger) throws NamingException {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    NamingEnumeration enu = ctx.list(start);
    while (enu.hasMoreElements()) {
      NameClassPair ncp = (NameClassPair)enu.next();
      String name = ncp.getName();
      logger.debug(indent + " +- " + name);
      boolean recursive = false;
      boolean isLinkRef = false;
      try {
        Class c = loader.loadClass(ncp.getClassName());
        if (Context.class.isAssignableFrom(c))
          recursive = true;
        if (LinkRef.class.isAssignableFrom(c))
          isLinkRef = true;
      } catch (ClassNotFoundException cnfe) {
      }

      if (isLinkRef) {
        try {
          LinkRef link = (LinkRef)ctx.lookupLink((start.length() > 0 ? start + "/" : "") + name);
          logger.debug("[link -> ");
          logger.debug(link.getLinkName());
          logger.debug(']');
        } catch (Throwable e) {
          logger.debug("failed", e);
          logger.debug("[invalid]");
        }
      }
      logger.debug("");

      if (recursive) {
        try {
          Object value = ctx.lookup((start.length() > 0 ? start + "/" : "") + name);
          if (value instanceof Context) {
            Context subctx = (Context)value;
            showTree(indent + " |  ", subctx, "", logger);
          } else {
            logger.debug(indent + " |   NonContext: " + value);
          }
        } catch (Throwable t) {
          logger.debug("Failed to lookup: " + name + ", errmsg=" + t.getMessage());
        }
      }

    }
  }

}
