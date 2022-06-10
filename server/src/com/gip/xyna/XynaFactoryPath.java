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

package com.gip.xyna;


public class XynaFactoryPath {

  private Class<? extends Department> department;
  private Class<? extends Section> section;
  private Class<? extends FunctionGroup> functionGroup;


  public XynaFactoryPath(Class<? extends Department> d, Class<? extends Section> s, Class<? extends FunctionGroup> fg) {
    department = d;
    section = s;
    functionGroup = fg;
  }
  
  public XynaFactoryPath(Class<? extends Department> d, Class<? extends Section> s) {
    department = d;
    section = s;
  }

  public Class<? extends Department> getDepartment() {
    return department;
  }


  public Class<? extends Section> getSection() {
    return section;
  }


  public Class<? extends FunctionGroup> getFunctionGroup() {
    return functionGroup;
  }

}
