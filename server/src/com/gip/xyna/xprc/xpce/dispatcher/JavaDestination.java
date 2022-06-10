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

package com.gip.xyna.xprc.xpce.dispatcher;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaOrderServerExtension.ExecutionType;


public abstract class JavaDestination extends DestinationValue {

  private static final long serialVersionUID = 3001401690254571968L;


  public JavaDestination(String fqName) {
    super(fqName);
  }


  public abstract GeneralXynaObject exec(XynaOrderServerExtension xose, GeneralXynaObject input) throws XynaException;


  @Override
  public final boolean isPoolable() {
    return false;
  }


  @Override
  public ExecutionType getDestinationType() {
    return ExecutionType.JAVA_DESTINATION;
  }

}
