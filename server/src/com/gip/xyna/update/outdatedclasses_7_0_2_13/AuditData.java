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

package com.gip.xyna.update.outdatedclasses_7_0_2_13;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedXynaObject;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaOrderServerExtension.ExecutionType;
import com.gip.xyna.xprc.exceptions.XPRC_CREATE_MONITOR_STEP_XML_ERROR;
import com.gip.xyna.xprc.xpce.ProcessStep;
import com.gip.xyna.xprc.xprcods.orderarchive.XynaExceptionInformation;


/*
 * AuditData are associated with a single order
 * while processAuditData belong to a workflow execution associated with that order
 * later one there should be several (most likely 3 for Planning/Execution/Cleanup) EngineSpecificAuditData
 * and AuditData wraps the access to those and provides a wrapper for a unified toXML()
 */
public class AuditData implements Serializable {

  public final static String UPDATE_ALREADY_RAN_IDENTIFIER = "UPDATE_ALREADY_RAN";
  
  private static final long serialVersionUID = -69779167889295138L;

  private static final Logger logger = CentralFactoryLogging.getLogger(AuditData.class);
  

  private EngineSpecificAuditData processAuditData;

  private transient GeneralXynaObject[] orderInputData;
  private long inputDataVersion;
  private transient GeneralXynaObject[] orderOutputData;
  private long outputDataVersion;

  private String processName;
  private Long revision;


  public AuditData(XynaOrder order) {
    GeneralXynaObject orig = order.getInputPayload();
    if (orig != null) {
      if (orig.supportsObjectVersioning()) {
        long version = XOUtils.nextVersion();
        setOrderInputData(new GeneralXynaObject[] {orig}, version);
      } else {
        GeneralXynaObject o = orig.clone();
        setOrderInputData(new GeneralXynaObject[] {o}, -1);
      }
    } else {
      setOrderInputData(XynaObject.EMPTY_XYNA_OBJECT_ARRAY, -1);
    }
  }


  private void setOrderInputData(GeneralXynaObject[] input, long version) {
    this.orderInputData = input;
    inputDataVersion = version;
  }


  public void setOrderOutputData(GeneralXynaObject[] output, long version) {
    this.orderOutputData = output;
    outputDataVersion = version;
  }


  public void addParameterPreStepValues(ExecutionType executionType, ProcessStep step,
                                        GeneralXynaObject[] generalXynaObjects, long version) {
    createProcessAuditDataLazily(executionType);
    processAuditData.addParameterPreStepValues(step, generalXynaObjects, version);
  }


  public void addParameterPostStepValues(ExecutionType executionType, ProcessStep step,
                                         GeneralXynaObject[] generalXynaObjects, long version) {
    createProcessAuditDataLazily(executionType);
    processAuditData.addParameterPostStepValues(step, generalXynaObjects, version);
  }


  public void addPreCompensationEntry(ExecutionType executionType, ProcessStep pstep) {
    createProcessAuditDataLazily(executionType);
    processAuditData.addPreCompensationEntry(pstep);
  }


  public void addPostCompensationEntry(ExecutionType executionType, ProcessStep pstep) {
    createProcessAuditDataLazily(executionType);
    processAuditData.addPostCompensationEntry(pstep);
  }


  public void addErrorStepValues(ExecutionType executionType, ProcessStep pstep) {
    createProcessAuditDataLazily(executionType);
    processAuditData.addErrorStepValues(pstep);
  }

  
  public void clearStepData() {
    if (processAuditData != null) {
      processAuditData.clearStepData();
    }
  }

  public void setProcessIfUnset(ProcessStep pstep, Long revision) {
    if (this.processName == null) {
      this.processName = pstep.getProcessName();
    }
    this.revision = revision;
  }
  
  public Long getRevision() {
    return revision;
  }
 
  public String getProcessName() {
    return this.processName;
  }


  public void setProcessIfUnset(String processName, Long revision) {
    if (this.processName == null) {
      this.processName = processName;
    }
    this.revision = revision;
  }

  //TODO eigtl sollte man hier die revision nicht �bergeben. processAuditData sollte die revision bereits vorher gesetzt bekommen.
  public String toXML(long id, long parentId, long startTime, long endTime, List<XynaExceptionInformation> exceptions,
                      ExecutionType executionType, Long revision, boolean removeAuditDataAfterwards) throws XPRC_CREATE_MONITOR_STEP_XML_ERROR {
    try {
      return processAuditData != null ? processAuditData.toXML(id, parentId, startTime, endTime, exceptions, revision, removeAuditDataAfterwards) : "";
    } catch (Throwable t) {
      throw new XPRC_CREATE_MONITOR_STEP_XML_ERROR("Unexpected error during audit generation (orderId=" + id + ").", t);
    }
  }


  private void createProcessAuditDataLazily(ExecutionType executionType) {
    if (processAuditData == null) {
      switch (executionType) {
        case XYNA_FRACTAL_WORKFLOW :
          processAuditData = new XynaFractalWorkflowAuditData(this);
          break;
        case SERVICE_DESTINATION :
          processAuditData = new ServiceDestinationAuditData(this);
          break;
        case JAVA_DESTINATION :
          processAuditData = new JavaDestinationAuditData(this);
          break;
        default :
          throw new RuntimeException("Unexpected execution type: " + executionType.getTypeAsString());
      }
    }
  }


  public GeneralXynaObject[] getOrderInputData() {
    return this.orderInputData;
  }


  public GeneralXynaObject[] getOrderOutputData() {
    return this.orderOutputData;
  }


  private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
    try {
      inputStream.defaultReadObject();
      int zeroOrOneOrTwo = inputStream.readInt();
      if (zeroOrOneOrTwo > 0) {
        int numberOfInputs = inputStream.readInt();
        this.orderInputData = new GeneralXynaObject[numberOfInputs];
        for (int index = 0; index < numberOfInputs; index++) {
          this.orderInputData[index] = ((SerializableClassloadedXynaObject) inputStream.readObject()).getXynaObject();
        }
        if (zeroOrOneOrTwo > 1) {
          int numberOfOutputs = inputStream.readInt();
          this.orderOutputData = new GeneralXynaObject[numberOfOutputs];
          for (int index = 0; index < numberOfOutputs; index++) {
            this.orderOutputData[index] = ((SerializableClassloadedXynaObject) inputStream.readObject()).getXynaObject();
          }
        }
      }
    } catch (StreamCorruptedException e) {
      throw new RuntimeException(UPDATE_ALREADY_RAN_IDENTIFIER);
    }
  }


  private void writeObject(ObjectOutputStream outputStream) throws IOException {
    Long rootRevision = XynaOrderServerExtension.getThreadLocalRootRevision();
    outputStream.writeLong(rootRevision);
    outputStream.defaultWriteObject();
    int zeroOrOneOrTwo = 0;
    if (this.orderInputData != null) {
      zeroOrOneOrTwo++;
      if (this.orderOutputData != null) {
        zeroOrOneOrTwo++;
      }
    }
    outputStream.writeInt(zeroOrOneOrTwo);
    if (this.orderInputData != null) {
      outputStream.writeInt(this.orderInputData.length);
      for (int index = 0; index < this.orderInputData.length; index++) {
        outputStream.writeObject(new SerializableClassloadedXynaObject(this.orderInputData[index]));
      }
      if (this.orderOutputData != null) {
        outputStream.writeInt(this.orderOutputData.length);
        for (int index = 0; index < this.orderOutputData.length; index++) {
          outputStream.writeObject(new SerializableClassloadedXynaObject(this.orderOutputData[index]));
        }
      }
    }
  }


  public void reloadGeneratedObjectsInsideAuditIfNecessary(AuditReloader reloader) {

    if (orderInputData != null && orderInputData.length > 0) {
      for (int i = 0; i < orderInputData.length; i++) {
        try {
          orderInputData[i] = reloader.reload(orderInputData[i]);
        } catch (Throwable t) {
          Department.handleThrowable(t);
          if (logger.isDebugEnabled()) {
            logger.debug("Caught exception during deployment, continueing: " + t.getMessage());
            if (logger.isTraceEnabled()) {
              logger.trace(null, t);
            }
          }
          //don't abort deployment
        }
      }
    }

    if (orderOutputData != null && orderOutputData.length > 0) {
      for (int i = 0; i < orderOutputData.length; i++) {
        try {
          orderInputData[i] = reloader.reload(orderInputData[i]);
        } catch (Throwable t) {
          Department.handleThrowable(t);
          if (logger.isDebugEnabled()) {
            logger.debug("Caught exception during deployment, continueing: " + t.getMessage());
            if (logger.isTraceEnabled()) {
              logger.trace(null, t);
            }
          }
          //don't abort deployment
        }
      }
    }

    if (processAuditData != null) {
      XynaEngineSpecificAuditData engineAudit = (XynaEngineSpecificAuditData) processAuditData;
      engineAudit.reloadGeneratedObjectsInsideStepContentIfNecessary(reloader);
    }

  }
  
  
  public interface AuditReloader {
    
    GeneralXynaObject reload(GeneralXynaObject gxo) throws XynaException;
    
  }


  public void addSubworkflowId(ProcessStep pstep, long subworkflowId) {
    if (processAuditData != null) {
      XynaEngineSpecificAuditData engineAudit = (XynaEngineSpecificAuditData) processAuditData;
      engineAudit.addSubworkflowId(pstep, subworkflowId);
    }
  }


  public long getOrderInputVersion() {
    return inputDataVersion;
  }


  public long getOrderOutputVersion() {
    return outputDataVersion;
  }



}
