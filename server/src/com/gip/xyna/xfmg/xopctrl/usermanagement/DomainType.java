/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.xfmg.xopctrl.usermanagement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XMOM.base.IP;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xopctrl.DomainTypeSpecificData;
import com.gip.xyna.xfmg.xopctrl.radius.PresharedKey;
import com.gip.xyna.xfmg.xopctrl.radius.RADIUSDomainSpecificData;
import com.gip.xyna.xfmg.xopctrl.radius.RADIUSServer;
import com.gip.xyna.xfmg.xopctrl.radius.RADIUSServerPort;
import com.gip.xyna.xfmg.xopctrl.radius.RADIUSUserAuthentication;
import com.gip.xyna.xfmg.xopctrl.usermanagement.ldap.LDAPDomainSpecificData;
import com.gip.xyna.xfmg.xopctrl.usermanagement.ldap.LDAPServer;
import com.gip.xyna.xfmg.xopctrl.usermanagement.ldap.LDAPUserAuthentication;
import com.gip.xyna.xfmg.xopctrl.usermanagement.ldap.SSLKeyAndTruststoreParameter;
import com.gip.xyna.xfmg.xopctrl.usermanagement.ldap.SSLKeystoreParameter;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;


public enum DomainType {
  
  LOCAL("Local") {

    public LocalUserAuthentication generateAuthenticationMethod(Domain domain) {
      return new LocalUserAuthentication();
    }

    public DomainTypeSpecificData generateDomainTypeSpecificData(Map<String, List<String>> specifics)
                    throws IllegalArgumentException {
      throw new UnsupportedOperationException("Can not set domain specific data on factory local domain");
    }
    
  },
  RADIUS("RADIUS") {
    
    public final static String RADIUS_SPECIFIC_ORDERTYPE_PARAMETER_IDENTIFIER = "ordertype";
    public final static String RADIUS_SPECIFIC_SERVER_PARAMETER_IDENTIFIER = "server";

    public RADIUSUserAuthentication generateAuthenticationMethod(Domain domain) {
      return new RADIUSUserAuthentication(domain);
    }
    
    public RADIUSDomainSpecificData generateDomainTypeSpecificData(Map<String, List<String>> specifics)
                    throws IllegalArgumentException {
      List<String> ordertype = specifics.get(RADIUS_SPECIFIC_ORDERTYPE_PARAMETER_IDENTIFIER);
      if (ordertype.size() > 1) {
        throw new IllegalArgumentException("Too many ordertypes!");
      } else if (ordertype.size() < 1) {
        throw new IllegalArgumentException("No ordertype for authentification!");
      }
      List<String> servers = specifics.get(RADIUS_SPECIFIC_SERVER_PARAMETER_IDENTIFIER);
      if (servers.size() < 1) {
        throw new IllegalArgumentException("No RADIUS servers for authentification!");
      }
      List<RADIUSServer> radiusServers = new ArrayList<RADIUSServer>();
      for (String server : servers) {
        // TODO only split first 3 as presharedKey might contain ',' ?
        String[] serverParts = server.split(",");
        if (serverParts.length != 3) {
          throw new IllegalArgumentException("Invalid RADIUS server format: expected <IPv4/IPv6>,<Port>,<PresharedKey>  received " + server);
        }
        String ipValue = serverParts[0];
        IP ip = IP.generateIPFromString(ipValue);
        int portValue = Integer.parseInt(serverParts[1]);
        RADIUSServerPort port = new RADIUSServerPort(portValue);
        PresharedKey key = new PresharedKey(serverParts[2]);
        radiusServers.add(new RADIUSServer(ip, port, key));
      }
      return new RADIUSDomainSpecificData(ordertype.get(0), radiusServers);
    }
  },
  LDAP("LDAP") {

    public final static String LDAP_SPECIFIC_ORDERTYPE_PARAMETER_IDENTIFIER = "ordertype";
    public final static String LDAP_SPECIFIC_WORKSPACE_PARAMETER_IDENTIFIER = "workspace";
    public final static String LDAP_SPECIFIC_APPLICATION_PARAMETER_IDENTIFIER = "application";
    public final static String LDAP_SPECIFIC_VERSION_PARAMETER_IDENTIFIER = "version";
    public final static String LDAP_SPECIFIC_SERVER_PARAMETER_IDENTIFIER = "server";
    
    public LDAPUserAuthentication generateAuthenticationMethod(Domain domain) {
      return new LDAPUserAuthentication(domain);
    }

    @Override
    public LDAPDomainSpecificData generateDomainTypeSpecificData(Map<String, List<String>> specifics)
                    throws IllegalArgumentException {
      List<String> ordertype = specifics.get(LDAP_SPECIFIC_ORDERTYPE_PARAMETER_IDENTIFIER);
      if (ordertype == null || ordertype.size() < 1) {
        throw new IllegalArgumentException("No ordertype for authentification!");
      } else if (ordertype.size() > 1) {
        throw new IllegalArgumentException("Too many ordertypes!");
      }
      long revision = -1;
      List<String> applications = specifics.get(LDAP_SPECIFIC_APPLICATION_PARAMETER_IDENTIFIER);
      List<String> versions = specifics.get(LDAP_SPECIFIC_VERSION_PARAMETER_IDENTIFIER);
      List<String> workspaces = specifics.get(LDAP_SPECIFIC_WORKSPACE_PARAMETER_IDENTIFIER);
      RuntimeContext rc = null;
      if (applications != null && applications.size() > 0 &&
          versions != null && versions.size() > 0) {
        rc = new Application(applications.get(0), versions.get(0));
      } else if (workspaces != null && workspaces.size() > 0) {
        rc = new Workspace(workspaces.get(0));
      }
      if (rc != null) {
        RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
        try {
          revision = revisionManagement.getRevision(rc);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          CentralFactoryLogging.getLogger(UserManagement.class).warn("Failed to retrieve revision for RuntimeContext " + rc, e);
          throw new RuntimeException(e);
        }
      }
      List<String> servers = specifics.get(LDAP_SPECIFIC_SERVER_PARAMETER_IDENTIFIER);
      if (servers == null || servers.size() < 1) {
        throw new IllegalArgumentException("No LDAP servers for authentification!");
      }
      List<LDAPServer> ldapServers = new ArrayList<LDAPServer>();
      for (String server : servers) {
        String[] serverParts = server.split(",");
        if (serverParts.length < 2) {
          throw new IllegalArgumentException("Invalid LDAP server format: expected at least <Host>,<Port>. Received " + server);
        }
        LDAPServer.Builder serverBuilder = new LDAPServer.Builder()
                      .host(serverParts[0])
                      .port(Integer.parseInt(serverParts[1]));
        if (serverParts.length > 3 && serverParts.length <= 5) {
          serverBuilder.sSLParameter(buildSSLKeystoreParameter(serverParts, 2));
        } else if (serverParts.length > 5) {
          serverBuilder.sSLParameter(new SSLKeyAndTruststoreParameter.Builder()
                                           .sSLKeystore(buildSSLKeystoreParameter(serverParts, 2))
                                           .sSLTruststore(buildSSLKeystoreParameter(serverParts, 5)).instance());
        }
        ldapServers.add(serverBuilder.instance());
      }
      return new LDAPDomainSpecificData(ordertype.get(0), revision, ldapServers);
    }
    
    private SSLKeystoreParameter buildSSLKeystoreParameter(String[] args, int index) {
      SSLKeystoreParameter.Builder sslBuilder = new SSLKeystoreParameter.Builder()
                          .path(args[index + 0])
                          .type(args[index + 1]);
      if (args.length > index + 2) {
        sslBuilder.passphrase(args[index + 2]);
      }
      return sslBuilder.instance();
    }
  };
  


  private final String name;
  
  private DomainType(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }

  
  public String getName() {
    return name;
  }

  public static DomainType valueOfNiceString(String name) {
    for (DomainType domain : values()) {
      if (domain.getName().equalsIgnoreCase(name)) {
        return domain;
      }
    }
    throw new IllegalArgumentException("Domain type '" + name + "' is not known.");
  }
  
  
  public abstract UserAuthentificationMethod generateAuthenticationMethod(Domain domain);
  
  public abstract DomainTypeSpecificData generateDomainTypeSpecificData(Map<String, List<String>> specifics) throws IllegalArgumentException;

}
