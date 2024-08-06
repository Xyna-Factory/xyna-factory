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
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.ServiceStepEventHandling;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.ServiceStepEventSource;



public class JavaOperation extends CodeOperation {

  private static Logger logger = CentralFactoryLogging.getLogger(JavaOperation.class);


  public JavaOperation(DOM parent) {
    super(parent, ATT.JAVA);
  }


  protected void generateJavaImplementationInternally(CodeBuffer cb) {
    cb.add(getImpl().trim()).addLB();
  }


  public void createProjectServiceImpl(CodeBuffer cb, Set<String> importedClassesFqStrings) {
    if (isStepEventListener()) {
      cb.addLine("//Obtaining ServiceStepEventSource");
      cb.addLine(ServiceStepEventSource.class.getSimpleName(), " eventSource = ", ServiceStepEventHandling.class.getSimpleName(),
                 ".getEventSource();");
      cb.addLine("//TODO implement your ServiceStepEventHandler then listen to events:");
      cb.addLine("//eventSource.listenOnAbortEvents(myServiceStepEventHandler);");
    }
    cb.addLine("//TODO implementation");
    cb.addLine("//TODO update dependency XML");
    if (getOutputVars().size() == 1) {
      AVariable output = getOutputVars().get(0);
      if (output.getDomOrExceptionObject() != null && output.getDomOrExceptionObject().isAbstract()) {
        cb.addLine("return null");
      } else {
        if (output.isList()) {
          cb.addLine("return new Array" // FIXME nasty hack!! (erzeugt wird new ArrayList<...>)
              + output.getEventuallyQualifiedClassNameWithGenerics(importedClassesFqStrings, false) + "()");
        } else if (output.isJavaBaseType()) {
          PrimitiveType javaType = output.getJavaTypeEnum();
          cb.addLine("return ", javaType.getDefaultConstructor());
        } else {
          cb.add("return ");
          output.generateConstructor(cb, importedClassesFqStrings);
          cb.addLB();
        }
      }
    } else if (getOutputVars().size() > 1) {
      cb.add("return new " + Container.class.getSimpleName() + "(");
      for (AVariable v : getOutputVars()) {
        if (v.isList()) {
          // FIXME the following "Array" prefix is a nasty hack to get an instantiable list class  (erzeugt wird new ArrayList<...>)
          cb.addListElement("new " + XynaObjectList.class.getSimpleName() + "<"
              + v.getEventuallyQualifiedClassNameNoGenerics(importedClassesFqStrings) + ">(new Array"
              + v.getEventuallyQualifiedClassNameWithGenerics(importedClassesFqStrings, false) + "(), "
              + v.getEventuallyQualifiedClassNameNoGenerics(importedClassesFqStrings) + ".class)");
        } else if (v.getDomOrExceptionObject().isAbstract()) {
          cb.addListElement("null /* " + v.getEventuallyQualifiedClassNameWithGenerics(importedClassesFqStrings) + " */");
        } else if (v.getDomOrExceptionObject() instanceof ExceptionGeneration) {
          // cb.addListElement("new " + v.getEventuallyQualifiedClassNameWithGenerics(importedClassesFqStrings, false) + "(\"TODO parameters\")");
          cb.addListElement("(" + v.getEventuallyQualifiedClassNameWithGenerics(importedClassesFqStrings, false) + ") null");
        } else {
          cb.addListElement("new " + v.getEventuallyQualifiedClassNameWithGenerics(importedClassesFqStrings, false) + "()");
        }
      }
      cb.add(")").addLB();
    }
  }


  //TODO diese methode rät derzeit, ob in java implementiert ist (werden soll), anhand des generierten codes.
  public boolean implementedInJavaLib() {
    if (isAbstract()) {
      return false;
    }
    if (hasEmptyImpl()) {
      return false;
    }
    String implInvocation = createImplCallSnippet(true, false);
    boolean isImpl = getImpl().trim().equals(implInvocation);
    if (!isImpl) {
      implInvocation = createImplCallSnippet(false, false);
      isImpl = getImpl().trim().equals(implInvocation);
    }
    return isImpl;
  }


  public String createImplCallSnippet(boolean includePackageName, boolean andSet) {
    CodeBuffer cb = new CodeBuffer("temp");
    if (getOutputVars() != null && getOutputVars().size() > 0) {
      cb.add("return ");
    }
    if (isStatic()) {
      if (includePackageName) {
        cb.add(getParent().getFqClassName() + "Impl.");
      } else {
        cb.add(getParent().getSimpleClassName() + "Impl.");
      }
    } else {
      cb.add(GenerationBase.buildGetter(DOM.INSTANCE_METHODS_IMPL_VAR), "()", ".");
    }
    generateJavaForInvocation(cb, getName());
    cb.addLB();
    String impl = cb.toString(false).trim();
    if (andSet) {
      setImpl(impl);
    }
    return impl;
  }

}
