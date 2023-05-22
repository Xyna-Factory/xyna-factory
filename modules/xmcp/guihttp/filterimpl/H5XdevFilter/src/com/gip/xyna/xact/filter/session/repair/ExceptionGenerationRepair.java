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

package com.gip.xyna.xact.filter.session.repair;



import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;



public class ExceptionGenerationRepair extends DOMandExceptionRepair<ExceptionGeneration> {

  @Override
  protected ExceptionGeneration getDomOrExceptionGenerationBaseObj(GenerationBaseObject obj) {
    return obj.getExceptionGeneration();
  }


  @Override
  protected void replaceParent(ExceptionGeneration obj) {
    ExceptionGeneration exceptionGeneration = createXynaExceptionBaseExceptionGeneration(obj.getRevision());
    obj.replaceParent(exceptionGeneration);
  }


  @Override
  public boolean responsible(GenerationBaseObject obj) {
    return obj.getGenerationBase() instanceof ExceptionGeneration;
  }


  public static ExceptionGeneration createXynaExceptionBaseExceptionGeneration(long revision) {
    return createExceptionGeneration("core.exception.XynaExceptionBase", revision);
  }


  private static ExceptionGeneration createExceptionGeneration(String fqn, long revision) {
    ExceptionGeneration result = null;
    try {
      result = ExceptionGeneration.getOrCreateInstance(fqn, new GenerationBaseCache(), revision);
    } catch (XPRC_InvalidPackageNameException e) {
      throw new RuntimeException("Could not create parent replacement");
    }

    return result;
  }
}
