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
package xmcp.manualinteraction;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.XynaOrderServerExtension;

public class MIServiceImpl {

  public MIServiceImpl() {
  }


  public static Result WaitForMI(XynaOrderServerExtension xo, Reason reason, Type type, UserGroup userGroup, Todo todo)
                  throws XynaException {
    return (Result) XynaFactory.getInstance().getXynaMultiChannelPortal().waitForMI(xo, reason.getReason(),
                                                                                    type.getType(),
                                                                                    userGroup.getUserGroup(),
                                                                                    todo.getTodo(),
                                                                                    (GeneralXynaObject) null);
  }

}
