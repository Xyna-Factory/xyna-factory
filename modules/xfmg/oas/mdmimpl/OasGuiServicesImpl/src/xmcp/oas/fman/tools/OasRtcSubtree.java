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

import java.util.HashSet;
import java.util.Set;


public class OasRtcSubtree {

  private Set<RtcData> rtcSet = new HashSet<>();
  private Set<ImplementedOasApiType> implementedTypes = new HashSet<>();
  private static RtcTools tools = new RtcTools();
  
  
  public OasRtcSubtree(RtcData depthOneRtc) {
    tools.getAllRtcsWhichReferenceRtcRecursive(depthOneRtc, rtcSet);
  }

  public boolean contains(ImplementedOasApiType ioat) {
    return rtcSet.contains(ioat.getRtc());
  }
  
  public void register(ImplementedOasApiType ioat) {
    implementedTypes.add(ioat);
  }
  
  public int getImplementedTypeCount() {
    return implementedTypes.size();
  }
  
}
