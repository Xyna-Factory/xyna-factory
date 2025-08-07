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
import xmcp.oas.fman.tools.GenTypeOpGroup;
import xmcp.oas.fman.tools.GeneratedOasApiType;
import xmcp.oas.fman.tools.ImplementedOasApiType;
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
      GenTypeOpGroup gtog = new GenTypeOpGroup(goat, opgroup);
      handleGeneratedType(ret, gtog);
    }
  }
  
  
  private void handleGeneratedType(List<OasApiDatatypeInfo> ret, GenTypeOpGroup gtog) {
    RtcData rtc = gtog.getGeneratedOasApiType().getRtc();
    String status = "";
    List<ImplementedOasApiType> list = _tools.getAllImplementedOasApiTypesInRefRtcs(gtog.getGeneratedOasApiType());
    if (list.size() > 1) {
      handleMultipleImplementations(ret, rtc, gtog, list);
      return;
    }
    OasApiDatatypeInfo.Builder builder = new OasApiDatatypeInfo.Builder();
    builder.generatedRtc(gtog.getGeneratedOasApiType().getRtc().toString());
    builder.apiDatatype(gtog.getGeneratedOasApiType().getFqName());
    if (list.size() == 0) {
      status = "Missing";
    } else if (list.size() == 1) {
      ImplementedOasApiType implType = list.get(0);
      OperationGroup opgroup = new OperationGroup(implType);
      if (opgroup.matches(gtog.getOperationGroup())) {
        status = "Complete";
      } else {
        status = "Incomplete";
      }
      builder.implementationDatatype(implType.getFqName());
      builder.implementationRtc(implType.getRtc().toString());
    }
    builder.status(status);
    ret.add(builder.instance());
  }
  
  
  private void handleMultipleImplementations(List<OasApiDatatypeInfo> ret, RtcData rtc, GenTypeOpGroup gtog, 
                                             List<ImplementedOasApiType> implList) {
    String status = "Error";
    for (ImplementedOasApiType item : implList) {
      OasApiDatatypeInfo.Builder builder = new OasApiDatatypeInfo.Builder();
      builder.generatedRtc(gtog.getGeneratedOasApiType().getRtc().toString());
      builder.apiDatatype(gtog.getGeneratedOasApiType().getFqName());
      builder.implementationRtc(item.getRtc().toString());
      builder.implementationDatatype(item.getFqName());
      builder.status(status);
      ret.add(builder.instance());
    }
  }
  
}
