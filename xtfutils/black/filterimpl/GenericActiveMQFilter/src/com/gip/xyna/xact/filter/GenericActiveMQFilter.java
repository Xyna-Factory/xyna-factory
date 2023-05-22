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
package com.gip.xyna.xact.filter;

import com.gip.xyna.xact.trigger.ActiveMQTriggerConnection;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import org.apache.log4j.Logger;
import com.gip.xyna.utils.exceptions.XynaException;

public class GenericActiveMQFilter extends ConnectionFilter<ActiveMQTriggerConnection> {

  private static Logger logger = CentralFactoryLogging.getLogger(GenericActiveMQFilter.class);

  /**
   * Analyzes TriggerConnection and creates XynaOrder if it accepts the connection.
   * The method return a FilterResponse object, which can include the XynaOrder if the filter is responsibleb for the request.
   * # If this filter is not responsible the returned object must be: FilterResponse.notResponsible()
   * # If this filter is responsible the returned object must be: FilterResponse.responsible(XynaOrder order)
   * # If this filter is responsible but it handle the request without creating a XynaOrder the
   *   returned object must be: FilterResponse.responsibleWithoutXynaorder()
   * # If this filter is responsible but the version of this filter is too new the returned
   *    object must be: FilterResponse.responsibleButTooNew(). The trigger will try an older version of the filter.
   * @param tc
   * @return FilterResponse object
   * @throws XynaException caused by errors reading data from triggerconnection or having an internal error.
   *         results in onError() being called by Xyna Processing.
   */
  public FilterResponse createXynaOrder(ActiveMQTriggerConnection tc) throws XynaException {


    //return FilterResponse.notResponsible() if next filter should be tried
    //TODO implementation
    //TODO update dependency xml file
    return FilterResponse.notResponsible();
  }

  /**
   * called when above XynaOrder returns successfully.
   * @param response by XynaOrder returned GeneralXynaObject
   * @param tc corresponding triggerconnection
   */
  public void onResponse(GeneralXynaObject response, ActiveMQTriggerConnection tc) {
    //TODO implementation
    //TODO update dependency xml file
  }

  /**
   * called when above XynaOrder returns with error or if an XynaException occurs in generateXynaOrder().
   * @param e
   * @param tc corresponding triggerconnection
   */
  public void onError(XynaException[] e, ActiveMQTriggerConnection tc) {
    //TODO implementation
    //TODO update dependency xml file
  }

  /**
   * @return description of this filter
   */
  public String getClassDescription() {
    //TODO implementation
    //TODO update dependency xml file
    return null;
  }

}
