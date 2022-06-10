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
package com.gip.xyna.xmcp.xfcli.impl;



import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Setxmomodsname;
import com.gip.xyna.xnwh.exceptions.XNWH_ODSNameMustBeUniqueException;
import com.gip.xyna.xnwh.persistence.xmom.ODSRegistrationParameter;
import com.gip.xyna.xnwh.persistence.xmom.XMOMODSMappingUtils;
import com.gip.xyna.xnwh.persistence.xmom.XMOMODSMappingUtils.DiscoveryResult;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;



public class SetxmomodsnameImpl extends XynaCommandImplementation<Setxmomodsname> {
  
  private final static Logger logger = CentralFactoryLogging.getLogger(SetxmomodsnameImpl.class);

  public void execute(OutputStream statusOutputStream, Setxmomodsname payload) throws XynaException {
    long revision =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
            .getRevision(payload.getApplicationName(), payload.getVersionName(), payload.getWorkspaceName());
    DOM dom;
    try {
      dom = DOM.generateUncachedInstance(payload.getFqDatatypeName(), true, revision);
    } catch (XPRC_InvalidPackageNameException e) {
      throw new RuntimeException(e);
    }
    GenerationBaseCache parseAdditionalCache = new GenerationBaseCache();
    Set<String> fqPaths;
    if (payload.getFqPath() == null &&
        payload.getPath() != null) {
      fqPaths = new HashSet<String>();
      Collection<DiscoveryResult> discoveries = XMOMODSMappingUtils.discoverFqPathsForPath(dom, payload.getPath(), parseAdditionalCache);
      for (DiscoveryResult discovery : discoveries) {
        fqPaths.add(discovery.getFqPath());
      }
    } else {
      fqPaths = Collections.singleton(payload.getFqPath() == null ? "" : payload.getFqPath());
    }
    
    if (fqPaths.size() > 1) {
      writeToCommandLine(statusOutputStream, "The given path was resolved to several matching objects " + fqPaths + ". Please specifiy a unique path.");
      return;
    }
    
    GenerationBaseCache cache = new GenerationBaseCache();
    CommandControl.tryLock(CommandControl.Operation.XMOM_ODS_NAME_SET, revision);
    try {
      
      for (String fqPath : fqPaths) {
        ODSRegistrationParameter odsRP = new ODSRegistrationParameter(dom, fqPath, payload.getOdsName(), false, cache);
        try {
          XynaFactory.getInstance().getXynaNetworkWarehouse().getXMOMPersistence().getXMOMPersistenceManagement().setODSName(odsRP);
        } catch(XNWH_ODSNameMustBeUniqueException e) {
          String odsIdentifier = odsRP.isTableRegistration() ? odsRP.getOdsName() : odsRP.getTableName() + "." + odsRP.getOdsName(); 
          writeToCommandLine(statusOutputStream, "The ODS name <" + odsIdentifier + "> is already in use. Please choose another name.");
        }   
      }
    } finally {
      CommandControl.unlock(CommandControl.Operation.XMOM_ODS_NAME_SET, revision);
    }

  }

}
