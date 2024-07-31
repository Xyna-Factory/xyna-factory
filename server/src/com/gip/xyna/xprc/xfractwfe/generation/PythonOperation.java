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
package com.gip.xyna.xprc.xfractwfe.generation;



import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xprc.xfractwfe.base.GenericInputAsContextStep;
import com.gip.xyna.xprc.xfractwfe.base.StartVariableContextStep;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.SpecialPurposeIdentifier;



public class PythonOperation extends CodeOperation {

  private static final Logger logger = CentralFactoryLogging.getLogger(PythonOperation.class);


  public PythonOperation(DOM parent) {
    super(parent, ATT.PYTHON);
  }


  protected void getImports(Set<String> imports) {
    for (AVariable v : getInputVars()) {
      if (v.isJavaBaseType) {
        if (v.getJavaTypeEnum() == PrimitiveType.ANYTYPE) {
          imports.add(XynaObject.class.getName());
        }
      } else if (v.getFQClassName() != null) {
        imports.add(v.getFQClassName());
      }
    }
    for (AVariable v : getOutputVars()) {
      if (v.isJavaBaseType) {
        if (v.getJavaTypeEnum() == PrimitiveType.ANYTYPE) {
          imports.add(XynaObject.class.getName());
        }
      } else if (v.getFQClassName() != null) {
        imports.add(v.getFQClassName());
      }
    }
    for (ExceptionVariable v : getThrownExceptions()) {
      if (v.getFQClassName() != null) {
        imports.add(v.getFQClassName());
      }
    }
    if (isSpecialPurpose(SpecialPurposeIdentifier.STARTDOCUMENTCONTEXT, SpecialPurposeIdentifier.STOPDOCUMENTCONTEXT,
                         SpecialPurposeIdentifier.RETRIEVEDOCUMENT)) {
      imports.add(StartVariableContextStep.class.getName());
    } else if (isSpecialPurpose(SpecialPurposeIdentifier.STARTGENERICCONTEXT, SpecialPurposeIdentifier.STOPGENERICCONTEXT)) {
      imports.add(GenericInputAsContextStep.class.getName());
    }
  }


  protected void generateJavaImplementationInternally(CodeBuffer cb) {
    cb.add(getImpl().trim()).addLB();
  }

}
