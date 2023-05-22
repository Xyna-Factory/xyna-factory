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
package com.gip.xyna.xprc.xfractwfe.base;



import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;



/**
 * sammlung aller kindauftr�ge, die implizit von einem wf-step gestartet werden, der einen java-call macht.
 * z.b. instanzmethoden die als workflow modelliert sind.
 * 
 * codegenerierung in instanzmethoden-impls:
 * <pre>
 * ChildOrderStorageStack _childOrderStorageStack = ChildOrderStorage.childOrderStorageStack.get();
    XynaOrderServerExtension _subworkflowXynaOrder = _childOrderStorageStack.createOrGetXynaOrder("myordertype", revision);
    try {
      _subworkflowXynaOrder.setInputPayload(this, ...);
      XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().startOrderSynchronous(_subworkflowXynaOrder, !_childOrderStorageStack.isFirstExecution());
    } catch (XynaException _exceptionVari) {
      throw new XynaExceptionResultingFromWorkflowCall(_exceptionVari);
    } catch (ProcessSuspendedException _exceptionVari) {
      _childOrderStorageStack.suspended();
      throw _exceptionVari;
    }
    </pre>
 * falls man in einem coded service eine instanzmethode aufruft, und darauf vorbereitet sein will, dass die instanzmethode als workflow implementiert ist
 * oder selbst wiederum einen workflow startet (inkl suspend/resume-support), gibt es folgendes zu beachten:
 * 1) man muss daf�r sorgen, dass der bereits ausgef�hrte code nicht mehrfach (beim resume) ausgef�hrt wird
 * 2) man darf die suspend-exceptions nicht fangen
 * 
 * verwendungspattern ist also:
 * <pre>
 * ChildOrderStorageStack _childOrderStorageStack = ChildOrderStorage.childOrderStorageStack.get();
 * if (_childOrderStorageStack.isFirstExecution()) { FIXME: nein geht so nicht. siehe unten. darin wird firstexecution getoggelt.
 *   //my code
 * }
 * try {
 *   callInstanceMethod(); //
 * } catch (ProcessSuspendedExcception e) {
 *   throw e; //damit suspend funktioniert!
 * } //andere exceptions d�rfen beliebig behandelt werden.
 * //} catch (XynaExceptionResultingFromWorkflowCall e) {
 * //  infrastruktur-exceptions von der workflow-ausf�hrung.
 * //}
 * 
 * //restlicher code
 * </pre>
 * 
 * TODO codepattern nachtesten
 */
public class ChildOrderStorage implements Serializable {
  
  private static final long serialVersionUID = 1L;

  private static final Logger logger = CentralFactoryLogging.getLogger(ChildOrderStorage.class);

  /**
   * oberstes element im stack ist der parent-ChildOrderStorage, der zu dem step geh�rt, der
   * den aufruf des letzten instanz-methodencalls dieses threads verursacht hat.
   * 
   * ACHTUNG: wird im generierten code von workflows und datentypen verwendet
   */
  public static final ThreadLocal<ChildOrderStorageStack> childOrderStorageStack =
      new ThreadLocal<ChildOrderStorageStack>() {

        @Override
        protected ChildOrderStorageStack initialValue() {
          return new ChildOrderStorageStack();
        }

      };


  public static class ChildOrderStorageStack {

    private final LinkedList<ChildOrderStorage> stack = new LinkedList<ChildOrderStorage>();


    //aus abw�rtskompatibilit�t vorhanden, verwendet revision von aufrufendem workflow
    public XynaOrderServerExtension createOrGetXynaOrder(String ordertype) {
      ChildOrderStorage cos = stack.getLast();
      return cos.createOrGetXynaOrder(ordertype, null);
    }
    
    /**
     * gibt xynaorder vom im stack am obersten liegenden storage zur�ck
     */
    public XynaOrderServerExtension createOrGetXynaOrder(String ordertype, long revision) {
      ChildOrderStorage cos = stack.getLast();
      return cos.createOrGetXynaOrder(ordertype, revision);
    }

    /**
     * gibt aktuell laufende XynaOrder zur�ck
     */
    public XynaOrderServerExtension getCorrelatedXynaOrder() {
      if (stack.isEmpty()) {
        return null;
      }
      return stack.getLast().getCorrelatedXynaOrder();
    }
    
    /**
     * gibt den aktuell laufenden Step zurück
     */
    public FractalProcessStep<?> getCurrentlyExecutingStep() {
      if (stack.isEmpty()) {
        return null;
      }
      return stack.getLast().getCurrentlyExecutingStep();
    }

    /**
     * f�gt storage zum stack hinzu
     */
    public void add(ChildOrderStorage cos) {
      stack.add(cos);
    }


    /**
     * entfernt storage vom stack
     */
    public void remove() {
      stack.removeLast();
    }


    /**
     * der zuletzt gestartete auftrag wurde suspendiert
     */
    public void suspended() {
      stack.getLast().suspended();
    }
    
    /**
     * @return true, falls die erste ausf�hrung der xynaorder, false beim resume
     * FIXME funktioniert derzeit so, dass der erste aufruf dieser funktion das flag umsetzt. das ist eigtl nicht so sch�n.
     *       weil nur im generierten code aufgerufen, funktioniert das so.
     *       D.h. man muss derzeit immer createOrGetXynaOrder aufrufen und darauf folgend isFirstExecution
     */
    public boolean isFirstExecution() {
      return stack.getLast().isFirstExecution();
    }
  }


  private final FractalProcessStep<?> step;

  /**
   * liste aller auftr�ge die in dem step mal gestartet wurden
   */
  private final List<XynaOrderServerExtension> orders = new ArrayList<XynaOrderServerExtension>();

  /**
   * zeiger auf den index vor dem gerade laufenden auftrag. bei suspendierung geht der index um eins runter<br>
   * beispiel:<br>
   * 1. step startet java, 
   * 2. java ruft als wf implementierte instanzmethode
   *    -&gt; liste enth�lt 1 auftrag, idx = 0, firstExecutionIdx = -1-&gt;0
   * 3. wf wird suspendiert
   *    -&gt; liste enth�lt 1 auftrag, idx = -1, firstExecutionIdx = 0
   * 4. wf wird resumed
   *    -&gt; liste enth�lt 1 auftrag, idx = 0, firstExecutionIdx = 0
   * 5. wf wird beendet
   * 6. java ruft weitere als wf implementierte instanzmethode
   *    -&gt; liste enth�lt 2 auftr�ge, idx = 1, firstExecutionIdx = 0-&gt;1
   * 7. wf wird suspendiert
   *    -&gt; liste enth�lt 2 auftr�ge, idx = 0, firstExecutionIdx = 1
   * 8. wf wird resumed
   *    -&gt; liste enth�lt 2 auftr�ge, idx = 1, firstExecutionIdx = 1
   * usw.
   */
  private int currentOrderIdx = -1;
  private int firstExecutionIdx = -1;

  public ChildOrderStorage(FractalProcessStep<?> step) {
    this.step = step;
  }


  private boolean isFirstExecution() {
    boolean ret = firstExecutionIdx < currentOrderIdx;
    if (ret) {
      firstExecutionIdx ++;
    }
    return ret;
  }


  /**
   * der zuletzt gestartete auftrag wurde suspendiert
   */
  private void suspended() {
    currentOrderIdx--;
  }


  /**
   * xynaorder von parentorder "ableiten", etc.
   * falls bereits erstellt, wiederverwenden.
   * 
   * achtung: dieser aufruf kann mehrfach auf dem gleichen objekt passieren, wenn eine instanzmethode mehrere
   * workflows nacheinander aufruft.
   * um welchen auftrag es sich jeweils handelt, wird �ber die reihenfolge der aufrufe festgelegt, damit folgendes
   * funktioniert:<br>
   * startOrder(1) -> suspend -> resume -> startOrder(1) -> finishOrder -> startOrder(2)
   * -> suspend -> resume -> startOrder(2) -> finishOrder -> startOrder(3) -> usw.
   */
  private XynaOrderServerExtension createOrGetXynaOrder(String ordertype, Long revision) {
    currentOrderIdx += 1;
    XynaOrderServerExtension xose = null;
    //orders muss beim readonly zugriff hier nicht synchronisiert werden, weil immer mit dem gleichen thread drauf zugegriffen wird
    if (orders.size() > currentOrderIdx) {
      xose = orders.get(currentOrderIdx);
    } else {
      xose = createXynaOrder(ordertype, revision);
      synchronized (orders) {
        orders.add(xose);
      }
    }
    return xose;
  }

  public XynaOrderServerExtension getCorrelatedXynaOrder() {
    return step.getProcess().getCorrelatedXynaOrder();
  }
  
  public FractalProcessStep<?> getCurrentlyExecutingStep() {
    return step;
  }
  

  private XynaOrderServerExtension createXynaOrder(String ordertype, Long revision) {
    if (revision == null) {
      if (step.getClass().getClassLoader() instanceof ClassLoaderBase) {
        revision = ((ClassLoaderBase) step.getClass().getClassLoader()).getRevision();
      } else if (step.getProcess().getCorrelatedXynaOrder() != null) {
        //z.b. bei einer servicedestination ist der classloader kein classloaderbase, weil der workflow ein interner ist
        revision = step.getProcess().getCorrelatedXynaOrder().getRevision();
      } else {
        //TODO gen�gt hier eine warnung?
        throw new RuntimeException("Could not determine revision of context, in which a child order is to be started.");
      }
    }
    //im planning wird revision in den destinationkey �bernommen
    XynaOrderServerExtension xo = new XynaOrderServerExtension(new DestinationKey(ordertype));
    xo.setRevision(revision);
    XynaOrderServerExtension cxo = step.getProcess().getCorrelatedXynaOrder();
    xo.setParentOrder(cxo);
    xo.setParentStepNo(step.getN());


    if (cxo != null) {
      if (cxo.getOrderContext() != null) {
        xo.setNewOrderContext();
      }
      xo.setCustom0(cxo.getCustom0());
      xo.setCustom1(cxo.getCustom1());
      xo.setCustom2(cxo.getCustom2());
      xo.setCustom3(cxo.getCustom3());
      
      try {
        XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getOrderArchive()
            .updateAuditDataAddSubworkflowId(cxo, step, xo.getId());
      } catch (PersistenceLayerException e) {
        logger.warn("Could not update audit data for order " + cxo.getId() + " starting child order " + xo.getId(), e);
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        logger.warn("Could not update audit data for order " + cxo.getId() + " starting child order " + xo.getId(), e);
      }
    } else {
      logger.warn("Could not set custom fields and update auditdata for subworkflow " + xo.getId()
          + " because own XynaOrder could not be obtained");
    }

    return xo;
  }


  /**
   * @return liste aller auftr�ge, die bisher von diesem step gestartet worden sind
   */
  public List<XynaOrderServerExtension> getChildXynaOrders() {
    synchronized (orders) {
      return new ArrayList<XynaOrderServerExtension>(orders);
    }
  }


}
