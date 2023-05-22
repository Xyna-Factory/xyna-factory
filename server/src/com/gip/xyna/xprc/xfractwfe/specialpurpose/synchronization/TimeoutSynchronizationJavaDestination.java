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

package com.gip.xyna.xprc.xfractwfe.specialpurpose.synchronization;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xfractwfe.XynaFractalWorkflowEngine;
import com.gip.xyna.xprc.xpce.dispatcher.JavaDestination;


public class TimeoutSynchronizationJavaDestination extends JavaDestination {

  private static final long serialVersionUID = -8006998606487346885L;

  public static final String TIMEOUT_SYNCHRONIZATION_DESTINATION = "com.gip.xyna.TimeoutSynchronization";


  public TimeoutSynchronizationJavaDestination() {
    super(TIMEOUT_SYNCHRONIZATION_DESTINATION);
  }


  @Override
  public GeneralXynaObject exec(XynaOrderServerExtension xose, GeneralXynaObject input) throws XynaException {

    if (!(input instanceof TimeoutSynchronizationInput)) {
      throw new RuntimeException("Unexpected input for java destination '" + getClass().getSimpleName()
          + "'. Expected: '" + TimeoutSynchronizationInput.class.getSimpleName() + "', got: '"
          + (input != null ? input.getClass().getSimpleName() : "null") + "'");
    }

    TimeoutSynchronizationInput castedInput = (TimeoutSynchronizationInput) input;

    ((XynaFractalWorkflowEngine) XynaFactory.getInstance().getProcessing().getWorkflowEngine())
        .getSynchronizationManagement().timeout(castedInput.getTargetCorrelationId());

    return new Container();

  }

}
