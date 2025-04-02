/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package xdev.yang.impl.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.yangcentral.yangkit.model.api.stmt.Module;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.XynaFactoryControl;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext.RuntimeDependencyContextType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;

import xdev.yang.datatypes.YangModuleDetails;
import xdev.yang.impl.XmomDbInteraction;
import xdev.yang.impl.operation.ModuleGroup;
import xdev.yang.impl.operation.ModuleParseData;
import xdev.yang.impl.operation.OperationAssignmentUtils;
import xmcp.yang.YangModuleCollection;
import xprc.xpce.Application;
import xprc.xpce.RuntimeContext;
import xprc.xpce.Workspace;


public class LoadModules {

  private static Logger _logger = CentralFactoryLogging.getLogger(LoadModules.class);
  
  
  public List<? extends YangModuleDetails> execute(RuntimeContext rtc) {
    try {
      List<YangModuleDetails> ret = new ArrayList<>();
      loadModules(ret, rtc);
      return ret;
    } catch (Exception e) {
      _logger.error(e.getMessage(), e);
      throw new RuntimeException(e.getMessage(), e);
    }
  }
  
  
  private void loadModules(List<YangModuleDetails> ret, RuntimeContext rtc) throws Exception {
    String rtcLabel = "";
    Long revision = 0L;
    XynaFactoryControl xynaFactoryCtrl = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl();
    RevisionManagement revMgmt = xynaFactoryCtrl.getRevisionManagement();
    if (rtc instanceof Workspace) {
      Workspace ws = (Workspace) rtc;
      revision = revMgmt.getRevision(null, null, ws.getName());
      rtcLabel = ws.getName();
      followReferencesOfWorkspace(ret, revMgmt, revision);
    } else if (rtc instanceof Application) {
      Application app = (Application) rtc;
      revision = revMgmt.getRevision(app.getName(), app.getVersion(), null);
      rtcLabel = app.getName() + " / " + app.getVersion();
      followReferencesOfApp(ret, revMgmt, revision);
    }
    loadModulesImpl(ret, revision, rtcLabel, rtc);
  }
  
  
  private void loadModulesImpl(List<YangModuleDetails> ret, Long revision, String rtcLabel, RuntimeContext rtc) throws Exception {
    _logger.warn("### Checking for yang modules in rtc " + rtcLabel);
    
    XmomDbInteraction interaction = new XmomDbInteraction();
    List<XMOMDatabaseSearchResultEntry> xmomDbResult = interaction.searchYangDTs(YangModuleCollection.class.getCanonicalName(), 
                                                                                 List.of(revision));
    for (XMOMDatabaseSearchResultEntry entry : xmomDbResult) {
      ModuleGroup group = OperationAssignmentUtils.loadModulesFromDt(entry.getFqName(), revision);
      for (ModuleParseData parsed : group.getAllModuleParseData()) {
        for (Module mod : parsed.getModuleList()) {
          YangModuleDetails.Builder builder = new YangModuleDetails.Builder();
          builder.fQDatatype(entry.getFqName())
                 .rTCLabel(rtcLabel)
                 .moduleName(mod.getModuleId().getModuleName())
                 .runtimeContext(rtc);
          ret.add(builder.instance());
        }
      }
    }
  }
  
  
  private void followReferencesOfWorkspace(List<YangModuleDetails> ret, RevisionManagement revisionManagement,
                                           Long revision) throws Exception {
    RuntimeContextDependencyManagement rtcDependencyManagement =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace ws = revisionManagement.getWorkspace(revision);
    Collection<RuntimeDependencyContext> dependencies = rtcDependencyManagement.getDependencies(ws);
    followReferences(ret, dependencies);
  }
  
  
  private void followReferencesOfApp(List<YangModuleDetails> ret, RevisionManagement revisionManagement,
                                     Long revision) throws Exception {
    RuntimeContextDependencyManagement rtcDependencyManagement =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application app = revisionManagement.getApplication(revision);
    Collection<RuntimeDependencyContext> dependencies = rtcDependencyManagement.getDependencies(app);
    followReferences(ret, dependencies);
  }
  
  
  private void followReferences(List<YangModuleDetails> ret, Collection<RuntimeDependencyContext> dependencies) 
               throws Exception {
    for (RuntimeDependencyContext rdc : dependencies) {
      if (rdc.getRuntimeDependencyContextType() == RuntimeDependencyContextType.Application) {
        Application.Builder builder = new Application.Builder();
        builder.name(rdc.getName()).version(rdc.getAdditionalIdentifier());
        loadModules(ret, builder.instance());
      } else if (rdc.getRuntimeDependencyContextType() == RuntimeDependencyContextType.Workspace) {
        Workspace.Builder builder = new Workspace.Builder();
        builder.name(rdc.getName());
        loadModules(ret, builder.instance());
      }
    }
  }

}
