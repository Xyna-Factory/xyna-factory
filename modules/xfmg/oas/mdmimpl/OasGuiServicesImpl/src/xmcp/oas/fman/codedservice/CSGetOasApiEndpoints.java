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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import xmcp.oas.fman.datatypes.OasApiDatatypeInfo;
import xmcp.oas.fman.tools.GeneratedOasApiType;
import xmcp.oas.fman.tools.ImplementedOasApiType;
import xmcp.oas.fman.tools.OasApiType.OasApiTypeCategory;
import xmcp.oas.fman.tools.OasGuiTools;
import xmcp.oas.fman.tools.OperationGroup;
import xmcp.oas.fman.tools.RtcData;
import xmcp.tables.datatypes.TableInfo;


public class CSGetOasApiEndpoints {

  private static Logger _logger = Logger.getLogger(CSGetOasApiEndpoints.class);
  
  private OasGuiTools _tools = new OasGuiTools();
  
  
  public List<? extends OasApiDatatypeInfo> execute(TableInfo info) {
    try {
      List<OasApiDatatypeInfo> ret = new ArrayList<>();
      List<RtcData> rtclist = _tools.getAllOasBaseApps();
      for (RtcData rtc : rtclist) {
        handleRtc(ret, rtc);
      }
      return ret;
    } catch (RuntimeException e) {
      _logger.error(e.getMessage(), e);
      throw e;
    } catch (Exception e) {
      _logger.error(e.getMessage(), e);
      throw new RuntimeException(e.getMessage(), e);
    }
  }
 
  
  private void handleRtc(List<OasApiDatatypeInfo> ret, RtcData rtc) {
    List<GeneratedOasApiType> list = _tools.getAllGeneratedOasApiTypesInRefRtcs(rtc);
    if (list.size() < 1) { return; }
    for (GeneratedOasApiType goat : list) {
      OperationGroup opgroup = new OperationGroup(goat);
      handleGeneratedType(ret, opgroup);
    }
  }
  
  
  private void handleGeneratedType(List<OasApiDatatypeInfo> ret, OperationGroup genTypeOpGroup) {
    if (genTypeOpGroup.getOasApiTypecategory() != OasApiTypeCategory.GENERATED) { return; }
    RtcData rtc = genTypeOpGroup.getXmomType().getRtc();
    String status = "";
    GeneratedOasApiType goat = new GeneratedOasApiType(genTypeOpGroup.getXmomType());
    List<ImplementedOasApiType> list = _tools.getAllImplementedOasApiTypesInRefRtcs(goat);
    if (list.size() > 1) {
      handleMultipleImplementations(ret, genTypeOpGroup, list);
      return;
    }
    OasApiDatatypeInfo.Builder builder = new OasApiDatatypeInfo.Builder();
    builder.generatedRtc(genTypeOpGroup.getXmomType().getRtc().toString());
    builder.apiDatatype(genTypeOpGroup.getXmomType().getFqName());
    if (list.size() == 0) {
      status = "Missing";
    } else if (list.size() == 1) {
      ImplementedOasApiType implType = list.get(0);
      status = getCompletionStatus(genTypeOpGroup, implType);
      builder.implementationDatatype(implType.getFqName());
      builder.implementationRtc(implType.getRtc().toString());
    }
    builder.status(status);
    ret.add(builder.instance());
  }
  
  
  private String getCompletionStatus(OperationGroup genTypeOpGroup, ImplementedOasApiType implType) {
    OperationGroup opgroup = new OperationGroup(implType);
    if (opgroup.operationsMatch(genTypeOpGroup)) {
      return "Complete";
    } 
    return "Incomplete";
  }
  
  
  private void handleMultipleImplementations(List<OasApiDatatypeInfo> ret, OperationGroup genTypeOpGroup,
                                             List<ImplementedOasApiType> implList) {
    String errorStatus = "Error";
    // condition: Status "Error" is avoided for an implemented type in the list,
    // if it is in a workspace, and all others are in different workspaces, and none in an application
    int appCount = 0;
    Map<RtcData, Integer> workspaceMap = new HashMap<>();
    for (ImplementedOasApiType item : implList) {
      RtcData rtc = item.getRtc();
      if (!rtc.isWorkspace()) {
        appCount++;
        continue;
      }
      Integer count = workspaceMap.get(rtc);
      if (count == null) {
        workspaceMap.put(rtc, 1);
        continue;
      }
      workspaceMap.put(rtc, count + 1);
    }
    for (ImplementedOasApiType item : implList) {
      String status = errorStatus;
      if (appCount == 0) {
        Integer wspCount = workspaceMap.get(item.getRtc());
        if (wspCount.equals((Integer.valueOf(1)))) {
          status = getCompletionStatus(genTypeOpGroup, item);
        }
      }
      OasApiDatatypeInfo.Builder builder = new OasApiDatatypeInfo.Builder();
      builder.generatedRtc(genTypeOpGroup.getXmomType().getRtc().toString());
      builder.apiDatatype(genTypeOpGroup.getXmomType().getFqName());
      builder.implementationRtc(item.getRtc().toString());
      builder.implementationDatatype(item.getFqName());
      builder.status(status);
      ret.add(builder.instance());
    }
  }
  
}
