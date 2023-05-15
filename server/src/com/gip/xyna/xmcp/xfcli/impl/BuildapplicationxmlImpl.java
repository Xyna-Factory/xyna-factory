/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.xmcp.xfcli.impl;



import java.io.File;
import java.io.OutputStream;
import java.io.StringWriter;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Buildapplicationxml;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;



public class BuildapplicationxmlImpl extends XynaCommandImplementation<Buildapplicationxml> {
  

  public void execute(OutputStream statusOutputStream, Buildapplicationxml payload) throws XynaException {
    ApplicationManagementImpl appMgmt =
        (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();

    ApplicationXmlEntry entry = appMgmt.createApplicationDefinitionXml(payload.getApplicationName(), payload.getVersionName(), payload.getWorkspaceName(), payload.getCreateStub());

    if (payload.getMinify()) {
      entry.minify();
    }

    Document doc = null;
    try {
      doc = entry.buildXmlDocument();
    } catch (ParserConfigurationException e) {
      throw new XynaException("Exception occurred while building xml. ", e);
    }

    
    StringWriter sw = new StringWriter();
    XMLUtils.saveDomToWriter(sw, doc);
    File file = new File(payload.getFileName() != null ? payload.getFileName() : ApplicationManagementImpl.XML_APPLICATION_FILENAME);
    XMLUtils.saveDom(file, doc);
  }

}
