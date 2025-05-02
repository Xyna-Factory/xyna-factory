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
package xprc.synchronization;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_DUPLICATE_CORRELATIONID;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXMLForObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_TIMEOUT_DURING_SYNCHRONIZATION;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.XynaFractalWorkflowEngine;
import com.gip.xyna.xprc.xfractwfe.specialpurpose.synchronization.SynchronizationManagement;
import com.gip.xyna.xprc.xpce.WorkflowEngine;

public class SynchronizationImpl {
  
  private static final String NULL_VALUE = "__NULL_VALUE_internal";


  public SynchronizationImpl() {
  }


  public static XynaObject awaitNotification(XynaOrderServerExtension xo, CorrelationId id, Timeout timeout,
                                                        Integer internalStepId, Long firstExecutionTime, String laneId )
      throws XynaException {

    String resultingAnswerString;
    try {
      resultingAnswerString =
          retrieveSynchronizationManagement().awaitNotification(id.getId(), timeout.getTime(), internalStepId,
                                                                firstExecutionTime, xo, false, laneId );
    } catch (XPRC_TIMEOUT_DURING_SYNCHRONIZATION e) {
      throw new TimeoutDuringSynchronization(e);
    } catch (XPRC_DUPLICATE_CORRELATIONID e) {
      throw new DuplicateCorrelationID(e);
    }

    return createAnswer(resultingAnswerString, xo.getRootOrder().getRevision());
  }


  private static XynaObject createAnswer(String resultingAnswerString, Long rootRevision) throws XynaException {
    if (resultingAnswerString == null) {
      return null;
    }
    if (resultingAnswerString.equals(NULL_VALUE)) {
      return null;
    }
    if (resultingAnswerString.startsWith("<Exception ")) {
      try {
        GeneralXynaObject obj = XynaObject.generalFromXml(resultingAnswerString, rootRevision);
        if (obj instanceof XynaException) {
          ((XynaException) obj).setStackTrace(new StackTraceElement[0]);
          throw (XynaException) obj;
        }
      } catch (XPRC_XmlParsingException | XPRC_InvalidXMLForObjectCreationException | XPRC_MDMObjectCreationException e) {
        throw e;
      }
    }
    if (resultingAnswerString.startsWith("<Data ")) {
      try {
        return XynaObject.fromXml(resultingAnswerString, rootRevision);
        //fehler alle ignorieren und als synchronizationanswer behandeln, ausser runtimeexceptions.
      } catch (XPRC_XmlParsingException e) {
      } catch (XPRC_InvalidXMLForObjectCreationException e) {
      } catch (XPRC_MDMObjectCreationException e) {
      }
    }

    return new SynchronizationAnswer(resultingAnswerString);
  }


  public static XynaObject longRunningAwait(XynaOrderServerExtension xo, CorrelationId id, Timeout timeout,
                                                       Integer internalStepId, Long firstExecutionTime, String laneId)
      throws XynaException {
    String resultingAnswerString;
    try {
      resultingAnswerString =
          retrieveSynchronizationManagement().awaitNotification(id.getId(), timeout.getTime(), internalStepId,
                                                                firstExecutionTime, xo, true, laneId);
    } catch (XPRC_TIMEOUT_DURING_SYNCHRONIZATION e) {
      throw new TimeoutDuringSynchronization(e);
    } catch (XPRC_DUPLICATE_CORRELATIONID e) {
      throw new DuplicateCorrelationID(e);
    }
    return createAnswer(resultingAnswerString, xo.getRootOrder().getRevision());
  }


  private static SynchronizationManagement retrieveSynchronizationManagement() {
    WorkflowEngine genericWorkflowEngine = XynaFactory.getInstance().getProcessing().getWorkflowEngine();
    if (genericWorkflowEngine instanceof XynaFractalWorkflowEngine) {
      XynaFractalWorkflowEngine xfractwfe = (XynaFractalWorkflowEngine) genericWorkflowEngine;
      return xfractwfe.getSynchronizationManagement();
    } else {
      throw new RuntimeException("Unsupported workflowengine: "
                      + (genericWorkflowEngine != null ? genericWorkflowEngine.getClass().getName() : "<null>"));
    }

  }


  public static void notifyWaitingOrder(XynaOrderServerExtension xo, CorrelationId id, GeneralXynaObject answer,
                                        Integer internalStepId, String laneId) throws XynaException {

    WorkflowEngine we = XynaFactory.getInstance().getProcessing().getWorkflowEngine();
    if (we instanceof XynaFractalWorkflowEngine) {      
      //Für alle Objekte die nicht im RuntimeContext des Root-Auftrags aufgelöst werden können (oder abhängige), 
      //muss beim toXML explizit der RuntimeContext inkludiert werden. Vgl. XynaObject.XMLHelper
      String xml = answer == null ? NULL_VALUE : answer
          .toXml(null, false, -1, GeneralXynaObject.XMLReferenceCache.getCacheObjectWithoutCaching(xo.getRootOrder().getRevision()));

      XynaFractalWorkflowEngine xfractwfe = (XynaFractalWorkflowEngine) we;
      try {
        xfractwfe.getSynchronizationManagement().notifyWaiting(id.getId(), xml, internalStepId, xo);
      } catch (XPRC_DUPLICATE_CORRELATIONID e) {
        throw new DuplicateCorrelationID(e);
      }
    } else {
      throw new RuntimeException("Unsupported workflowengine: " + (we != null ? we.getClass().getName() : "<null>"));
    }

  }
  
  

}
