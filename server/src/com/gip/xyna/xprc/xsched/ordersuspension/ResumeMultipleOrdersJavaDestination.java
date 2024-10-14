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

package com.gip.xyna.xprc.xsched.ordersuspension;



import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_INVALID_INPUT_PARAMETER_TYPE;
import com.gip.xyna.xprc.xpce.dispatcher.JavaDestination;
import com.gip.xyna.xprc.xpce.ordersuspension.ResumeTarget;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeManagement;



public class ResumeMultipleOrdersJavaDestination extends JavaDestination {

  private static final long serialVersionUID = -2858253968667283277L;
  private static final Logger logger = CentralFactoryLogging.getLogger(ResumeMultipleOrdersJavaDestination.class);

  public static final String RESUME_MULTIPLE_DESTINATION = "com.gip.xyna.ResumeMultipleOrders";


  public ResumeMultipleOrdersJavaDestination() {
    super(RESUME_MULTIPLE_DESTINATION);
  }


  @Override
  public GeneralXynaObject exec(XynaOrderServerExtension xose, GeneralXynaObject input) throws XPRC_INVALID_INPUT_PARAMETER_TYPE {

    if (!(input instanceof ResumeMultipleOrdersBean)) {
      throw new XPRC_INVALID_INPUT_PARAMETER_TYPE("1", ResumeMultipleOrdersBean.class.getName(), input.getClass()
          .getName());
    }

    ResumeMultipleOrdersBean bean = (ResumeMultipleOrdersBean) input;

    boolean success = resumeMultipleOrders(bean.getTargets());
  
    bean.setRequestSucceeded(success);

    return bean;

  }
  
  public boolean resumeMultipleOrders(List<ResumeTarget> targets) {
    SuspendResumeManagement srm = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().
        getSuspendResumeManagement();
    
    Map<Long, ArrayList<ResumeTarget>> groupedTargets = CollectionUtils.group(targets, ResumeTarget.transformationGetRootId() );
   
    logger.info("Resuming "+targets.size()+" targets for "+groupedTargets.size()+" root orders");
    
    Map<Long,Pair<String, PersistenceLayerException>> failedResumes = srm.resumeRootOrdersWithRetries(groupedTargets);
    
    if( failedResumes.isEmpty() ) {
      return true;
    } else {
      logger.warn("Could not suspend following RootOrders: "+failedResumes.keySet() );
      if( logger.isInfoEnabled() ) {
        for( Map.Entry<Long,Pair<String, PersistenceLayerException>> entry : failedResumes.entrySet() ) {
          PersistenceLayerException ple = entry.getValue().getSecond();
          String cause = entry.getValue().getFirst();
          String msg = "Resume of RootOrder "+entry.getKey()+" failed:"+(cause==null?"":(" "+cause));
          if( ple == null ) {
            logger.info(msg);
          } else {
            logger.info(msg,ple);
          }
        }
      }
      return false;
    }    
  }

}
