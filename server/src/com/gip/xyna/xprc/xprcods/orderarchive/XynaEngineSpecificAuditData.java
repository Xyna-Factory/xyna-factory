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
package com.gip.xyna.xprc.xprcods.orderarchive;



import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedXynaObject;
import com.gip.xyna.xprc.xfractwfe.base.DetachedCall;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep;
import com.gip.xyna.xprc.xfractwfe.base.ProcessStepCatch;
import com.gip.xyna.xprc.xfractwfe.base.SubworkflowCall;
import com.gip.xyna.xprc.xpce.ProcessStep;
import com.gip.xyna.xprc.xprcods.orderarchive.AuditData.AuditReloader;



public abstract class XynaEngineSpecificAuditData implements EngineSpecificAuditData {

  private static final long serialVersionUID = 2891411616402696586L;
  private static final Logger logger = CentralFactoryLogging.getLogger(XynaEngineSpecificAuditData.class);


  private final Map<Integer, Integer> mapRefIdToStepId;
  protected final Map<Integer, StepAuditDataContainer> mapStepIdToStepAuditDataContainer;

  private boolean containsCompensation = false;
  private AuditData parentAuditData;


  public XynaEngineSpecificAuditData(AuditData parent) {

    if (parent == null) {
      throw new IllegalArgumentException("Parent audit data may not be null");
    }
    this.parentAuditData = parent;

    int initialCapacity = 10;
    float loadFactor = 0.75f;
    int defaultSegments = 2; //wenig parallelität erwartet. könnte verbessert werden, wenn die tatsächliche parallelität des wfs ermittelt wird.

    mapStepIdToStepAuditDataContainer =
        new ConcurrentHashMap<Integer, StepAuditDataContainer>(initialCapacity, loadFactor, defaultSegments);
    mapRefIdToStepId = new ConcurrentHashMap<Integer, Integer>(initialCapacity, loadFactor, defaultSegments);
  }

  public boolean containsCompensation() {
    return containsCompensation;
  }
  

  protected final String getProcess() {
    return this.parentAuditData.getProcessName();
  }

  protected final GeneralXynaObject[] getOrderInputData() {
    return this.parentAuditData.getOrderInputData();
  }

  protected long getOrderInputVersion() {
    return parentAuditData.getOrderInputVersion();
  }
  
  protected AuditData getParentAuditData() {
    return parentAuditData;
  }

  /**
   * fügt subworkflowid für den step hinzu
   * (für dynamisch zur laufzeit eines step gestartete kindaufträge)
   */
  public void addSubworkflowId(ProcessStep pstep, long subworkflowId) {

    if (!(pstep instanceof FractalProcessStep)) {
      throw new RuntimeException("Unsupported step class: " + pstep.getClass().getSimpleName());
    }
    FractalProcessStep<?> fpstep = (FractalProcessStep<?>) pstep;
    Integer[] forEachIndices = FractalProcessStep.calculateForEachIndices(fpstep)[0];
    StepAuditDataContainer container = mapStepIdToStepAuditDataContainer.get(fpstep.getN());
    
    if (container == null) {
      container = new StepAuditDataContainer();
      mapStepIdToStepAuditDataContainer.put(fpstep.getN(), container);
    }
    container.addSubworkflowId(fpstep.getN(), forEachIndices, fpstep.getRetryCounter(), subworkflowId);
  }
  
  /**
   * setzt beim prestephandler die evtl gesetzte subworkflow id
   * (wenn der step genau einen subauftrag startet, kann das im prestephandler gemacht werden)
   */
  private void setSubworkflowId(ProcessStep pstep) {

    if (pstep == null) {
      return;
    }

    if (!(pstep instanceof FractalProcessStep)) {
      throw new RuntimeException("Unsupported step class: " + pstep.getClass().getSimpleName());
    }
    FractalProcessStep<?> fpstep = (FractalProcessStep<?>) pstep;
    Integer[] forEachIndices = FractalProcessStep.calculateForEachIndices(fpstep)[0];

    Long childOrderId = null;
    //implizit gestartete subaufträge über instanzmethoden von javacalls werden rufen explizit addSubworkflowId()
    if (pstep instanceof SubworkflowCall) {
      SubworkflowCall swc = (SubworkflowCall) pstep;
      childOrderId = swc.getChildOrder().getId();
    } else if (pstep instanceof ProcessStepCatch) {
      FractalProcessStep<?> possibleSubworkflowCall = ((ProcessStepCatch<?>) pstep).getRegularExecutionStep();
      if (possibleSubworkflowCall instanceof SubworkflowCall) {
        SubworkflowCall swc = (SubworkflowCall) possibleSubworkflowCall;
        childOrderId = swc.getChildOrder().getId();
      }
    } else if (pstep instanceof DetachedCall) {
      childOrderId = ((DetachedCall)pstep).getChildOrderId();
    }
    
    if (childOrderId != null) {
      StepAuditDataContainer container = mapStepIdToStepAuditDataContainer.get(fpstep.getN());
      
      if (container == null) {
        container = new StepAuditDataContainer();
        mapStepIdToStepAuditDataContainer.put(fpstep.getN(), container);
      }
      
      container.addSubworkflowId(fpstep.getN(), forEachIndices, fpstep.getRetryCounter(), childOrderId);
    }


  }


  public void addParameterPreStepValues(ProcessStep pstep, GeneralXynaObject[] value, long version) {
    if (!(pstep instanceof FractalProcessStep) && pstep != null) {
      throw new RuntimeException("Unsupported step class: " + pstep.getClass().getSimpleName());
    }
    FractalProcessStep<?> fpstep = (FractalProcessStep<?>) pstep;
    Integer[] forEachIndices = FractalProcessStep.calculateForEachIndices(fpstep)[0];
    Integer n = (fpstep != null ? fpstep.getN() : Integer.MAX_VALUE);
    long retryCounter = pstep != null ? fpstep.getRetryCounter() : 0;
    StepAuditDataContainer container = mapStepIdToStepAuditDataContainer.get(n);
    
    if (container == null) {
      container = new StepAuditDataContainer();
      mapStepIdToStepAuditDataContainer.put(n, container);
    }
    
    container.addPreStepInformation(n, forEachIndices, value, retryCounter, version);
    
    addMapXmlIdToStepId(pstep);
    setSubworkflowId(pstep);
  }


  public void addErrorStepValues(ProcessStep pstep) {

    if (!(pstep instanceof FractalProcessStep) && pstep != null) {
      throw new RuntimeException("Unsupported step class: " + pstep.getClass().getSimpleName());
    }
    FractalProcessStep<?> fpstep = (FractalProcessStep<?>) pstep;
    Integer[] forEachIndices = FractalProcessStep.calculateForEachIndices(fpstep)[0];

    XynaExceptionInformation t = fpstep.getCurrentUnhandledThrowable();
    if (t == null) {
      t = fpstep.getCaughtException();
    }
    if (t == null) {
      return;
    }

    StepAuditDataContainer container = mapStepIdToStepAuditDataContainer.get(fpstep.getN());
    
    if (container == null) {
      container = new StepAuditDataContainer();
      mapStepIdToStepAuditDataContainer.put(fpstep.getN(), container);
    }

    container.addErrorStepInformation(fpstep.getN(), forEachIndices, fpstep.getRetryCounter(), t);

    addMapXmlIdToStepId(pstep);

  }


  public void addParameterPostStepValues(ProcessStep pstep, GeneralXynaObject[] value, long version) {
    if (!(pstep instanceof FractalProcessStep) && pstep != null) {
      throw new RuntimeException("Unsupported step class: " + pstep.getClass().getSimpleName());
    }
    FractalProcessStep<?> fpstep = (FractalProcessStep<?>) pstep;
    Integer[] forEachIndices = FractalProcessStep.calculateForEachIndices(fpstep)[0];
    Integer n = pstep != null ? fpstep.getN() : Integer.MAX_VALUE;
    StepAuditDataContainer container = mapStepIdToStepAuditDataContainer.get(n);

    if (container == null) {
      container = new StepAuditDataContainer();
      mapStepIdToStepAuditDataContainer.put(n, container);
    }

    long retryCounter = fpstep != null ? fpstep.getRetryCounter() : 0;
    container.addPostStepInformation(n, forEachIndices, retryCounter, value, version);

    addMapXmlIdToStepId(pstep);
  }


  private void addMapXmlIdToStepId(ProcessStep pstep) {

    if (pstep == null) {
      return;
    }

    if (!(pstep instanceof FractalProcessStep)) {
      throw new RuntimeException("Unsupported step class: " + pstep.getClass().getSimpleName());
    }
    FractalProcessStep<?> fpstep = (FractalProcessStep<?>) pstep;
    if (fpstep.getXmlId() != null) {
      mapRefIdToStepId.put(fpstep.getXmlId(), fpstep.getN());
    }

  }


  public void addPreCompensationEntry(ProcessStep pstep) {
    if (!(pstep instanceof FractalProcessStep) && pstep != null) {
      throw new RuntimeException("Unsupported step class: " + pstep.getClass().getSimpleName());
    }
    FractalProcessStep<?> fpstep = (FractalProcessStep<?>) pstep;
    Integer[] forEachIndices = FractalProcessStep.calculateForEachIndices(fpstep)[0];
    addMapXmlIdToStepId(pstep);
    this.containsCompensation = true;
    StepAuditDataContainer container = mapStepIdToStepAuditDataContainer.get(fpstep.getN());
    
    if (container == null) {
      container = new StepAuditDataContainer();
      mapStepIdToStepAuditDataContainer.put(fpstep.getN(), container);
    }
    
    container.addPreCompensateStepInformation(fpstep.getN(), forEachIndices, fpstep.getRetryCounter());
  }


  public void addPostCompensationEntry(ProcessStep pstep) {
    if (!(pstep instanceof FractalProcessStep) && pstep != null) {
      throw new RuntimeException("Unsupported step class: " + pstep.getClass().getSimpleName());
    }
    FractalProcessStep<?> fpstep = (FractalProcessStep<?>) pstep;
    Integer[] forEachIndices = FractalProcessStep.calculateForEachIndices(fpstep)[0];
    this.containsCompensation = true;
    addMapXmlIdToStepId(pstep);
    StepAuditDataContainer container = mapStepIdToStepAuditDataContainer.get(fpstep.getN());
    
    if (container == null) {
      container = new StepAuditDataContainer();
      mapStepIdToStepAuditDataContainer.put(fpstep.getN(), container);
    }

    container.addPostCompensateStepInformation(fpstep.getN(), forEachIndices, fpstep.getRetryCounter());
  }
  
  
  public void clearStepData() {
    mapRefIdToStepId.clear();
    mapStepIdToStepAuditDataContainer.clear();
  }
  
  
  void reloadGeneratedObjectsInsideStepContentIfNecessary(AuditReloader reloader) {
    if (mapStepIdToStepAuditDataContainer != null && mapStepIdToStepAuditDataContainer.size() > 0) {
      for (StepAuditDataContainer sadc : mapStepIdToStepAuditDataContainer.values()) {
        for (StepAuditDataContent sadCont : sadc.getAllStepAuditDataEntries()) {
          sadCont.reloadGeneratedObjectsIfNecessary(reloader);
        }
      }
    }
  }


  public static class StepAuditDataContent implements Serializable {

    private static final long serialVersionUID = 5948195886350761080L;

    int stepId;
    Integer[] forEachIndices;
    long retryCounter;

    Long startTime;
    Long stopTime;

    Long errorTime;
    XynaExceptionInformation exceptionInformation;

    Long startCompensateTime;
    Long endCompensateTime;

    /**
     * @deprecated use {@link #subworkflowIds}
     */
    @Deprecated
    Long subworkflowId;
    Set<Long> subworkflowIds;

    transient GeneralXynaObject[] inputVariables;
    transient GeneralXynaObject[] outputVariables;

    public long versionInput;
    public long versionOutput;

    private void readObject(ObjectInputStream inputStream) throws IOException, ClassNotFoundException {
      inputStream.defaultReadObject();
      int zeroOrOneOrTwo = inputStream.readInt();
      if (zeroOrOneOrTwo > 0) {
        int numberOfInputs = inputStream.readInt();
        inputVariables = new GeneralXynaObject[numberOfInputs];
        for (int index = 0; index < numberOfInputs; index++) {
          inputVariables[index] = ((SerializableClassloadedXynaObject) inputStream.readObject()).getXynaObject();
        }
        if (zeroOrOneOrTwo > 1) {
          int numberOfOutputs = inputStream.readInt();
          outputVariables = new GeneralXynaObject[numberOfOutputs];
          for (int index = 0; index < numberOfOutputs; index++) {
            outputVariables[index] = ((SerializableClassloadedXynaObject) inputStream.readObject()).getXynaObject();
          }
        }
      }
    }


    private void writeObject(ObjectOutputStream outputStream) throws IOException {
      outputStream.defaultWriteObject();
      int zeroOrOneOrTwo = 0;
      if (inputVariables != null) {
        zeroOrOneOrTwo++;
        if (outputVariables != null) {
          zeroOrOneOrTwo++;
        }
      }
      outputStream.writeInt(zeroOrOneOrTwo);
      if (inputVariables != null) {
        outputStream.writeInt(inputVariables.length);
        for (int index = 0; index < inputVariables.length; index++) {
          outputStream.writeObject(new SerializableClassloadedXynaObject(inputVariables[index]));
        }
        if (outputVariables != null) {
          outputStream.writeInt(outputVariables.length);
          for (int index = 0; index < outputVariables.length; index++) {
            outputStream.writeObject(new SerializableClassloadedXynaObject(outputVariables[index]));
          }
        }
      }
    }

    
    void reloadGeneratedObjectsIfNecessary(AuditReloader reloader) {
      if (inputVariables != null && inputVariables.length > 0) {
        for (int i = 0; i < inputVariables.length; i++) {
          try {
            inputVariables[i] = reloader.reload(inputVariables[i]);
          } catch (Throwable t) {
            //don't abort deployment
          }
        }
      }

      if (outputVariables != null && outputVariables.length > 0) {
        for (int i = 0; i < outputVariables.length; i++) {
          try {
            outputVariables[i] = reloader.reload(outputVariables[i]);
          } catch (Throwable t) {
            //don't abort deployment
          }
        }
      }
    }
  }
  
  
  


  public static class StepAuditDataContainer implements Serializable {

    private static final long serialVersionUID = -3807689761380381669L;

    private static final StepAuditDataContent[] emptyContentArray = new StepAuditDataContent[0];

    
    // Als Key wird in der Map der RetryCounter verwendet. In der inneren Map werden die Steps den forEach-Indizies 
    // zugeordnet (versteckt im StepAuditDataKey) - falls kein forEach und somit kein Indizies, so wird ein besonderes
    // StepAuditDataKey verwendet.
    private Map<Long, Map<StepAuditDataKey, StepAuditDataContent>> manySteps = new HashMap<Long, Map<StepAuditDataKey, StepAuditDataContent>>(1); 
    //TODO concurrentmap verwenden. achtung: ist nicht serialisierungs-abwärtskompatibel

    private boolean containsCompensationInfo = false;


    public StepAuditDataContainer() {

    }


    public void addSubworkflowId(Integer stepId, Integer[] forEachIndices, long retryCounter, Long childOrderId) {
      addPreStepInformationLazyly(stepId, forEachIndices, null, retryCounter, -1);
      
      StepAuditDataContent content = identifyExistingContainerForUpdate(forEachIndices, retryCounter);
      if (content == null) {
        throw new RuntimeException("Tried to set subprocess id for a step that has not been registered.");
      }
      if (content.subworkflowIds == null) {
        content.subworkflowIds = new HashSet<Long>();
      }
      content.subworkflowIds.add(childOrderId);
    }


    public StepAuditDataContent[] getAllStepAuditDataEntries() {
      
      SortedSet<StepAuditDataContent> result = new TreeSet<StepAuditDataContent>(new Comparator<StepAuditDataContent>() {

        public int compare(StepAuditDataContent o1, StepAuditDataContent o2) {
          // the two entries are supposed to be of the same length
          if (o1 == null) {
            if (o2 == null) {
              return 0;
            } else {
              // can this ever happen?
              return 1;
            }
          }
          if (o2 == null) {
            return -1;
          }
          if (o1.retryCounter == o2.retryCounter) {
            if (o1.forEachIndices == null) {
              if (o2.forEachIndices == null) {
                return 0;
              } else {
                return 1;
              }
            } else if (o2.forEachIndices == null) {
              return -1;
            }
            if (o1.forEachIndices.length != o2.forEachIndices.length) {
              throw new RuntimeException("ForEach indices of different depth encountered within the same step.");
            }
            for (int i = 0; i < o1.forEachIndices.length; i++) {
              int next = o1.forEachIndices[i].compareTo(o2.forEachIndices[i]);
              if (next != 0) {
                return next;
              }
            }
            return 0;
          } else {
            return (int)(o1.retryCounter - o2.retryCounter); 
          }
        }
      });

      synchronized (manySteps) {
        for (Map<StepAuditDataKey, StepAuditDataContent> indices : manySteps.values()) {
          result.addAll(indices.values());
        }
      }
      if (result.isEmpty()) {
        return emptyContentArray;
      } else {
        return result.toArray(new StepAuditDataContent[result.size()]);
      }       
    }
    
    public Set<Long> getAllRetrys() {
      SortedSet<Long> result = new TreeSet<Long>();
      synchronized (manySteps) {
        result.addAll(manySteps.keySet());
      }
      return result;
    }
    

    public SortedSet<Integer[]> getAllForEachIndicesOrdered(long retry) {
      SortedSet<Integer[]> result = new TreeSet<Integer[]>(new Comparator<Integer[]>() {

        public int compare(Integer[] o1, Integer[] o2) {
          // the two entries are supposed to be of the same length
          if (o1 == null) {
            if (o2 == null) {
              return 0;
            } else {
              // can this ever happen?
              return 1;
            }
          }
          if (o2 == null) {
            return -1;
          }
          if (o1.length != o2.length) {
            throw new RuntimeException("ForEach indices of different depth encountered within the same step.");
          }
          for (int i = 0; i < o1.length; i++) {
            int next = o1[i].compareTo(o2[i]);
            if (next != 0) {
              return next;
            }
          }
          return 0;
        }

      });

      synchronized (manySteps) {
        Map<StepAuditDataKey, StepAuditDataContent> retryIndices = manySteps.get(retry);
        for (StepAuditDataKey key : retryIndices.keySet()) {
          result.add(key.forEachIndices);
        }
      }
      return result;
    }


    public Long getStartTime(Integer[] forEachIndices, long retry) {
      StepAuditDataContent content = identifyExistingContainerForUpdate(forEachIndices, retry);
      if (content != null) {
        return content.startTime;
      } else {
        return null;
      }
    }


    public Long getStopTime(Integer[] forEachIndices, long retry) {
      StepAuditDataContent content = identifyExistingContainerForUpdate(forEachIndices, retry);
      if (content != null) {
        return content.stopTime;
      } else {
        return null;
      }
    }


    public Long getErrorTime(Integer[] forEachIndices, long retry) {
      StepAuditDataContent content = identifyExistingContainerForUpdate(forEachIndices, retry);
      if (content != null) {
        return content.errorTime;
      } else {
        return null;
      }
    }


    public Set<Long> getSubworkflowIds(Integer[] forEachIndices, long retryCounter) {
      StepAuditDataContent content = identifyExistingContainerForUpdate(forEachIndices, retryCounter);
      if (content != null) {
        if (content.subworkflowId != null) {
          Set<Long> l = new HashSet<Long>();
          l.add(content.subworkflowId);
          return l;
        }
        return content.subworkflowIds;
      } else {
        return null;
      }
    }


    public Pair<GeneralXynaObject[], Long> getInputObjects(Integer[] forEachIndices, long retry, boolean remove) {
      StepAuditDataContent content = identifyExistingContainerForUpdate(forEachIndices, retry);
      if (content != null) {
        GeneralXynaObject[] r = content.inputVariables;
        if (remove) {
          content.inputVariables = null;
        }
        return Pair.of(r, content.versionInput);
      } else {
        return null;
      }
    }


    public Pair<GeneralXynaObject[], Long> getOutputObjects(Integer[] forEachIndices, long retry, boolean remove) {
      StepAuditDataContent content = identifyExistingContainerForUpdate(forEachIndices, retry);
      if (content != null) {
        GeneralXynaObject[] r = content.outputVariables;
        if (remove) {
          content.outputVariables = null;
        }
        return Pair.of(r, content.versionOutput);
      } else {
        return null;
      }
    }


    public XynaExceptionInformation getException(Integer[] forEachIndices, long retry, boolean remove) {
      StepAuditDataContent content = identifyExistingContainerForUpdate(forEachIndices, retry);
      if (content != null) {
        XynaExceptionInformation xei = content.exceptionInformation;
        if (remove) {
          content.exceptionInformation = null;
        }
        return xei;
      } else {
        return null;
      }
    }


    public void addPreStepInformation(Integer stepId, Integer[] forEachIndices, GeneralXynaObject[] inputdata, long retryCounter, long version) {

      StepAuditDataContent newStep = new StepAuditDataContent();
      newStep.stepId = stepId;
      newStep.forEachIndices = forEachIndices;
      newStep.inputVariables = inputdata;
      newStep.startTime = System.currentTimeMillis();
      newStep.retryCounter = retryCounter;
      newStep.versionInput = version;
      
      // prüfen, gibt es schon einen Eintrag für diesen Retry-Versuch
      synchronized (manySteps) {
        Map<StepAuditDataKey, StepAuditDataContent> entry = manySteps.get(retryCounter);
        if (entry == null) {
          entry = new HashMap<StepAuditDataKey, StepAuditDataContent>(1);
          manySteps.put(retryCounter, entry);
        }
        entry.put(new StepAuditDataKey(forEachIndices), newStep);
      }      
    }
    
    private void addPreStepInformationLazyly(Integer stepId, Integer[] forEachIndices, GeneralXynaObject[] inputdata, long retryCounter, long version) {
      Map<StepAuditDataKey, StepAuditDataContent> steps;
      synchronized (manySteps) {
        steps = manySteps.get(retryCounter);
      }
      
      if (steps == null) {
        addPreStepInformation(stepId, forEachIndices, inputdata, retryCounter, version);
      }
    }
    

    private void addPostStepInformation(Integer stepId, Integer[] forEachIndices, long retry, GeneralXynaObject[] outputData, long version) {
      addPreStepInformationLazyly(stepId, forEachIndices, null, retry, version);
      
      StepAuditDataContent existingEntry = identifyExistingContainerForUpdate(forEachIndices, retry);
      if (existingEntry != null) {
        // for functino things an output overwrites the possibly previously set error information
        existingEntry.errorTime = null;
        existingEntry.exceptionInformation = null;

        existingEntry.stopTime = System.currentTimeMillis();
        existingEntry.outputVariables = outputData;
        existingEntry.versionOutput = version;
      } else {
        throw new RuntimeException("Got post step information for a step for which no pre step information had been created.");
      }
    }


    private void addPreCompensateStepInformation(int n, Integer[] forEachIndices, long retry) {
      containsCompensationInfo = true;
      StepAuditDataContent existingEntry = identifyExistingContainerForUpdate(forEachIndices, retry);
      if (existingEntry != null) {
        existingEntry.startCompensateTime = System.currentTimeMillis();
      } else {
        throw new RuntimeException("Got pre compensate step information for a step for which no pre step information had been created.");
      }
    }


    private void addPostCompensateStepInformation(int n, Integer[] forEachIndices, long retry) {
      containsCompensationInfo = true;
      StepAuditDataContent existingEntry = identifyExistingContainerForUpdate(forEachIndices, retry);
      if (existingEntry != null) {
        existingEntry.endCompensateTime = System.currentTimeMillis();
      } else {
        throw new RuntimeException("Got pre compensate step information for a step for which no pre step information had been created.");
      }
    }


    public void addErrorStepInformation(Integer stepId, Integer[] forEachIndices, long retry, XynaExceptionInformation e) {
      StepAuditDataContent existingEntry = identifyExistingContainerForUpdate(forEachIndices, retry);
      if (existingEntry != null) {
        existingEntry.errorTime = System.currentTimeMillis();
        existingEntry.exceptionInformation = e;
      } else {
        throw new RuntimeException("Got error step information for a step for which no pre step information had been created.");
      }
    }


    private StepAuditDataContent identifyExistingContainerForUpdate(Integer[] forEachIndices, long retry) {
      Map<StepAuditDataKey, StepAuditDataContent> steps;
      synchronized (manySteps) {
        steps = manySteps.get(retry);
     
        if (steps == null) {
          throw new RuntimeException("Requested audit data for unexpected retry count <" + retry + ">");
        }
      
        return steps.get(new StepAuditDataKey(forEachIndices));
      }
    }


    public boolean containsCompensateInformation() {
      return containsCompensationInfo;
    }

  }


  public static class StepAuditDataKey implements Serializable {

    private static final long serialVersionUID = 1316961104190697819L;

    private final Integer[] forEachIndices;

    public StepAuditDataKey(Integer[] forEachIndices) {
      this.forEachIndices = forEachIndices;
    }


    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof StepAuditDataKey)) {
        return false;
      }
      return Arrays.equals(forEachIndices, ((StepAuditDataKey) obj).forEachIndices);
    }

    private transient int hashcode;

    @Override
    public int hashCode() {
      if (hashcode == 0) {
        int result = 1;
        Integer[] ar = forEachIndices;
        if (ar != null) { //TODO null-überprüfung nur aus abwärtskompatibilität. kann eigtl nie null sein
          for (int i=0; i<ar.length; i++) {
            Integer v = ar[i];
            result = result * 31 + (v == null ? 0 : v.hashCode());
          }
        }
        hashcode = result;
      }
      return hashcode;
    }

  }

  protected Integer getStepIdByRefId(Integer refId) {
    return mapRefIdToStepId == null ? null : mapRefIdToStepId.get(refId);
  }

  protected StepAuditDataContainer getStepAuditDataContainerByStepId(Integer stepId, boolean createLazy) {
    StepAuditDataContainer container = mapStepIdToStepAuditDataContainer.get(stepId);
    if (createLazy && container == null) {
      container = new StepAuditDataContainer();
      mapStepIdToStepAuditDataContainer.put(stepId, container);
    }
    
    return container;
  }

}
