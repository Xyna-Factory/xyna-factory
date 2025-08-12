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
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagement;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
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

  
  public List<RtcData> getAllOasBaseApps() {
    List<RtcData> ret = new ArrayList<>();
    try {
      ApplicationManagement appMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().
                                                  getApplicationManagement();
      if (appMgmt instanceof ApplicationManagementImpl) {
        List<ApplicationInformation> applist = ((ApplicationManagementImpl) appMgmt).listApplications(true, false);
        for (ApplicationInformation info : applist) {
          RuntimeContext rtc = info.asRuntimeContext();
          if (!(rtc instanceof Application)) { continue; }
          if (OasGuiConstants.OAS_BASE_APP_NAME.equals(info.getName())) {
            RtcData rtcdata = new RtcData(rtc);
            ret.add(rtcdata);
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
    return ret;
  }
  
  
  public List<XmomType> getAllChildTypesInReferencedRtcs(FqName fqname, RtcData rtc) {
    List<XmomType> ret = new ArrayList<>();
    try {
      HashMap<String, String> filters = new HashMap<>();
      filters.put(XMOMDatabaseEntryColumn.FQNAME.getColumnName(), fqname.getFqName());
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
        RtcData foundRtc = new RtcData(entry.getRuntimeContext());
        XmomType xt = new XmomType(entry.getFqName(), foundRtc);
        ret.add(xt);
      }
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
    return ret;
  }
  
  
  public List<GeneratedOasApiType> getAllGeneratedOasApiTypesInRefRtcs(RtcData rtc) {
    List<GeneratedOasApiType> ret = new ArrayList<>();
    FqName fqname = new FqName(OasGuiConstants.FQN_OAS_BASE_API);
    List<XmomType> list = getAllChildTypesInReferencedRtcs(fqname, rtc);
    for (XmomType item : list) {
      ret.add(new GeneratedOasApiType(item));
    }
    return ret;
  }
  
  
  public List<ImplementedOasApiType> getAllImplementedOasApiTypesInRefRtcs(GeneratedOasApiType goat) {
    List<ImplementedOasApiType> ret = new ArrayList<>();
    List<XmomType> list = getAllChildTypesInReferencedRtcs(goat.getXmomType().getFqNameInstance(), goat.getRtc());
    for (XmomType item : list) {
      ret.add(new ImplementedOasApiType(item));
    }
    return ret;
  }
  
  
  public Set<String> getOperationsOfXmomType(XmomType xmom) {
    Set<String> ret = new TreeSet<>();
    try {
      String fqname = xmom.getFqName();
      String path = fqname.substring(0, fqname.lastIndexOf("."));
      String typename = fqname.substring(fqname.lastIndexOf(".") + 1, fqname.length());
      long revision = xmom.getRtc().getRevision();
      
      SearchRequestBean srb = new SearchRequestBean();
      srb.setArchiveIdentifier(ArchiveIdentifier.xmomcache);
      srb.setMaxRows(-1);
      srb.addFilterEntry(XMOMDatabaseEntryColumn.PATH.getColumnName(), path);
      srb.setSelection(OasGuiConstants.OP_SEARCH_SELECT);
      XMOMDatabaseSelect select = (XMOMDatabaseSelect) SelectionParser.generateSelectObjectFromSearchRequestBean(srb);
      select.addDesiredResultTypes(XMOMDatabaseType.OPERATION);
      XMOMDatabase xmomDB = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getXMOMDatabase();
      XMOMDatabaseSearchResult searchResult = xmomDB.searchXMOMDatabase(List.of(select), -1, revision);
      List<XMOMDatabaseSearchResultEntry> results = searchResult.getResult();
    
      for (XMOMDatabaseSearchResultEntry entry : results) {
        String op = entry.getSimplename();
        if (!op.contains(".")) { continue; }
        String prefix = op.substring(0, op.indexOf("."));
        if (!prefix.equals(typename)) { continue; }
        op = op.substring(op.lastIndexOf(".") + 1, op.length());
        ret.add(op);
      }
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
    return ret;
  }
  
}
