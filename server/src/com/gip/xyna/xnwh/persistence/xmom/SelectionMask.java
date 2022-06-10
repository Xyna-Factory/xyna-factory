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
package com.gip.xyna.xnwh.persistence.xmom;

import java.util.Arrays;
import java.util.List;


// TODO could be server side representation for xmom-obj
public class SelectionMask {
  
  String roottype;
  List<String> columns;
  
  public SelectionMask(String roottype, List<String> columns) {
    this.roottype = roottype;
    this.columns = columns;
  }
  
  public SelectionMask(String roottype, String... columns) {
    this(roottype, Arrays.asList(columns));
  }

}
