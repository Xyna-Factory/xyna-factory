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

package com.gip.xyna.xfmg.xclusteringservices.clusterprovider;

import java.util.List;

import com.gip.xyna.utils.db.ExtendedParameter;
import com.gip.xyna.utils.db.Parameter;
import com.gip.xyna.utils.db.SQLUtils;
import com.gip.xyna.utils.db.types.BooleanWrapper;
import com.gip.xyna.xfmg.xclusteringservices.ClusterState;


public class ClusterAlgorithmTwoNodes extends ClusterAlgorithmAbstract {

  public ClusterState createCluster(SQLUtils sqlUtils, RemoteInterfaceForClusterStateChangesImplAQ interconnect, int binding) {
    throw new UnsupportedOperationException("createCluster is only implemented by ClusterAlgorithmSingleNode");
  }

  public ClusterState join(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows, int newBinding, RemoteInterfaceForClusterStateChangesImplAQ interconnect) {
    
    ClusterState ownState = ClusterState.STARTING;
    
    Parameter insertionParameters = new ExtendedParameter(
      newBinding, ownState.toString(), new BooleanWrapper(true));

    sqlUtils.executeDML(SQLStrings.INSERT_INTO_CLUSTERSETUP_SQL, insertionParameters);
    

    try {

      interconnect.start(newBinding);

      // notify all other nodes that the local node joined
      interconnect.join(sqlUtils,newBinding); //erledigt implizit das Commit auf den sqlUtils

    } catch (InterFactoryConnectionDoesNotWorkException e) {
      //Fehler: anderer Knoten antwortet nicht korrekt
      rows = new ClusterSetupRowsForUpdate(sqlUtils,newBinding);
      if( rows.getOwn() == null ) {
        //nun ist doch schon ein Rollback geschehen, daher nochmal eintragen
        ownState = ClusterState.DISCONNECTED_MASTER;
        insertionParameters = new ExtendedParameter(
          newBinding, ownState.toString(), new BooleanWrapper(true));
        sqlUtils.executeDML(SQLStrings.INSERT_INTO_CLUSTERSETUP_SQL, insertionParameters);
       
        rows = new ClusterSetupRowsForUpdate(sqlUtils,newBinding);
      }
      if( rows.size() < 2 ) {
        throw new IllegalStateException("Central cluster setup table is broken, too few entries.");
      } else {
        ownState = ClusterState.DISCONNECTED_MASTER;
        setStateInternally(sqlUtils, rows.getOwn().getBinding(), ownState, rows.getOwn().isOnline());
        XynaClusterSetup other = rows.getOthers().get(0);
        setStateInternally(sqlUtils, other.getBinding(), ClusterState.DISCONNECTED_SLAVE, other.isOnline());

        //Dem anderen Knoten m�sste nun mitgeteilt werden, dass er nun Slave ist. Allerdings 
        //geht dies �ber RemoteInterfaceForClusterStateChangesImplAQ nicht, sonst w�re dieser 
        //Catch-Block nicht gerufen worden. Es bleibt die Hoffnung, dass das eigentlich nicht
        //vorkommen sollte. Grund daf�r ist, dass der fremde Knoten wegen Nichterreichbarkeit 
        //des Clusters bereits auf DISCONNECTED_SLAVE gegangen ist.
        //Nicht passieren darf, dass der ListenerThread im RemoteInterfaceForClusterStateChangesImplAQ
        //ausf�llt.
        logger.warn( "Could not notify other Binding "+other.getBinding()+" of stateChange to DISCONNECTED_SLAVE");
      }
      sqlUtils.commit();
    } catch (DBNotReachableException e) {
      //RemoteInterfaceForClusterStateChangesImplAQ konnte nicht gestartet werden,
      //oder sonstige SQL-Fehler beim Enqueue sind aufgetreten
      //damit wird nun joinOrCreateClusterInternally komplett abgebrochen
      throw new SQLRuntimeException(e);
    }
    
    return ownState;
  }

  public ClusterState restorePrepare(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows, RemoteInterfaceForClusterStateChangesImplAQ interconnect) {
    XynaClusterSetup ownRow = rows.getOwn();
    ClusterState ownState;
    
    if( ownRow.isOnline() ) {
      //das h�tte nicht sein sollen: der Knoten war abgest�rzt bzw. konnte beim Herunterfahren die DB nicht erreichen
      logger.warn( "Own XynaClusterSetup-row was marked as online");
    }

    ownState = ClusterState.STARTING;
    setStateInternally(sqlUtils, ownRow.getBinding(), ownState, true);
    
    try {
      interconnect.start(ownRow.getBinding());
    } catch( DBNotReachableException e ) {
      //RemoteInterfaceForClusterStateChangesImplAQ konnte nicht gestartet werden,
      //damit wird nun restorePrepare komplett abgebrochen
      throw new SQLRuntimeException(e);
    }
    return ownState;
  }
  
  public ClusterState restoreConnect(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows, RemoteInterfaceForClusterStateChangesImplAQ interconnect) {
    XynaClusterSetup ownRow = rows.getOwn();
    XynaClusterSetup foreignRow = rows.getOthers().get(0);
    ClusterState ownState = ownRow.getState();
    boolean foreignIsOnline = foreignRow.isOnline();
    
    if( foreignIsOnline ) {
      // notify other node that the local node started
      try {
        interconnect.startup(sqlUtils, ownRow.getBinding()); //erledigt implizit das Commit auf den sqlUtils
      } catch (InterFactoryConnectionDoesNotWorkException e) {
        //anderer Knoten antwortet nicht, m�glicherweise gecrasht und deshalb f�lschlich auf "online"
        foreignIsOnline = false;
      } catch( DBNotReachableException e ) {
        logger.error("restoreCluster: interconnect.startup failed: "+e.getMessage(),e);
        //SQL-Fehler beim Enqueue, dies ist unwahrscheinlich und deutet auf einen Programmierfehler 
        //oder eine pl�tzlich defekte Connection hin -> Wechsel auf DISC_SLAVE
        ownState = ClusterState.DISCONNECTED_SLAVE;
        setStateInternally(sqlUtils, ownRow.getBinding(), ownState, true);
        throw new SQLRuntimeException(e);
      }
    }
    
    if( ! foreignIsOnline ) {
      //fremder Knoten ist nicht online, deswegen auf DISCONNECTED_MASTER gehen
      ownState = ClusterState.DISCONNECTED_MASTER;
      ClusterState foreignState = ClusterState.DISCONNECTED_SLAVE;
      setStateInternally(sqlUtils, ownRow.getBinding(), ownState, true);
      setStateInternally(sqlUtils, foreignRow.getBinding(), foreignState, foreignRow.isOnline());
    }

    return ownState;
  }
  
  public ClusterState connect(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows, RemoteInterfaceForClusterStateChangesImplAQ interconnect) {
    XynaClusterSetup ownRow = rows.getOwn();
    XynaClusterSetup foreignRow = rows.getOthers().get(0);
    
    ClusterState ownState = ownRow.getState();
    ClusterState foreignState = foreignRow.getState();
    
    try {
    
      if( foreignState == ClusterState.STARTING ) {
        //Normalfall: anderer Knoten wartet auf den Connect
        ownState = ClusterState.CONNECTED;
        foreignState = ClusterState.CONNECTED;

        setStateInternally(sqlUtils, ownRow.getBinding(), ownState, true);
        setStateInternally(sqlUtils, foreignRow.getBinding(), foreignState, foreignRow.isOnline());

        interconnect.connect(sqlUtils, ownRow.getBinding()); //erledigt implizit das Commit auf den sqlUtils
      } else if( foreignState == ClusterState.CONNECTED ) {
        if( ownState == ClusterState.CONNECTED ) {
          //der andere Knoten hat gleichzeitig den Wechsel auf CONNECTED vollzogen 
          interconnect.connect(sqlUtils, ownRow.getBinding()); //erledigt implizit das Commit auf den sqlUtils
        } else {
          logger.warn( "Unexpected clusterState "+foreignState+" for foreign node while connecting. Own state is "+ownState );
          //FIXME sollte nicht passieren. Wie darauf reagieren?
        }
      } else if( foreignState == ClusterState.DISCONNECTED_SLAVE ) {
        //anderer Knoten wartet nicht mehr, hat sich bereits abgeschaltet
        if( ownState != ClusterState.DISCONNECTED_MASTER ) {
          logger.warn( "Unexpected own clusterState "+ownState+" for foreign node while connecting. Foreign state is "+foreignState+", switching own state to "+ClusterState.DISCONNECTED_MASTER );
          ownState = ClusterState.DISCONNECTED_MASTER;
          setStateInternally(sqlUtils, ownRow.getBinding(), ownState, true);
        }
      } else {
        logger.warn( "Unexpected clusterState "+foreignState+" for foreign node while connecting. Own state is "+ownState );
        //FIXME sollte nicht passieren. Wie darauf reagieren?

      } 
      
    } catch (InterFactoryConnectionDoesNotWorkException e) {
      //anderer Knoten antwortet nicht, m�glicherweise gecrasht und deshalb f�lschlich auf "online"
      //deswegen doch auf DISCONNECTED_MASTER gehen
      ownState = ClusterState.DISCONNECTED_MASTER;
      foreignState = ClusterState.DISCONNECTED_SLAVE;
      setStateInternally(sqlUtils, ownRow.getBinding(), ownState, true);
      setStateInternally(sqlUtils, foreignRow.getBinding(), foreignState, foreignRow.isOnline());
    } catch( DBNotReachableException e ) {
      logger.error("connect: interconnect.connect failed: "+e.getMessage(),e);
      //SQL-Fehler beim Enqueue, dies ist unwahrscheinlich und deutet auf einen Programmierfehler 
      //oder eine pl�tzlich defekte Connection hin -> Wechsel auf DISC_SLAVE
      ownState = ClusterState.DISCONNECTED_SLAVE;
      setStateInternally(sqlUtils, ownRow.getBinding(), ownState, true);
      throw new SQLRuntimeException(e);
    }
      
    return ownState;
  }

  
  
  public ClusterState changeClusterState(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows, RemoteInterfaceForClusterStateChangesImplAQ interconnect, ClusterState newState) {
    ClusterState currentState = rows.getOwn().getState();
    if( currentState == newState ) {
      return currentState; //nichts zu tun
    }
    switch( newState ) {
      case DISCONNECTED :
      case DISCONNECTED_SLAVE :
      case DISCONNECTED_MASTER :
        return disconnect(sqlUtils, rows, interconnect, newState);
      case CONNECTED :
        throw new IllegalArgumentException("Use \"joinCluster\" instead.");
      case NO_CLUSTER :
        throw new IllegalArgumentException("Use \"leaveCluster\" instead.");
      default:
        throw new IllegalArgumentException("Colun not change clusterState to "+newState);
    }
  }

  
  
  public ClusterState disconnect(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows, RemoteInterfaceForClusterStateChangesImplAQ interconnect, ClusterState newState) {
    if( ! newState.isDisconnected() ) {
      throw new IllegalArgumentException("New state "+newState+" is no disconnected state");
    }
    ClusterState currentState = rows.getOwn().getState();
    XynaClusterSetup ownRow = rows.getOwn();
    XynaClusterSetup foreignRow = rows.getOthers().get(0);
    ClusterState ownState = null;
    boolean foreignIsOnline = foreignRow.isOnline();
    
    if( newState == ClusterState.DISCONNECTED ) {
      //hier steht eine Entscheidung an, ob lokaler Knoten MASTER oder SLAVE wird
      //wahrscheinlich wird disconnect zeitgleich auf dem anderen Knoten aufgerufen 
      //lokal war der Aufruf aber schneller (select for update)
      switch( currentState ) {
        case CONNECTED:
          //lokaler Knoten kann sich aussuchen, ob er MASTER oder SLAVE wird -> MASTER
          ownState = ClusterState.DISCONNECTED_MASTER;
          break;
        case DISCONNECTED_SLAVE:
          //fremder Knoten hat wahrscheinlich das Rennen gewonnen und lokal ein SLAVE eingetragen
          ownState = ClusterState.DISCONNECTED_SLAVE; //Status nicht umsetzen
          break;
        case DISCONNECTED_MASTER:
          ownState = ClusterState.DISCONNECTED_MASTER; //Status nicht umsetzen
          break;
        case DISCONNECTED:
        case STARTING:
          //das sollte eigentlich nicht auftreten
          if( foreignRow.getState() == ClusterState.DISCONNECTED_SLAVE ) {
            ownState = ClusterState.DISCONNECTED_MASTER;
          } else {
            //je nachdem ob fremder Knoten reagiert kann lokal ein DISCONNECTED_MASTER angenommen werden
            foreignIsOnline = checkForeignNodeAlive(sqlUtils, interconnect, ownRow.getBinding());
            ownState = foreignIsOnline ? ClusterState.DISCONNECTED_SLAVE : ClusterState.DISCONNECTED_MASTER;
          }
          break;
        default:
          throw new RuntimeException( "Unexpected local clusterState "+ currentState);
      }
     
    } else if( newState == ClusterState.DISCONNECTED_SLAVE ) {
      //dieser Wunsch kann immer erf�llt werden
      ownState = ClusterState.DISCONNECTED_SLAVE;
      
    } else if (newState == ClusterState.DISCONNECTED_MASTER) {
      //Achtung: nicht beide Knoten d�rfen auf DISCONNECTED_MASTER gehen!
      if( foreignRow.getState().in( ClusterState.DISCONNECTED_SLAVE, ClusterState.CONNECTED) ) {
        ownState = ClusterState.DISCONNECTED_MASTER;
      } else if( foreignRow.getState().in( ClusterState.DISCONNECTED_MASTER ) ) {
        
        //fremder Knoten ist DISCONNECTED_MASTER, daher sollte er online sein und ein Wechsel 
        //des eigenen Knotens auf DISCONNECTED_MASTER sollte verboten sein. Aber vielleicht ist der fremde 
        //Knoten ja gecrasht und deshalb f�lschlich auf DISCONNECTED_MASTER. Dies wird nun getestet.
        foreignIsOnline = checkForeignNodeAlive(sqlUtils, interconnect, ownRow.getBinding());
        if( foreignIsOnline ) {
          throw new RuntimeException("Foreign node is online, disconnect to state DISCONNECTED_MASTER is not possible. other node is in state "
            + foreignRow.getState());
        } else {
          ownState = ClusterState.DISCONNECTED_MASTER;
        }
      } else {
        throw new RuntimeException("disconnect to state DISCONNECTED_MASTER is not possible. other node is in state "
            + foreignRow.getState());
      }

    } else {
      throw new RuntimeException( "Unconsidered disconnected state "+ newState);
    }
    
    if (ownState != currentState) {
      if (logger.isInfoEnabled()) {
        logger.info("Disconnecting from state " + currentState + " to " + ownState);
      }
      
      ClusterState foreignState = null;
      if( ownState == ClusterState.DISCONNECTED_SLAVE ) {
        foreignState = ClusterState.DISCONNECTED_MASTER;
      } else {
        foreignState = ClusterState.DISCONNECTED_SLAVE;
      }

      setStateInternally(sqlUtils, ownRow.getBinding(),     ownState,     true);
      setStateInternally(sqlUtils, foreignRow.getBinding(), foreignState, foreignRow.isOnline());

      if( foreignIsOnline ) {
        interconnect.disconnect(sqlUtils, ownRow.getBinding());
      }
    }
    return ownState;
  }

  private boolean checkForeignNodeAlive(SQLUtils sqlUtils, RemoteInterfaceForClusterStateChangesImplAQ interconnect, int ownBinding) {
    //Ob der fremde Knoten alive ist, wird durch einen Ping getestet. 
    //Als Ping eignet sich die waiting-Methode, da diese keine Nebenwirkungen hat
    boolean alive = false;
    try {
      interconnect.waiting(sqlUtils, ownBinding);
      alive = true;
    } catch (DBNotReachableException e) {
      logger.info("waiting could not be communicated over aq", e);
    } catch (InterFactoryConnectionDoesNotWorkException e) {
      logger.info("waiting could not be communicated over aq", e);
    }
    if (logger.isInfoEnabled()) {
      logger.info("foreign node " + (alive ? "is" : "is not") + " alive");
    }
    return alive;
  }

  public ClusterState shutdown(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows, RemoteInterfaceForClusterStateChangesImplAQ interconnect) {
    // lokaler Knoten f�hrt herunter
    XynaClusterSetup ownRow = rows.getOwn();
    XynaClusterSetup foreignRow = rows.getOthers().get(0);
    ClusterState ownState;
    if (ownRow.getState() == ClusterState.CONNECTED) {

      // Knoten f�hrt im Normalbetrieb herunter
      ownState = ClusterState.DISCONNECTED_SLAVE;
      setStateInternally(sqlUtils, ownRow.getBinding(), ownState, false);
      if (!foreignRow.isOnline()) {
        logger.warn("Local state is <" + ClusterState.CONNECTED + "> but foreign node is not marked as <online>");
      }
      if (foreignRow.getState() != ClusterState.CONNECTED) {
        throw new RuntimeException("Cluster state is inconsistent");
      }
      setStateInternally(sqlUtils, foreignRow.getBinding(), ClusterState.DISCONNECTED_MASTER,
                                  foreignRow.isOnline());
      
    } else if (ownRow.getState().isDisconnected()) {
      // Knoten ist gerade disconnected und f�hrt herunter
      
      ownState = ownRow.getState();
      switch( ownState ) {
        case DISCONNECTED:
        case STARTING:
          ownState = ClusterState.DISCONNECTED_SLAVE;
          break;
        default:
          //ownState beibehalten
      }
      
      setStateInternally(sqlUtils, ownRow.getBinding(), ownState, false);
      if( foreignRow.getState().isDisconnected() ) {
        //anderer Knoten ist bereits disconnected, daher State nicht umsetzen
      } else {
        setStateInternally(sqlUtils, foreignRow.getBinding(), ClusterState.DISCONNECTED_MASTER,
                                     foreignRow.isOnline());
      }
      
    } else {
      throw new RuntimeException("own state=" + ownRow.getState());
    }

    //Remote-Knoten benachrichtigen
    if( foreignRow.isOnline() ) {
      interconnect.disconnect(sqlUtils,ownRow.getBinding());
    }
    
    return ownState;
  }

  

  /* (non-Javadoc)
   * @see com.gip.xyna.xfmg.xclusteringservices.clusterprovider.ClusterAlgorithm#leaveCluster(com.gip.xyna.utils.db.SQLUtils, com.gip.xyna.xfmg.xclusteringservices.clusterprovider.OracleRACClusterProvider.ClusterSetupRowsForUpdate)
   */
  public ClusterState leaveCluster(SQLUtils sqlUtils, ClusterSetupRowsForUpdate rows) {
    List<XynaClusterSetup> others = rows.getOthers();

    //Status f�r den anderen Knoten auf SINGLE setzen.
    setStateInternally(sqlUtils, others.get(0).getBinding(), ClusterState.SINGLE, others.get(0).isOnline());
    
    //eigenen Eintrag entfernen
    sqlUtils.executeDML(SQLStrings.DELETE_FOR_BINDING_SQL, new Parameter(rows.getOwn().getBinding()));
    return ClusterState.NO_CLUSTER;
  }

}
