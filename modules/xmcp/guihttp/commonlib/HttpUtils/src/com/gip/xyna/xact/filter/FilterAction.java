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
package com.gip.xyna.xact.filter;

import java.io.Serializable;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.CallStatistics.StatisticsEntry;
import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter.FilterResponse;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;


public interface FilterAction {

  boolean match(URLPath url, Method method);

  FilterActionInstance act(URLPath url, HTTPTriggerConnection tc) throws XynaException;

  String getTitle();

  void appendIndexPage(HTMLPart body);

  boolean hasIndexPageChanged();

  public interface FilterActionInstance extends Serializable {

    FilterResponse filterResponse() throws XynaException;
    
    void onResponsibleWithoutXynaOrder(HTTPTriggerConnection tc);

    void onResponse(GeneralXynaObject response, HTTPTriggerConnection tc);

    void onError(XynaException[] xynaExceptions, HTTPTriggerConnection tc);

    void fillStatistics(StatisticsEntry statisticsEntry);
 
  }

}
