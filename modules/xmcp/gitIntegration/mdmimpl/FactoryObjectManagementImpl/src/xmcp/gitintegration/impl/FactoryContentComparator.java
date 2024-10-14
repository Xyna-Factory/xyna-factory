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
package xmcp.gitintegration.impl;

import java.util.List;

import xmcp.gitintegration.FactoryContent;
import xmcp.gitintegration.FactoryContentDifference;
import xmcp.gitintegration.FactoryContentDifferences;
import xmcp.gitintegration.impl.processing.FactoryContentProcessingPortal;
import xmcp.gitintegration.storage.FactoryDifferenceListStorage;

public class FactoryContentComparator {

  
  public FactoryContentDifferences compareFactoryContent(FactoryContent fc1, FactoryContent fc2, boolean persist) {
    FactoryContentDifferences result = new FactoryContentDifferences();
    FactoryContentProcessingPortal portal = new FactoryContentProcessingPortal();
    List<FactoryContentDifference> itemDifferences = portal.compare(fc1, fc2);

    long id = -1l;
    
    result.setDifferences(itemDifferences);
    result.setListId(id);
    
    if (persist && !itemDifferences.isEmpty()) {
      FactoryDifferenceListStorage storage = new FactoryDifferenceListStorage();
      storage.persist(result);
    }
    
    return result;
  }
}
