/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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

package com.gip.xyna.xprc.xfractwfe.generation;

import java.util.HashSet;
import java.util.Set;

import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;


public class WfForTest extends WF {

  private static Set<GenerationBase> _cache = new HashSet<>();
  
  
  public WfForTest(String originalName, String wfInputName, Long revision) {
    super(originalName, wfInputName, revision);
  }

  
  public DOM getCachedDOMInstanceOrCreate(String originalDomInputName, long useRevision) throws XPRC_InvalidPackageNameException {
    for (GenerationBase gb : _cache) {
      if (gb instanceof DOM) {
        if (gb.getFqClassName().equals(originalDomInputName)) {
          return (DOM) gb;
        }
      }
    }
    return new DomForTest(originalDomInputName, originalDomInputName, -1L);
  }
  
  
  public void addToCache(GenerationBase gb) {
    _cache.add(gb);
  }
  
}
