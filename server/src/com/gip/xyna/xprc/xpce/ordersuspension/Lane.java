/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package com.gip.xyna.xprc.xpce.ordersuspension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gip.xyna.utils.StringUtils;

/**
 * Lane besteht aus einer LaneId (Beispiel P1-3,P2-1). Über getLanePartsForParallelExecutors kann die Lane in einzelne LaneParts zerteilt werden,
 * diese bestehen aus Paaren, die mit Minuszeichen verbunden sind (parallelExecutorId-taskId).
 * 
 * Eine Lane(Id) bezeichnet die Koordinate/Pfad eines Knotens in der Hierarchie von ParallelExecutors
 * Ein LanePart ist der Pfadanteil pro ParallelExecutor.
 * 
 * Reihenfolge der LaneParts sind von Kind -&gt; Parent (vgl FractalProcessStep.getLaneId())
 */
public class Lane {
 
  
  public static class LanePart {
    private String parallelExecutorId;
    private String lanePart;
    public LanePart(String parallelExecutorId, String lanePart) {
      this.parallelExecutorId = parallelExecutorId;
      this.lanePart = lanePart; //peId-taskId
    }
    public String getParallelExecutorId() {
      return parallelExecutorId;
    }
    @Override
    public String toString() {
      return lanePart;
    }
    public String getLanePart() {
      return lanePart;
    }
  }

  
  private final String laneId;
  private List<LanePart> laneParts;
  
  public Lane(String laneId) {
    this.laneId = laneId;
  }

  @Override
  public int hashCode() {
    return laneId.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Lane other = (Lane) obj;
    return laneId.equals(other.laneId);
  }


  @Override
  public String toString() {
    return "Lane("+laneId+")";
  }

  public String getLaneId() {
    return laneId; //peId-taskId,peId-taskId
  }
  
  

  public List<LanePart> getLanePartsForParallelExecutors() {
    if( laneParts != null ) {
      return laneParts;
    }
    ArrayList<LanePart> lps = new ArrayList<LanePart>();
    for (String lp : StringUtils.fastSplit(laneId, ',', -1)) {
      String[] parts =  StringUtils.fastSplit(lp, '-', -1); //Format peId-taskId
      if (parts[0].indexOf('P') != -1 || parts[0].indexOf('F') != -1) {
        lps.add( new LanePart(parts[0], lp ) );
      }
      //else retry-id ignorieren
    }
    laneParts = Collections.unmodifiableList(lps);
    return laneParts;
  }

  /**
   * @return
   */
  public boolean isResumeAll() {
    return laneId == null;
  }

  public boolean isResumeParallelExecutor() {
    if( laneId == null ) {
      return true;
    }
    if( laneId.indexOf('P') != -1 ) {
      return true;
    }
    if( laneId.indexOf('F') != -1 ) {
      return true;
    }
    return false;
  }

  
}
