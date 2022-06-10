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

package com.gip.xyna.xprc.xsched.orderseries.tasks;

import java.util.concurrent.CountDownLatch;

import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.exceptions.XPRC_DUPLICATE_CORRELATIONID;
import com.gip.xyna.xprc.xprcods.orderarchive.MasterWorkflowStatus;
import com.gip.xyna.xprc.xsched.OrderSeriesManagement;
import com.gip.xyna.xprc.xsched.orderseries.OSMCache;
import com.gip.xyna.xprc.xsched.orderseries.OSMCache.SearchResult;
import com.gip.xyna.xprc.xsched.orderseries.OSMInterface.OrderState;
import com.gip.xyna.xprc.xsched.orderseries.RescheduleSeriesOrderInformation;
import com.gip.xyna.xprc.xsched.orderseries.RescheduleSeriesOrderInformation.Problem;
import com.gip.xyna.xprc.xsched.orderseries.RescheduleSeriesOrderInformation.Solution;
import com.gip.xyna.xprc.xsched.orderseries.SeriesInformationStorable;
import com.gip.xyna.xprc.xsched.orderseries.SeriesInformationStorable.OrderStatus;




/**
 * OSMTask_Reschedule:
 *
 *
 */
public class OSMTask_Reschedule extends OSMTask {

  private CountDownLatch finishLatch;
  //private OrderStatus status;
  private long orderId;
  private boolean force;
  private int binding;
  private String correlationId;
  private String comment;
  private String archiveStatus;
  private int startedSuccessors;
  private Solution solution;
  private Problem problem;
  private OrderSeriesManagement orderSeriesManagement;
  private OrderStatus orderStatusGuess;
  
  public OSMTask_Reschedule(long orderId, boolean force, OrderSeriesManagement orderSeriesManagement) {
    this.orderId = orderId;
    this.force = force;
    this.finishLatch = new CountDownLatch(1);
    this.orderSeriesManagement = orderSeriesManagement;
    //orderStatusGuess wird durch searchOrder gesetzt
  }
  
  @Override
  public String getCorrelationId() {
    return correlationId; 
  }

  @Override
  protected void executeInternal() {
    if( ! canBeStarted() ) {
      return; //sollte erst gar nicht hierhergelangen, pr¸ft bspw., dass orderStatusGuess gesetzt ist
    }
    osmCache.lock( correlationId );
    try {
      SeriesInformationStorable sis = osmCache.refresh(correlationId);
      if( sis == null ) {
        //SeriesInformationStorable fehlt!
        problem = Problem.MissingSeriesInformation;
        return;
      }
      
      //wartet der Auftrag immer noch im Zustand WaitingCause.Series?
      boolean stillWaiting = orderSeriesManagement.isOrderWaiting( sis.getId() );
      if( stillWaiting ) {
        solution = handleWaiting(sis);
      } else {
        if( orderStatusGuess.isFinished() ) {
          //Auftrag war bei Erstellung des Tasks bereits fertig
          solution = handleFinished(sis);
        } else {
          //Auftrag wartete bei Erstellung des Tasks noch, ist nun aber bereits fertig: das Problem hat sich von alleine gelˆst
          solution = Solution.Disappeared;
        }
      }
     
    } finally {
      osmCache.unlock( correlationId );
      finishLatch.countDown();
    }
  }


  /**
   * @param sis
   */
  private Solution handleWaiting(SeriesInformationStorable sis) {
    OrderStatus sisStatus = sis.getOrderStatus();
    switch( sisStatus ) {
      case WAITING:
        //konsistenter Zustand: als Waiting gemeldet, als WAITING im SeriesInformationStorable eingetragen
        return checkPredecessorsAndTryStarting(sis);
      case RUNNING:
        //Auftrag l‰uft bereits, ist aber stillWaiting.
        if( force ) {
          //Achtung: damit kˆnnte Auftrag zweimal laufen!
          return finishOrder( sis, OrderStatus.SUCCEEDED, false );
        } else {
          problem = Problem.OrderSeemsRunning;
          return Solution.None;
        }
      case CANCELED:
      case CANCELING: //sollte eigentlich nicht auftreten kˆnnen
      case FAILED:
      case SUCCEEDED:
        //Auftrag ist bereits fertig, wartet aber auch noch.
        if( force ) {
          finishOrder( sis, sisStatus, false );
          //aus OrderSeriesManagement und AllOrdersList austragen
          orderSeriesManagement.removeOrder(orderId); //Achtung: damit kˆnnte Auftrag zweimal laufen!
          return Solution.RemovedFromOSM;
        } else {
          problem = Problem.OrderAlreadyFinished;
          return Solution.None;
        }
    }
    return Solution.None;
  }

  /**
   * @param sis
   */
  private Solution handleFinished(SeriesInformationStorable sis) {
    SeriesInformationStorable.OrderStatus sisStatus = sis.getOrderStatus();
    MasterWorkflowStatus mws = MasterWorkflowStatus.fromName(archiveStatus);
    switch( sisStatus ) {
      case RUNNING:
      case WAITING:
        //Auftrag ist bereits fertig, aber immer noch als wartend eingetragen.
        //der zugehˆrige OSMTask_Finish ist also verloren gegegangen, dies nun nachholen
        SeriesInformationStorable.OrderStatus newStatus = null;
        if( mws != null ) {
          switch( mws ) {
            case FINISHED:
              newStatus = SeriesInformationStorable.OrderStatus.SUCCEEDED;
              break;
            case CANCELED:
              newStatus = SeriesInformationStorable.OrderStatus.CANCELING;
              break;
            case SCHEDULING_TIME_OUT:
            case XYNA_ERROR:
              newStatus = SeriesInformationStorable.OrderStatus.FAILED;
              break;
          }
        }
        if( newStatus != null ) {
          return finishOrder( sis, newStatus, false );
        } else {
          problem = Problem.UnexpectedOrderState;
          return Solution.None;
        }
      case CANCELING:
        if( mws == MasterWorkflowStatus.CANCELED ) {
          //dies ist normaler Zustand, dass im Zustand CANCELING der Auftrag bereits gecancelt wurde,
          //er aber noch auf Predecessoren wartet
          return Solution.NoProblem;
        } else {
          //dies sollte nicht auftreten kˆnnen
          comment = "SeriesInformationStorable is in state "+sisStatus+" but order is in state "+mws+" ("+archiveStatus+")";
          problem = Problem.Unimplemented;
          return Solution.None;
        }
      case CANCELED:
      case FAILED:
      case SUCCEEDED:
        //Auftrag ist bereits fertig, daher war der Aufruf "RescheduleSeriesOrder" ¸berfl¸ssig. Aber er war evtl.
        //so gemeint, dass wartende Successoren gestartet werden sollen
        return finishOrder( sis, sisStatus, true );
    }
    return Solution.None;
  }
  

  /**
   * @param sis
   * @return 
   */
  private Solution checkPredecessorsAndTryStarting(SeriesInformationStorable sis) {
    int countMissingPredecessors = 0;
    int countFinishedPredecessors = 0;
    int countWaitingPredecessors = 0;
    
    for( String preCorrId : sis.getPredecessorCorrIds() ) {
      SeriesInformationStorable sisPre = osmCache.refresh(preCorrId);
      if( sisPre == null ) {
        ++countMissingPredecessors;
      } else {
        if( sisPre.getOrderStatus().isFinished() ) {
          ++countFinishedPredecessors;
        } else {
          ++countWaitingPredecessors; //evtl. genauer nachschauen? Aber Anwender kann auch rescheduleseriesorder f¸r die Predecessoren aufrufen
        }
      }
    }
    boolean startPreschedule = false;
    if( countMissingPredecessors + countWaitingPredecessors == 0 ) {
      //es gibt keine Predecessoren, die das Starten des Auftrags verhindern
      startPreschedule = true;
    } else if( countFinishedPredecessors != 0 ) {
      //es gibt keine Predecessoren, die das Starten des Auftrags verhindern, aber es sind auch Predecessoren vorhanden, 
      //die nicht mehr eingetragen sein sollten. Diese nun nachttragen 
      startPreschedule = true;
    }
    boolean orderStarted = false;
    if( startPreschedule ) {
      OSMTask_Preschedule preschedule = new OSMTask_Preschedule(correlationId);
      preschedule.execute(osmCache, osm, localOsm, remoteOsm, predecessorTrees);
      orderStarted = preschedule.isOrderStarted();
    }
    if( orderStarted ) {
      return Solution.OrderStarted;
    } else {
      if( force ) {
        return startOrder(sis);
      } else {
        if( countMissingPredecessors != 0 ) {
          problem = Problem.MissingPredecessors;
          comment = String.valueOf( countMissingPredecessors );
        } else {
          problem = Problem.WaitingPredecessors;
          comment = String.valueOf( countWaitingPredecessors );
        }
        return Solution.None;
      }
    }
  }

  private Solution startOrder(SeriesInformationStorable sis) {
    sis.setOrderStatus( OrderStatus.RUNNING );
    osmCache.update(sis);
    osm.readyToRun( sis.getCorrelationId(), sis.getId(), OrderState.CanBeStarted, null );
    return Solution.OrderStarted;
  }

    
 
  private Solution finishOrder(SeriesInformationStorable sis,
                               OrderStatus status, boolean startOnlySuccessors ) {
    String correlationId = sis.getCorrelationId();
    if( sis.getBinding() != orderSeriesManagement.getBinding() ) {
      problem = Problem.OtherBinding; //nicht Daten zu fremdem Binding ver‰ndern!
      return Solution.None;
    }
    OSMTask_Finish finish = new OSMTask_Finish(correlationId, status );
    finish.execute(osmCache, osm, localOsm, remoteOsm, predecessorTrees);
    startedSuccessors = finish.getCountStartedSuccessors();
    if( startOnlySuccessors ) {
      if( startedSuccessors != 0 ) {
        comment = String.valueOf(startedSuccessors);
        return Solution.SuccessorsStarted;
      } else {
        return Solution.Disappeared; //war ja doch kein Fehler...
      }
    } else {
      return Solution.OrderFinished;
    }
  }

  public void await() throws InterruptedException {
    finishLatch.await();
  }

  public RescheduleSeriesOrderInformation getInfo() {
    RescheduleSeriesOrderInformation info = new RescheduleSeriesOrderInformation();
    info.setOrderStatus(orderStatusGuess);
    info.setBinding(binding);
    info.setCorrelationId(correlationId);
    info.setSolution(solution);
    info.setComment(comment);
    info.setProblem(problem);
    return info;
  }
  
  /**
   * Entscheidung, ob Einstellen des Tasks in die Queue noch sinnvoll ist
   * @return
   */
  public boolean canBeStarted() {
    if( problem != null ) {
      return false; //Problem ist aufgetreten -> Task darf nicht laufen
    }
    if( solution != null ) {
      return false; //Problem (teilweise) gelˆst -> Task sollte nicht laufen
    }
    if( orderStatusGuess == null ) {
      return false; //Task kann nicht laufen
    }
    if( correlationId == null ) {
      solution = Solution.None;
      comment = "Unknown correlationId";
      return false; //Task kann nicht laufen
    }
    return true;
  }

  /**
   * Suchen der Daten zur im Konstruktor ¸bergebenen OrderId
   * @param osmCache
   */
  public void searchOrder(OSMCache osmCache) {
    boolean orderIsWaiting = orderSeriesManagement.isOrderWaiting(orderId);
    archiveStatus = OSMTask_Resume.searchOrderStatusInArchive(orderId);
    MasterWorkflowStatus mws = MasterWorkflowStatus.fromName(archiveStatus);
    if( mws == null ) {
      if( orderIsWaiting ) {
        orderStatusGuess = OrderStatus.WAITING;
      } else {
        if( force ) {
          //Fehlen sollte kein Problem darstellen: Verhalten so, als ob Auftrag erfolgreich gewesen w‰re
          orderStatusGuess = OrderStatus.SUCCEEDED;
        } else {
          problem = Problem.OrderNotFound;
        }
      }
    } else {
      switch( mws ) {
        case CANCELED:
          orderStatusGuess = OrderStatus.CANCELED;
          break;
        case FINISHED:
          orderStatusGuess = OrderStatus.SUCCEEDED;
          break;
        case SCHEDULING_TIME_OUT:
        case XYNA_ERROR:
          orderStatusGuess = OrderStatus.FAILED;
          break;
        default:
          problem = Problem.UnexpectedOrderState;
      }
    }
    SearchResult search = osmCache.search(orderId);
    if( search.getType() != SearchResult.Type.NotFound ) {
      binding = search.getBinding();
      correlationId = search.getCorrelationId();
    } else {
      //SeriesInformationStorable nicht gefunden. 
      if( orderIsWaiting ) {
        //Wenn der Auftrag im OrderSeriesManagement wartet, sollte auch ein SeriesInformationStorable existieren.
        handleMissingSeriesInformationStorable(osmCache);
      }
    }
  }

  private void handleMissingSeriesInformationStorable(OSMCache osmCache) {
    XynaOrder xo = orderSeriesManagement.getWaitingOrder(orderId);
    if( xo != null ) {
      if( force ) {
        SeriesInformationStorable sis = orderSeriesManagement.createSeriesInformationStorable(xo);
        try {
          logger.info("Created missing SeriesInformationStorable "+sis);
          osmCache.insert(sis);
          solution = Solution.MissingSeriesInformationInserted;
          comment = sis.toString();
        }
        catch (XNWH_GeneralPersistenceLayerException e) {
          logger.error( "Could not insert SeriesInformationStorable", e );
          problem = Problem.MissingSeriesInformation;
        }
        catch (XPRC_DUPLICATE_CORRELATIONID e) {
          logger.info( "Could not insert SeriesInformationStorable", e );
          problem = Problem.DuplicateCorrelationId;
        }
      } else {
        problem = Problem.MissingSeriesInformation;
      }
    } else {
      //XynaOrder nicht auffindbar
      if( force ) {
        //unlesbare XynaOrder -> nichts mehr zu machen auﬂer Auftrag zu entfernen
        orderSeriesManagement.removeOrder(orderId);
        solution = Solution.RemovedFromOSM;
      } else {
        problem = Problem.MissingSeriesInformation_UnreadableXynaOrder;
      }
    }
  }

}
