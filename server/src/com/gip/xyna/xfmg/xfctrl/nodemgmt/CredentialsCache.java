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
package com.gip.xyna.xfmg.xfctrl.nodemgmt;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.exceptions.XFMG_NodeConnectException;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.IPropertyChangeListener;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyDuration;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionCredentials;
import com.gip.xyna.xfmg.xopctrl.usermanagement.User;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaCredentials;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaPlainSessionCredentials;
import com.gip.xyna.xfmg.xopctrl.usermanagement.XynaUserCredentials;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;


public class CredentialsCache implements IPropertyChangeListener {
  
  // TODO property name is remoteCall specific
  private static final XynaPropertyString userNameProperty = new XynaPropertyString("xfmg.xfctrl.nodemgmt.remotecall.user", "XYNAADMIN")
                  .setDefaultDocumentation(DocumentationLanguage.EN, "Name of User to use for Remote Calls. Must be defined locally as well.")
                  .setDefaultDocumentation(DocumentationLanguage.DE, "User-Name für Remote-Calls. (Muss auch lokal definiert sein)")
                  .setHidden(true);

  private static final XynaPropertyDuration minOffsetForSessionDeletion = new XynaPropertyDuration("xfmg.xfctrl.nodemgmt.interlink.sessioncache.deletion.minage", "5 s").
      setDefaultDocumentation(DocumentationLanguage.EN, "Minimum age of interlink session to be eligible for removal when detecting an interlink disconnect.");

  
  private static final ArrayList<String> props = new ArrayList<String>();
  static {
    props.add(userNameProperty.getPropertyName());
  }
  
  private static class Session {
    private final XynaPlainSessionCredentials creds;
    private final long age;
    
    public Session(XynaPlainSessionCredentials creds, long age) {
      this.creds = creds;
      this.age = age;
    }
  }
  
  private Map<String, XynaUserCredentials> credsPerNode = new ConcurrentHashMap<String, XynaUserCredentials>();
  private Map<String, Session> sessionsPerNode = new ConcurrentHashMap<String, Session>();
  private Map<String, Object> locksPerNode = new ConcurrentHashMap<String, Object>(); 
  
  //deprecated nur für externe verwendung. es soll das singleton verwendet werden!
  @Deprecated
  public CredentialsCache() {
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration().addPropertyChangeListener(this);
  }
  
  private static CredentialsCache instance;
  

  public static synchronized CredentialsCache getInstance() {
    if (instance == null) {
      instance = new CredentialsCache();
    }
    return instance;
  }
  

  private void ensureLockIsPresent(String nodeName) {
    if (!locksPerNode.containsKey(nodeName)) {
      synchronized (this) {
        if (!locksPerNode.containsKey(nodeName)) {
          locksPerNode.putIfAbsent(nodeName, new Object());
        }
      }
    }
  }

  public XynaCredentials getCredentials(String nodeName, InfrastructureLinkProfile infrastructure) throws XFMG_NodeConnectException {
    ensureLockIsPresent(nodeName);
    synchronized (locksPerNode.get(nodeName)) {
      Session session = sessionsPerNode.get(nodeName);
      if (session == null) {
        XynaUserCredentials creds = credsPerNode.get(nodeName);
        if (creds == null) {
          creds = getXynaUserCredentials(nodeName);
          credsPerNode.put(nodeName, creds);
        }
        SessionCredentials createdSession = infrastructure.createSession(creds.getUserName(), creds.getPassword());
        session = new Session(new XynaPlainSessionCredentials(createdSession.getSessionId(), createdSession.getToken()),
                              System.currentTimeMillis());
        sessionsPerNode.put(nodeName, session);
      }
      return session.creds;
    }
  }
  
  public XynaCredentials getCredentialsIfPresent(String nodeName) {
    ensureLockIsPresent(nodeName);
    synchronized(locksPerNode.get(nodeName)) {
      Session session = sessionsPerNode.get(nodeName);
      if(session == null) {
        return null;
      }
      return session.creds;
    }
  }
  
  
  public void clearSession(String nodeName) {
    ensureLockIsPresent(nodeName);
    synchronized (locksPerNode.get(nodeName)) {
      Session session = sessionsPerNode.get(nodeName);
      if (session != null && System.currentTimeMillis() - session.age >= minOffsetForSessionDeletion.getMillis()) {
        //nicht die session entfernen, wenn sie gerade erst neu gemacht wurde (das passiert, wenn mehrere threads grob gleichzeitig einen fehler sehen)
        //TODO schöner wäre es, wenn stattdessen alle interlink-verwender einfach konsistenz die session-fehler behandeln, d.h. wenn das alles über
        //einen gemeinsamen codepfad gehen würde
        sessionsPerNode.remove(nodeName);
      }
    }
  }

  private XynaUserCredentials getXynaUserCredentials(String nodeName) {
    //TODO username pro factorynode
    String userName = userNameProperty.get();
    String password = (String) XynaFactory.getInstance().getXynaMultiChannelPortal().getSecureStorage().retrieve("remotecall", nodeName + "." + userName);
    if (password == null) {
      password = (String) XynaFactory.getInstance().getXynaMultiChannelPortal().getSecureStorage().retrieve("remotecall", userName);
      if (password == null) {
        try {
          User user = XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getUserManagement().getUser(userName);
          if (user == null) {
            throw new RuntimeException("User " + userName + " not found");  
          }
          password = user.getPassword();
        } catch (PersistenceLayerException e) {
          throw new RuntimeException("User " + userName + " not found", e);
        }
      }
    }
    return new XynaUserCredentials(userName, password);
  }

  public ArrayList<String> getWatchedProperties() {
    return props;
  }

  public void propertyChanged() {
    credsPerNode.clear();
  }
  
  public void shutdown() {
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration().removePropertyChangeListener(this);
  }
  
}