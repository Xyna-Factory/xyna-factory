/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
package xprc.xpce.datatype.impl;



import base.Text;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;

import java.lang.ClassNotFoundException;

import org.apache.log4j.Logger;

import xprc.xpce.datatype.NamedXMOMMemberSuperProxy;
import xprc.xpce.datatype.NamedXMOMMemberInstanceOperation;
import xprc.xpce.datatype.NamedXMOMMember;



public class NamedXMOMMemberInstanceOperationImpl extends NamedXMOMMemberSuperProxy implements NamedXMOMMemberInstanceOperation {

  private static final long serialVersionUID = 1L;
  private static Logger logger = CentralFactoryLogging.getLogger(NamedXMOMMemberInstanceOperationImpl.class);


  public NamedXMOMMemberInstanceOperationImpl(NamedXMOMMember instanceVar) {
    super(instanceVar);
  }


  public Text getDocumentationAsText() {
    return new Text(getInstanceVar().getDocumentation());
  }


  public Text getLabelAsText() {
    return new Text(getInstanceVar().getLabel());
  }


  public GeneralXynaObject getMemberObject() {
    return anyType;
  }


  private GeneralXynaObject anyType;
  private DOM dom;


  public void setAnyType(GeneralXynaObject anyType) {
    this.anyType = anyType;
  }


  public void setDOM(DOM dom) {
    this.dom = dom;
  }


  public Boolean isInstanceOf(Text text) {
    try {
      if (logger.isInfoEnabled()) {
        logger.info("isInstanceOf: " + text.getText());
      }
      if (this.dom != null) {
        DOM currentDOM = dom;
        while (currentDOM != null) {
          if (logger.isInfoEnabled()) {
            logger.info("currentDOM: " + currentDOM.getOriginalFqName());
            logger.info("is equal: " + currentDOM.getOriginalFqName().equals(text.getText()));
          }
          if (currentDOM.getOriginalFqName().equals(text.getText())) {
            if (logger.isInfoEnabled()) {
              logger.info("returning true");
            }
            return true;
          }
          currentDOM = currentDOM.getSuperClassGenerationObject();
        }
      }
      return false;

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  public Boolean isReservedObject() {
    return GenerationBase.isReservedServerObjectByFqOriginalName(getInstanceVar().getFQXMLName());
  }


  public void setMemberObject(GeneralXynaObject anyType) {
    this.anyType = anyType;
    try {
      parentObject.set(getInstanceVar().getVarName(), anyType);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  private GeneralXynaObject parentObject;


  public void setParentObject(GeneralXynaObject parentObject) {
    this.parentObject = parentObject;
  }


  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    s.defaultWriteObject();
  }


  private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    s.defaultReadObject();
  }

}
