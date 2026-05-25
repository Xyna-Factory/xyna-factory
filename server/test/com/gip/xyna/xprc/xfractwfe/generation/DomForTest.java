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

import org.w3c.dom.Element;

import com.gip.xyna.xprc.exceptions.XPRC_InconsistentFileNameAndContentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.FactoryManagedRevisionXMLSource;


public class DomForTest extends DOM {

  private static GenerationBaseCache globalCache = new GenerationBaseCache();
  
  public DomForTest(String originalName, Element root) throws Exception {
    //super(originalName, originalName, -1L);
    super(originalName, originalName, globalCache, -1L, null, new FactoryManagedRevisionXMLSource());
    super.parseXmlInternally(root);
  }

  public DomForTest(String originalName, String domInputNameFQ, Long revision) {
    super(originalName, domInputNameFQ, globalCache, revision, null, new FactoryManagedRevisionXMLSource());
  }
  
}
