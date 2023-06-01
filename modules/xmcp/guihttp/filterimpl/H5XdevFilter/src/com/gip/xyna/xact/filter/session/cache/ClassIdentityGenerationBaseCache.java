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

package com.gip.xyna.xact.filter.session.cache;

import com.gip.xyna.xact.filter.session.FQName;
import com.gip.xyna.xact.filter.session.XMOMLoader;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;

public class ClassIdentityGenerationBaseCache extends ClassIdentityCache<GenerationBase>{

  
  protected GenerationBase addOrReplaceEntry(String fqXmlName, Long revision, Class<? extends GeneralXynaObject> clazz) {
    try {
      GenerationBase gb = XMOMLoader.loadNewGB(new FQName(revision, fqXmlName));
      synchronized (actualCache) {
        actualCache.put(clazz, gb);
      }

      return gb;
    } catch (Exception e) {
      return null;
    }
  }
}
