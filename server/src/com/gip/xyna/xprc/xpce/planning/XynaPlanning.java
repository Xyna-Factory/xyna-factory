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
package com.gip.xyna.xprc.xpce.planning;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.XynaOrderServerExtension;



public class XynaPlanning extends FunctionGroup {

  public static final String DEFAULT_NAME = "Xyna Planning";

  private PlanningDispatcher planningEngineDispatcher;


  public XynaPlanning() throws XynaException {
    super();
  }


  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  public void init() throws XynaException {
    planningEngineDispatcher = new PlanningDispatcher();
  }


  public void shutdown() throws XynaException {
  }


  public void validate(XynaOrder xo) {
  }


  public void dispatch(XynaOrderServerExtension xo) throws XynaException {
    planningEngineDispatcher.dispatch(xo);
  }


  public PlanningDispatcher getPlanningDispatcher() {
    return planningEngineDispatcher;
  }

}
