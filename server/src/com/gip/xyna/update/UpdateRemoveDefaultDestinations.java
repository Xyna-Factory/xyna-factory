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
package com.gip.xyna.update;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.update.outdatedclasses_5_1_4_3.DispatcherDestinationStorable;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xprc.xpce.cleanup.CleanupDispatcher;
import com.gip.xyna.xprc.xpce.dispatcher.XynaDispatcher;
import com.gip.xyna.xprc.xpce.execution.ExecutionDispatcher;
import com.gip.xyna.xprc.xpce.planning.PlanningDispatcher;

/**
 * überflüssigerweise angelegte (persistent) default destinations entfernen, die werden beim deployment ja wieder angelegt. 
 * das sind genau alle destinations, bei denen der key und der value gleich sind und die zugehörigen destinations der anderen dispatcher.
 */
public class UpdateRemoveDefaultDestinations extends Update {

  private static final Logger logger = CentralFactoryLogging.getLogger(UpdateRemoveEmptyServiceFolders.class);

  private final Version allowedForUpdate;
  private final Version afterUpdate;
  private final boolean mustRegenerate;


  UpdateRemoveDefaultDestinations(Version allowedForUpdate, Version afterUpdate, boolean mustRegenerate) {
    this.allowedForUpdate = allowedForUpdate;
    this.afterUpdate = afterUpdate;
    this.mustRegenerate = mustRegenerate;
  }


  @Override
  protected void update() throws XynaException {
    /*
     * defaults werden wie folgt gesetzt:
          if (wf.getOutputTypeFullyQualified().equals(SchedulerBean.class.getName())) {

                            getXynaPlanning().getPlanningDispatcher().setDestination(dk, dv, true);
                            getXynaExecution().getExecutionEngineDispatcher()
                                            .setDestination(dk, XynaDispatcher.DESTINATION_EMPTY_WORKFLOW, false);
                          }
                          else {
                            getXynaPlanning().getPlanningDispatcher()
                                              .setDestination(dk, XynaDispatcher.DESTINATION_DEFAULT_PLANNING, false);
                            getXynaExecution().getExecutionEngineDispatcher().setDestination(dk, dv, true);
                          }
                          getXynaCleanup().getCleanupEngineDispatcher()
                                          .setDestination(dk, XynaDispatcher.DESTINATION_EMPTY_WORKFLOW, false);
                        }
                        
        beim löschen wie folgt vorgehen:
        1. falls planning-destinationkey == destinationvalue:
           -> planning destination entfernen
           -> falls execution-destinationvalue = EMPTY_WORKFLOW -> entfernen
           -> falls cleanup-destinationvalue = EMPTY_WORKFLOW -> entfernen
        oder
        2. falls execution-destinationkey == destinationvalue:
           -> execution destination entfernen
           -> falls planning-destinationvalue == DEFAULT_PLANNING -> entfernen
           -> falls cleanup-destinationvalue = EMPTY_WORKFLOW -> entfernen
     */
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(DispatcherDestinationStorable.class);
    ODSConnection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      List<DispatcherDestinationStorable> toDelete = new ArrayList<DispatcherDestinationStorable>();
      Map<String, DispatcherDestinationStorable> wfToDDSs = new HashMap<String, DispatcherDestinationStorable>();
      
      //hauptdestinations (key = value) finden die man löschen kann
      Collection<DispatcherDestinationStorable> coll = con.loadCollection(DispatcherDestinationStorable.class);      
      for (DispatcherDestinationStorable dds : coll) {
        if (dds.getDestinationKey().equals(dds.getDestinationValue())) {
          if (dds.getDispatcherName().equals(ExecutionDispatcher.DEFAULT_NAME) || dds.getDispatcherName().equals(PlanningDispatcher.DEFAULT_NAME)) {
            toDelete.add(dds);
            wfToDDSs.put(dds.getDestinationKey(), dds);
          }
        }
      }
      
      //nebendestinations (key ist gelöschte hauptdestination) finden, die man löschen kann
      for (DispatcherDestinationStorable dds : coll) {
        DispatcherDestinationStorable wfDDS = wfToDDSs.get(dds.getDestinationKey());
        if (wfDDS != null && wfDDS != dds) {
          if (dds.getDispatcherName().equals(CleanupDispatcher.DEFAULT_NAME)) {
            if (dds.getDestinationValue().equals(XynaDispatcher.DESTINATION_EMPTY_WORKFLOW.getFQName())) {
              toDelete.add(dds);
            }
          } else if (dds.getDispatcherName().equals(PlanningDispatcher.DEFAULT_NAME)) {
            if (dds.getDestinationValue().equals(XynaDispatcher.DESTINATION_DEFAULT_PLANNING.getFQName())) {
              toDelete.add(dds);
            }
          } else if (dds.getDispatcherName().equals(ExecutionDispatcher.DEFAULT_NAME)) {
            if (dds.getDestinationValue().equals(XynaDispatcher.DESTINATION_EMPTY_WORKFLOW.getFQName())) {
              toDelete.add(dds);
            }
          }
        }
      }
      logger
          .info("removing "
              + toDelete.size()
              + "redundant dispatcher configurations (dispatcherName, applicationname, versionName, destinationKey, destinationValue, destinationType): ");
      for (DispatcherDestinationStorable dds : toDelete) {
        logger.info("removing " + dds.getDispatcherName() + ", " + dds.getApplicationname() + ", "
            + dds.getVersionname() + ", " + dds.getDestinationKey() + ", " + dds.getDestinationValue() + ", "
            + dds.getDestinationType());
      }
      con.delete(toDelete);
      con.commit();
    } finally {
      con.closeConnection();
      ods.unregisterStorable(DispatcherDestinationStorable.class);
    }
  }


  @Override
  protected Version getAllowedVersionForUpdate() {
    return allowedForUpdate;
  }


  @Override
  protected Version getVersionAfterUpdate() throws XynaException {
    return afterUpdate;
  }


  @Override
  public boolean mustUpdateGeneratedClasses() {
    return mustRegenerate;
  }
}
