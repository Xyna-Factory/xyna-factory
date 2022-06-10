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
package com.gip.xyna.update;



import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedObject;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedObject.DeserializationFailedHandler;
import com.gip.xyna.xnwh.persistence.FactoryWarehouseCursor;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerInstanceBean;
import com.gip.xyna.xnwh.persistence.TableConfiguration;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;



public class UpdateDontAllowOrdersWithExecutionInstanceInOrderBackup extends UpdateJustVersion {

  private static Logger logger = CentralFactoryLogging
      .getLogger(UpdateDontAllowOrdersWithExecutionInstanceInOrderBackup.class);

  /**
   * dieses update muss nicht mehrfach bei einem serverstart ausgeführt werden.
   */
  private static boolean hasBeenExecuted = false;


  public UpdateDontAllowOrdersWithExecutionInstanceInOrderBackup(Version vStart, Version vEnd) {
    super(vStart, vEnd);
  }


  public UpdateDontAllowOrdersWithExecutionInstanceInOrderBackup(Version vStart, Version vEnd,
                                                                 boolean mustUpdateGeneratedClasses) {
    super(vStart, vEnd, mustUpdateGeneratedClasses);
  }


  @Override
  protected void update() throws XynaException {

    if (hasBeenExecuted) {
      logger.debug("skipping update because it has already been executed.");
      return;
    }

    final Map<Object, Set<String>> undeserializableFields = new HashMap<Object, Set<String>>();
    DeserializationFailedHandler deserializationHandler =
        new SerializableClassloadedObject.DeserializationFailedHandler() {
          public void failed(Object parent, String name) {
            Set<String> previousNames = undeserializableFields.get(parent);
            if (previousNames == null) {
              previousNames = new HashSet<String>();
              undeserializableFields.put(parent, previousNames);
            }
            previousNames.add(name);
          }
        };

    SerializableClassloadedObject.deserializationFailedHandler = deserializationHandler;
    try {
      SerializableClassloadedObject.setIgnoreExceptionsWhileDeserializing(true);
      Set<Long> violatingRootOrderIDs = new HashSet<Long>();
      try {

        ODS ods = ODSImpl.getInstance();
        ods.registerStorable(OrderInstanceBackup.class);
        ODSConnection con = ods.openConnection(ODSConnectionType.DEFAULT);
        try {

          FactoryWarehouseCursor<OrderInstanceBackup> cursor =
              con.getCursor("select * from " + OrderInstanceBackup.TABLE_NAME + " where "
                                + OrderInstanceBackup.COL_XYNAORDER + " is not null", new Parameter(),
                            OrderInstanceBackup.getReaderWarnIfNotDeserializableNoDetails(), 100);

          for (List<OrderInstanceBackup> nextOibs : cursor.batched(100)) {
            for (OrderInstanceBackup oib : nextOibs) {

              XynaOrderServerExtension xose = oib.getXynaorder();
              if (xose == null) {
                violatingRootOrderIDs.add(oib.getRootId());
                logger.warn("Order for id <" + oib.getId() + "> (root order id <" + oib.getRootId()
                    + ">) could not be read at all.");
                continue;
              }

              long rootOrderId = xose.getRootOrder().getId();

              if (xose.getExecutionProcessInstance() != null) {
                violatingRootOrderIDs.add(rootOrderId);
                logger.debug("Execution instance found for order <" + xose.getId() + "> (root order id <" + rootOrderId
                    + ">)");
                continue;
              }

              Set<String> undeserializableFieldsForThisEntry = undeserializableFields.get(xose);
              boolean foundButUndeserializable =
                  undeserializableFieldsForThisEntry != null
                      && undeserializableFieldsForThisEntry.contains(XynaOrderServerExtension.EXECUTION_WF_FIELD_NAME);
              if (foundButUndeserializable) {
                logger.debug("Execution instance for order <" + xose.getId() + "> (root order id <" + rootOrderId
                    + ">) could not be deserialized, expecting non-null execution instance.");
                violatingRootOrderIDs.add(rootOrderId);
                continue;
              }

            }
            // frequently clear the map to avoid memory problems. this has to be done before the ResultSetReader
            // performs the next deserialization
            undeserializableFields.clear();
          }

          cursor.checkForExceptions();

        } finally {
          con.closeConnection();
          ods.unregisterStorable(OrderInstanceBackup.class);
        }

        if (violatingRootOrderIDs.size() > 0) {
          throw new RuntimeException(createErrorMessage(ods, violatingRootOrderIDs));
        }

      } catch (XynaException xe) {
        throw new RuntimeException("Error occurred during critical update", xe);
      } finally {
        SerializableClassloadedObject.setIgnoreExceptionsWhileDeserializing(false);
      }

    } finally {
      SerializableClassloadedObject.deserializationFailedHandler = null;
    }

    hasBeenExecuted = true;

  }


  private String createErrorMessage(ODS ods, Set<Long> violatingRootOrderIDs) throws XynaException {

    StringBuilder errMsgBuilder =
        new StringBuilder("Critical update " + getAllowedVersionForUpdate().getString() + "->"
            + getVersionAfterUpdate().getString()
            + " can only be applied if there are no orders present within the system that haven't reached the"
            + " execution phase before. Violating root order IDs are: ");

    int cnt = 0;
    for (Long l : violatingRootOrderIDs) {
      errMsgBuilder.append(l);
      if (cnt < violatingRootOrderIDs.size() - 1) {
        errMsgBuilder.append(", ");
      }
      cnt++;
    }
    errMsgBuilder.append(". ");

    try {
      String lineseperator = System.getProperty("line.separator");
      errMsgBuilder.append(lineseperator).append("Running orders are stored in:").append(lineseperator);
      TableConfiguration[] configurations = ods.getTableConfigurations();
      Set<Long> persLayerIdsForOrderBackup = new HashSet<Long>();
      for (TableConfiguration config : configurations) {
        if (config.getTable().equals(OrderInstanceBackup.TABLE_NAME)) {
          persLayerIdsForOrderBackup.add(config.getPersistenceLayerInstanceID());
        }
      }

      for (PersistenceLayerInstanceBean persLayer : ods.getPersistenceLayerInstances()) {
        if (persLayerIdsForOrderBackup.contains(persLayer.getPersistenceLayerInstanceID())) {
          errMsgBuilder.append(persLayer.getConnectionType()).append(": ")
              .append(persLayer.getPersistenceLayerInstance().getClass().getSimpleName()).append(" [")
              .append(persLayer.getConnectionParameter()).append("]").append(lineseperator);
        }
      }
    } catch (Throwable t) {
      Department.handleThrowable(t);
      //errors during generation of a nicer exception message don't really concern us
      logger.debug("Error creating log message, output will be incomplete", t);
    }

    return errMsgBuilder.toString();

  }

}
