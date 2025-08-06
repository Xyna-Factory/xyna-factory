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

import xmcp.oas.fman.datatypes.OasApiDatatypeInfo;
import xmcp.oas.fman.tools.GenTypeOpGroup;
import xmcp.oas.fman.tools.GeneratedOasApiType;
import xmcp.oas.fman.tools.ImplementedOasApiType;
import xmcp.oas.fman.tools.OasGuiTools;
import xmcp.oas.fman.tools.RtcData;
import xmcp.tables.datatypes.TableInfo;


public class CSGetOasApiEndpoints {

  private OasGuiTools _tools = new OasGuiTools();
  
  
  public List<? extends OasApiDatatypeInfo> execute(TableInfo info) {
    List<OasApiDatatypeInfo> ret = new ArrayList<>();
    
    
    List<RtcData> rtclist = _tools.getAllAppsAndWorkspaces();
    for (RtcData rtc : rtclist) {
      handleRtc(ret, rtc);
    }
    return ret;
  }
 
  
  private void handleRtc(List<OasApiDatatypeInfo> ret, RtcData rtc) {
    List<GeneratedOasApiType> list = _tools.getAllGeneratedOasApiTypesInRtc(rtc);
    if (list.size() < 1) { return; }
    
    
  }
  
  
  private void handleGeneratedType(List<OasApiDatatypeInfo> ret, RtcData rtc, GenTypeOpGroup gtog) {
    List<RtcData> rtclist = _tools.getAllRtcsWhichReferenceRtcRecursive(rtc);
  }
  
  
  private void handleImplementedTypes(List<OasApiDatatypeInfo> ret, RtcData rtc, GenTypeOpGroup gtog) {
    List<ImplementedOasApiType> list = _tools.getAllImplementedOasApiTypesInRtc(gtog.getGeneratedOasApiType(), rtc);
  }
  
}
