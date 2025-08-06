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

package xmcp.oas.fman.codedservice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationDefinitionInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.WorkspaceInformation;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.workspacemgmt.WorkspaceManagement;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseEntryColumn;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResult;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSelect;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xnwh.exceptions.XNWH_NoSelectGivenException;
import com.gip.xyna.xnwh.exceptions.XNWH_SelectParserException;
import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;
import com.gip.xyna.xnwh.selection.parsing.ArchiveIdentifier;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser;

import base.Text;
import xmcp.oas.fman.datatypes.OasApiDatatypeInfo;
import xmcp.oas.fman.tools.OasGuiTools;
import xmcp.tables.datatypes.TableInfo;


public class CSGetOasApiEndpoints {

  public List<? extends OasApiDatatypeInfo> execute(TableInfo info) {
    List<OasApiDatatypeInfo> ret = new ArrayList<>();
    
    //fillForTest(ret);
    OasGuiTools tools = new OasGuiTools();
    
    
    return ret;
  }
 
  
  
  
  private void fillForTest(List<OasApiDatatypeInfo> list) {
    OasApiDatatypeInfo info = new OasApiDatatypeInfo();
    info.setApiDatatype("test type 1");
    list.add(info);
    info = new OasApiDatatypeInfo();
    info.setApiDatatype("test type 2");
    list.add(info);
  }
  
  
  
  
  public Text testListTypes() {
    Text ret = new Text();
    
      String str = "";
      
      
      
    //String fqname = "xact.http.enums.httpmethods.HTTPMethod";
    String fqname = "xmcp.oas.datatype.OASBaseApi";
    
    HashMap<String, String> filters = new HashMap<>();
    filters.put("fqname", fqname);
    SearchRequestBean srb = new SearchRequestBean();
    srb.setArchiveIdentifier(ArchiveIdentifier.xmomcache);
    srb.setMaxRows(-1);
    //srb.setSelection("extendedBy");
    srb.setSelection(XMOMDatabaseEntryColumn.EXTENDEDBY.toString());
    srb.setFilterEntries(filters);
    
    
    try {
      
          
      
      str += writeDeps(322);
      
      str += getOperations("xmcp.oas.provider", 323);
        
      str += getOperations("ztest.us.oas.datatypes", 323);
      ret.setText(str);
        
      //if (true) { return ret; }
      
      
      
      ApplicationManagementImpl applicationManagement = (ApplicationManagementImpl) XynaFactory.getInstance()
                    .getFactoryManagement().getXynaFactoryControl().getApplicationManagement();
    
      List<ApplicationInformation> appsinfo = applicationManagement.listApplications(true, false);    
      for (ApplicationInformation app : appsinfo) {
        
        if (app instanceof ApplicationDefinitionInformation ) { continue; }
        /*
        long revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().
                     getRevision(((ApplicationDefinitionInformation) app).getParentWorkspace());
                     */
        long revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().
                        getRevision(app.asRuntimeContext());
        //long revision = 315;
        str += writeRevision(revision, srb);
      }
      
      WorkspaceManagement wspManagement = (WorkspaceManagement) XynaFactory.getInstance()
                    .getFactoryManagement().getXynaFactoryControl().getWorkspaceManagement();
    
      List<WorkspaceInformation> wspinfo = wspManagement.listWorkspaces(true);
      for (WorkspaceInformation wsp : wspinfo) {
        
        long revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().
                        getRevision(wsp.asRuntimeContext());
        //long revision = 315;
        str += writeRevision(revision, srb);
      }
      
      ret.setText(str);
        
      return ret;
        
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
  
  
  private String writeDeps(long revision) throws Exception {
    String ret = "### deps";
    RuntimeContextDependencyManagement rtcDependencyManagement = 
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    RevisionManagement revisionManagement = 
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    
          RuntimeContext runtimeContext = revisionManagement.getRuntimeContext(revision);
          
          ret += " of " + runtimeContext.getName() + " | " + runtimeContext.getGUIRepresentation() + "\n\n";
          
          Map<RuntimeDependencyContext, Collection<RuntimeDependencyContext>> map = rtcDependencyManagement.getAllDependencies();
          for (Map.Entry<RuntimeDependencyContext, Collection<RuntimeDependencyContext>> entry : map.entrySet()) {
            for (RuntimeDependencyContext dep : entry.getValue()) {
              //ret += dep.getName() + " | " + dep.getAdditionalIdentifier() + "\n";
              
              if(dep.asCorrespondingRuntimeContext().equals(runtimeContext)) {
                ret += entry.getKey().getName() + " | " + entry.getKey().getAdditionalIdentifier()+ "\n";
              }
              
            }
          }
          /*
          rtcDependencyManagement.getAllDependencies().forEach((rdc, deps) -> {
            for (RuntimeDependencyContext dep : deps) {
              if(dep.asCorrespondingRuntimeContext().equals(runtimeContext)) {
                ret += dep.getName() + " | " + dep.getAdditionalIdentifier();
              }
            }
          });
          */
    ret += "\n\n";
    return ret;
  }
  
  
  private String writeRevision(long revision, SearchRequestBean srb) throws Exception {
    String str = "";
        XynaMultiChannelPortal multiChannelPortal = (XynaMultiChannelPortal) XynaFactory.getInstance().getXynaMultiChannelPortal();
        
        XMOMDatabaseSelect select = (XMOMDatabaseSelect) SelectionParser.generateSelectObjectFromSearchRequestBean(srb);
        select.addAllDesiredResultTypes(List.of(XMOMDatabaseType.DATATYPE, XMOMDatabaseType.SERVICEGROUP));
        
        XMOMDatabaseSearchResult searchResult = multiChannelPortal.searchXMOMDatabase(Arrays.asList(select), -1, revision);
        List<XMOMDatabaseSearchResultEntry> results = searchResult.getResult();
      
        if (results.size() > 0) {
          str += "### " + revision + "\n\n";
          for (XMOMDatabaseSearchResultEntry entry : results) {
            str += entry.getFqName() + " | " + entry.getType() + "\n";
            str += this.getOperations(entry, revision);
          }
          str += "\n\n";
        } else {
          str += "### no match for rev " + revision + "\n\n";
        }
        return str;
  }
  
  
  
  private String getOperations(XMOMDatabaseSearchResultEntry input, long revision) {
    
    String fqname = input.getFqName();
    String path = fqname.substring(0, fqname.lastIndexOf("."));
    return getOperations(path, revision);
  }
  
  
  private String getOperations(String path, long revision) {
    String str = "";
  
    str += "(rev = " + revision + ") ";
          
    //HashMap<String, String> filters = new HashMap<>();
    //filters.put("fqname", fqname);
    SearchRequestBean srb = new SearchRequestBean();
    srb.setArchiveIdentifier(ArchiveIdentifier.xmomcache);
    srb.setMaxRows(-1);
    srb.addFilterEntry(XMOMDatabaseEntryColumn.PATH.getColumnName(), path);
      //srb.addFilterEntry(XMOMDatabaseEntryColumn.NAME.getColumnName(), null);
    srb.setSelection(XMOMDatabaseEntryColumn.CASE_SENSITIVE_LABEL.getColumnName()+","+XMOMDatabaseEntryColumn.NAME.getColumnName()+","+XMOMDatabaseEntryColumn.PATH.getColumnName()+","+XMOMDatabaseEntryColumn.REVISION.getColumnName());
    
    //srb.setSelection("extendedBy");
    //srb.setFilterEntries(filters);
        
    try {
        
      
      //for (ApplicationInformation app : appsinfo) {
        
        //if (app instanceof ApplicationDefinitionInformation ) { continue; }
        /*
        long revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().
                     getRevision(((ApplicationDefinitionInformation) app).getParentWorkspace());
                     */
        //long revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().
         //               getRevision(app.asRuntimeContext());
        //long revision = 315;
        //XynaMultiChannelPortal multiChannelPortal = (XynaMultiChannelPortal) XynaFactory.getInstance().getXynaMultiChannelPortal();
        
        XMOMDatabaseSelect select = (XMOMDatabaseSelect) SelectionParser.generateSelectObjectFromSearchRequestBean(srb);
        //select.addAllDesiredResultTypes(List.of(XMOMDatabaseType.OPERATION, XMOMDatabaseType.SERVICEGROUP, XMOMDatabaseType.SERVICE));
        
        select.addDesiredResultTypes(XMOMDatabaseType.OPERATION);
        select.addDesiredResultTypes(XMOMDatabaseType.SERVICEGROUP);
        select.addDesiredResultTypes(XMOMDatabaseType.DATATYPE);
        
        //XMOMDatabaseSearchResult searchResult = multiChannelPortal.searchXMOMDatabase(Arrays.asList(select), -1, revision);
        
        XMOMDatabase xmomDB = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getXMOMDatabase();
        //return xmomDB.searchXMOMDatabase(selects, -1, revision);
        XMOMDatabaseSearchResult searchResult = xmomDB.searchXMOMDatabase(List.of(select), -1, revision);
        List<XMOMDatabaseSearchResultEntry> results = searchResult.getResult();
      
        if (results.size() > 0) {
          str += "\n--- ops for path: " + path + "\n";
          for (XMOMDatabaseSearchResultEntry entry : results) {
            str += entry.getSimplename() + " | " + entry.getSimplepath() + "\n";
            
          }
          str += "\n\n";
        } else {
          str += "\n--- no ops found for path: " + path + "\n";
        }
      //}
        
      return str;
        
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
  
  
}
