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
package com.gip.xyna.xfmg.xopctrl.usermanagement.ldap;


import java.io.File;
import java.util.List;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xopctrl.DomainTypeSpecificData;
import com.gip.xyna.xfmg.xopctrl.radius.RADIUSServer;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;


public class LDAPDomainSpecificData implements DomainTypeSpecificData {
  
  private static final long serialVersionUID = 7177523157271609582L;
  
  private List<LDAPServer> serverList;
  private String associatedOrdertype;
  private long revision;
  
  private transient RuntimeContext runtimeContext;
  
  
  public LDAPDomainSpecificData() {
    super();
  }
  
  
  public LDAPDomainSpecificData(String associatedOrdertype, long revision, List<LDAPServer> serverList) {
    this();
    this.associatedOrdertype = associatedOrdertype;
    this.revision = revision;
    this.serverList = serverList;
  }
  
  
  public List<LDAPServer> getServerList() {
   return serverList; 
  }

  
  public void setServerList(List<LDAPServer> serverList) {
    this.serverList = serverList; 
  }
  
  
  public String getAssociatedOrdertype() {
    return associatedOrdertype; 
   }

   
   public void setAssociatedOrdertype(String associatedOrdertype) {
     this.associatedOrdertype = associatedOrdertype; 
   }
   
   
   public long getRevision() {
     return revision; 
    }

    
    public void setRevision(long revision) {
      this.revision = revision; 
    }
    
    
    public RuntimeContext getRuntimeContext() {
      if (runtimeContext == null) {
        RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
        try {
          runtimeContext = revisionManagement.getRuntimeContext(revision);
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          CentralFactoryLogging.getLogger(UserManagement.class).warn("Failed to restore RuntimeContext for revision " + revision, e);
          try {
            return revisionManagement.getRuntimeContext(-1L);
          } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
            // should not happen
            throw new RuntimeException("Failed to resolve default revision.");
          }
        }
      }
      return runtimeContext;
    }
   
   
   public void appendInformation(StringBuilder output) {
     output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA)
           .append("AssociatedOrderType: ").append(getAssociatedOrdertype()).append(" @rev_").append(revision).append("\n");
     List<LDAPServer> servers = getServerList();
     for (LDAPServer server : servers) {
       output.append(UserManagement.INDENT_FOR_DOMAIN_SPECIFIC_DATA)
             .append(server.getHost()).append(":").append(server.getPort());
       if (server.getSSLParameter() != null) {
         if (server.getSSLParameter() instanceof TrustEveryone) {
           output.append(" - TrustEveryone");
         } else if (server.getSSLParameter() instanceof SSLKeystoreParameter) {
           SSLKeystoreParameter keystore = (SSLKeystoreParameter) server.getSSLParameter();
           output.append(" - ");
           int lastPathSeperator = keystore.getPath().lastIndexOf(File.separatorChar);
           if (lastPathSeperator > 0) {
             output.append(keystore.getPath().substring(lastPathSeperator));
           } else {
             output.append(keystore.getPath());
           }
           output.append(" [").append(keystore.getType()).append("]");
         } else {
           output.append(" ").append(server.getSSLParameter());
         }
       }
       output.append("\n");
     }
   }
  
  
}
