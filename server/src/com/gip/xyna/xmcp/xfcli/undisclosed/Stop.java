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
package com.gip.xyna.xmcp.xfcli.undisclosed;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl;
import com.gip.xyna.xfmg.xfctrl.cmdctrl.CommandControl.Operation;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyDuration;
import com.gip.xyna.xmcp.xfcli.AllArgs;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCLIConnection.CommandExecution;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCommandLineInterface;
import com.gip.xyna.xprc.exceptions.XPRC_FactoryShutdownViolation;
import com.gip.xyna.xprc.exceptions.XPRC_FactoryShutdownViolation_ActiveOperation;


public class Stop implements CommandExecution {
  
  public static final String STOP_PARAM_CHECKONRUNNINGORDERS = "-ifnoactiveorders";
  public static final String STOP_PARAM_IGNOREACTIVEOPERATIONS = "-ignoreactiveoperations";
  
  private static Logger logger = CentralFactoryLogging.getLogger(Stop.class);
  
  public void execute(AllArgs allArgs, CommandLineWriter clw) throws Exception {
    logger.debug("received stop as: " + allArgs.getCommand() );
    
    XynaPropertyDuration timeout = XynaProperty.TIMEOUT_SHUTDOWN_ACTIVE_OPERATIONS;
    clw.writeLineToCommandLine("Waiting for active operations ...");
    
    //WriteLock für alle Operations holen
    Pair<Operation, Operation> failure = CommandControl.tryWriteLock(CommandControl.Operation.SERVER_STOP, CommandControl.Operation.all(), timeout.get().getNumber(), timeout.get().getUnit());

    if (failure != null) {
      //mind. eine Operation konnte nicht innerhalb des Timeouts gelocked werden
      if (logger.isDebugEnabled()) {
        logger.debug(failure.getFirst() + " could not be locked because it is locked by another process of type " + failure.getSecond() + ".");
      }
      //bei writeLocks ist der lockOwner (failure.getSecond()) bekannt, bei readLocks ist 
      //die gelockte Operation (failure.getFirst()) meistens auch der lockOwner
      Operation locked = failure.getSecond() != null ? failure.getSecond() : failure.getFirst();
      if (allArgs.containsArg(STOP_PARAM_IGNOREACTIVEOPERATIONS)) {
        //Stoppen soll trotz laufender Operationen ausgeführt werden
        clw.writeLineToCommandLine(locked + " is still active. Shut down anyway.");
      } else {
        //Stoppen abbrechen
        throw new XPRC_FactoryShutdownViolation_ActiveOperation(locked.toString());
      }
    }
    
    //Factory stoppen
    try {
      if (allArgs.getArgCount() == 0 ) {
        logger.debug("stopping...");
        XynaFactoryCommandLineInterface.shutdown();
      } else {
        if (allArgs.containsArg(STOP_PARAM_CHECKONRUNNINGORDERS)
            && XynaFactory.getInstance().getProcessing().getWorkflowEngine().checkForActiveOrders()) {
          throw new XPRC_FactoryShutdownViolation();
        } else {
          logger.debug("stopping...");
          XynaFactoryCommandLineInterface.shutdown();
        }
      }
    } finally {
      if (failure == null) {
        //alle geholten Locks wieder freigeben
        CommandControl.wunlock(CommandControl.Operation.all());
      }
    }
  }
}
