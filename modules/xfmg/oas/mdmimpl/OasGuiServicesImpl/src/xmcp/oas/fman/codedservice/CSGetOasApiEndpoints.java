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
import java.util.List;

import org.apache.log4j.Logger;

import xmcp.oas.fman.datatypes.OasApiDatatypeInfo;
import xmcp.oas.fman.tablehandling.OasEndpointsFilterData;
import xmcp.oas.fman.tablehandling.SortTool;
import xmcp.oas.fman.tools.OasGuiContext;
import xmcp.oas.fman.tools.GeneratedOasApiType;
import xmcp.oas.fman.tools.ImplementedOasApiType;
import xmcp.oas.fman.tools.OasApiType.OasApiTypeCategory;
import xmcp.oas.fman.tools.OasGuiConstants;
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
      OasEndpointsFilterData filter = new OasEndpointsFilterData(info);
      OasGuiContext context = new OasGuiContext();
      List<RtcData> rtclist = _tools.getAllOasBaseApps();
      for (RtcData rtc : rtclist) {
        handleRtc(context, ret, rtc, filter);
      }
      new SortTool().sort(ret, info);
      return ret;
    } catch (RuntimeException e) {
      _logger.error(e.getMessage(), e);
      throw e;
    } catch (Exception e) {
      _logger.error(e.getMessage(), e);
      throw new RuntimeException(e.getMessage(), e);
    }
  }
 
  
  private void handleRtc(OasGuiContext context, List<OasApiDatatypeInfo> ret, RtcData rtc, OasEndpointsFilterData filter) {
    List<GeneratedOasApiType> genApiTypes = _tools.getAllGeneratedOasApiTypesInRefRtcs(rtc);
    if (genApiTypes.size() < 1) { return; }
    for (GeneratedOasApiType goat : genApiTypes) {
      if (!filter.matchesGeneratedRtcFilter(goat.getRtc().toString())) { continue; }
      if (!filter.matchesApiDatatypeFilter(goat.getFqName())) { continue; }
      OperationGroup opgroup = new OperationGroup(goat, context);
      List<RtcData> implRtcs = _tools.findImplRtcs(goat.getRtc());
      handleGeneratedType(context, ret, opgroup, implRtcs, filter);
    }
  }
  
  
  private void handleGeneratedType(OasGuiContext context, List<OasApiDatatypeInfo> ret, OperationGroup genTypeOpGroup, 
                                   List<RtcData> implRtcs, OasEndpointsFilterData filter) {
    if (genTypeOpGroup.getOasApiTypecategory() != OasApiTypeCategory.GENERATED) { return; }
    List<OasApiDatatypeInfo> candidatesToAdd = new ArrayList<>();
    if(implRtcs.isEmpty()) {
      OasApiDatatypeInfo.Builder builder = createBuilder(genTypeOpGroup);
      builder.status(OasGuiConstants.EndpointStatus.MISSING);
      OasApiDatatypeInfo built = builder.instance();
      if (matchFilter(filter, built)) {
        ret.add(built);
      }
    }
    
    GeneratedOasApiType goat = new GeneratedOasApiType(genTypeOpGroup.getXmomType());
    List<ImplementedOasApiType> list = _tools.getAllImplementedOasApiTypesInRefRtcs(goat);
    for(RtcData implRtc : implRtcs) {
      List<ImplementedOasApiType> accessibleImpls  =_tools.getAcessibleImplDts(implRtc, list);
      if(accessibleImpls.isEmpty()) {
        OasApiDatatypeInfo.Builder builder = createBuilder(genTypeOpGroup);
        builder.implementationRtc(implRtc.toString());
        builder.implementationRtcRevision(implRtc.getRevision());
        builder.status(OasGuiConstants.EndpointStatus.MISSING);
        candidatesToAdd = List.of(builder.instance());
      } else if(accessibleImpls.size() == 1) {
        ImplementedOasApiType implType = accessibleImpls.get(0);
        OasApiDatatypeInfo.Builder builder = createBuilder(genTypeOpGroup);
        setImpl(builder, implType);
        builder.status(getCompletionStatus(context, genTypeOpGroup, implType));
        candidatesToAdd = List.of(builder.instance());
      } else {
        for(ImplementedOasApiType accessibleImpl : accessibleImpls) {
          OasApiDatatypeInfo.Builder builder = createBuilder(genTypeOpGroup);
          setImpl(builder, accessibleImpl);
          builder.status(OasGuiConstants.EndpointStatus.ERROR);
          candidatesToAdd.add(builder.instance());
        }
      }
      
      for(OasApiDatatypeInfo built : candidatesToAdd) {
        if (matchFilter(filter, built)) {
          ret.add(built);
        }
      }
    }
  }
  
  private boolean matchFilter(OasEndpointsFilterData filter, OasApiDatatypeInfo candidate) {
    return filter.matchesImplementationRtcFilter(candidate.getImplementationRtc()) &&
        filter.matchesImplementationDatatypeFilter(candidate.getImplementationDatatype()) &&
        filter.matchesStatusFilter(candidate.getStatus());
  }

  private OasApiDatatypeInfo.Builder createBuilder(OperationGroup genTypeOpGroup) {
    OasApiDatatypeInfo.Builder builder = new OasApiDatatypeInfo.Builder();;
    builder.generatedRtc(genTypeOpGroup.getXmomType().getRtc().toString());
    builder.generatedRtcRevision(genTypeOpGroup.getXmomType().getRtc().getRevision());
    builder.apiDatatype(genTypeOpGroup.getXmomType().getFqName());
    return builder;
  }
  
  private OasApiDatatypeInfo.Builder setImpl(OasApiDatatypeInfo.Builder builder, ImplementedOasApiType accessibleImpl) {
    builder.implementationRtc(accessibleImpl.getRtc().toString());
    builder.implementationDatatype(accessibleImpl.getFqName());
    builder.implementationRtcRevision(accessibleImpl.getRtc().getRevision());
    builder.implementationRtcIsWorkspace(accessibleImpl.getRtc().isWorkspace());
    return builder;
  }
  
  private String getCompletionStatus(OasGuiContext context, OperationGroup genTypeOpGroup, ImplementedOasApiType implType) {
    OperationGroup opgroup = new OperationGroup(implType, context);
    if (opgroup.operationsMatch(genTypeOpGroup)) {
      return OasGuiConstants.EndpointStatus.COMPLETE;
    }
    return OasGuiConstants.EndpointStatus.INCOMPLETE;
  }
  
}
