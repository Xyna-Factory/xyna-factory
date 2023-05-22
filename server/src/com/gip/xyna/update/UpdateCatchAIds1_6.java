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
package com.gip.xyna.update;



import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;



public class UpdateCatchAIds1_6 extends MDMUpdate {

  @Override
  protected Version getAllowedVersionForUpdate() throws XynaException {
    return new Version("1.5");
  }


  @Override
  protected Version getVersionAfterUpdate() throws XynaException {
    return new Version("1.6");
  }
  
  private static int getMaxId(Element e, int oldMaxId) {
    String id = e.getAttribute(GenerationBase.ATT.ID);
    if (id != null) {
      try {
        int idInt = Integer.valueOf(id);
        if (idInt > oldMaxId) {
          oldMaxId = idInt;
        }
      } catch (NumberFormatException ex) {
      }
    }
    List<Element> children = XMLUtils.getChildElements(e);
    for (Element child : children) {
      oldMaxId = Math.max(oldMaxId, getMaxId(child, oldMaxId));
    }
    return oldMaxId;
  }


  @Override
  protected void update(Document doc) throws XynaException {
    Element root = doc.getDocumentElement();
    int maxId = getMaxId(root, 0);
    if (root.getTagName().equals(GenerationBase.EL.SERVICE)) {
      List<Element> catches = XMLUtils.getChildElementsRecursively(root, GenerationBase.EL.CATCH);
      for (Element el : catches) {
        String id = el.getAttribute(GenerationBase.ATT.ID);
        if (id == null || id.length() == 0) {
          maxId++;
          el.setAttribute(GenerationBase.ATT.ID, "" + maxId);
        }
      }
    }
  }

}
