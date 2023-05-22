/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.xfmg.xfmon.fruntimestats.path;

import java.util.List;


public interface StatisticsPath {
  
  public List<StatisticsPathPart> getPath();
  
  public StatisticsPathPart getPathPart(int index);
  
  public StatisticsPath append(StatisticsPathPart part);
  
  public StatisticsPath append(StatisticsPath part);
  
  public StatisticsPath append(String part);
  
  public int length();
  
  public boolean isSimple(); 
  
  
  public static interface StatisticsPathPart {

    public String getPartName();
    
    public StatisticsNodeTraversal getStatisticsNodeTraversal();
    
  }
  
  
  public static interface UnknownPathHandlingHolder {
    
    public UnknownPathOnTraversalHandling getUnknownPathHandling();
    
  }
  

  public static enum StatisticsNodeTraversal {
    SINGLE, MULTI, MULTI_OR_SELF, MULTI_AND_SELF, ALL, ALL_OR_SELF, ALL_AND_SELF;
  }
  
  public static enum UnknownPathOnTraversalHandling {
    IGNORE, THROW_IF_ANY, THROW_IF_ALL;
  }
  
}
