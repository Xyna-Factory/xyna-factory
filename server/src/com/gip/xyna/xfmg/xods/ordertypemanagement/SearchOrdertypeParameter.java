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
package com.gip.xyna.xfmg.xods.ordertypemanagement;

import java.io.Serializable;

import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;


public class SearchOrdertypeParameter implements Serializable {

  private static final long serialVersionUID = 1L;
  
  private final RuntimeContext ctx;
  private final boolean includeRequirements;
  
  
 private SearchOrdertypeParameter(RuntimeContext ctx, boolean includeRequirements) {
    this.ctx = ctx;
    this.includeRequirements = includeRequirements;
  }
  
  
  public RuntimeContext getRuntimeContext() {
    return ctx;
  }

  
  public boolean includeRequirements() {
    return includeRequirements;
  }
  
  
  public boolean allOrdertypes() {
    return ctx == null;
  }
  
  
  public static SearchOrdertypeParameter all() {
    return new SearchOrdertypeParameter(null, false);
  }
  
  
  public static SearchOrdertypeParameter single(RuntimeContext ctx) {
    return new SearchOrdertypeParameter(ctx, false);
  }
  
  
  public static SearchOrdertypeParameter hierarchy(RuntimeContext ctx) {
    return new SearchOrdertypeParameter(ctx, true);
  }

}
