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

package com.gip.xyna.xprc.xpce.dispatcher;

import com.gip.xyna.xprc.XynaOrderServerExtension.ExecutionType;



public class FractalWorkflowDestination extends DestinationValue {

  private static final long serialVersionUID = 7183569295153050072L;

  @Deprecated
  public FractalWorkflowDestination(String fqName, Long revision) {
    this(fqName);
  }
  
  public FractalWorkflowDestination(String fqName) {
    super(fqName);
  }


  @Override
  public ExecutionType getDestinationType() {
    return ExecutionType.XYNA_FRACTAL_WORKFLOW;
  }


  @Override
  public boolean isPoolable() {
    return true;
  }

}
