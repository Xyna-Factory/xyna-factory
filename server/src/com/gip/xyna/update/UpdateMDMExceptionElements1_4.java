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

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

/**
 *  1.3 &lt;Exception&gt; elements get referencename and referencepath if it does not exist 
 */
public class UpdateMDMExceptionElements1_4 extends MDMUpdate {

  private static final Logger logger = CentralFactoryLogging.getLogger(UpdateMDMExceptionElements1_4.class);


  @Override
  protected Version getAllowedVersionForUpdate() throws XynaException {
    return new Version("1.3");
  }


  @Override
  protected Version getVersionAfterUpdate() throws XynaException {
    return new Version("1.4");
  }


  @Override
  protected void update(Document doc) throws XynaException {

    Element root = doc.getDocumentElement();
    String currentFqName = root.getAttribute(GenerationBase.ATT.TYPEPATH) + "." + root
                    .getAttribute(GenerationBase.ATT.TYPENAME);
    if (logger.isDebugEnabled()) {
      logger.debug("Updating '" + currentFqName + "' from version " + getAllowedVersionForUpdate() + " ");
    }
    List<Element> exceptionElements = XMLUtils.getChildElementsRecursively(root, GenerationBase.EL.EXCEPTION);
    for (Element exceptionElement : exceptionElements) {
      String oldRefName = exceptionElement.getAttribute(GenerationBase.ATT.REFERENCENAME);
      String oldRefPath = exceptionElement.getAttribute(GenerationBase.ATT.REFERENCEPATH);
      boolean oldRefNameIsEmpty = oldRefName == null || oldRefName.length() == 0;
      boolean oldRefPathIsEmpty = oldRefPath == null || oldRefPath.length() == 0;
      if (oldRefNameIsEmpty || oldRefPathIsEmpty) {
        if (!(oldRefNameIsEmpty && oldRefPathIsEmpty)) {
          throw new XPRC_XmlParsingException(currentFqName, new Exception(
                       "Attributes ReferenceName and ReferencePath have to be either both non null or null"));
        }
        exceptionElement.setAttribute(GenerationBase.ATT.REFERENCENAME,
                                      GenerationBase.DEFAULT_EXCEPTION_REFERENCE_NAME);
        exceptionElement.setAttribute(GenerationBase.ATT.REFERENCEPATH,
                                      GenerationBase.DEFAULT_EXCEPTION_REFERENCE_PATH);
      }
    }

  }

}
