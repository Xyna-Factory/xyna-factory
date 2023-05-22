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

package com.gip.xyna.xsor.protocol;

import java.util.Arrays;

import org.apache.log4j.Logger;

import com.gip.xyna.cluster.ClusterManagement;
import com.gip.xyna.cluster.ClusterState;
import com.gip.xyna.xsor.common.ReplyCode;
import com.gip.xyna.xsor.common.Replyable;
import com.gip.xyna.xsor.common.ResultCode;
import com.gip.xyna.xsor.common.XSORUtil;
import com.gip.xyna.xsor.common.ResultCodeWrapper.CreateResultCode;
import com.gip.xyna.xsor.common.ResultCodeWrapper.DeleteResultCode;
import com.gip.xyna.xsor.common.ResultCodeWrapper.GrabResultCode;
import com.gip.xyna.xsor.common.ResultCodeWrapper.ReleaseResultCode;
import com.gip.xyna.xsor.common.ResultCodeWrapper.WriteResultCode;
import com.gip.xyna.xsor.persistence.PersistenceException;
import com.gip.xyna.xsor.persistence.PersistenceStrategy;

public class XSORProcess {


  private static final Logger logger=Logger.getLogger(XSORProcess.class.getName());


  private static PersistenceStrategy persistenceStrategy;

  private static ClusterManagement clusterManagement;

  static public CreateResultCode create(XSORPayload xsorPayload, XSORMemory xsorMemory, boolean isStrictlyCoherent) {
    Object objectID=xsorPayload.getPrimaryKey();

    final int objectIndex = xsorMemory.freeListGet();

    xsorPayload.copyIntoByteArray(xsorMemory.getData(objectIndex), xsorMemory.getOffsetBytes(objectIndex));

    xsorMemory.setState(objectIndex,'N');//= Not exists, zeigt an, dass Objekt trotz Indexeintrags ung�ltig ist
    xsorMemory.lockObject(objectIndex);
    boolean objectStillLocked = true;
    try {
      if (! xsorMemory.testPkIndexPut(objectID,objectIndex)){//Einpflegen in PK-Index
        xsorMemory.freeListPut(objectIndex);
        return CreateResultCode.wrapResultCode(ResultCode.NON_UNIQUE_IDENTIFIER);//1
      }
      xsorMemory.setState(objectIndex,'S');
      long modTime = xsorMemory.updateChecksumAndModificationtime(objectIndex);
      long releaseTimeNew=System.currentTimeMillis();

      ClusterState clusterState = clusterManagement.getCurrentState();
      switch (clusterState){
        case SHUTDOWN:
          //muss man nicht aus index werfen, weil nur inmemory, und gleich verschwunden
          xsorMemory.freeListPut(objectIndex);
          return CreateResultCode.wrapResultCode(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE);//2
        case CONNECTED:
        case SYNC_MASTER:case SYNC_PARTNER:case SYNC_SLAVE:
          xsorMemory.updateReleaseTime(objectIndex, releaseTimeNew);
          int corrId=xsorMemory.sendMessage(objectID, objectIndex,'S', true);
          XSORPayload copyForBackingStore = xsorMemory.copyFromXSOR(objectIndex);
          indexCreate(xsorMemory, objectIndex, xsorPayload);
          xsorMemory.unlockObject(objectIndex);
          objectStillLocked = false;
          ReplyCode replyCode=xsorMemory.getWaitManagement().waitFor(corrId, isStrictlyCoherent);
          switch(replyCode){
            case OK:
              backingStoreCreateObject(releaseTimeNew, modTime, copyForBackingStore, xsorMemory);
              return CreateResultCode.wrapResultCode(ResultCode.RESULT_OK);//3
            case TIMEOUT:
              backingStoreCreateObject(releaseTimeNew, modTime, copyForBackingStore, xsorMemory);
              return CreateResultCode.wrapResultCode(ResultCode.TIMEOUT_LOCAL_CHANGES);//4
            case CLUSTERSTATECHANGE:
              backingStoreCreateObject(releaseTimeNew, modTime, copyForBackingStore, xsorMemory);
              return CreateResultCode.wrapResultCode(ResultCode.CLUSTER_TIMEOUT_LOCAL_CHANGES);//6
            case DEADLOCK_DETECTED:
              xsorMemory.lockObject(objectIndex);
              objectStillLocked = true;
              xsorMemory.delete(objectIndex);
              indexDelete(xsorMemory, objectIndex, xsorPayload);
              throw new RuntimeException("PRESUMABLE DEADLOCK DETECTED");
            default: //CONFLICT
              xsorMemory.lockObject(objectIndex);
              objectStillLocked = true;
              if (logger.isInfoEnabled()) {
                logger.info("Got CONFLICT-C: id=" + objectID + "," + xsorMemory.toString(objectIndex, true));
              }
              xsorMemory.delete(objectIndex);
              indexDelete(xsorMemory, objectIndex, xsorPayload);
              /*
               * usecase: konflikt wegen gleichzeitigem create:
               *   dann wurde remote das objekt nach konfliktaufl�sung in shared hinterlassen.
               *   kann es nun passieren, dass der remotethread das objekt hier nicht anlegt und trotzdem remote nicht entfernt?
               *     szenario:
               *       zeitliche abfolge:
               *       1. n1 hat objekt im index erzeugt
               *       2. n2 hat objekt im index erzeugt
               *       3. n1 remote conflict
               *       4. jetzt kann "n2 remote" nie erfolg haben, solange n1 nicht fertig ist.
               *          d.h. n2 remote hinterl�sst 's', und dann l�scht n2 das objekt auch
               *       oder
               *       1. n1 hat objekt im index erzeugt
               *       2. n2 hat objekt im index erzeugt
               *       3. n2 remote conflict
               *       4. n1 remote conflict
               *       5. danach ist es auf beiden seiten gel�scht
               *       
               *       oder
               *       1. n1 hat objekt im index erzeugt
               *       2. n2 hat objekt im index erzeugt
               *       3. n1 remote conflict
               *       4. n1 entfernt objekt
               *       5. n2 remote erzeugt objekt -> n2 stand wird hinterlassen
               */
              return CreateResultCode.wrapResultCode(ResultCode.CONFLICTING_REQUEST_FROM_OTHER_NODE);//5
          }
        case DISCONNECTED_MASTER:
          copyForBackingStore = xsorMemory.copyFromXSOR(objectIndex);
          indexCreate(xsorMemory, objectIndex, copyForBackingStore);
          backingStoreCreateObject(0, modTime, copyForBackingStore, xsorMemory);
          return CreateResultCode.wrapResultCode(ResultCode.RESULT_OK_ONLY_LOCAL_CHANGES_MASTER);//6
        case STARTUP:
        case INIT:
        case DISCONNECTED:
        case NEVER_CONNECTED:
          if (isStrictlyCoherent){
            xsorMemory.delete(objectIndex);
            xsorMemory.removeFromPkIndex(xsorPayload.getPrimaryKey(), objectIndex);
            return CreateResultCode.wrapResultCode(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE);//7
          }
          xsorMemory.updateReleaseTime(objectIndex, releaseTimeNew);
          corrId=xsorMemory.sendMessage(objectID, objectIndex,'S',true);
          xsorMemory.getWaitManagement().doNotWaitFor(corrId);
          indexCreate(xsorMemory, objectIndex, xsorPayload);
          copyForBackingStore = xsorMemory.copyFromXSOR(objectIndex);
          backingStoreCreateObject(releaseTimeNew, modTime, copyForBackingStore, xsorMemory);
          return CreateResultCode.wrapResultCode(ResultCode.RESULT_OK_ONLY_LOCAL_CHANGES_MASTER);//8
        default :
          xsorMemory.delete(objectIndex);
          xsorMemory.removeFromPkIndex(xsorPayload.getPrimaryKey(), objectIndex);
          throw new RuntimeException("unsupported cluster state: " + clusterState);
      }
    } finally {
      if (objectStillLocked) {
        xsorMemory.unlockObject(objectIndex);
      }
    }
  }


  public static GrabResultCode grab(final int objectIndex, XSORPayload xsorPayload,  XSORMemory xsorMemory, boolean isStrictlyCoherent) {
    final Object objectID=xsorPayload.getPrimaryKey();
    if (objectIndex == -1) {
      return GrabResultCode.wrapResultCode(ResultCode.OBJECT_NOT_FOUND);//1
    }
    xsorMemory.lockObject(objectIndex);
    boolean objectStillLocked = true;
    try {
      switch(clusterManagement.getCurrentState()){

        case CONNECTED:
        case SYNC_MASTER:case SYNC_PARTNER: case SYNC_SLAVE:
          switch (xsorMemory.getState(objectIndex)) {
            case 'I' :
            case 'M' :
            case 'E' :
            case 'N' : //kann passieren, wenn ein remote request ankommt und das objekt updated
              return GrabResultCode.wrapResultCode(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_LOCAL_XSOR_STATE);//2
            case 'S' :
              // ok, continue, returned otherwise
              break;
            default :
              throw new RuntimeException("unexpected state " + xsorMemory.getState(objectIndex));
          }

          if (xsorMemory.hasOutstandingTransactions(objectID)) {
            /*
             * spezialbehandlung f�r den fall, dass die queue bereits eine nachricht I und S enth�lt.
             * also zustand = S (q=IS) / S (q=)
             *  -> lokal [tx1] E (q=ISI) / S (q=)
             *  -> remote [tx2] grab E (q=ISI) / E (q=I)
             *  -> remote [tx2] sendMsg S (q=ISI) / E (q=) -> konflikt
             *  -> remote [tx2] konflikt verarbeitung S (q=ISI) / S (q=)
             *  -> lokal [tx1] queueverarbeitung -> S (q=) / I (q=)
             *  -> lokal [tx1] release -> fehler, weil nicht mehr lokal in E -> dann wird remote nicht mehr von I weggegangen.
             *
             */
            if (isStrictlyCoherent){

              return GrabResultCode.wrapResultCode(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_TO_PENDING_ACTIONS);//11
            } else {
              xsorMemory.setState(objectIndex, 'E');
              xsorMemory.copyForWrite(objectIndex);
              return GrabResultCode.wrapResultCode(ResultCode.PENDING_ACTIONS_LOCAL_CHANGES);//12
            }
          } else{
            switch (clusterManagement.getCurrentState()){
              case STARTUP:
              case SHUTDOWN:
              case INIT:
                return GrabResultCode.wrapResultCode(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE);
              case DISCONNECTED:
              case NEVER_CONNECTED :
                if (isStrictlyCoherent){
                  return GrabResultCode.wrapResultCode(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE);//8
                } else {
                  xsorMemory.setState(objectIndex, 'E');
                  xsorMemory.copyForWrite(objectIndex);
                  int corrId=xsorMemory.sendMessage(objectID, objectIndex, 'I',  false);
                  xsorMemory.getWaitManagement().doNotWaitFor(corrId);
                  return GrabResultCode.wrapResultCode(ResultCode.RESULT_OK_ONLY_LOCAL_CHANGES);//9
                }
              case DISCONNECTED_MASTER:
                xsorMemory.setState(objectIndex, 'E');
                xsorMemory.copyForWrite(objectIndex);
                return GrabResultCode.wrapResultCode(ResultCode.RESULT_OK_ONLY_LOCAL_CHANGES_MASTER);//10
              default :
                //continue
            }
            xsorMemory.setState(objectIndex, 'E');
            xsorMemory.copyForWrite(objectIndex);
            int corrId=xsorMemory.sendMessage(objectID, objectIndex, 'I',  false);
            xsorMemory.unlockObject(objectIndex);
            objectStillLocked = false;
            ReplyCode rp= xsorMemory.getWaitManagement().waitFor(corrId, isStrictlyCoherent);

            ResultCode rc=ResultCode.NOBODY_EXPECTS;
            switch (rp){
              case OK:
                rc=ResultCode.RESULT_OK;//4
                break;
              case TIMEOUT:
                rc=ResultCode.TIMEOUT_LOCAL_CHANGES;//5
                break;
              case CLUSTERSTATECHANGE:
                rc=ResultCode.CLUSTER_TIMEOUT_LOCAL_CHANGES;//6
                break;
              case DEADLOCK_DETECTED:
                throw new RuntimeException("PRESUMABLE DEADLOCK DETECTED");
              default:
                if (logger.isInfoEnabled()) {
                  logger.info("Got CONFLICT-G: id=" + objectID + "," + xsorMemory.toString(objectIndex, true));
                }
                rc=ResultCode.CONFLICTING_REQUEST_FROM_OTHER_NODE;//7
            }
            return GrabResultCode.wrapResultCode(rc);
          }

        case SHUTDOWN:
          return GrabResultCode.wrapResultCode(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE);//13
        case STARTUP:
        case INIT:
        case DISCONNECTED:
        case NEVER_CONNECTED:
          if (isStrictlyCoherent){
            return GrabResultCode.wrapResultCode(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE);//14
          } else {
            switch(xsorMemory.getState(objectIndex)){
              case 'M':
                return GrabResultCode.wrapResultCode(ResultCode.NOBODY_EXPECTS);
              case 'I':
              case 'S':
                xsorMemory.setState(objectIndex,'E');
                xsorMemory.copyForWrite(objectIndex);
                //nobreak
              case 'E':
                if (xsorMemory.hasOutstandingTransactions(objectID)){//kein sendMessage(grab) bei outstanding Transactions
                  int corrId=xsorMemory.sendMessage(objectID, objectIndex, 'I',  false);
                  xsorMemory.getWaitManagement().doNotWaitFor(corrId);
                }
                return GrabResultCode.wrapResultCode(ResultCode.RESULT_OK_ONLY_LOCAL_CHANGES);//15
              default :
                throw new RuntimeException("unexpected state " + xsorMemory.getState(objectIndex));
            }
          }
        case DISCONNECTED_MASTER:
          switch(xsorMemory.getState(objectIndex)){
              case 'M':
                return GrabResultCode.wrapResultCode(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_LOCAL_XSOR_STATE);//17
              case 'I':
              case 'S':
                xsorMemory.setState(objectIndex, 'E');
                xsorMemory.copyForWrite(objectIndex);
                //nobreak !!!
              case 'E':
                //continue
                break;
              default :
                throw new RuntimeException("unexpected state " + xsorMemory.getState(objectIndex));
          }
          return GrabResultCode.wrapResultCode(ResultCode.RESULT_OK_ONLY_LOCAL_CHANGES_MASTER);//18
        default:
          return GrabResultCode.wrapResultCode(ResultCode.NOBODY_EXPECTS);

      }
    } finally {
      if (objectStillLocked) {
        xsorMemory.unlockObject(objectIndex);
      }
    }
  }

  public static ReleaseResultCode release(final int objectIndex, XSORMemory xsorMemory, boolean isStrictlyCoherent) {
    /*
     * FIXME: eigtl br�uchte man hier gar nich so viel machen, wenn man w�sste, dass nur ein 
     * grab->release passiert ist.
     * wenn dies der fall ist, ist der writecopy-index �berfl�ssig und man muss auch nicht die indizes anpassen.
     * man kann sich also einiges sparen.
     * dann kann man sich auch die transactionlock-sonderl�sung um die entsprechenden release-aufrufe sparen.
     */
    if(objectIndex==-1){
      return ReleaseResultCode.wrapResultCode(ResultCode.OBJECT_NOT_FOUND);
    }
    ClusterState clusterStateOuter = clusterManagement.getCurrentState();
    switch (clusterStateOuter) {
      case CONNECTED :
      case SYNC_MASTER:case SYNC_PARTNER:case SYNC_SLAVE:
      case DISCONNECTED:
      case NEVER_CONNECTED:
      case DISCONNECTED_MASTER:
      case STARTUP :
      case INIT :
        xsorMemory.lockObject(objectIndex);
        boolean objectStillLocked = true;
        try {
          final XSORPayload oldPayload = xsorMemory.copyFromXSORWithUnadjustedIndex(objectIndex);
          if (isStrictlyCoherent && xsorMemory.hasOutstandingTransactions(oldPayload.getPrimaryKey())) {
            //eigtl eine runtimeexception, weil kann nicht vorkommen.
            //weil das eigene grab schon beantwortet worden sein muss und danach gibts nichts mehr in der queue
            return ReleaseResultCode.wrapResultCode(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_TO_PENDING_ACTIONS);//1
          }
          char currentState = xsorMemory.getState(objectIndex);
          switch (currentState) {
            case 'M' :
            case 'E' : //M, E: erwartete zust�nde
            case 'S' : //S: vorher gab es einen konflikt, der in processIncommingRequest verarbeitet wurde und den zustand auf S umgebogen hat.
              ClusterState clusterState = clusterManagement.getCurrentState();
              switch(clusterState){
                case SHUTDOWN:
                  return ReleaseResultCode.wrapResultCode(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE);//2
                case CONNECTED:
                case SYNC_MASTER:case SYNC_PARTNER:case SYNC_SLAVE:
                  xsorMemory.setState(objectIndex, 'S');
                  long releaseTimeNew = System.currentTimeMillis();

                  indexUpdate(xsorMemory, objectIndex, oldPayload); //nun ist der ehemalige writecopy-index sichtbar.

                  xsorMemory.updateForRelease(objectIndex, releaseTimeNew);
                  Object objectId = oldPayload.getPrimaryKey();
                  int corrId=xsorMemory.sendMessage(objectId, objectIndex, 'S',  currentState=='M' || currentState=='S'); //bei S k�nnte zustand eigtl M sein.

                  //nun braucht man den alten stand nicht mehr
                  //gibt den backup-index frei
                  int newObjectIndex = xsorMemory.releaseBackup(objectIndex);
                  long modTime = xsorMemory.getModificationTime(newObjectIndex);
                  xsorMemory.unlockObject(objectIndex);
                  objectStillLocked = false;
                  XSORPayload currentPayload = xsorMemory.copyFromXSOR(newObjectIndex);
                  backingStoreUpdateObject(releaseTimeNew, modTime, currentPayload, xsorMemory);
                  ReplyCode replyCode= xsorMemory.getWaitManagement().waitFor(corrId, isStrictlyCoherent);
                  switch(replyCode){
                    case OK:
                      return ReleaseResultCode.wrapResultCode(ResultCode.RESULT_OK);//3
                    case TIMEOUT:
                      return ReleaseResultCode.wrapResultCode(ResultCode.TIMEOUT_LOCAL_CHANGES);//4
                    case CLUSTERSTATECHANGE:
                      return ReleaseResultCode.wrapResultCode(ResultCode.CLUSTER_TIMEOUT_LOCAL_CHANGES);//6
                    case DEADLOCK_DETECTED:
                      throw new RuntimeException("PRESUMABLE DEADLOCK DETECTED");
                    default:
                      if (logger.isInfoEnabled()) {
                        logger.info("Got CONFLICT-R: id=" + objectId + "," + xsorMemory.toString(objectIndex, true));
                      }
                      return ReleaseResultCode.wrapResultCode(ResultCode.CONFLICTING_REQUEST_FROM_OTHER_NODE);//5
                  }
                  //not reachable
                case DISCONNECTED:
                case NEVER_CONNECTED:
                case STARTUP:
                case INIT:
                  if (isStrictlyCoherent){
                    switch(xsorMemory.getState(objectIndex)){
                      case 'M':
                        xsorMemory.rollback(objectIndex);
                        //NOBREAK
                      case 'E':
                        xsorMemory.setState(objectIndex,'S');
                        corrId=xsorMemory.sendMessage(oldPayload.getPrimaryKey(), objectIndex, 'S',  false);
                        xsorMemory.getWaitManagement().doNotWaitFor(corrId);
                        return ReleaseResultCode.wrapResultCode(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE);//7
                        default:
                          return ReleaseResultCode.wrapResultCode(ResultCode.NOBODY_EXPECTS);
                    }
                  } else {
                    xsorMemory.setState(objectIndex,'S');
                    releaseTimeNew=System.currentTimeMillis();            
                    
                    //alte writecopy => neues original, ben�tigt noch zugriff aufs backup
                    indexUpdate(xsorMemory, objectIndex, oldPayload);
                    xsorMemory.updateForRelease(objectIndex, releaseTimeNew);
                    corrId=xsorMemory.sendMessage(oldPayload.getPrimaryKey(), objectIndex, 'S',  currentState=='M');

                    //nun braucht man den alten stand nicht mehr
                    //gibt den backup-index frei
                    newObjectIndex = xsorMemory.releaseBackup(objectIndex);
                    modTime = xsorMemory.getModificationTime(newObjectIndex);
                    currentPayload = xsorMemory.copyFromXSOR(newObjectIndex);
                    backingStoreUpdateObject(releaseTimeNew, modTime, currentPayload, xsorMemory);
                    xsorMemory.getWaitManagement().doNotWaitFor(corrId);
                    return ReleaseResultCode.wrapResultCode(ResultCode.CLUSTER_TIMEOUT_LOCAL_CHANGES);//8
                  }
                case DISCONNECTED_MASTER:
                  xsorMemory.setState(objectIndex,'S');
                  releaseTimeNew=System.currentTimeMillis();            
  
                  //alte writecopy => neues original, ben�tigt noch zugriff aufs backup
  
                  indexUpdate(xsorMemory, objectIndex, oldPayload);
                  //gibt den backup-index frei

                  xsorMemory.updateForRelease(objectIndex, releaseTimeNew);
                  newObjectIndex = xsorMemory.releaseBackup(objectIndex);
                  modTime = xsorMemory.getModificationTime(newObjectIndex);
                  currentPayload = xsorMemory.copyFromXSOR(newObjectIndex);
                  backingStoreUpdateObject(releaseTimeNew, modTime, currentPayload, xsorMemory);
                  return ReleaseResultCode.wrapResultCode(ResultCode.RESULT_OK_ONLY_LOCAL_CHANGES);//9
                default :
                  throw new RuntimeException("unexpected cluster state " + clusterState);
              }
            case 'N': //kann passieren, wenn ein remote request ankommt und das objekt updated
              return ReleaseResultCode.wrapResultCode(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_LOCAL_XSOR_STATE);//10
            case 'I' :
              return ReleaseResultCode.wrapResultCode(ResultCode.NOBODY_EXPECTS);//10a
            default :
              throw new RuntimeException("unexpected state " + currentState);
          }
        } finally {
          if (objectStillLocked) {
            xsorMemory.unlockObject(objectIndex);
          }
        }
      case SHUTDOWN :
        return ReleaseResultCode.wrapResultCode(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE);
      default :
        throw new RuntimeException("unexpected cluster state " + clusterStateOuter);
    }
  }


  //ACHTUNG bei den backingstore methoden kopien von payload-objekten �bergeben, die nicht anderweitig verwendet werden, weil diese evtl eine
  //zeitlang in der persistencestrategy gequeued werden.

  /*
   * man kann argumentieren, dass die backingstore-zugriffe nicht innerhalb des xc-memory locks passieren sollten, damit man
   * die locks weniger lange h�lt. da man trotzdem noch pro objekt gew�hrleisten muss, dass die backingstore-zugriffe in der
   * korrekten reihenfolge passieren (damit nicht ein update vor einem create passiert o.�.), muss man dann aber doch
   * wieder pro objekt synchronisieren.
   * der einzige vorteil davon w�re dann, dass man auf ein anderes objekt-spezifisches lock synchronisieren k�nnte, und 
   * zugriffe aufs xc, die keinen backingstore-zugriff nach sich ziehen, w�rden dann schneller gehen.
   * 
   * von denen haben wir aber kaum welche! => backingstore-zugriffe innerhalb des xc-memory locks haben kaum nachteile
   * und sind erstmal einfacher!
   * 
   * da es noch das transaktions-lock gibt, das die reihenfolge pro objekt in XCProcess sicherstellt, k�nnen die
   * backingstore-zugriffe weiterhin ausserhalb des xc-memory locks passieren.
   */

  private static void backingStoreCreateObject(long releaseTime, long modificationTime, XSORPayload xsorPayload, XSORMemory xsorMemory) {
    try {
      persistenceStrategy.createObject(xsorPayload, releaseTime, modificationTime);
    } catch (PersistenceException e) {
      logger.error("Error creating Object " + xsorPayload.getPrimaryKey(), e);
    }
  }


  private static void backingStoreUpdateObject(long releaseTime, long modificationTime, XSORPayload xsorPayload, XSORMemory xsorMemory) {
    try {
      persistenceStrategy.updateObject(xsorPayload, releaseTime, modificationTime);
    } catch (PersistenceException e) {
      logger.error("Error updating Object " + xsorPayload.getPrimaryKey(), e);
    }
  }


  private static void backingStoreDeleteObject(XSORPayload xsorPayload, XSORMemory xsorMemory) {
    try {
      persistenceStrategy.deleteObject(xsorPayload);
    } catch (PersistenceException e) {
      logger.error("Error deleting Object " + xsorPayload.getPrimaryKey(), e);
    }
  }

  public static WriteResultCode write(int objectIndex, XSORPayload xsorPayload, XSORMemory xsorMemory) {
    if(objectIndex==-1){
      return WriteResultCode.wrapResultCode(ResultCode.OBJECT_NOT_FOUND);
    }

    int writeCopyIndex = xsorMemory.getInternalIdOfWriteCopy(objectIndex);

    ClusterState clusterState = clusterManagement.getCurrentState();
    switch (clusterState) {

      case SHUTDOWN :
        return WriteResultCode.wrapResultCode(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE);//3
      case STARTUP :
      case INIT:
      case CONNECTED :
      case SYNC_MASTER:case SYNC_PARTNER:case SYNC_SLAVE:
      case DISCONNECTED:
      case NEVER_CONNECTED:
      case DISCONNECTED_MASTER :
        xsorMemory.lockObject(objectIndex);
        try {
          char objectState = xsorMemory.getState(objectIndex);
          switch (objectState) {
            case 'I' :
            case 'S' :
            case 'N': //kann passieren, wenn ein remote request ankommt und das objekt updated
              return WriteResultCode.wrapResultCode(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_LOCAL_XSOR_STATE);//2
            case 'E' :
              xsorMemory.setState(objectIndex, 'M');
              //NOBREAK;
            case 'M' :
              xsorPayload.copyIntoByteArray(xsorMemory.getData(writeCopyIndex), xsorMemory.getOffsetBytes(writeCopyIndex));
              xsorMemory.updateChecksumAndModificationtime(objectIndex);
              break;
            default :
              throw new RuntimeException("unexpected state " + objectState);
          }
        } finally {
          xsorMemory.unlockObject(objectIndex);
        }
        break;
      default :
        throw new RuntimeException("unexpected state " + clusterState);
    }
    return WriteResultCode.wrapResultCode(ResultCode.RESULT_OK);//1
  }

  //<0, falls not ok
  public static int read(Object objectId, XSORMemory xsorMemory) {
    final Integer objectIndex = xsorMemory.getObjectIndex(objectId);
    if(objectIndex==-1){
      return -1;
    }

    switch (clusterManagement.getCurrentState()){
      case CONNECTED:
      case SYNC_MASTER:case SYNC_PARTNER:case SYNC_SLAVE:
      case STARTUP: case INIT:
        xsorMemory.lockObject(objectIndex);
        try {
          switch(xsorMemory.getState(objectIndex)){
            case 'I':
              return -1;
            default:
              return objectIndex;
          }
        } finally {
          xsorMemory.unlockObject(objectIndex);
        }
      case SHUTDOWN:
        return -1;
      case DISCONNECTED:
      case NEVER_CONNECTED:
      case DISCONNECTED_MASTER:
        return objectIndex;
    }
    return -1;//SHOULD NOT HAPPEN
  }

  //Liefert eine - ungebundene - Kopie -
  public static XSORPayload read(final int objectIndex, XSORMemory xsorMemory) {

    ClusterState currentState = clusterManagement.getCurrentState();
    switch (currentState){
      case CONNECTED:
      case SYNC_MASTER:case SYNC_PARTNER:case SYNC_SLAVE:
            case STARTUP: case INIT:
        xsorMemory.lockObject(objectIndex);
        try {
          switch(xsorMemory.getState(objectIndex)){
            case 'I':
              return null;
            default:
              XSORPayload payload = xsorMemory.copyFromXSOR(objectIndex);
              return payload;
          }
        } finally {
          xsorMemory.unlockObject(objectIndex);
        }
      case SHUTDOWN:
        return null;
      case DISCONNECTED:
      case NEVER_CONNECTED:
      case DISCONNECTED_MASTER:
        xsorMemory.lockObject(objectIndex);
        XSORPayload payload = null;
        try {
          payload = xsorMemory.copyFromXSOR(objectIndex);
        } finally {
          xsorMemory.unlockObject(objectIndex);
        }
        return payload;
    }
    Exception e=new Exception("ERROR reading xsordata in state="+currentState);
    logger.error("ERROR reading xsordata in state="+currentState,e);
    return null;//SHOULD NOT HAPPEN
  }



   //<0, falls not ok
  public static int readIgnore(Object objectId, XSORMemory xsorMemory) {
    return xsorMemory.getObjectIndex(objectId);
  }


  public static DeleteResultCode delete(final Object objectId, XSORMemory xsorMemory, boolean isStrictlyCoherent) {
    final int objectIndex = xsorMemory.getObjectIndex(objectId);
    if (objectIndex == -1) {
      return DeleteResultCode.wrapResultCode(ResultCode.OBJECT_NOT_FOUND);//0
    }

    XSORPayload xsorPayload=null;
    switch(clusterManagement.getCurrentState()){

      case CONNECTED:
      case SYNC_MASTER:case SYNC_PARTNER:case SYNC_SLAVE:
        if (! isStrictlyCoherent){
          return DeleteResultCode.wrapResultCode(ResultCode.REQUESTED_ACTION_NOT_ALLOWED_IN_NOT_STRICT_MODE);//1
        }
        xsorMemory.lockObject(objectIndex);
        boolean objectStillLocked = true;
        try {
          char objectState = xsorMemory.getState(objectIndex);
          switch (objectState){
            case 'I':
            case 'S':
            case 'N': //kann passieren, wenn ein remote request ankommt und das objekt updated
              return DeleteResultCode.wrapResultCode(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_LOCAL_XSOR_STATE);//2
            case 'E':
            case 'M':

              switch(clusterManagement.getCurrentState()){
                case STARTUP:
                case INIT:
                case SHUTDOWN:
                  return DeleteResultCode.wrapResultCode(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE);//3
                case CONNECTED:
                case SYNC_MASTER:case SYNC_PARTNER:case SYNC_SLAVE:
                  int corrId=xsorMemory.sendMessage(objectId, objectIndex, 'N', false);
                  xsorMemory.unlockObject(objectIndex);
                  objectStillLocked = false;
                  ReplyCode replyCode=xsorMemory.getWaitManagement().waitFor(corrId, isStrictlyCoherent);
                  switch (replyCode){
                    case OK:
                      xsorMemory.lockObject(objectIndex);
                      objectStillLocked = true;
                      xsorPayload = xsorMemory.copyFromXSOR(objectIndex);
                      xsorMemory.delete(objectIndex);
                      indexDelete(xsorMemory, objectIndex, xsorPayload);
                      backingStoreDeleteObject(xsorPayload, xsorMemory); //nach dem indexdelete kann das create bereits wieder kommen und es gibt keine sicherheit auf die reihenfolge
                      return DeleteResultCode.wrapResultCode(ResultCode.RESULT_OK);//4
                    case TIMEOUT:
                      return DeleteResultCode.wrapResultCode(ResultCode.NOBODY_EXPECTS);//4a
                    case CLUSTERSTATECHANGE:
                      xsorMemory.lockObject(objectIndex);
                      objectStillLocked = true;
                      xsorPayload = xsorMemory.copyFromXSOR(objectIndex);
                      xsorMemory.delete(objectIndex);
                      indexDelete(xsorMemory, objectIndex, xsorPayload);
                      backingStoreDeleteObject(xsorPayload, xsorMemory);
                      return DeleteResultCode.wrapResultCode(ResultCode.CLUSTER_TIMEOUT_LOCAL_CHANGES);//6
                    case DEADLOCK_DETECTED:
                      throw new RuntimeException("PRESUMABLE DEADLOCK DETECTED");
                    default: //CONFLICT
                      if (logger.isInfoEnabled()) {
                        logger.info("Got CONFLICT-D: id=" + objectId + "," + xsorMemory.toString(objectIndex, true));
                      }
                      return DeleteResultCode.wrapResultCode(ResultCode.CONFLICTING_REQUEST_FROM_OTHER_NODE);//5
                  }
                case DISCONNECTED:
                case NEVER_CONNECTED:
                  return DeleteResultCode.wrapResultCode(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE);//7
                case DISCONNECTED_MASTER:
                  xsorPayload = xsorMemory.copyFromXSOR(objectIndex);
                  xsorMemory.delete(objectIndex);
                  indexDelete(xsorMemory, objectIndex, xsorPayload);
                  backingStoreDeleteObject(xsorPayload, xsorMemory);
                  return DeleteResultCode.wrapResultCode(ResultCode.RESULT_OK_ONLY_LOCAL_CHANGES);//8

              }
            default :
              throw new RuntimeException("unexpected state " + objectState);
          }
        } finally {
          if (objectStillLocked) {
            xsorMemory.unlockObject(objectIndex);
          }
        }
      case STARTUP:
      case INIT:
      case SHUTDOWN:
        return DeleteResultCode.wrapResultCode(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE);//9
      case DISCONNECTED:
      case NEVER_CONNECTED:
        return DeleteResultCode.wrapResultCode(ResultCode.REQUESTED_ACTION_NOT_POSSIBLE_DUE_CLUSTER_STATE);//10
      case DISCONNECTED_MASTER:
        xsorMemory.lockObject(objectIndex);
        try {
          xsorPayload = xsorMemory.copyFromXSOR(objectIndex);
          xsorMemory.delete(objectIndex);
          indexDelete(xsorMemory, objectIndex, xsorPayload);
        } finally {
          xsorMemory.unlockObject(objectIndex);
        }
        backingStoreDeleteObject(xsorPayload, xsorMemory);
        return DeleteResultCode.wrapResultCode(ResultCode.RESULT_OK_ONLY_LOCAL_CHANGES);//11
    }

    return null;
  }


  // enum added for readabilty, function could have returned boolean
  private static enum ConflictResolution {
    LOCAL_WIN, REMOTE_WIN;
  }


  private static class ProcessIncomingRequest {

    private boolean newObjectInserted = false;
    private int objectIndex;
    private char newState;
    private int type;
    private long modTime;
    private long relTimeRemoteBackup;
    private long relTimeNew;
    private int objectIdLength;
    private boolean hasReceivedPayload;
    private Object objectID;
    private byte[] payload;
    private int objectIndexWriteCopy;
    private int checksumRemoteBackup;
    private int cid;
    private byte[] objectIDAsBytes;
    private int checkSumNewCalculatedRemotely;
    private int checkSumNew;


    private void prepare() {
      if (logger.isDebugEnabled()) {
        logger.debug("Processing incomming request" + xsorMemory.name + ":" + XSORUtil.prettyPrint(received));
      }
      newState = (char) received[0];
      type = XSORUtil.getInt(1, received);
      cid = XSORUtil.getInt(5, received);
      checksumRemoteBackup = XSORUtil.getInt(9, received);
      modTime = XSORUtil.getLong(13, received);
      relTimeRemoteBackup = XSORUtil.getLong(21, received);
      relTimeNew = XSORUtil.getLong(29, received);
      objectIdLength = XSORUtil.getInt(37, received);
      objectIDAsBytes = Arrays.copyOfRange(received, 41, 41 + objectIdLength);

      objectID = xsorMemory.example.byteArrayToPk(objectIDAsBytes);

      checkSumNewCalculatedRemotely = XSORUtil.getInt(received.length - 4, received);


      payload = XSORUtil.lreZeroDecode(received, 41 + objectIdLength, received.length - 4 - (41 + objectIdLength));
      checkSumNew = XSORUtil.calculateChecksum(payload, 0, payload.length);

      if (logger.isDebugEnabled()) {
        StringBuffer sb = new StringBuffer();
        sb.append("newState=").append(String.valueOf(newState));
        sb.append(", type=").append(String.valueOf(type));
        sb.append(", cid=").append(String.valueOf(cid));
        sb.append(", checksumRemoteBackup=").append(String.valueOf(checksumRemoteBackup));
        sb.append(", checkSumNewCalculatedRemotely=").append(String.valueOf(checkSumNewCalculatedRemotely));
        sb.append(", checkSumNew=").append(String.valueOf(checkSumNew));
        sb.append(", modTime=").append(String.valueOf(modTime));
        sb.append(", relTimeRemoteBackup=").append(String.valueOf(relTimeRemoteBackup));
        sb.append(", relTimeNew=").append(String.valueOf(relTimeNew));
        sb.append(", objectID=").append(String.valueOf(objectID));
        logger.debug(sb.toString());
      }

      if (payload.length > 0 && checkSumNewCalculatedRemotely != checkSumNew) {
        logger.error("PROCESSING INCOMMING REQUEST CHECKSUM ERROR" + objectID + ":" + cid + ":" + checkSumNewCalculatedRemotely + "n:"
            + checkSumNew + "l:" + payload.length + "modTime:" + modTime + "RelTime:" + relTimeRemoteBackup + "relTimeNew:" + relTimeNew);

      }

      hasReceivedPayload = (payload.length > 0);

    }


    private XSORMemory xsorMemory;
    private byte[] received;
    private Replyable replyable;


    public void process(XSORMemory xsorMemory, byte[] received, Replyable replyable) {
      this.xsorMemory = xsorMemory;
      this.received = received;
      this.replyable = replyable;

      prepare(); //decode payload
      determineAndLockObjectIndex();
      try {
        if (!newObjectInserted) {
          handleExistingObject();
        } else {// Object existiert noch nicht
          if (!hasReceivedPayload) {
            conflictDeleted();
          } else {
            create();
          }
        }
      } finally {
        this.xsorMemory.unlockObject(objectIndex);
      }
    }


    private void determineAndLockObjectIndex() {
      boolean lockedIndexAndEnsuredIdentity = false;
      while (!lockedIndexAndEnsuredIdentity) {
        boolean locked = false;
        objectIndex = xsorMemory.getObjectIndex(objectID);
        try {
          if (objectIndex < 0) { // Im PK reservieren
            objectIndex = xsorMemory.freeListGet();
            xsorMemory.lockObjectForceUnlock(objectIndex); // FIXME nur wegen bug so hartes unlock. eigtl gen�gt normales lock.
            locked = true;
            newObjectInserted = xsorMemory.testPkIndexPut(objectID, objectIndex);
            if (!newObjectInserted) {
              xsorMemory.freeListPut(objectIndex);
              xsorMemory.unlockObject(objectIndex);
              locked = false;
              objectIndex = xsorMemory.getObjectIndex(objectID);// ggf. wurde Objekt zwischenzeitlich erzeugt
              continue;
            }
          } else {
            xsorMemory.lockObjectForceUnlock(objectIndex); // FIXME nur wegen bug so hartes unlock. eigtl gen�gt normales lock.
            locked = true;
          }
          if (xsorMemory.getObjectIndex(objectID) == objectIndex) {
            lockedIndexAndEnsuredIdentity = true;
          }
        } finally {
          if (locked && !lockedIndexAndEnsuredIdentity) {
            xsorMemory.unlockObject(objectIndex);
          }
        }

      }

      objectIndexWriteCopy = xsorMemory.getInternalIdOfWriteCopy(objectIndex);
    }


    private void conflictDeleted() {
      logConflict("F");
      xsorMemory.freeListPut(objectIndex);
      xsorMemory.removeFromPkIndex(objectID, objectIndex);
      xsorMemory.sendReplyMessage(ReplyCode.CONFLICT, XSORUtil.getInt(5, received), 9, replyable, xsorMemory.getName());
      return;
    }


    private void create() {
      XSORPayload xsorPayload = null;
      int newIndex = xsorMemory.setPayloadAndUpdateIndexAndLockNewIndex(objectID, objectIndex, payload, modTime, relTimeNew, true);
      try {
        xsorMemory.setState(newIndex, 'S');
        xsorPayload = xsorMemory.copyFromXSOR(newIndex);
      } finally {
        if (newIndex != objectIndex) {
          xsorMemory.unlockObject(newIndex);
        }
      }
      backingStoreCreateObject(relTimeNew, modTime, xsorPayload, xsorMemory);
      xsorMemory.sendReplyMessage(ReplyCode.OK, XSORUtil.getInt(5, received), 10, replyable, xsorMemory.getName());
    }


    private int currentState;


    private void handleExistingObject() {
      currentState = xsorMemory.getState(objectIndex);
      if (xsorMemory.getCheckSum(objectIndexWriteCopy) == checksumRemoteBackup
          && (xsorMemory.getReleaseTime(objectIndex) == relTimeRemoteBackup)) {//ist der lokale stand = remote backup? dann kein konflikt
        if (newState == 'N') {//Angefordertes delete
          delete();
        } else {//Kein delete und kein create:
          if ((newState == 'I' && currentState == 'S') || (newState == 'S' && currentState == 'I')
              || (newState == 'S' && currentState == 'S')) {// angeforderter Zustandswechsel m�glich
            modify();
          } else { //Zustandswechsel nicht m�glich
            invalidStateChangeRequested();
          }
        }
      } else { //Aenderung in Pr�fsumme oder ReleaseDate
        traceChange();
        if (newState != 'N') {
          if (hasReceivedPayload) {
            conflictReceivedPayload();
          } else { //keine Payload im Release
            conflictNoPayload();
          }
        } else { //received[0] == 'N'
          logConflict("N");
          xsorMemory.sendReplyMessage(ReplyCode.CONFLICT, XSORUtil.getInt(5, received), 8, replyable, xsorMemory.getName());
        }
      }
    }


    private void conflictNoPayload() {
      logConflict("S");
      xsorMemory.rollback(objectIndex);
      if (currentState != 'M' && currentState != 'I') {
        xsorMemory.updateReleaseTime(objectIndex, Math.max(xsorMemory.getReleaseTime(objectIndex), relTimeNew));
      }
      xsorMemory.setState(objectIndex, 'S');
      xsorMemory.sendReplyMessage(ReplyCode.CONFLICT, XSORUtil.getInt(5, received), 7, replyable, xsorMemory.getName());
      return;
    }


    private void conflictReceivedPayload() {
      logConflict("C");
      XSORPayload xsorPayload = null;
      ConflictResolution resolution = determineConflictWinner(modTime, objectIndex);
      if (resolution == ConflictResolution.REMOTE_WIN) {
        int newIndex =
            xsorMemory.setPayloadAndUpdateIndexAndLockNewIndex(objectID, objectIndex, payload, modTime,
                                                               Math.max(xsorMemory.getReleaseTime(objectIndex), relTimeNew), false);
        try {
          xsorPayload = xsorMemory.copyFromXSOR(newIndex);
          xsorMemory.setState(newIndex, 'S');
        } finally {
          if (newIndex != objectIndex) {
            xsorMemory.unlockObject(newIndex);
          }
        }
      } else if (currentState != 'S') {
        xsorMemory.rollback(objectIndex);
        xsorMemory.setState(objectIndex, 'S');
      }
      if (xsorPayload == null) {
        //in den anderen f�llen ist objectIndex nicht mehr der aktuelle objektindex f�r das objekt und releasetime wurde bereits korrekt gesetzt.
        xsorMemory.updateReleaseTime(objectIndex, Math.max(xsorMemory.getReleaseTime(objectIndex), relTimeNew));
      } else {
        backingStoreUpdateObject(relTimeNew, modTime, xsorPayload, xsorMemory);
      }
      xsorMemory.sendReplyMessage(ReplyCode.CONFLICT, XSORUtil.getInt(5, received), 6, replyable, xsorMemory.getName());
      return;
    }


    private void traceChange() {
      if (logger.isDebugEnabled() && xsorMemory.getCheckSum(objectIndexWriteCopy) != checksumRemoteBackup) {
        logger.debug(XSORProcess.class.getSimpleName() + ": diff in Checksum" + xsorMemory.getCheckSum(objectIndexWriteCopy) + "*"
            + checksumRemoteBackup);
        if (logger.isTraceEnabled()) {
          logger.trace(" for object " + xsorMemory.toString(objectIndex, false));
        }
      }
      if (logger.isDebugEnabled() && xsorMemory.getReleaseTime(objectIndexWriteCopy) != relTimeRemoteBackup) {
        logger.debug(XSORProcess.class.getSimpleName() + ": diff in Release-Time" + xsorMemory.getReleaseTime(objectIndex) + "*"
            + relTimeRemoteBackup);
        if (logger.isTraceEnabled()) {
          logger.trace(" for object " + xsorMemory.toString(objectIndex, false));
        }
      }
    }


    private void invalidStateChangeRequested() {
      if (hasReceivedPayload) {//Payload in Request vorhanden
        logConflict("D");
        XSORPayload xsorPayload = null;
        int newIndex =
            xsorMemory.setPayloadAndUpdateIndexAndLockNewIndex(objectID, objectIndex, payload, modTime,
                                                               Math.max(xsorMemory.getReleaseTime(objectIndex), relTimeNew), false);
        try {
          xsorMemory.setState(newIndex, 'S');
          xsorPayload = xsorMemory.copyFromXSOR(newIndex);
        } finally {
          if (newIndex != objectIndex) {
            xsorMemory.unlockObject(newIndex);
          }
        }
        backingStoreUpdateObject(relTimeNew, modTime, xsorPayload, xsorMemory);
        xsorMemory.sendReplyMessage(ReplyCode.CONFLICT, cid, 4, replyable, xsorMemory.getName());
      } else { //keine Paylod im Request
        logConflict("E");
        xsorMemory.rollback(objectIndex);//Rollback auf Payload-Backup
        xsorMemory.setState(objectIndex, 'S');
        xsorMemory.updateReleaseTime(objectIndex, Math.max(relTimeNew, xsorMemory.getReleaseTime(objectIndex)));
        xsorMemory.sendReplyMessage(ReplyCode.CONFLICT, XSORUtil.getInt(5, received), 5, replyable, xsorMemory.getName());
      }
    }


    private void logConflict(String type) {
      if (logger.isInfoEnabled()) {
        logger.info("CONFLICT-" + type + " processing remote update: id=" + objectID + ",newState=" + newState + ",backupReltime="
            + relTimeRemoteBackup + "relTimeNew=" + relTimeNew + ",backupChecksum=" + checksumRemoteBackup + ",receivedPayload="
            + hasReceivedPayload + ",localobject=" + xsorMemory.toString(objectIndex, true));
      }
    }


    private void modify() {
      XSORPayload xsorPayload = null;
      xsorMemory.setState(objectIndex, newState);
      if (relTimeNew > relTimeRemoteBackup) {//d.h. release
        xsorMemory.updateReleaseTime(objectIndex, relTimeNew);
      }
      if (hasReceivedPayload) {// d.h. Payload vorhanden => release mit Payload
        int newIndex = xsorMemory.setPayloadAndUpdateIndexAndLockNewIndex(objectID, objectIndex, payload, modTime, relTimeNew, false);
        try {
          xsorPayload = xsorMemory.copyFromXSOR(newIndex);
        } finally {
          if (newIndex != objectIndex) {
            xsorMemory.unlockObject(newIndex);
          }
        }
        backingStoreUpdateObject(relTimeNew, modTime, xsorPayload, xsorMemory);
      }
      xsorMemory.sendReplyMessage(ReplyCode.OK, cid, 3, replyable, xsorMemory.getName());
    }


    private void delete() {
      XSORPayload xsorPayload = null;
      if (currentState == 'I') {
        xsorPayload = xsorMemory.copyFromXSOR(objectIndex);
        xsorMemory.delete(objectIndex);
        indexDelete(xsorMemory, objectIndex, xsorPayload);

        //TODO in processingincommingrequest daf�r sorgen, dass backingstore-requests 
        //in der richtigen reihenfolge passieren.
        backingStoreDeleteObject(xsorPayload, xsorMemory);
        xsorMemory.sendReplyMessage(ReplyCode.OK, cid, 1, replyable, xsorMemory.getName());
      } else {
        logConflict("G");
        xsorMemory.sendReplyMessage(ReplyCode.CONFLICT, cid, 2, replyable, xsorMemory.getName());
      }
    }


    private ConflictResolution determineConflictWinner(long remoteModificationTime, int objectIndex) {
      long localModificationTime = xsorMemory.getModificationTime(objectIndex);
      if (localModificationTime > remoteModificationTime) {
        return ConflictResolution.LOCAL_WIN;
      } else if (localModificationTime < remoteModificationTime) {
        return ConflictResolution.REMOTE_WIN;
      } else if (xsorMemory.isWinner()) {
        return ConflictResolution.LOCAL_WIN;
      } else {
        return ConflictResolution.REMOTE_WIN;
      }
    }
  }


  public static void processIncommingRequest(XSORMemory xsorMemory, byte[] received,  Replyable replyable) {
    new ProcessIncomingRequest().process(xsorMemory, received, replyable);
  }


  private static void indexUpdate(XSORMemory xsorMemory, int objectIndex, XSORPayload xsorPayload) {
    xsorMemory.updateIndex(objectIndex, xsorPayload);
  }

  private static void indexDelete(XSORMemory xsorMemory, int objectIndex, XSORPayload xsorPayload) {
    xsorMemory.updateIndex(-1, null, objectIndex, xsorPayload); //aus index l�schen
  }

  private static void indexCreate(XSORMemory xsorMemory, int objectIndex, XSORPayload xsorPayload) {
    xsorMemory.updateIndex(objectIndex, xsorPayload, -1, null);
  }

  public static void init(PersistenceStrategy thePersistenceStrategy, ClusterManagement theClusterManagement) {
    persistenceStrategy=thePersistenceStrategy;
    clusterManagement=theClusterManagement;
  }

  //Liefert eine - ungebundene - Kopie -
  public static XSORPayload readForceUnlock(int objectIndex, XSORMemory xsorMemory) {

    ClusterState currentState = clusterManagement.getCurrentState();
    switch (currentState){
      case CONNECTED:
      case SYNC_MASTER:case SYNC_PARTNER:case SYNC_SLAVE:
            case STARTUP: case INIT:
        xsorMemory.lockObjectForceUnlock(objectIndex);
        try {
          switch(xsorMemory.getState(objectIndex)){
            case 'I':
              return null;
            default:
              XSORPayload payload = xsorMemory.copyFromXSOR(objectIndex);
              return payload;
          }
        } finally {
          xsorMemory.unlockObject(objectIndex);
        }
      case SHUTDOWN:
        return null;
      case DISCONNECTED:
      case NEVER_CONNECTED:
      case DISCONNECTED_MASTER:
        xsorMemory.lockObject(objectIndex);
        XSORPayload payload = null;
        try {
          payload = xsorMemory.copyFromXSOR(objectIndex);
        } finally {
          xsorMemory.unlockObject(objectIndex);
        }
        return payload;
    }
    Exception e=new Exception("ERROR reading xsordata in state="+currentState);
    logger.error("ERROR reading xsordata in state="+currentState,e);
    return null;//SHOULD NOT HAPPEN
  }

}
