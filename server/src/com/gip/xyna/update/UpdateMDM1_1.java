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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.exceptions.XPRC_FILE_UPDATE_ERROR;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;



public class UpdateMDM1_1 extends MDMUpdate {

  private static Logger logger = CentralFactoryLogging.getLogger(UpdateMDM1_1.class);

  public static final String TARGET_MDM_VERSION = "1.1";

  protected void update(Document doc) throws XynaException {
    fixServiceReference(doc);
  }


  protected Version getAllowedVersionForUpdate() {
    return new Version(Updater.START_MDM_VERSION);
  }


  protected Version getVersionAfterUpdate() throws XynaException {
    Version v = new Version(Updater.START_MDM_VERSION);
    // letzte stelle um eins erhöhen
    v.increaseToNextMajorVersion(v.length());
    return v;
  }


  private void fixServiceReference(Document doc) throws XynaException {
    Element root = doc.getDocumentElement();
    if (root.getTagName().equals(GenerationBase.EL.SERVICE)) {
      NodeList list = root.getElementsByTagName(GenerationBase.EL.SERVICEREFERENCE);
      for (int i = 0; i < list.getLength(); i++) {
        Element serviceRef = (Element) list.item(i);
        String path = serviceRef.getAttribute(GenerationBase.ATT.REFERENCEPATH);
        String name = serviceRef.getAttribute(GenerationBase.ATT.REFERENCENAME);
        String dtName = path.substring(path.lastIndexOf(".") + 1, path.length());
        if (path.contains("."))
          path = path.substring(0, path.lastIndexOf("."));
        else {
          if (path.length() != 0) {
            throw new XPRC_FILE_UPDATE_ERROR(root.getAttribute(GenerationBase.ATT.TYPEPATH) + "."
                            + root.getAttribute(GenerationBase.ATT.TYPENAME),
                                             "Existing file does not comply with previous version "
                                                             + Updater.START_MDM_VERSION, TARGET_MDM_VERSION);
          }
        }
        name = dtName + "." + name;
        serviceRef.setAttribute(GenerationBase.ATT.REFERENCEPATH, path);
        serviceRef.setAttribute(GenerationBase.ATT.REFERENCENAME, name);
      }
    } else {
      // datentypen benötigen keine änderungen
    }
    NodeList nl = doc.getChildNodes();
    if (nl.getLength() == 0 || nl.item(0).getNodeType() != Node.COMMENT_NODE) {
      doc.insertBefore(doc.createComment("\n *\n" 
                      + " * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -\n"
                      + " * Copyright 2022 GIP SmartMercial GmbH, Germany\n" 
                      + " *\n" 
                      + " * Licensed under the Apache License, Version 2.0 (the \"License\");\n"
                      + " * you may not use this file except in compliance with the License.\n"
                      + " * You may obtain a copy of the License at\n" 
                      + " *\n"
                      + " * http://www.apache.org/licenses/LICENSE-2.0\n"
                      + " *\n"
                      + " * distributed under the License is distributed on an \"AS IS\" BASIS,\n"
                      + " * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n"
                      + " * See the License for the specific language governing permissions and\n"
                      + " * limitations under the License.\n"
                      + " * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -\n *\n"), root);
    }
  }


}
