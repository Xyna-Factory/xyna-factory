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
package com.gip.xyna.xprc.xfractwfe.generation;

import com.gip.xyna.utils.exceptions.utils.codegen.InvalidClassNameException;
import com.gip.xyna.utils.exceptions.xmlstorage.InvalidValuesInXMLException;

/*
 * this abomination only exists because exception.generateJavaInternally is protected
 */
public class XynaExceptionCodeGenerator {
  
  private final ExceptionGeneration exception;
  
  public XynaExceptionCodeGenerator(ExceptionGeneration exception) {
    this.exception = exception;
  }
  
  
  public void generateJava(CodeBuffer cb) {
    try {
      cb.add(exception.generateJavaInternally(new CodeBuffer("Actually...I'm never used...neither is the second parameter..."), false)[0]);
    } catch (InvalidClassNameException e) {
      throw new RuntimeException(e);
    } catch (InvalidValuesInXMLException e) {
      throw new RuntimeException(e);
    }
  }

}
