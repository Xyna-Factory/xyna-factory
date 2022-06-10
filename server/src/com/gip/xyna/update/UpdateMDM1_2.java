/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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



import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;



public class UpdateMDM1_2 extends MDMUpdate {

  private static Logger logger = CentralFactoryLogging.getLogger(UpdateMDM1_2.class);

  public static final String TARGET_MDM_VERSION = "1.2";

  protected void update(Document doc) throws XynaException {
    logger.debug("Fixing namespace");
    fixNameSpace(doc);
  }


  protected Version getAllowedVersionForUpdate() {
    return new Version(UpdateMDM1_1.TARGET_MDM_VERSION);
  }


  protected Version getVersionAfterUpdate() throws XynaException {
    Version v = getAllowedVersionForUpdate();
    // letzte stelle um eins erhöhen
    v.increaseToNextMajorVersion(v.length());
    return v;
  }


  private void fixNameSpace(Document doc) throws XynaException {
    Element root = doc.getDocumentElement();
    if (root.getAttribute(GenerationBase.ATT.XMLNS) == null || !root.getAttribute(GenerationBase.ATT.XMLNS).equals(Constants.FRACTAL_MODELLING_NAMESPACE)) {
      root.setAttribute(GenerationBase.ATT.XMLNS, Constants.FRACTAL_MODELLING_NAMESPACE);
    }
  }


}
