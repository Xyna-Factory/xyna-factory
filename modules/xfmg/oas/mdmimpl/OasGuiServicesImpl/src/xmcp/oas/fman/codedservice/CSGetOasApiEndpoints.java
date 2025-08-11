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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import xmcp.oas.fman.datatypes.OasApiDatatypeInfo;
import xmcp.oas.fman.tablehandling.OasApiDatatypeInfoComparator;
import xmcp.oas.fman.tablehandling.OasEndpointsFilterData;
import xmcp.oas.fman.tools.GeneratedOasApiType;
import xmcp.oas.fman.tools.ImplementedOasApiType;
import xmcp.oas.fman.tools.OasApiType.OasApiTypeCategory;
import xmcp.oas.fman.tools.OasGuiConstants;
import xmcp.oas.fman.tools.OasGuiTools;
import xmcp.oas.fman.tools.OasRtcTree;
import xmcp.oas.fman.tools.OperationGroup;
import xmcp.oas.fman.tools.RtcData;
import xmcp.tables.datatypes.TableInfo;


public class CSGetOasApiEndpoints {

  private static Logger _logger = Logger.getLogger(CSGetOasApiEndpoints.class);
  
  private OasGuiTools _tools = new OasGuiTools();
  
  
  public List<? extends OasApiDatatypeInfo> execute(TableInfo info) {
    try {
      List<OasApiDatatypeInfo> ret = new ArrayList<>();
      OasEndpointsFilterData filter = new OasEndpointsFilterData(info);
      List<RtcData> rtclist = _tools.getAllOasBaseApps();
      for (RtcData rtc : rtclist) {
        if (!filter.matchesGeneratedRtcFilter(rtc.toString())) { continue; }
        handleRtc(ret, rtc, filter);
      }
      Collections.sort(ret, new OasApiDatatypeInfoComparator(info));
      return ret;
    } catch (RuntimeException e) {
      _logger.error(e.getMessage(), e);
      throw e;
    } catch (Exception e) {
      _logger.error(e.getMessage(), e);
      throw new RuntimeException(e.getMessage(), e);
    }
  }
 
  
  private void handleRtc(List<OasApiDatatypeInfo> ret, RtcData rtc, OasEndpointsFilterData filter) {
    List<GeneratedOasApiType> list = _tools.getAllGeneratedOasApiTypesInRefRtcs(rtc);
    if (list.size() < 1) { return; }
    for (GeneratedOasApiType goat : list) {
      if (!filter.matchesApiDatatypeFilter(goat.getFqName())) { continue; }
      OperationGroup opgroup = new OperationGroup(goat);
      handleGeneratedType(ret, opgroup, filter);
    }
  }
  
  
  private void handleGeneratedType(List<OasApiDatatypeInfo> ret, OperationGroup genTypeOpGroup, OasEndpointsFilterData filter) {
    if (genTypeOpGroup.getOasApiTypecategory() != OasApiTypeCategory.GENERATED) { return; }
    String status = "";
    GeneratedOasApiType goat = new GeneratedOasApiType(genTypeOpGroup.getXmomType());
    List<ImplementedOasApiType> list = _tools.getAllImplementedOasApiTypesInRefRtcs(goat);
    if (list.size() > 1) {
      handleMultipleImplementations(ret, genTypeOpGroup, goat, list, filter);
      return;
    }
    OasApiDatatypeInfo.Builder builder = new OasApiDatatypeInfo.Builder();
    builder.generatedRtc(genTypeOpGroup.getXmomType().getRtc().toString());
    builder.apiDatatype(genTypeOpGroup.getXmomType().getFqName());
    if (list.size() == 0) {
      status = OasGuiConstants.EndpointStatus.MISSING;
    } else if (list.size() == 1) {
      ImplementedOasApiType implType = list.get(0);
      status = getCompletionStatus(genTypeOpGroup, implType);
      builder.implementationDatatype(implType.getFqName());
      builder.implementationRtc(implType.getRtc().toString());
    }
    builder.status(status);
    OasApiDatatypeInfo built = builder.instance();
    if (filter.matchesImplementationRtcFilter(built.getImplementationRtc()) &&
        filter.matchesImplementationDatatypeFilter(built.getImplementationDatatype()) &&
        filter.matchesStatusFilter(built.getStatus())) {
      ret.add(built);
    }
  }
  
  
  private String getCompletionStatus(OperationGroup genTypeOpGroup, ImplementedOasApiType implType) {
    OperationGroup opgroup = new OperationGroup(implType);
    if (opgroup.operationsMatch(genTypeOpGroup)) {
      return OasGuiConstants.EndpointStatus.COMPLETE;
    } 
    return OasGuiConstants.EndpointStatus.INCOMPLETE;
  }
  
  
  private void handleMultipleImplementations(List<OasApiDatatypeInfo> ret, OperationGroup genTypeOpGroup,
                                             GeneratedOasApiType goat, List<ImplementedOasApiType> implList, 
                                             OasEndpointsFilterData filter) {
    String errorStatus = OasGuiConstants.EndpointStatus.ERROR;
    OasRtcTree tree = new OasRtcTree(goat, implList);
    for (ImplementedOasApiType item : implList) {
      String status = errorStatus;
      int count = tree.countImplementedTypesInSubtree(item);
      if (count == 0) {
        throw new RuntimeException("Inconsistent data: Implemented oas type not found in rtc subtrees: " + item.getFqName());
      } else if (count == 1) {
        // implemented types are in distinct workspaces (including referenced run time contexts), no error
        status = getCompletionStatus(genTypeOpGroup, item);
      }
      OasApiDatatypeInfo.Builder builder = new OasApiDatatypeInfo.Builder();
      builder.generatedRtc(genTypeOpGroup.getXmomType().getRtc().toString());
      builder.apiDatatype(genTypeOpGroup.getXmomType().getFqName());
      builder.implementationRtc(item.getRtc().toString());
      builder.implementationDatatype(item.getFqName());
      builder.status(status);
      OasApiDatatypeInfo built = builder.instance();
      if (filter.matchesImplementationRtcFilter(built.getImplementationRtc()) &&
          filter.matchesImplementationDatatypeFilter(built.getImplementationDatatype()) &&
          filter.matchesStatusFilter(built.getStatus())) {
        ret.add(built);
      }
    }
  }
  
}
