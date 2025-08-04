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
package xmcp.oas.fman.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationDefinitionInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.WorkspaceManagement;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResult;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSelect;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xnwh.selection.parsing.ArchiveIdentifier;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser;

import base.KeyValue;
import base.Text;
import xmcp.oas.fman.OasGuiServicesServiceOperation;


public class OasGuiServicesServiceOperationImpl implements ExtendedDeploymentTask, OasGuiServicesServiceOperation {

  public void onDeployment() throws XynaException {
    // TODO do something on deployment, if required
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public void onUndeployment() throws XynaException {
    // TODO do something on undeployment, if required
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.
    return null;
  }

  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }


  @Override
  public List<? extends KeyValue> createWorkspaceInputDownloadData() {
    List<KeyValue> result = new ArrayList<KeyValue>();
    result.add(new KeyValue.Builder().key("<Application>").value(" ").instance());
    WorkspaceManagement wsMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getWorkspaceManagement();
    wsMgmt.listWorkspaces(false).forEach(
      ws -> result.add(new KeyValue.Builder().key(ws.getWorkspace().getName()).value(ws.getWorkspace().getName()).instance())
    );
    return result;
  }

  @Override
  public Text testListTypes() {
    Text ret = new Text();
    
    String fqname = "xact.http.enums.httpmethods.HTTPMethod";
    
    HashMap<String, String> filters = new HashMap<>();
    filters.put("fqname", fqname);
    SearchRequestBean srb = new SearchRequestBean();
    srb.setArchiveIdentifier(ArchiveIdentifier.xmomcache);
    srb.setMaxRows(-1);
    srb.setSelection("extendedBy");
    srb.setFilterEntries(filters);
    
    
    ApplicationManagementImpl applicationManagement = (ApplicationManagementImpl) XynaFactory.getInstance()
                    .getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
    
    List<ApplicationInformation> appsinfo = applicationManagement.listApplications(true, false);
    
    try {
        
      String str = "";
      
      for (ApplicationInformation app : appsinfo) {
        
        if (app instanceof ApplicationDefinitionInformation ) { continue; }
        /*
        long revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().
                     getRevision(((ApplicationDefinitionInformation) app).getParentWorkspace());
                     */
        long revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().
                        getRevision(app.asRuntimeContext());
        //long revision = 315;
        XynaMultiChannelPortal multiChannelPortal = (XynaMultiChannelPortal) XynaFactory.getInstance().getXynaMultiChannelPortal();
        
        XMOMDatabaseSelect select = (XMOMDatabaseSelect) SelectionParser.generateSelectObjectFromSearchRequestBean(srb);
        select.addAllDesiredResultTypes(List.of(XMOMDatabaseType.DATATYPE));
        
        XMOMDatabaseSearchResult searchResult = multiChannelPortal.searchXMOMDatabase(Arrays.asList(select), -1, revision);
        List<XMOMDatabaseSearchResultEntry> results = searchResult.getResult();
      
        str += "### " + revision + "\n\n";
        for (XMOMDatabaseSearchResultEntry entry : results) {
          str += entry.getFqName() + "\n";
        }
        str += "\n\n";
      }
      ret.setText(str);
        
      return ret;
        
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

}
