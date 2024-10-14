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
package com.gip.xyna.xact.trigger;



import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.actions.BuildFilterImplAction;
import com.gip.xyna.xact.trigger.actions.BuildServiceImplAction;
import com.gip.xyna.xact.trigger.actions.BuildTriggerImplAction;
import com.gip.xyna.xact.trigger.actions.FileDownloadAction;
import com.gip.xyna.xact.trigger.actions.FileUploadAction;
import com.gip.xyna.xact.trigger.actions.IndexAction;
import com.gip.xyna.xact.trigger.actions.SendCrossDomainXmlAction;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;



public class GUIHTTPFilter extends ConnectionFilter<HTTPTriggerConnection> {

  private static final long serialVersionUID = 1L;


  private static Logger logger = CentralFactoryLogging.getLogger(GUIHTTPFilter.class);


  public static final String BUILD_SERVICE_IMPL_TEMPLATE_REQUEST = "/buildServiceImplTemplate";
  public static final String BUILD_TRIGGER_IMPL_TEMPLATE_REQUEST = "/buildTriggerImplTemplate";
  public static final String BUILD_FILTER_IMPL_TEMPLATE_REQUEST = "buildFilterImplTemplate";
  
  
  private static final List<FilterAction> allFilterActions;
  static {
    allFilterActions = new ArrayList<FilterAction>();
    allFilterActions.add( new SendCrossDomainXmlAction() );
    allFilterActions.add( new BuildFilterImplAction() );
    allFilterActions.add( new BuildServiceImplAction() );
    allFilterActions.add( new BuildTriggerImplAction() );
    allFilterActions.add( new FileUploadAction() );
    allFilterActions.add( new FileDownloadAction() );
    allFilterActions.add( new IndexAction(allFilterActions) );
  }
  
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
  public FilterResponse createXynaOrder(HTTPTriggerConnection tc) throws XynaException {
    try {
      tc.read(true);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    String uri = tc.getUri();
    String method = tc.getMethod();
    for( FilterAction fa : allFilterActions ) {
      if( fa.match(uri, method) ) {
        return fa.act( logger, tc );
      }
    }
    
    return FilterResponse.notResponsible();
  }
  

  
  public void onResponse(GeneralXynaObject response, HTTPTriggerConnection tc) {
    //ntbd
  }


  public void onError(XynaException[] e, HTTPTriggerConnection tc) {
    try {
      tc.sendError(e);
    } catch (SocketNotAvailableException e1) {      
      logger.error("could not reply error", e1);
      if (e.length == 1) {
        logger.error("original error ", e[0]);
      } else {
        for (int i = 0; i<e.length; i++) {
          logger.error(i + ". original error ", e[i]);
        }
      }
    }
  }


  /* (non-Javadoc)
   * @see com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter#getClassDescription()
   */
  @Override
  public String getClassDescription() {
    return "This Filter handles HTTP requests coming directly from the flash GUI, to download project impl templates.";
  }

}
