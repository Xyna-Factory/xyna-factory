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



package com.gip.xyna.xprc.xfractwfe.generation;

import com.gip.xyna.utils.exceptions.ExceptionStorage;
import com.gip.xyna.utils.exceptions.utils.codegen.FqClassNameAdapter;


public class ExceptionGenFqClassNameAdapter extends FqClassNameAdapter {

  @Override
  public String adaptFqClassName(String fqClassName) {
    if (GenerationBase.isReservedServerObjectByFqOriginalName(fqClassName)) {
      Class<?> clazz = GenerationBase.getReservedClass(fqClassName);
      return clazz.getName();
    }
    return fqClassName;
  }
  
  
  public static void main(String[] args) {
    ExceptionStorage.mainImpl(args, new ExceptionGenFqClassNameAdapter());
  }
  
}
