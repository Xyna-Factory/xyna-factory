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
package com.gip.xyna.utils.exception;



import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.gip.xyna.utils.exceptions.DuplicateExceptionCodeException;
import com.gip.xyna.utils.exceptions.ExceptionHandler;
import com.gip.xyna.utils.exceptions.XynaException;



public class MultipleExceptions extends XynaException {

  private static final long serialVersionUID = 1L;
  private static final String CODE = "XYNA-MULT";

  private final List<? extends Throwable> exceptions;

  static {
    try {
      ExceptionHandler.cacheErrorMessage(CODE, "Multiple (%0%) exceptions occurred: %1%", ExceptionHandler.LANG_EN);
      ExceptionHandler.cacheErrorMessage(CODE, "Es sind mehrere (%0%) Fehler aufgetreten: %1%", ExceptionHandler.LANG_DE);
    } catch (DuplicateExceptionCodeException e) {
      throw new RuntimeException(e);
    }
  }


  private MultipleExceptions(Collection<? extends Throwable> exceptions, String message) {
    super(CODE, new String[] {exceptions.size() + "", message});
    this.exceptions = new ArrayList<>(exceptions);
  }


  public static MultipleExceptions create(Collection<? extends Throwable> causes) {
    StringBuilder sb = new StringBuilder();
    for (Throwable t : causes) {
      sb.append("\n");
      sb.append(t.getClass().getSimpleName()).append(": ").append(t.getMessage());
    }
    MultipleExceptions t = new MultipleExceptions(causes, sb.toString());
    if (causes.size() > 0) {
      t.initCauses(causes.toArray(new Throwable[0]));
    }
    return t;
  }


  public List<? extends Throwable> getCauses() {
    return exceptions;
  }

}