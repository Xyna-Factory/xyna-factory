/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package xmcp.xypilot.impl.locator;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.FactoryManagedRevisionXMLSource;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

public class UnsavedChangesXmlSource extends FactoryManagedRevisionXMLSource {
  private Document xml;
  private String fqn;
  private Long revision;
  
  public UnsavedChangesXmlSource(String xml, String fqn, Long revision) throws XPRC_XmlParsingException {
    try {
      this.xml = XMLUtils.parseString(xml, true);
    } catch (Ex_FileAccessException e) {
      throw new XPRC_XmlParsingException("currentXml", e);
    }
    this.fqn = fqn;
    this.revision = revision;
  }
  

  @Override
  public Document getOrParseXML(GenerationBase generator, boolean fromDeploy) throws Ex_FileAccessException, XPRC_XmlParsingException {
    if(fqn.equals(generator.getOriginalFqName()) && revision.equals(generator.getRevision())) {
      return xml;
    }
    return super.getOrParseXML(generator, fromDeploy);
  }
  

  @Override
  public XMOMType determineXMOMTypeOf(String fqName, Long originalRevision) throws Ex_FileAccessException, XPRC_XmlParsingException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    var xmlSource = new DOMSource(xml);
    var outputTarget = new StreamResult(outputStream);
    try {
      TransformerFactory.newInstance().newTransformer().transform(xmlSource, outputTarget);
    } catch (Exception e) {
      return super.determineXMOMTypeOf(fqName, originalRevision);
    }
    try (InputStream is = new ByteArrayInputStream(outputStream.toByteArray())) {
      return XMOMType.getXMOMTypeByRootTag(XMLUtils.getRootElementName(is));
    } catch (Exception e) {

    }
    return super.determineXMOMTypeOf(fqName, originalRevision);
  }
}
