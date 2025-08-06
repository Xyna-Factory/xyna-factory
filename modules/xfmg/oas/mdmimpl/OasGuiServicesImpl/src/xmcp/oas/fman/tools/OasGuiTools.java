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

package xmcp.oas.fman.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagement;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.WorkspaceInformation;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseEntryColumn;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResult;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSelect;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xnwh.selection.parsing.ArchiveIdentifier;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser;


public class OasGuiTools {

  public List<RtcData> getAllAppsAndWorkspaces() {
    List<RtcData> ret = new ArrayList<>();
    try {
      ApplicationManagement appMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().
                                                  getApplicationManagement();
      if (appMgmt instanceof ApplicationManagementImpl) {
        List<ApplicationInformation> applist = ((ApplicationManagementImpl) appMgmt).listApplications(true, false);
        for (ApplicationInformation app : applist) {
          RtcData rtc = new RtcData(app.asRuntimeContext());
          ret.add(rtc);
        }
      }
      List<WorkspaceInformation> wsplist = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().
                                                       getWorkspaceManagement().listWorkspaces(true);
      for (WorkspaceInformation wsp : wsplist) {
        RtcData rtc = new RtcData(wsp.asRuntimeContext());
        ret.add(rtc);
      }
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
    return ret;
  }
  
  
  
  public List<XmomType> getAllChildTypesInRtc(XmomType xmom, RtcData rtc) {
    List<XmomType> ret = new ArrayList<>();
    try {
      HashMap<String, String> filters = new HashMap<>();
      filters.put(XMOMDatabaseEntryColumn.FQNAME.getColumnName(), xmom.getFqName());
      SearchRequestBean srb = new SearchRequestBean();
      srb.setArchiveIdentifier(ArchiveIdentifier.xmomcache);
      srb.setMaxRows(-1);
      srb.setSelection(XMOMDatabaseEntryColumn.EXTENDEDBY.toString());
      srb.setFilterEntries(filters);
  
      XynaMultiChannelPortal multiChannelPortal = (XynaMultiChannelPortal) XynaFactory.getInstance().
                                                                                       getXynaMultiChannelPortal();
      XMOMDatabaseSelect select = (XMOMDatabaseSelect) SelectionParser.generateSelectObjectFromSearchRequestBean(srb);
      select.addAllDesiredResultTypes(List.of(XMOMDatabaseType.DATATYPE, XMOMDatabaseType.SERVICEGROUP));
      XMOMDatabaseSearchResult searchResult = multiChannelPortal.searchXMOMDatabase(Arrays.asList(select), -1,
                                                                                    rtc.getRevision());
      for (XMOMDatabaseSearchResultEntry entry : searchResult.getResult()) {
        XmomType xt = new XmomType(entry.getFqName());
        ret.add(xt);
      }
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
    return ret;
  }
  
  
  public List<GeneratedOasApiType> getAllGeneratedOasApiTypesInRtc(RtcData rtc) {
    List<GeneratedOasApiType> ret = new ArrayList<>();
    XmomType xmom = new XmomType(OasGuiConstants.FQN_OAS_BASE_API);
    List<XmomType> list = this.getAllChildTypesInRtc(xmom, rtc);
    for (XmomType item : list) {
      ret.add(new GeneratedOasApiType(item, rtc));
    }
    return ret;
  }
  
  
  public List<ImplementedOasApiType> getAllImplementedOasApiTypesInRtc(GeneratedOasApiType goat, RtcData rtc) {
    List<ImplementedOasApiType> ret = new ArrayList<>();
    List<XmomType> list = this.getAllChildTypesInRtc(goat.getXmomType(), rtc);
    for (XmomType item : list) {
      ret.add(new ImplementedOasApiType(item, rtc));
    }
    return ret;
  }
  
  
  public List<RtcData> getAllRtcsWhichReferenceRtcRecursive(RtcData rtc) {
    List<RtcData> ret = new ArrayList<>();
    ret.add(rtc);
    try {
      RuntimeContextDependencyManagement rtcDependencyManagement =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
      RevisionManagement revisionManagement =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      
      RuntimeContext runtimeContext = revisionManagement.getRuntimeContext(rtc.getRevision());
      Map<RuntimeDependencyContext, Collection<RuntimeDependencyContext>> map = rtcDependencyManagement.getAllDependencies();
      for (Map.Entry<RuntimeDependencyContext, Collection<RuntimeDependencyContext>> entry : map.entrySet()) {
        for (RuntimeDependencyContext dep : entry.getValue()) {
          RuntimeContext tmpRtc = dep.asCorrespondingRuntimeContext();
          if (tmpRtc.equals(runtimeContext)) {
            RtcData refRtc = new RtcData(tmpRtc);
            ret.add(refRtc);
            ret.addAll(getAllRtcsWhichReferenceRtcRecursive(refRtc));
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
    return ret;
  }
  
  
  public List<ImplementedOasApiType> getAllImplementedOasApiTypesInRefRtcs(GeneratedOasApiType goat, RtcData rtc) {
    List<ImplementedOasApiType> ret = new ArrayList<>();
    List<RtcData> rtclist = getAllRtcsWhichReferenceRtcRecursive(rtc);
    for (RtcData refRtc : rtclist) {
      List<ImplementedOasApiType> list = getAllImplementedOasApiTypesInRtc(goat, refRtc);
      ret.addAll(list);
    }
    return ret;
  }
  
  
  public Set<String> getOperationsOfXmomType(XmomType xmom, RtcData rtc) {
    Set<String> ret = new TreeSet<>();
    try {
      String fqname = xmom.getFqName();
      String path = fqname.substring(0, fqname.lastIndexOf("."));
      String typename = fqname.substring(fqname.lastIndexOf(".") + 1, fqname.length());
      
      SearchRequestBean srb = new SearchRequestBean();
      srb.setArchiveIdentifier(ArchiveIdentifier.xmomcache);
      srb.setMaxRows(-1);
      srb.addFilterEntry(XMOMDatabaseEntryColumn.PATH.getColumnName(), path);
      srb.setSelection(XMOMDatabaseEntryColumn.CASE_SENSITIVE_LABEL.getColumnName() + "," +
                       XMOMDatabaseEntryColumn.NAME.getColumnName() + "," +
                       XMOMDatabaseEntryColumn.PATH.getColumnName() + "," +
                       XMOMDatabaseEntryColumn.REVISION.getColumnName());
    
      XMOMDatabaseSelect select = (XMOMDatabaseSelect) SelectionParser.generateSelectObjectFromSearchRequestBean(srb);
      select.addDesiredResultTypes(XMOMDatabaseType.OPERATION);
      //select.addDesiredResultTypes(XMOMDatabaseType.SERVICEGROUP);
      //select.addDesiredResultTypes(XMOMDatabaseType.DATATYPE);
      
      XMOMDatabase xmomDB = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getXMOMDatabase();
      XMOMDatabaseSearchResult searchResult = xmomDB.searchXMOMDatabase(List.of(select), -1, rtc.getRevision());
      List<XMOMDatabaseSearchResultEntry> results = searchResult.getResult();
    
      for (XMOMDatabaseSearchResultEntry entry : results) {
        String op = entry.getSimplename();
        if (!op.contains(".")) { continue; }        
        String prefix = op.substring(0, fqname.indexOf("."));
        if (!prefix.equals(typename)) { continue; }
        op = op.substring(fqname.lastIndexOf(".") + 1, op.length());
        ret.add(op);
      }
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
    return ret;
  }
  
}
