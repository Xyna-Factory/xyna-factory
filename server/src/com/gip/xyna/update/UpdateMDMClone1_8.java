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

import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableMemberNameException;
import com.gip.xyna.xprc.exceptions.XPRC_ParsingModelledExpressionException;
import com.gip.xyna.xprc.xfractwfe.formula.Assign;
import com.gip.xyna.xprc.xfractwfe.formula.TypeInfo;
import com.gip.xyna.xprc.xfractwfe.formula.Variable;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.IdentityCreationVisitor;
import com.gip.xyna.xprc.xfractwfe.generation.VariableContextIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;


public class UpdateMDMClone1_8 extends MDMUpdate {
  
  private static final Logger logger = CentralFactoryLogging.getLogger(UpdateMDMClone1_8.class);

  @Override
  protected Version getAllowedVersionForUpdate() throws XynaException {
    return new Version("1.7");
  }

  @Override
  protected Version getVersionAfterUpdate() throws XynaException {
    return new Version("1.8");
  }

  @Override
  protected void update(Document doc) throws XynaException {
    Element root = doc.getDocumentElement();
    
    if (root.getTagName().equals(GenerationBase.EL.SERVICE)) { 
      List<Element> operationElems = XMLUtils.getChildElementsByName(root, GenerationBase.EL.OPERATION);
      if (operationElems != null && operationElems.size() > 0) {
        for (Element operation : operationElems) {
          List<Element> mappingsElems = XMLUtils.getChildElementsRecursively(operation, GenerationBase.EL.MAPPINGS);
          for (Element mappings : mappingsElems) {
            if (mappings != null) {
              Element metaElem = XMLUtils.getChildElementByName(mappings, GenerationBase.EL.META);
              if (metaElem != null) {
                Element isTemplateElem = XMLUtils.getChildElementByName(metaElem, GenerationBase.EL.ISTEMPLATE);
                if (isTemplateElem != null) {
                  String isTemplateElemText = isTemplateElem.getTextContent();
                  if (isTemplateElemText != null && Boolean.valueOf(isTemplateElemText)) {
                    continue; //Templates nicht updaten
                  }
                }
              }
              //Mapping updaten: '=' mit '~=' vertauschen
              List<Element> mappingElems = XMLUtils.getChildElementsByName(mappings, GenerationBase.EL.MAPPING);
              for (Element mapping : mappingElems) {
                if (mapping != null) {
                  String mappingText = mapping.getTextContent();
                  if (mappingText != null && !"".equals(mappingText.trim())) {
                    try {
                      mapping.setTextContent(updateClone(mappingText));
                    } catch (XPRC_ParsingModelledExpressionException e) {
                      // TypeName="CoherenceWF" TypePath="cl"
                      logger.error("could not update mapping in " + root.getAttribute(GenerationBase.ATT.TYPEPATH) + "."
                                       + root.getAttribute(GenerationBase.ATT.TYPENAME), e);
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
  
  
  private static String updateClone(String xfl) throws XPRC_ParsingModelledExpressionException {
    ModelledExpression me = ModelledExpression.parse(new VariableContextIdentification() {
      public VariableInfo createVariableInfo(Variable v, boolean followAccessParts) throws XPRC_InvalidVariableIdException,
          XPRC_InvalidVariableMemberNameException {
        return null;
      }

      public TypeInfo getTypeInfo(String originalXmlName) {
        return null;
      }

      public Long getRevision() {
        return null;
      }

      public VariableInfo createVariableInfo(TypeInfo resultType) {
        return null;
      }
    }, xfl);
    
    Assign assign = me.getFoundAssign();
    if (assign != null) {
      IdentityCreationVisitor icv = new IdentityCreationVisitor();
      me.visitTargetExpression(icv);
      String targetExpression = icv.getXFLExpression();
      
      icv = new IdentityCreationVisitor();
      me.visitSourceExpression(icv);
      String sourceExpression = icv.getXFLExpression();
      
      Assign newAssign;
      switch(assign) {
        case DEEP_CLONE:
          newAssign = Assign.SHALLOW_CLONE;
          break;
        case SHALLOW_CLONE:
          newAssign = Assign.DEEP_CLONE;
          break;
        default:
          newAssign = assign;
      }
      return targetExpression + newAssign.toXFL() + sourceExpression;
    }
    
    return me.getExpression();
  }
}
