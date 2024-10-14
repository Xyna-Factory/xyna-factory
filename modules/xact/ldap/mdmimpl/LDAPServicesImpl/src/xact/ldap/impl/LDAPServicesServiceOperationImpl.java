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

import base.Credentials;
import base.Host;
import base.Port;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.gip.xyna.xfmg.xopctrl.usermanagement.ldap.SSLKeyAndTruststoreParameter;
import com.gip.xyna.xfmg.xopctrl.usermanagement.ldap.SSLKeystoreParameter;
import com.gip.xyna.xfmg.xopctrl.usermanagement.ldap.SSLParameter;
import com.gip.xyna.xfmg.xopctrl.usermanagement.ldap.TrustEveryone;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xfractwfe.base.ChildOrderStorage;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPExtendedOperation;
import com.novell.ldap.LDAPJSSESecureSocketFactory;
import com.novell.ldap.LDAPSearchResults;
import com.novell.ldap.LDAPSocketFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.IllegalArgumentException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

import javax.naming.ldap.Rdn;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.log4j.Logger;

import xact.ldap.DeleteOldRDN;
import xact.ldap.DeleteRecursivly;
import xact.ldap.LDAPAttribute;
import xact.ldap.LDAPBaseObjectclass;
import xact.ldap.LDAPCompareMatch;
import xact.ldap.LDAPCompareMiss;
import xact.ldap.LDAPCompareResult;
import xact.ldap.LDAPControl;
import xact.ldap.LDAPDataNode;
import xact.ldap.LDAPDistinguishedName;
import xact.ldap.LDAPExtendedRequest;
import xact.ldap.LDAPExtendedResponse;
import xact.ldap.LDAPModification;
import xact.ldap.LDAPMultiValueAttribute;
import xact.ldap.LDAPRelativeDistinguishedName;
import xact.ldap.LDAPSearchResult;
import xact.ldap.LDAPSession;
import xact.ldap.LDAPSingleValueAttribute;
import xact.ldap.SearchResultScope;
import xact.ldap.dictionary.LDAPDictionaryProvider;
import xact.ldap.dictionary.LDAPObjectClassDictionaryEntry;
import xact.ldap.dictionary.LDAPSchemaDictionary;
import xact.ldap.dictionary.ObjectClassType;
import xact.ldap.exceptions.AuthenticationFault;
import xact.ldap.exceptions.ConnectionFault;
import xact.ldap.exceptions.ControlNotSupported;
import xact.ldap.exceptions.ExtendedOperationNotSupported;
import xact.ldap.exceptions.InsufficientAccessRights;
import xact.ldap.exceptions.InvalidDNSyntax;
import xact.ldap.exceptions.NamingViolation;
import xact.ldap.exceptions.NoSuchObject;
import xact.ldap.exceptions.NotAllowedOnNonLeaf;
import xact.ldap.exceptions.ObjectClassViolation;
import xact.ldap.exceptions.ObjectDoesAlreadyExist;
import xact.ldap.impl.LDAPDataNodeTraversalAndVisitors.LDAPAttributeSetGenerationVisitor;
import xact.ldap.impl.LDAPDataNodeTraversalAndVisitors.LDAPFilterBuilderVisitor;
import xact.ldap.impl.LDAPDataNodeTraversalAndVisitors.XynaObjectFillingVisitor;
import xact.ldap.LDAPServicesServiceOperation;


public class LDAPServicesServiceOperationImpl implements ExtendedDeploymentTask, LDAPServicesServiceOperation {
  
  
  final static String OBJECTCLASS_ATTRIBUTE = "objectClass";
  final static String OBJECTCLASS_ATTRIBUTE_VALUE_FOR_TOP = "top";

  private final static XynaPropertyString DEFAULT_HOST = new XynaPropertyString("xact.ldap.defaultHost", null, true)
    .setDefaultDocumentation(DocumentationLanguage.DE, "Der Standard-Host gegen denn eine LDAP-Verbindung aufgebaut wird wenn kein anderer Host angegeben wurde.")
    .setDefaultDocumentation(DocumentationLanguage.EN, "The default host used for connection establishment if no host was given.");
  private final static XynaPropertyInt DEFAULT_PORT = new XynaPropertyInt("xact.ldap.defaultPort", 389)
    .setDefaultDocumentation(DocumentationLanguage.DE, "Der Standard-Port über denn eine LDAP-Verbindung aufgebaut wird wenn kein anderer Port angegeben wurde.")
    .setDefaultDocumentation(DocumentationLanguage.EN, "The default port used for connection establishment if no port was given.");
  private final static XynaPropertyString DEFAULT_USER = new XynaPropertyString("xact.ldap.defaultUser", "")
    .setDefaultDocumentation(DocumentationLanguage.DE, "Der Standard-Benutzer mit dem eine LDAP-Verbindung aufgebaut wird wenn kein anderer Benutzer angegeben wurde.")
    .setDefaultDocumentation(DocumentationLanguage.EN, "The default user used for connection establishment if no user was given.");
  private final static XynaPropertyString DEFAULT_PASSWORD = new XynaPropertyString("xact.ldap.defaultPassword", "")
    .setDefaultDocumentation(DocumentationLanguage.DE, "Das Standard-Passwort mit dem eine LDAP-Verbindung aufgebaut wird wenn kein anderes Passwort angegeben wurde.")
    .setDefaultDocumentation(DocumentationLanguage.EN, "The default password used for connection establishment if no password was given.");

  private Map<Long, LDAPConnection> connectionCache;
  private AtomicLong sessionIdGenerator;
  private Charset utf8Charset;
  
  static final Logger logger = CentralFactoryLogging.getLogger(LDAPServicesServiceOperationImpl.class);
  
  static final XynaPropertyBoolean includeObjectClassesInSearchFilters = new XynaPropertyBoolean("xact.ldap.services.search.filterOnObjectclasses", true)
    .setDefaultDocumentation(DocumentationLanguage.DE, "Steuert ob die Objectclasses des Filter-Objektes im LDAP-Filter inkludiert werden.")
    .setDefaultDocumentation(DocumentationLanguage.EN, "Controls wether the objectclasses of the filter node will be included in the LDAP filter.");
  

  public void onDeployment() {
    connectionCache = new HashMap<Long, LDAPConnection>();
    sessionIdGenerator = new AtomicLong(0);
    utf8Charset = Charset.forName("UTF-8");
    DEFAULT_HOST.registerDependency("LDAPServices");
    DEFAULT_PORT.registerDependency("LDAPServices");
    DEFAULT_USER.registerDependency("LDAPServices");
    DEFAULT_PASSWORD.registerDependency("LDAPServices");
  }

  public void onUndeployment() {
    for (LDAPConnection ldapConnection : connectionCache.values()) {
      try {
        ldapConnection.disconnect();
      } catch (Throwable t) {
        logger.debug("Received error while trying to close all cached connections on undeployment", t);
      }
    }
  }


  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.;
    return null;
  }


  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return BehaviorAfterOnUnDeploymentTimeout.IGNORE;
  }


  public void lDAPActivateControls(LDAPSession session, List<? extends LDAPControl> controls) throws ControlNotSupported {
    LDAPConnection con = getConnection(session);
    for (LDAPControl ldapControl : controls) {
      try {
        if (!LDAPRootDSEDataProvider.isControlSupported(ldapControl.getOid(), con)) {
          throw new ControlNotSupported("Discovery did not list the control " + ldapControl.getOid() + " as supported.",
                                        LDAPExceptionEnum.CONTROL_NOT_FOUND.getLdapExceptionCode());
        }
      } catch (LDAPRootDSEDataProvider.LDAPRootDSEDiscoveryFailed e) {
        throw new ControlNotSupported(e.getMessage(),
                                      LDAPExceptionEnum.CONTROL_NOT_FOUND.getLdapExceptionCode());
      }
    }
    List<com.novell.ldap.LDAPControl> newControls = new ArrayList<com.novell.ldap.LDAPControl>(Arrays.asList(con.getConstraints().getControls()));
    for (xact.ldap.LDAPControl ldapControl : controls) {
      int index = findControl(ldapControl, newControls);
      if (index >= 0) {
        newControls.set(index, new com.novell.ldap.LDAPControl(ldapControl.getOid(), ldapControl.getCriticality(), asBytes(ldapControl.getControlValue())));
      } else {
        newControls.add(new com.novell.ldap.LDAPControl(ldapControl.getOid(), ldapControl.getCriticality(), asBytes(ldapControl.getControlValue())));
      }
    }
    con.getConstraints().setControls(newControls.toArray(new com.novell.ldap.LDAPControl[newControls.size()]));
  }


  public LDAPDistinguishedName lDAPAdd(LDAPSession session, LDAPDistinguishedName lDAPDistinguishedName, LDAPDataNode lDAPDataNode) throws ObjectDoesAlreadyExist, InvalidDNSyntax, NamingViolation, ObjectClassViolation {
    LDAPConnection con = getConnection(session);
    return persistLdapTree(con, lDAPDistinguishedName.getDistinguishedName(), lDAPDataNode);
  }


  public LDAPSession lDAPBind(Host host, Port port, Credentials credentials) throws AuthenticationFault, ConnectionFault {
    LDAPConnection con = createConnection();
    return lDAPBindImpl(host, port, credentials, con);
  }


  public LDAPSession lDAPBindSSL(Host host, Port port, Credentials credentials, SSLParameter sSLParameter) throws AuthenticationFault, ConnectionFault {
    LDAPConnection con = createConnection(sSLParameter);
    return lDAPBindImpl(host, port, credentials, con);
  }
  

  public LDAPCompareResult lDAPCompare(LDAPSession session, LDAPDistinguishedName dn, LDAPAttribute attribute) throws InvalidDNSyntax, NoSuchObject {
    LDAPConnection con = getConnection(session);
    try {
      boolean result = con.compare(dn.getDistinguishedName(), convertXynaObjectToCorrespondingLDAPAttribute(attribute));
      if (result) {
        return new LDAPCompareMatch();
      } else {
        return new LDAPCompareMiss();
      }
    } catch (LDAPException e) {
      Throwable xynaOrRuntime = LDAPExceptionEnum.transformLDAPExceptionToAppropriateXynaException(e);
      if (xynaOrRuntime instanceof xact.ldap.exceptions.LDAPException ) {
        LDAPServicesServiceOperationImpl.<InvalidDNSyntax>throwAs(xynaOrRuntime);
        LDAPServicesServiceOperationImpl.<NoSuchObject>throwAs(xynaOrRuntime);
        throw new RuntimeException("Unexpected failure", xynaOrRuntime);
      } else {
        throw (RuntimeException)xynaOrRuntime;
      }
    }
  }

  public void lDAPDeactivateControls(LDAPSession session, List<? extends LDAPControl> controls) {
    LDAPConnection con = getConnection(session);
    List<com.novell.ldap.LDAPControl> newControls = new ArrayList<com.novell.ldap.LDAPControl>(Arrays.asList(con.getConstraints().getControls()));
    for (LDAPControl ldapControl : controls) {
      int index = findControl(ldapControl, newControls);
      if (index >= 0) {
        newControls.remove(index);
      }
    }
    con.getConstraints().setControls(newControls.toArray(new com.novell.ldap.LDAPControl[newControls.size()]));
  }


  public void lDAPDelete(LDAPSession session, LDAPDistinguishedName lDAPDistinguishedName, DeleteRecursivly deleteRecursivly) throws NotAllowedOnNonLeaf, NoSuchObject, InvalidDNSyntax, InsufficientAccessRights {
    LDAPConnection con = getConnection(session);
    try {
      if (deleteRecursivly.getDeleteRecursivly()) {
        SortedSet<String> allDns = getAllAffectedDNsInHierarchicalOrder(con, lDAPDistinguishedName.getDistinguishedName());
        for (String dn : allDns) {
          con.delete(dn);
        }
      } else {
        con.delete(lDAPDistinguishedName.getDistinguishedName());
      }
    } catch (LDAPException e) {
      Throwable xynaOrRuntime = LDAPExceptionEnum.transformLDAPExceptionToAppropriateXynaException(e);
      if (xynaOrRuntime instanceof xact.ldap.exceptions.LDAPException ) {
        LDAPServicesServiceOperationImpl.<InvalidDNSyntax>throwAs(xynaOrRuntime);
        LDAPServicesServiceOperationImpl.<NotAllowedOnNonLeaf>throwAs(xynaOrRuntime);
        LDAPServicesServiceOperationImpl.<NoSuchObject>throwAs(xynaOrRuntime);
        LDAPServicesServiceOperationImpl.<InsufficientAccessRights>throwAs(xynaOrRuntime);
        throw new RuntimeException("Unexpected failure", xynaOrRuntime);
      } else {
        throw (RuntimeException)xynaOrRuntime;
      }
    }
  }


  public Container lDAPExtendedOperation(LDAPSession session, LDAPExtendedRequest request) throws ExtendedOperationNotSupported {
    LDAPConnection con = getConnection(session);
    LDAPExtendedOperation op = new LDAPExtendedOperation(request.getRequestName(), asBytes(request.getRequestValue()));
    try {
      com.novell.ldap.LDAPExtendedResponse response = con.extendedOperation(op);
      return new Container(new LDAPExtendedResponse(response.getID(), asString(response.getValue())),
                           new XynaObjectList<xact.ldap.LDAPControl>(xact.ldap.LDAPControl.class));
    } catch (LDAPException e) {
      Throwable xynaOrRuntime = LDAPExceptionEnum.transformLDAPExceptionToAppropriateXynaException(e);
      if (xynaOrRuntime instanceof ExtendedOperationNotSupported) {
        throw (ExtendedOperationNotSupported)xynaOrRuntime;
      } else if (xynaOrRuntime instanceof RuntimeException) {
        throw (RuntimeException)xynaOrRuntime;
      } else {
        throw new RuntimeException("Unexpected failure",xynaOrRuntime);
      }
    }
  }
  
  
  public void lDAPModify(LDAPSession session, LDAPDistinguishedName dn, List<? extends LDAPModification> modifications) throws InvalidDNSyntax, NoSuchObject, ObjectDoesAlreadyExist, ObjectClassViolation {
    LDAPConnection con = getConnection(session);
    try {
      com.novell.ldap.LDAPModification[] ldapModifications = new com.novell.ldap.LDAPModification[modifications.size()];
      for (int i = 0; i < modifications.size(); i++) {
        LDAPModification xynaModification = modifications.get(i);
        LDAPModificationOperationEnum modificationOperation = LDAPModificationOperationEnum.getLDAPSearchScopeByInstance(xynaModification.getModificationOperation());
        com.novell.ldap.LDAPAttribute attrib = convertXynaObjectToCorrespondingLDAPAttribute(xynaModification.getLDAPAttribute());
        ldapModifications[i] = new com.novell.ldap.LDAPModification(modificationOperation.getValueForModification(), attrib);
      }
      con.modify(dn.getDistinguishedName(), ldapModifications);
    } catch (LDAPException e) {
      Throwable xynaOrRuntime = LDAPExceptionEnum.transformLDAPExceptionToAppropriateXynaException(e);
      if (xynaOrRuntime instanceof xact.ldap.exceptions.LDAPException ) {
        LDAPServicesServiceOperationImpl.<InvalidDNSyntax>throwAs(xynaOrRuntime);
        LDAPServicesServiceOperationImpl.<NoSuchObject>throwAs(xynaOrRuntime);
        LDAPServicesServiceOperationImpl.<ObjectDoesAlreadyExist>throwAs(xynaOrRuntime);
        LDAPServicesServiceOperationImpl.<ObjectClassViolation>throwAs(xynaOrRuntime);
        throw new RuntimeException("Unexpected failure", xynaOrRuntime);
      } else {
        throw (RuntimeException)xynaOrRuntime;
      }
    }
  }


  public LDAPDataNode lDAPRead(LDAPSession session, LDAPDistinguishedName lDAPDistinguishedName) throws InvalidDNSyntax, NoSuchObject {
    LDAPConnection con = getConnection(session);
    try {
      LDAPEntry entry = con.read(lDAPDistinguishedName.getDistinguishedName());
      LDAPDataNode ret = instantiateSingleEntryFromLDAPEntry(entry);
      return ret;
    } catch (LDAPException e) {
      Throwable xynaOrRuntime = LDAPExceptionEnum.transformLDAPExceptionToAppropriateXynaException(e);
      if (xynaOrRuntime instanceof xact.ldap.exceptions.LDAPException ) {
        LDAPServicesServiceOperationImpl.<InvalidDNSyntax>throwAs(xynaOrRuntime);
        LDAPServicesServiceOperationImpl.<NoSuchObject>throwAs(xynaOrRuntime);
        throw new RuntimeException("Unexpected failure", xynaOrRuntime);
      } else {
        throw (RuntimeException)xynaOrRuntime;
      }
    }
  }


  public void lDAPRename(LDAPSession session, LDAPDistinguishedName dn, LDAPRelativeDistinguishedName rdn, DeleteOldRDN deleteOldRdn) throws InvalidDNSyntax, NoSuchObject {
    LDAPConnection con = getConnection(session);
    try {
      con.rename(dn.getDistinguishedName(), rdn.getRelativeDistinguishedName(), deleteOldRdn.getDeleteOldRdn());
    } catch (LDAPException e) {
      Throwable xynaOrRuntime = LDAPExceptionEnum.transformLDAPExceptionToAppropriateXynaException(e);
      if (xynaOrRuntime instanceof xact.ldap.exceptions.LDAPException ) {
        LDAPServicesServiceOperationImpl.<InvalidDNSyntax>throwAs(xynaOrRuntime);
        LDAPServicesServiceOperationImpl.<NoSuchObject>throwAs(xynaOrRuntime);
        throw new RuntimeException("Unexpected failure", xynaOrRuntime);
      } else {
        throw (RuntimeException)xynaOrRuntime;
      }
    }
  }


  public Container lDAPSearch(LDAPSession session, LDAPDistinguishedName lDAPDistinguishedName, LDAPDataNode lDAPDataNode, SearchResultScope searchResultScope) throws InvalidDNSyntax {
    // TODO how and what controls do we want to return from a search?
    SearchResultScopeEnum searchResultScopeEnum = SearchResultScopeEnum.getLDAPSearchScopeByInstance(searchResultScope);
    LDAPConnection con = getConnection(session);
    List<Pair<LDAPDataNode, String>> workingResult = ldapTreeBottomUpSearch(con, lDAPDistinguishedName.getDistinguishedName(), lDAPDataNode);
    int depth = -1;
    switch (searchResultScopeEnum) {
      case ROOT:
        break;
      case FILTEDEPTH:
        depth = getFilterDepth(lDAPDataNode);
        // fall through
      case RECURSIVE:
        for (Pair<LDAPDataNode, String> pair : workingResult) {
          appendToDepth(depth, con, pair.getSecond(), pair.getFirst());
        }
        break;
    }
    List<LDAPSearchResult> results = new ArrayList<LDAPSearchResult>();
    for (Pair<LDAPDataNode, String> result : workingResult) {
      results.add(new LDAPSearchResult(new LDAPDistinguishedName(result.getSecond()), result.getFirst()));
    }
    return new Container(new XynaObjectList<xact.ldap.LDAPSearchResult>(results, xact.ldap.LDAPSearchResult.class),
                         new XynaObjectList<xact.ldap.LDAPControl>(xact.ldap.LDAPControl.class));
  }


  public void lDAPUnbind(LDAPSession session) {
    LDAPConnection con = getConnection(session);
    try {
      con.disconnect();
    } catch (LDAPException e) {
      throw new RuntimeException("Unexpected failure",e);
    } finally {
      connectionCache.remove(session.getSessionIdentifier());
    }
  }
  

  private static LDAPDistinguishedName persistLdapTree(LDAPConnection con, String dn, LDAPDataNode lDAPDataNode)
                                       throws ObjectDoesAlreadyExist, InvalidDNSyntax, NamingViolation, ObjectClassViolation {
    LDAPEntry entry = convertLDAPNodeToLDAPEntry(dn, lDAPDataNode); 
    try {
      con.add(entry);
      List<? extends LDAPDataNode> children = lDAPDataNode.getChildNodes();
      if (children != null && children.size() > 0) {
        for (LDAPDataNode child : children) {
          persistLdapTree(con, entry.getDN(), child);
        }
      }
    } catch (LDAPException e) {
      Throwable ldapException = LDAPExceptionEnum.transformLDAPExceptionToAppropriateXynaException(e);
      if (ldapException instanceof xact.ldap.exceptions.LDAPException ) {
        LDAPServicesServiceOperationImpl.<ObjectDoesAlreadyExist>throwAs(ldapException);
        LDAPServicesServiceOperationImpl.<InvalidDNSyntax>throwAs(ldapException);
        LDAPServicesServiceOperationImpl.<NamingViolation>throwAs(ldapException);
        LDAPServicesServiceOperationImpl.<ObjectClassViolation>throwAs(ldapException);
        throw new RuntimeException("Unexpected failure", ldapException);
      } else {
        throw (RuntimeException)ldapException;
      }
    }
    return new LDAPDistinguishedName(entry.getDN());
  }

  
  static LDAPObjectClassDictionaryEntry resolveXynaObjectFromDictionary(XynaObject xynaObject) {
    String xynaObjectName = xynaObject.getClass().getCanonicalName();
    return LDAPDictionaryProvider.getInstance().getDictionary().lookupByXynaObjectName(xynaObjectName);
  }

  
  private static LDAPEntry convertLDAPNodeToLDAPEntry(String dn, LDAPDataNode lDAPDataNode) {
    LDAPAttributeSetGenerationVisitor visitor = new LDAPAttributeSetGenerationVisitor(lDAPDataNode.getLDAPRelativeDistinguishedName().getRelativeDistinguishedName());
    LDAPDataNodeTraversalAndVisitors.traverseLDAPNodeHierarchy(lDAPDataNode, visitor);
    String fullDn = escapeRdn(lDAPDataNode.getLDAPRelativeDistinguishedName().getRelativeDistinguishedName());
    if (dn != null && dn.length() > 0) {
      fullDn += "," + dn; 
    }
    LDAPAttributeSet attributes = visitor.getAttributeSet();
    return new LDAPEntry(fullDn, attributes);
  }


  @Deprecated // it looks like passing null as attributes will select all, at least there are such constructs in the source
  private static String[] getAllAttributeNamesAlongTheHierarchy(LDAPDataNode node) {
    return null;
    /*LDAPSelectAllAttributesVisitor visitor = new LDAPSelectAllAttributesVisitor();
    LDAPDataNodeTraversalAndVisitors.traverseLDAPNodeHierarchy(node, visitor);
    return visitor.getAllAttributeNames();*/
  }
  
  
  static String escapeRdn(String rdn) {
    int valueStart = rdn.indexOf('=') + 1;
    return rdn.substring(0, valueStart) + Rdn.escapeValue(rdn.substring(valueStart));
  }
  
  
  static String unescapeRdn(String rdn) {
    int valueStart = rdn.indexOf('=') + 1;
    return rdn.substring(0, valueStart) + Rdn.unescapeValue(rdn.substring(valueStart));
  }

  

  /**
   * erzeugt LDAPSocketFactory, die allen Zertifikaten blind vertraut;
   * WARNUNG: Verwendung ist sicherheitstechnisch sehr bedenklich
   */
  public static LDAPSocketFactory buildTrustAllSocketFactory() {
    TrustManager[] trustAllCerts = new TrustManager[] {
          new X509TrustManager() {
              public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                  return null;
              }
              public void checkClientTrusted(
                  java.security.cert.X509Certificate[] certs, String authType) {
                  }
              public void checkServerTrusted(
                  java.security.cert.X509Certificate[] certs, String authType) {
              }
          }
      };
    try {
      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
      return new LDAPJSSESecureSocketFactory(sc.getSocketFactory());
    }
    catch (GeneralSecurityException e) {
      //do nothing
    }
    return null;
  }



  private LDAPSession lDAPBindImpl(Host host, Port port, Credentials credentials, LDAPConnection con) throws AuthenticationFault, ConnectionFault {
    String hostName;
    if (host == null || host.getHostname() == null) {
      hostName = DEFAULT_HOST.get();
      if (hostName == null) {
        throw new RuntimeException("no host given!");
      }
    } else {
      hostName = host.getHostname();
    }

    int portValue;
    if (port == null) {
      portValue = DEFAULT_PORT.get();
    } else {
      portValue = port.getValue();
    }


    String userName;
    String password;
    if (credentials == null) {
      userName = DEFAULT_USER.get();
      password = DEFAULT_PASSWORD.get();
    } else {
      if (credentials.getPassword() == null) {
        password = DEFAULT_PASSWORD.get();
      } else {
        password = credentials.getPassword();
      }
      if (credentials.getUsername() == null) {
        userName = DEFAULT_USER.get();
      } else {
        userName = credentials.getUsername();
      }
    }

    try {
      con.connect(hostName, portValue);
      con.bind(LDAPConnection.LDAP_V3, userName, asBytes(password));
    } catch (LDAPException e) {
      Throwable xynaOrRuntime = LDAPExceptionEnum.transformLDAPExceptionToAppropriateXynaException(e);
      if (xynaOrRuntime instanceof xact.ldap.exceptions.LDAPException ) {
        LDAPServicesServiceOperationImpl.<AuthenticationFault>throwAs(xynaOrRuntime);
        LDAPServicesServiceOperationImpl.<ConnectionFault>throwAs(xynaOrRuntime);
        throw new RuntimeException("Unexpected failure", xynaOrRuntime);
      } else {
        throw (RuntimeException)xynaOrRuntime;
      }
    }
    Long id = sessionIdGenerator.incrementAndGet();
    connectionCache.put(id, con);
    return new LDAPSession(id, host, port, credentials, null);
  }


  private static SortedSet<String> getAllAffectedDNsInHierarchicalOrder(LDAPConnection con, String dn) throws LDAPException {
    LDAPSearchResults results = con.search(dn, LDAPConnection.SCOPE_SUB, "(objectClass=*)", null, true);
    SortedSet<String> allDns = new TreeSet<String>(new Comparator<String>() {
      public int compare(String o1, String o2) {
        int comp = o2.length()-o1.length();
        if (comp == 0) {
          return -1;
        } else {
          return comp;
        }
      }
    });
    while (results.hasMore()) {
      allDns.add(results.next().getDN());
    }
    return allDns;
  }



  private static int getFilterDepth(LDAPDataNode rootNode) {
    int depth = 0;
    LDAPDataNode currentNode = rootNode;
    while (currentNode != null && currentNode.getChildNodes() != null && currentNode.getChildNodes().size() > 0) {
      depth++;
      currentNode = currentNode.getChildNodes().get(0);
    }
    return depth;
  }


  private static void appendToDepth(int depth, LDAPConnection con, String dnToRoot, LDAPDataNode root) throws InvalidDNSyntax {
    if (depth == 0) {
      return;
    } else {
      List<Pair<LDAPDataNode, String>> childPairs = singleSearch(con, dnToRoot, root, "(objectClass=*)", LDAPSearchScopeEnum.ONE);
      if (childPairs.size() == 0) {
        return;
      }
      List<LDAPDataNode> children = new ArrayList<LDAPDataNode>();
      for (Pair<LDAPDataNode, String> childPair : childPairs) {
        children.add(childPair.getFirst());
        appendToDepth(depth-1, con, childPair.getSecond(), childPair.getFirst());
      }
      root.setChildNodes(children);
    }
  }



  /*
   * with bottom up we'll search for the lowest node first and then need to confirm the paths that lead to it
   * with top down we'd search the node tree in order, extend the base each time and narrow the result down
   * with out expected useCase, only having children if they contain the restricting values a bottom up search should perform better
   */
  private static List<Pair<LDAPDataNode, String>> ldapTreeBottomUpSearch(LDAPConnection con, String base, LDAPDataNode node) throws InvalidDNSyntax {
    List<? extends LDAPDataNode> children = node.getChildNodes();
    if (children == null || children.size() == 0) {
      String filterString = buildFilterFromNode(node);
      logger.info("build filterString: " + filterString);
      return singleSearch(con, base, node, filterString, LDAPSearchScopeEnum.SUBTREE);
    } else {
      List<Pair<LDAPDataNode, String>> subNodeAndDnList =  new ArrayList<Pair<LDAPDataNode,String>>();
      for (LDAPDataNode child : children) {
        subNodeAndDnList.addAll(ldapTreeBottomUpSearch(con, base, child));
      }
      if (subNodeAndDnList.size() == 0) {
        return Collections.emptyList();
      } else {
        Set<String> rdnsToEvaluate = collectRdnsFromSubResultList(subNodeAndDnList, base);
        List<String> rdnFilter = buildRdnFilter(rdnsToEvaluate);
        String filterString = buildFilterFromNode(node, rdnFilter);
        logger.info("build filterString: " + filterString);
        return singleSearch(con, base, node, filterString, LDAPSearchScopeEnum.SUBTREE);
      }
    }
  }


  private static List<Pair<LDAPDataNode, String>> singleSearch(LDAPConnection con, String base, LDAPDataNode node, String filter, LDAPSearchScopeEnum searchScope) throws InvalidDNSyntax {
    try {
      LDAPSearchResults results = con.search(base, searchScope.getValueForConnection(), filter, getAllAttributeNamesAlongTheHierarchy(node), false);
      List<Pair<LDAPDataNode, String>> nodeAndDnList = new ArrayList<Pair<LDAPDataNode, String>>();
      while (results.hasMore()) {
        try {
          LDAPEntry entry = results.next();
          nodeAndDnList.add(new Pair<LDAPDataNode, String>(instantiateSingleEntryFromLDAPEntry(entry), entry.getDN()));
        } catch (LDAPException e) {
          if (e.getResultCode() == LDAPExceptionEnum.SIZE_LIMIT_EXCEEDED.getLdapExceptionCode()) {
            break;
          } else {
            throw e;
          }
        }
      }
      return nodeAndDnList;
    } catch (LDAPException e) {
      Throwable xynaOrRuntime = LDAPExceptionEnum.transformLDAPExceptionToAppropriateXynaException(e);
      if (xynaOrRuntime instanceof InvalidDNSyntax) {
        throw (InvalidDNSyntax)xynaOrRuntime;
      } else if (xynaOrRuntime instanceof RuntimeException) {
        throw (RuntimeException)xynaOrRuntime;
      } else {
        throw new RuntimeException("Unexpected failure",xynaOrRuntime);
      }
    }
  }


  private static Set<String> collectRdnsFromSubResultList(List<Pair<LDAPDataNode, String>> nodeAndDnList, String baseDn) {
    Set<String> rdns = new HashSet<String>();
    for (Pair<LDAPDataNode, String> pair : nodeAndDnList) {
      rdns.add(dnToRdn(cutLastRdn(pair.getSecond())));
    }
    return rdns;
  }

  private static List<String> buildRdnFilter(Collection<String> rdns) {
    List<String> rdnFilter = new ArrayList<String>();
    if (rdns.size() >= 1) {
      rdnFilter.add(rdns.iterator().next());
    } else {
      StringBuilder filterBuilder = new StringBuilder("|");
      for (String rdn : rdns) {
        filterBuilder.append('(')
                     .append(rdn)
                     .append(')');
      }
      rdnFilter.add(filterBuilder.toString());
    }
    return rdnFilter;
  }



  private static String buildFilterFromNode(LDAPDataNode node) {
    LDAPFilterBuilderVisitor visitor = new LDAPFilterBuilderVisitor();
    return buildFilterFromNode(node, visitor);
  }

  private static String buildFilterFromNode(LDAPDataNode node, List<String> presetFilters) {
    LDAPFilterBuilderVisitor visitor = new LDAPFilterBuilderVisitor(presetFilters);
    return buildFilterFromNode(node, visitor);
  }


  private static String buildFilterFromNode(LDAPDataNode lDAPDataNode, LDAPFilterBuilderVisitor visitor) {
    LDAPDataNodeTraversalAndVisitors.traverseLDAPNodeHierarchy(lDAPDataNode, visitor);
    return visitor.getCompleteFilterString();
  }



  private static LDAPDataNode instantiateSingleEntryFromLDAPEntry(com.novell.ldap.LDAPEntry entry) {
    List<LDAPObjectClassDictionaryEntry> localObjectClasses = new ArrayList<LDAPObjectClassDictionaryEntry>();
    Set<LDAPObjectClassDictionaryEntry> structuralEntries = new HashSet<LDAPObjectClassDictionaryEntry>();
    com.novell.ldap.LDAPAttribute objectClassValues = entry.getAttribute("objectClass");
    for (String objectClassvalue : objectClassValues.getStringValueArray()) {
      LDAPObjectClassDictionaryEntry objectClass = LDAPDictionaryProvider.getInstance().getDictionary().
                      lookupByNameOrOid(objectClassvalue.toLowerCase());
      if (objectClass == null) {
        logger.debug("objectClass in ldap dictionary is null for objectClassvalue= " + objectClassvalue);
        continue;
      }
      
      
      if (ObjectClassType.getTypeByIdentifier(objectClass.getObjectclasstype()) == ObjectClassType.STRUCTURAL) {
        boolean alreadyContainedAsSuper = false;
        outer: for (LDAPObjectClassDictionaryEntry structuralEntry : structuralEntries) {
          for (LDAPObjectClassDictionaryEntry superClass : structuralEntry.getLDAPSuperclasses()) {
            if (superClass.getOid().equals(objectClass.getOid())) {
              alreadyContainedAsSuper = true;
              break outer;
            }
          }
        }
        if (!alreadyContainedAsSuper) {
          structuralEntries.add(objectClass);
        }
        Iterator<LDAPObjectClassDictionaryEntry> iter = structuralEntries.iterator();
        while (iter.hasNext()) {
          LDAPObjectClassDictionaryEntry structEntry = iter.next();
          for (LDAPObjectClassDictionaryEntry superClass : objectClass.getLDAPSuperclasses()) {
            if (superClass.getOid().equals(structEntry.getOid())) {
              iter.remove();
            }
          }
        }
      } else {
        localObjectClasses.add(objectClass);
      }
    }
    

    if (structuralEntries == null || structuralEntries.size() <= 0) {
      throw new RuntimeException("No structural objectClass contained in searchResult!");
    } else {
      LDAPObjectClassDictionaryEntry structuralObjectClass = structuralEntries.iterator().next();
      logger.debug("Found structuralObjectClass: " + structuralObjectClass.toString());

      LDAPDataNode node = instantiateStructuralObjectClass(structuralObjectClass, localObjectClasses);
      XynaObjectFillingVisitor visitor = new XynaObjectFillingVisitor(entry);
      LDAPDataNodeTraversalAndVisitors.traverseLDAPNodeHierarchy(node, visitor);
      // magic happened...it should now be filled
      return node;
    }
  }


  private static LDAPDataNode instantiateStructuralObjectClass(LDAPObjectClassDictionaryEntry structuralObjectClasses, List<LDAPObjectClassDictionaryEntry> localObjectClasses) {
    XynaOrderServerExtension xose = ChildOrderStorage.childOrderStorageStack.get().getCorrelatedXynaOrder().getRootOrder();
    LDAPSchemaDictionary schemaDict = LDAPDictionaryProvider.getInstance().getDictionary();
    Class<? extends XynaObject> structuralClass = schemaDict.resolveNameOrOidToXynaObjectClass(structuralObjectClasses.getProminentName(),
                                                                                                         xose.getRevision());
    LDAPDataNode node;
    try {
      node = (LDAPDataNode) structuralClass.newInstance();

      for (LDAPObjectClassDictionaryEntry ldapObjectClassDictionaryEntry : localObjectClasses) {
        
        Class<? extends XynaObject> xynaLdapBaseClass = schemaDict.resolveNameOrOidToXynaObjectClass(ldapObjectClassDictionaryEntry.getProminentName(),
                                                                                                               xose.getRevision());
        LDAPBaseObjectclass baseObjectInstance = (LDAPBaseObjectclass) xynaLdapBaseClass.newInstance();
        node.addToLocalObjectclasses(baseObjectInstance);
      }
      return node;
    } catch (Throwable t) {
      throw new RuntimeException("Could not instantiate objectclass!",t);
    }
  }

  private static String dnToRdn(String dn) {
    return dn.substring(0, dn.indexOf(","));
  }

  private static String cutLastRdn(String dn) {
    return dn.substring(dn.indexOf(",")+1);
  }





  private static int findControl(LDAPControl newControl, List<? extends com.novell.ldap.LDAPControl> curentlyActivatedControls) {
    for (int i = 0; i < curentlyActivatedControls.size(); i++) {
      com.novell.ldap.LDAPControl ldapControl = curentlyActivatedControls.get(i);
      if (ldapControl.getID().equals(newControl.getOid())) {
        return i;
      }
    }
    return -1;
  }



  private static com.novell.ldap.LDAPAttribute convertXynaObjectToCorrespondingLDAPAttribute(LDAPAttribute attrib) {
    com.novell.ldap.LDAPAttribute ldapattrib = new com.novell.ldap.LDAPAttribute(attrib.getAttributeName());
    if (attrib instanceof LDAPSingleValueAttribute) {
      ldapattrib.addValue(((LDAPSingleValueAttribute) attrib).getAttributeValue());
    } else {
      for (String value : ((LDAPMultiValueAttribute) attrib).getAttributeValue()) {
        ldapattrib.addValue(value);
      }
    }
    return ldapattrib;
  }
  
  
  private final LDAPConnection createConnection() {
    return createConnection(null);
  }
  
  
  private final LDAPConnection createConnection(SSLParameter sslParams) {
    if (sslParams == null) {
      return new LDAPConnection();
    } else {
      LDAPSocketFactory sf = null;
      if (sslParams instanceof TrustEveryone) {
        sf = buildTrustAllSocketFactory();
      } else if (sslParams instanceof SSLKeystoreParameter ||
                 sslParams instanceof SSLKeyAndTruststoreParameter) {
        SSLKeystoreParameter keyStoreParams;
        SSLKeystoreParameter trustStoreParams;
        if (sslParams instanceof SSLKeystoreParameter) {
          keyStoreParams = (SSLKeystoreParameter) sslParams;
          trustStoreParams = (SSLKeystoreParameter) sslParams;
        } else {
          SSLKeyAndTruststoreParameter keyAndTrust = (SSLKeyAndTruststoreParameter) sslParams;
          keyStoreParams = keyAndTrust.getSSLKeystore();
          trustStoreParams = keyAndTrust.getSSLTruststore();
        }
        
        SSLContext context;
        try {
          if (sslParams instanceof SSLKeystoreParameter && 
              (keyStoreParams.getType() == null || keyStoreParams.getType().equalsIgnoreCase("default"))) {
            // SSLContext.getDefault() once java 1.6
            context = SSLContext.getInstance("Default");
          } else { // TODO cache keystore by params? ks.load might cost us
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            char[] keyStorePassphrase = keyStoreParams.getPassphrase().toCharArray();
            kmf.init(buildKeyStore(keyStoreParams), keyStorePassphrase);
            
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(buildKeyStore(trustStoreParams));
            
            context = SSLContext.getInstance("TLS");
            context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
          }
        } catch (Throwable t) {
          Department.handleThrowable(t);
          throw new RuntimeException("Unexpected failure",t);
        }
        sf = new LDAPJSSESecureSocketFactory(context.getSocketFactory());
      } else {
        throw new IllegalArgumentException("Unknown SSLParameter-Type: " + (sslParams == null ? "null" : sslParams.getClass().getName()));
      }
      return new LDAPConnection(sf);
    }
  }
  
  
  private KeyStore buildKeyStore(SSLKeystoreParameter keyStoreParams) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException {
    char[] passphrase = keyStoreParams.getPassphrase().toCharArray();
    KeyStore ks = KeyStore.getInstance(keyStoreParams.getType());
    ks.load(new FileInputStream(keyStoreParams.getPath()), passphrase);
    return ks;
  }
  
  
  private static <E extends xact.ldap.exceptions.LDAPException> void throwAs(Throwable t) throws E {
    try {
      E expectedException = (E) t;
    } catch (ClassCastException e) {
      // ntbd
    }
  }
  
  
  private LDAPConnection getConnection(LDAPSession session) {
    LDAPConnection con = connectionCache.get(session.getSessionIdentifier());
    if (con == null) {
      con = createConnection(session.getSSLParameter());
      try {
        lDAPBindImpl(session.getHost(), session.getPort(), session.getCredentials(), con);
      } catch (AuthenticationFault e) {
        throw new RuntimeException("Unexpected failure",e);
      } catch (ConnectionFault e) {
        throw new RuntimeException("Unexpected failure",e);
      }
    }
    return con;
  }

  
  private final byte[] asBytes(String string) {
    if (string == null) {
      return new byte[0];
    }
    // just use getBytes(utf8Charset) once java1.6 
    try {
      return string.getBytes(utf8Charset.displayName());
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  private final String asString(byte[] bytes) {
    // just use new String(string, utf8Charset) once java1.6 
    try {
      return new String(bytes, utf8Charset.displayName());
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
  

  /*
  private final static String ALIAS_OBJECT_CLASS_NAME = "alias";
  private final static String ALIAS_TARGET_ATTRIBUTE_NAME = "aliasedObjectName";
  private final static String EXTENSIBLEOBJECT_OBJECT_CLASS_NAME = "extensibleObject";
  
  public LDAPDataNode lDAPGenerateAlias(LDAPDistinguishedName lDAPDistinguishedName) {
    Class<? extends XynaObject> aliasClass = LDAPDictionaryProvider.getInstance().getDictionary().resolveNameOrOidToXynaObjectClass(ALIAS_OBJECT_CLASS_NAME,
                                                                                                                                    LDAPServicesServiceOperationImpl.class.getClassLoader());
  
    Class<? extends XynaObject> extensibleClass = LDAPDictionaryProvider.getInstance().getDictionary().resolveNameOrOidToXynaObjectClass(EXTENSIBLEOBJECT_OBJECT_CLASS_NAME,
                                                                                                                                         LDAPServicesServiceOperationImpl.class.getClassLoader());
    try {
      LDAPDataNode aliasInstance = (LDAPDataNode) aliasClass.newInstance();
      aliasInstance.set("ldap"+ALIAS_TARGET_ATTRIBUTE_NAME, lDAPDistinguishedName.getDistinguishedName());
      LDAPBaseObjectclass extensibleAux = (LDAPBaseObjectclass) extensibleClass.newInstance();
      aliasInstance.addToLocalObjectclasses(extensibleAux);
      return aliasInstance;
    } catch (Throwable t) {
      throw new RuntimeException("Could not create Alias!",t);
    }
  }*/
  
  //public static void lDAPCommitTransaction(LDAPSession lDAPSession) throws ExtendedOperationNotSupported {
    //throw new ExtendedOperationNotSupported("Extended operation with oid 1.3.6.1.1.21.3 not supported",80);
    // remove the transactionControl from the connection.controls after the call
  //}
  
  //public static void lDAPRollbackTransaction(LDAPSession lDAPSession) throws ExtendedOperationNotSupported {
    //throw new ExtendedOperationNotSupported("Extended operation with oid 1.3.6.1.1.21.3 not supported",80);
    // remove the transactionControl from the connection.controls after the call
  //}
  
  //public static void lDAPStartTransaction(LDAPSession lDAPSession) throws ExtendedOperationNotSupported {
    //throw new ExtendedOperationNotSupported("Extended operation with oid 1.3.6.1.1.21.1 not supported",80);
    // TODO this request will return a LDAPTransactionControl, this control should be added to the connectionControls
    //      so it get's send along to every request (do we need to remove it from some request? There was a listing afaik)
  //}



}
