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
package com.gip.xyna.xmcp;

import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXMLForObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;


/**
 * Erweiterung der XynaOrderCreationParameter, da die in XynaOrderCreationParameter
 * enthaltene InputPayload als XynaObject nicht außerhalb der Factory 
 * deserialisiert werden kann.
 *
 */
public final class RemoteXynaOrderCreationParameter extends XynaOrderCreationParameter {

  private static final long serialVersionUID = 941920723981666044L;


  public RemoteXynaOrderCreationParameter(DestinationKey dk, GeneralXynaObject... inputPayload) {
    super(dk, inputPayload);
  }

  
  public RemoteXynaOrderCreationParameter(XynaOrderCreationParameter xocp) {
    super(xocp);
    setInputPayload(xocp.getInputPayload());
    removeXynaObjectInputPayload();
  }


  private String inputPayloadAsXmlString;
  private boolean isContainer = true;


  @Override
  public void setInputPayload(GeneralXynaObject payload) {
    if (payload != null) {
      inputPayloadAsXmlString = payload.toXml();
      isContainer = payload instanceof Container;
    }
  }


  @Override
  public void setInputPayload(GeneralXynaObject... payload) {
    if (payload != null) {
      inputPayloadAsXmlString = new Container(payload).toXml();
      isContainer = payload.length != 1;
    }
  }

  /**
   * unterstützte payloads
   * - &lt;Data&gt;/&lt;ExceptionStorage&gt; element
   * - Wrapper-Element um Liste von &lt;Data&gt;/&lt;ExceptionStorage&gt; elemente
   * @param payload
   */
  public void setInputPayload(String payload) {
    if (payload == null) {
      inputPayloadAsXmlString = null;
      isContainer = true;
      return;
    }
    Document doc;
    try {
      doc = XMLUtils.parseString(payload);
    } catch (XPRC_XmlParsingException e) {
      throw new RuntimeException(e);
    }
    String rootElName = doc.getDocumentElement().getNodeName();
    if (rootElName.equals(GenerationBase.EL.DATA) || rootElName.equals(GenerationBase.EL.EXCEPTIONSTORAGE)) {
      isContainer = false;
      inputPayloadAsXmlString = payload;
    } else {
      //wrapper element
      isContainer = true;
      List<Element> childEls = XMLUtils.getChildElements(doc.getDocumentElement());
      StringBuilder xmlWithoutWrapperEl = new StringBuilder();
      for (Element child : childEls) {
        xmlWithoutWrapperEl.append(XMLUtils.getXMLString(child, false));
      }
      inputPayloadAsXmlString = xmlWithoutWrapperEl.toString();
    }
  }

  
  public void convertInputPayload() throws XPRC_XmlParsingException, XPRC_InvalidXMLForObjectCreationException, XPRC_MDMObjectCreationException {
    if (inputPayloadAsXmlString == null) {
      super.setInputPayload(new Container());
    } else {
      Long revision;
      try {
        revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(getDestinationKey().getRuntimeContext());
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        CentralFactoryLogging.getLogger(RemoteXynaOrderCreationParameter.class).warn("Could not find revision for destinationKey trying WorkingSet",e);
        revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
      }
      String xmlString = inputPayloadAsXmlString;
      if (isContainer && !XynaProperty.CONTAINER_XML_WRAP.get()) {
        xmlString = "<Container>" + xmlString + "</Container>";
      }
      super.setInputPayload(XynaObject.generalFromXml(xmlString, revision));
    }
    inputPayloadAsXmlString = null;
    isContainer = true;
  }

  /**
   * gibt xml immer ohne wrapper-element zurück
   * @return
   */
  public String getInputPayloadAsXML() {
    return inputPayloadAsXmlString;
  }
  
  /**
   * Entfernen der XynaObject-InputPayload, damit die RemoteXorderCreationParameter
   * auch wieder außerhalb der Factory deserialisiert werden kann
   */
  public void removeXynaObjectInputPayload() {
    super.setInputPayload((GeneralXynaObject)null);
  }


}
