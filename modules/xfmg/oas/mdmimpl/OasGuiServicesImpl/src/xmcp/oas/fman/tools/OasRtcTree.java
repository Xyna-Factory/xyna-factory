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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class OasRtcTree {

  private List<OasRtcSubtree> subtrees = new ArrayList<>();
  private Map<ImplementedOasApiType, OasRtcSubtree> subtreeOfImplTypeMap = new HashMap<>();
  private static RtcTools tools = new RtcTools();
  
  
  public OasRtcTree(GeneratedOasApiType goat, List<ImplementedOasApiType> implList) {
    Set<RtcData> set = tools.getAllRtcsWhichReferenceRtcRecursiveLimited(goat.getRtc(), 1);
    for (RtcData rtc : set) {
      subtrees.add(new OasRtcSubtree(rtc));
    }
    for (ImplementedOasApiType ioat: implList) {
      register(ioat);
    }
  }
  
  
  private void register(ImplementedOasApiType ioat) {
    OasRtcSubtree subtree = findSubtree(ioat);
    subtree.register(ioat);
    subtreeOfImplTypeMap.put(ioat, subtree);
  }
  
  
  private OasRtcSubtree findSubtree(ImplementedOasApiType ioat) {
    for (OasRtcSubtree subtree : subtrees) {
      if (subtree.contains(ioat)) {
        return subtree;
      }
    }
    throw new RuntimeException("Inconsistent data: Could not find OasRtcSubtree which contains xmom type " +
                               ioat.getXmomType().getFqName());
  }
  
  
  public int countImplementedTypesInSubtree(ImplementedOasApiType ioat) {
    OasRtcSubtree subtree = subtreeOfImplTypeMap.get(ioat);
    if (subtree == null) {
      throw new RuntimeException("Inconsistent data: Could not find OasRtcSubtree which contains xmom type " +
                                 ioat.getXmomType().getFqName());
    }
    return subtree.getImplementedTypeCount();
  }
  
}
