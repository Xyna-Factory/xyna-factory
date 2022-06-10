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
package com.gip.xyna.xprc;

import java.io.Serializable;

import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;

/**
 * gedacht als wrapper um einen workflow. die input und output parameter dieses workflows
 * werden auf die hier angegebenen input und output parameter gemappt.
 * beispiel:
 * <pre>
 * WorkflowAbstractionLayer&lt;InputType, OutputType&gt; wfal = ...;
 * InputType i = ...
 * OutputType o = wfal.createOutputData(startOrder(wfal.getOrderType(), wfal.createInputData(i)));
 * </pre>
 */
public interface WorkflowAbstractionLayer<I extends GeneralXynaObject, O extends GeneralXynaObject> extends Serializable {

  /**
   * mappt gegebene outputdaten auf verlangte outputdaten
   */
  public O createOutputData(GeneralXynaObject obj);

  /**
   * mappt gegebene inputdaten auf verlangte inputdaten und erstellt zusammen mit dem richtigen ordertype eine xynaorder
   */
  public XynaOrderServerExtension createOrder(I in, XynaOrderServerExtension redirectedOrder);

}
