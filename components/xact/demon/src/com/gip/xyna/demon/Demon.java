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
package com.gip.xyna.demon;

import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.gip.xyna.demon.persistency.PersistableEnum;
import com.gip.xyna.utils.snmp.SnmpAccessData;


/**
 * The Demon is running the DemonWorker, does the status handling an acts as an SnmpAgent through DemonSnmpAgent.
 *
 */
public class Demon {
  static Logger logger = Logger.getLogger(Demon.class.getName());
  static Logger statusLogger = Logger.getLogger("DemonStatus");

  private static final String NAME = ".name";
  private static final String INDEX = ".snmp.oid.index";
  private static final String STATUS_FILENAME = ".status.filename";
  private static final String DEMON_STATUS = "demon.status";

  public static enum Status {
    RUNNING("running",1),
    STOPPED("stopped",15),
    UNKNOWN("unknown",-1);
    
    private String name;
    private int i;

    private Status( String name, int i ) {
      this.name = name;
      this.i = i;
    }
    
    @Override
    public String toString() {
      return name;
    }

    public static Status fromString(String status) {
      for( Status s : values() ) {
        if( s.name.equals(status) ) {
          return s;
        }
      }
      return UNKNOWN;
    }

    public int toInt() {
      return i;
    }
  }
  
  private DemonSnmpAgent demonSnmpAgent;
  private PersistableEnum<Status> status;
  private DemonWorker demonWorker;
  private String demonIndex;
  private DemonKiller demonKiller;
  private String demonName;
  private Date startDate;
  
  /**
   * Sole constructor
   * @param demonName
   * @param demonWorker what should the demon do?
   * @param snmpAccessData on which snmp-port the demon is listening 
   * @param statusFile the statusFile
   * @param demonIndex Index of the snmp-Data to distinguish several Demons
   */
  public Demon(String demonName, DemonWorker demonWorker, SnmpAccessData snmpAccessData, String demonIndex ) {
    this.demonWorker = demonWorker;
    this.demonIndex = demonIndex;
    this.demonName = demonName;
    demonSnmpAgent = new DemonSnmpAgent(this, demonIndex, snmpAccessData );
    demonKiller = new DemonKiller();    
    status = new PersistableEnum<Status>( DEMON_STATUS, Status.class);
    DemonPersistency.getInstance().registerPersistable( status );
  }
    
  /**
   * Convenient way to create a new Demon: all data is read from the DemonProperties
   * @param demonName
   * @return
   */
  public static Demon createDemon(String demonPrefix) {
    DemonPersistency.createInstance( DemonProperties.getProperty( demonPrefix+STATUS_FILENAME ) );
    
    return new Demon(DemonProperties.getProperty( demonPrefix+NAME), null,
        DemonProperties.getSnmpAccessData( SnmpAccessData.VERSION_2c, demonPrefix ),
        DemonProperties.getProperty( demonPrefix+INDEX )
      );
  }
  
  /**
   * Starts the Demon.
   * In einer Endlosschleife wird versucht, den Demon zu starten. Die Bemühungen werden alle
   * 5 Sekunden wiederholt und ins Log ausgegeben. Dies verhindert das permanente Starten durch
   * den Respawn-Mechanismus.
   * @throws IOException
   */
  public void startDemon() {
    boolean started = false;
    do {
      try {
        logger.debug( "Starting SnmpAgent ");
        demonSnmpAgent.start();
        startTimer();
        started = true;
        startDate =  new Date();
      } catch(Exception e) {
        logger.error( "Could not start Demon ",e);
        try {
          Thread.sleep(5000);
        } catch (InterruptedException e1) {
          logger.error( "Sleep interrupted ",e1);
        }
        logger.info( "Retry to start Demon ");
      }
      
    } while( ! started );
    logger.debug( "Demon has started");
    demonKiller.start();
    tryDemonWorkerStart();
  }

  /**
   * periodischer Aufruf von LogStatus 
   */
  private void startTimer() {
    Timer timer = new Timer("DemonTimer");
    timer.schedule( new TimerTask(){
      @Override
      public void run() {
        try {
          logStatus();
        } catch( Throwable t ) {
          logger.error("Exception in logStatus",t);
        }
      }}, 1000, 600*1000 );
  }

  protected void logThreads() {
    Thread[] tarray = new Thread[ Thread.activeCount() ];
    Thread.enumerate(tarray);
    for( Thread t : tarray ) {
      System.err.println( t );
      /*
      StackTraceElement[] stes = t.getStackTrace();
      for( StackTraceElement ste :stes ) {
        System.err.println( "  " + ste );
      }
      */
    }
  }
  
  public void stopDemon() {
    logger.info("stopping the demon");
    if(demonWorker != null ) {
      demonWorker.terminate();
    }
    updateStatus( Status.STOPPED );
  }
  
  public void setDemonWorker(DemonWorker demonWorker) {
    if( this.demonWorker == null ) {
      this.demonWorker = demonWorker;
      try {
        demonWorker.configureDemonSnmp( new DemonSnmpConfigurator( demonSnmpAgent ) );
      } catch( Exception e ) {
        logger.error( "Error while configuring demonSnmpAgent", e);
        demonSnmpAgent.reset();
        this.demonWorker = null;
      }
    } else {
      throw new IllegalStateException("DemonWorker is already set");
    }
  }
 

  public void startDemonWorker() {
    tryDemonWorkerStart();
  }


  private void tryDemonWorkerStart() {
   
    if( status.is(Status.RUNNING) && demonWorker != null ) {
      startDemonWorkerThread();
    } else {
      if( status.is(Status.STOPPED) && demonWorker != null ) {
        logger.info( "DemonWorker ist stopped");
      }
    }
  }

  private boolean startDemonWorkerThread() {
    if( demonWorker != null ) {
      Thread t = new Thread() {
        @Override
        public void run() {
          try {
            demonWorker.run();
          } catch( Throwable throwable ) {
            logger.fatal( "demonWorker "+demonWorker.getName()+" crashed",throwable);
            try { 
              demonWorker.terminate();
            } catch( Throwable throwable2 ) {
              logger.fatal( "Could not terminate demonWorker",throwable2);
            }
            demonWorker = null;
            stopDemon();
            logger.warn( "Demon is now stopped due to fatal exception in demonWorker");
          }
        }
      };
      t.setName( demonWorker.getName() );
      t.setDaemon( false ); //demonWorker darf kein Daemon-Thread sein, damit das Programm weiterläuft
      t.start();
      logger.debug( "DemonWorker has started");
      return true;
    } else {
      logger.debug( "DemonWorker is not configured yet");
      return false;
    }
  }

  /**
   * Calls the matching signal-function
   * @param signal
   * @return true, if signal can be processed
   */
  public boolean setSignal( DemonSignal signal ) {
    logger.info( "Signal " + signal );
    switch( signal ) {
    case EXIT:
      demonKiller.kill();
      break;
    case TERM:
      if( isRunning() ) {
        stopDemon();
      } else {
        logger.info( "Signal to start already stopped demon");
        return false;
      }
      break;
    case LOG:
      logStatus();
      break;
    case START:
      if( isRunning() ) {
        logger.info( "Signal to start already started demon");
        return false;
      } else {
        if( startDemonWorkerThread() ) {
          updateStatus(Status.RUNNING);
        }
      }
      break;
    default:
      logger.warn("Unhandled Signal "+ signal );
      return false;
    }
    return true;
  }
  
  /**
   * Loggt den Status durch logStatus
   */
  private void logStatus() {
    if( statusLogger.isInfoEnabled() ) {
      StringBuilder sb = new StringBuilder();
      sb.append("Demon ");
      if( ! "unknown".equals( DemonProperties.getProperty(DemonProperties.BUILD_VERSION) ) ) {
        sb.append("(version ").append(DemonProperties.getProperty(DemonProperties.BUILD_VERSION));
        sb.append(" from ").append(DemonProperties.getProperty(DemonProperties.BUILD_DATE));
        sb.append(") ");
      }
      sb.append("was started at ").append(startDate);
      statusLogger.info(sb.toString());
      
      sb = new StringBuilder();
      Thread[] tarray = new Thread[ Thread.activeCount() ];
      Thread.enumerate(tarray);
      for( Thread t : tarray ) {
        if( t != null ) {
          sb.append(", ").append( t.getName() );
        }
      }
      statusLogger.info( "running Threads: "+sb.substring(2) );
      
      if( statusLogger.isDebugEnabled() ) {
        for( Thread t : tarray ) {
          if( t != null ) {
            sb = new StringBuilder();
            StackTraceElement[] stes = t.getStackTrace();
            for( StackTraceElement ste :stes ) {
              sb.append( ", "+ste );
            }
            if( sb.length() > 0 ) {
              statusLogger.debug( t.getName()+": "+sb.substring(2) );
            } else {
              statusLogger.debug( t.getName()+": -");
            }
          }
        }
      }
      
    }
    if( demonWorker != null ) {
      demonWorker.logStatus(statusLogger);
    }
  }
  
  /**
   * Updates the status
   * @param newStatus
   */
  private void updateStatus( Status newStatus ) {
    status.set(newStatus);
    DemonPersistency.getInstance().commit(); //FIXME Exception?
  }
  
  /**
   * @return the current status
   */
  public Status getStatus() {
    return status.get();
  }

  /**
   * @return
   */
  public boolean isRunning() {
    return status.is(Status.RUNNING);
  }
  
  public String getName() {
   return demonName;
  }

  public String getIndex() {
    return demonIndex;
  }
 
}
