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

import org.w3c.dom.Document;

import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.FactoryManagedRevisionXMLSource;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

public class UnsavedChangesXmlSource extends FactoryManagedRevisionXMLSource {
  private Document xml;
  private String fqn;
  private Long revision;
  
  public UnsavedChangesXmlSource(String xml, String fqn, Long revision) throws XPRC_XmlParsingException {
    this.xml = XMLUtils.parseString(xml);
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
}
