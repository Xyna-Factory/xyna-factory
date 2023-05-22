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

package com.gip.xyna.xprc.xsched.ordersuspension;

import java.util.Arrays;
import java.util.HashSet;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_FeatureRelatedExceptionDuringSuspensionHandling;
import com.gip.xyna.xprc.exceptions.XPRC_INVALID_INPUT_PARAMETER_TYPE;
import com.gip.xyna.xprc.xpce.dispatcher.JavaDestination;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeManagement;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendRootOrderData;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendRootOrderData.SuspensionFailedAction;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendRootOrderData.SuspensionSucceededAction;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendRootOrderData.SuspensionTimedOutAction;
import com.gip.xyna.xprc.xsched.ordersuspension.SuspendOrdertypeJavaDestination.SuspendOrdertypeSuspensionCause;



public class SuspendOrderJavaDestination extends JavaDestination {

  private static final long serialVersionUID = -2858253968667283277L;
  private static Logger logger = CentralFactoryLogging.getLogger(SuspendOrderJavaDestination.class);

  public static final String SUSPEND_DESTINATION = "com.gip.xyna.SuspendOrder";

  public SuspendOrderJavaDestination() {
    super(SUSPEND_DESTINATION);
  }


  @Override
  public GeneralXynaObject exec(XynaOrderServerExtension xose, final GeneralXynaObject input) throws XPRC_INVALID_INPUT_PARAMETER_TYPE,
                  PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, XPRC_FeatureRelatedExceptionDuringSuspensionHandling {

    if (!(input instanceof SuspendOrderBean)) {
      throw new XPRC_INVALID_INPUT_PARAMETER_TYPE("1", SuspendOrderBean.class.getName(), input.getClass().getName());
    }

    SuspendOrderBean bean = (SuspendOrderBean) input;
    
    logger.info("trying to suspend rootOrders: "+ bean.getTargetId() );
    SuspendRootOrderData suspendRootOrderData = 
        new SuspendRootOrderData(new HashSet<Long>(Arrays.asList(bean.getTargetId())))
    .suspensionCause(new SuspendOrdertypeSuspensionCause() )
    .suspensionSuccededAction(SuspensionSucceededAction.None)
    .suspensionTimedOutAction(SuspensionTimedOutAction.None)
    .suspensionFailedAction(SuspensionFailedAction.UndoSuspensions);
     
    SuspendResumeManagement srm = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getSuspendResumeManagement();
    SuspendRootOrderData result = srm.suspendRootOrders(suspendRootOrderData);

    switch( result.getSuspensionResult() ) {
      case Suspended:
        logger.info("orders is suspended: "+ result.getResumeTargets().size()+" resumetargets" );
        bean.setRequestSucceeded(true);
        break;
      case Timeout:
        logger.warn("suspendRootOrders has timeout" ); //wegen Undo sind alle Auftr�ge betroffen
        bean.setRequestSucceeded(false);
        break;
      case Failed:
        logger.warn("suspendRootOrders failed and could not resume "+result.getResumeTargets().size()+" targets" );
        bean.setRequestSucceeded(false);
        break;
      default:
        logger.warn("Unexpected SuspensionResult "+result);
        bean.setRequestSucceeded(false);
    }

    return bean;
  }

}
