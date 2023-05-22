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
package com.gip.xyna.xfmg.xods.orderinputsource.constant;



import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItemBuilder;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.DeploymentItemBuilder.DetachedOrderTypeEmployment;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.OperationInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.TypeInterface;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputCreationInstance;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSource;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceManagement.OptionalOISGenerateMetaInformation;
import com.gip.xyna.xmcp.RemoteXynaOrderCreationParameter;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DatatypeVariable;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;



public class ConstantInputSource implements OrderInputSource, OrderInputCreationInstance {

  private static final Logger logger = CentralFactoryLogging.getLogger(ConstantInputSource.class);
  private final RemoteXynaOrderCreationParameter constantParas;
  private final String payloadXML;


  public ConstantInputSource(RemoteXynaOrderCreationParameter constantParas, String payloadXML) {
    this.constantParas = constantParas;
    this.payloadXML = payloadXML;
  }


  public OrderInputCreationInstance createInstance() {
    return this;
  }


  public XynaOrderCreationParameter generate(long generationContextId,
                                             OptionalOISGenerateMetaInformation parameters) throws XynaException {
    //clonen, damit nicht bei parallelem start mehrere instanzen die parameter durcheinander kommen
    RemoteXynaOrderCreationParameter xocp = new RemoteXynaOrderCreationParameter(constantParas);

    //immer neuen destinationkey setzen, weil die instanzen nicht immutable sind.
    constantParas.setDestinationKey(new DestinationKey(xocp.getDestinationKey().getOrderType(), xocp.getDestinationKey()
        .getRuntimeContext()));

    //immer erst beim starten des auftrags konvertieren, weil ansonsten die classloader evtl veraltet sind - es gibt hier keinen mechanismus, der f�r ein reload sorgen w�rde.
    //FIXME performance: das convert sparen, wenn sich nichts am classloader ge�ndert hat...
    xocp.setInputPayload(payloadXML);
    xocp.convertInputPayload();
    return xocp;
  }


  public void notifyOnOrderStart() {
    //ntbd    
  }


  public Set<DeploymentItemInterface> getDeployedInterfaces() {
    Set<DeploymentItemInterface> l = new HashSet<DeploymentItemInterface>();

    List<TypeInterface> input = new ArrayList<TypeInterface>();

    //alle typen/membervariablen, die in der konstante verwendet werden
    Document doc;
    try {
      doc = XMLUtils.parseString(payloadXML);
    } catch (XPRC_XmlParsingException e) {
      throw new RuntimeException(e); //wurde ja bereits gecheckt
    }
    Element root = doc.getDocumentElement();
    if (!root.getTagName().equals(GenerationBase.EL.DATA)) {
      //wrapper
      List<Element> varEls = XMLUtils.getChildElements(root);
      if (varEls.size() > 1) {
        for (Element e : varEls) {
          if (!e.getTagName().equals(GenerationBase.EL.DATA)) {
            continue;
          }
          try {
            add(e, input, l);
          } catch (Exception ex) {
            logger.debug("parsing problems with input element", ex);
            continue;
          }
        }
      } else if (varEls.size() == 1) {
        try {
          add(varEls.get(0), input, l);
        } catch (Exception ex) {
          logger.debug("parsing problems with input element", ex);
        }
      } else {
      }
    } else {
      //einzelnes data element
      try {
        add(root, input, l);
      } catch (Exception ex) {
        logger.debug("parsing problems with input element", ex);
      }
    }

    //verwendete inputschnittstelle vom aufgerufenen ordertype:
    OperationInterface opif = OperationInterface.of(null, input, null);
    l.add(new DetachedOrderTypeEmployment(constantParas.getDestinationKey(), opif));

    return l;
  }


  private void add(Element e, List<TypeInterface> input, Set<DeploymentItemInterface> l) throws XynaException {
    String originalClassName = e.getAttribute(GenerationBase.ATT.REFERENCENAME);
    String originalPath = e.getAttribute(GenerationBase.ATT.REFERENCEPATH);
    if (isEmpty(originalPath) && isEmpty(originalClassName)) {
      return;
    }

    long revision =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
            .getRevision(constantParas.getDestinationKey().getRuntimeContext());
    String originalFqName = originalPath + "." + originalClassName;
    DOM dom = DOM.getOrCreateInstance(originalFqName, new GenerationBaseCache(), revision);
    dom.parseGeneration(true, false, false); //cache darf nicht geleert werden, weil er dann im parsexml von der variable verwendet wird
    DatatypeVariable v = new DatatypeVariable(dom);
    v.parseXML(e);
    v.fillVariableContents();
    TypeInterface providingType = TypeInterface.of(dom);
    l.add(providingType);
    input.add(providingType);

    for (AVariable childVar : v.getChildren()) {
      DeploymentItemBuilder.extractConstants(providingType, childVar, v.isList(), l);
    }
  }


  private boolean isEmpty(String s) {
    return s == null || s.length() == 0;
  }

}
