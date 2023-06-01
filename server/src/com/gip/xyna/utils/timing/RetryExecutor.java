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
package com.gip.xyna.utils.timing;

import java.util.concurrent.TimeUnit;


/**
 * RetryExecutor f�hrt eine {@link Executable}-Implementierung mehrfach aus, bis das Executable "fertig" signalisiert,
 * die maximale Anzahl an Retries �berschritten ist oder ein Timeout erreicht wurde.
 * <p>
 * Konfiguriert werden kann
 * <ul>
 * <li>absoluteTimeout: Retries werden nur bis (absoluteTimeout-timeBetweenRetries/2) durchgef�hrt, Default 0=unbeschr�nkt</li>
 * <li>maxRetries: Maximale Anzahl an Retries, Default 0=unbeschr�nkt</li>
 * <li>timeBetweenRetries: Wartezeit zwischen den Retries, Default 0=keine</li>
 * <li>interruptable: Darf RetryExecutor durch eine Thread.interrupt abgebrochen werden? Default true</li>
 * </ul>
 * <p>
 * Verwendungsbeispiele:
 * <pre>
 * RetryExecutor.retryUntil(System.currentTimeMillis()+1000).sleep(10).execute( new Executable() {...} );
 * RetryExecutor.retryTimeout(1000).maxRetries(20).sleep(10).execute( new Executable() {...} );
 * RetryExecutor.retryTimeout(1,TimeUnit.SECONDS).sleep(10).interruptable(false).execute( new Executable() {...} );
 * RetryExecutor.retryMax(3).execute( new Executable() {...} );
 * RetryExecutor.retry().execute( new Executable() {...} );
 * new RetryExecutor().maxRetries(20).sleep(10).execute( new Executable() {...} );
 * </pre>
 */
public class RetryExecutor {
  
  /**
   * Executable wird von {@link RetryExecutor} mehrfach ausgef�hrt.
   * <p>
   * Methoden:
   * <ul>
   * <li>execute(): erste Ausf�hrung</li>
   * <li>retry(int): n.te Wiederholung</li>
   * <li>failed(boolean): Retries alle fehlgeschlagen</li>
   * </ul>
   */
  public interface Executable {

    /**
     * erste Ausf�hrung
     * @return true, wenn kein Retry n�tig ist
     */
    boolean execute();
    /**
     * Retry-Ausf�hrung
     * @param retry wievielter Retry, beginnt mit 1
     * @return true, wenn kein weiterer Retry n�tig ist
     */
    boolean retry(int retry);
    
    /**
     * Wird nur gerufen, wenn alle Retries fehlgeschlagen sind 
     * @param timeout true: Retries werden wegen Timeout abgebrochen
     *                false: Retries werden wegen MaxRetries abgebrochen
     */
    void failed( boolean timeout);
    
  }
  
  private long absoluteTimeout = 0;
  private long timeBetweenRetries = 0;
  private boolean interruptable = true;
  private int maxRetries = 0;

  
  /**
   * Retries mit relativem Timeout ab jetzt 
   * @param timeoutInMillis
   * @return
   */
  public static RetryExecutor retryTimeout(long timeoutInMillis) {
    return new RetryExecutor().until(System.currentTimeMillis()+timeoutInMillis);
  }
  
  /**
   * Retries mit relativem Timeout ab jetzt 
   * @param timeout
   * @param unit
   * @return
   */
  public static RetryExecutor retryTimeout(long timeout, TimeUnit unit) {
    return new RetryExecutor().until(System.currentTimeMillis()+TimeUnit.MILLISECONDS.convert(timeout,unit));
  }

  /**
   * Retries mit absolutem Timeout 
   * @param absoluteTimeout
   * @return
   */
  public static RetryExecutor retryUntil(long absoluteTimeout) {
    return new RetryExecutor().until(absoluteTimeout);
  }
 
  /**
   * Retries mit maximale Anzahl
   * @param maxRetries
   * @return
   */
  public static RetryExecutor retryMax(int maxRetries) {
    return new RetryExecutor().maxRetries(maxRetries);
  }
  
  /**
   * Unbeschr�nkte Retries
   * @return
   */
  public static RetryExecutor retry() {
    return new RetryExecutor();
  }

  /**
   * Ausf�hren des Executables
   * @param executable
   * @return true, wenn Executable erfolgreich ausgef�hrt wurde
   *         false, wenn Timeout erreicht wurde oder maximale Anzahl an Retries erreicht wurde
   */
  public boolean execute(Executable executable) {
    boolean finished = executable.execute();
    int retry = 0;
    while( ! finished ) {
      ++retry;
      finished = executable.retry(retry);
      if( finished ) {
        break;
      }

      //maxRetries �berwachen
      if( maxRetries > 0 ) {
        if( retry >= maxRetries ) {
          //maximale Anazahl an Retries erreicht
          executable.failed(false);
          break;
        }
      }
      
      long sleep = timeBetweenRetries;
      
      //Timeout �berwachen
      if( absoluteTimeout > 0 ) {
        long now = System.currentTimeMillis();
        //evtl. k�rzer warten
        sleep = Math.min(sleep, absoluteTimeout-now);
        if( sleep < 0 ) {
          //nicht l�nger warten, Timeout ist abgelaufen
          executable.failed(true);
          break;
        } else if( sleep < timeBetweenRetries/2 ) {
          //nicht l�nger warten, Timeout l�uft bald ab
          executable.failed(true);
          break;
        }
      }
      
      //Warten vor n�chster Ausf�hrung
      try {
        Thread.sleep(sleep);
      } catch (InterruptedException e) {
        if( interruptable ) {
          //Abbruch wird gew�nscht
          break;
        } else {
          //dann halt k�rzer warten
        }
      }
      
    }
    return finished;
  }

  /**
   * Setzen des Timouts, bis zu dem Retries ausgef�hrt werden
   * @param absoluteTimeout
   * @return
   */
  public RetryExecutor until(long absoluteTimeout) {
    this.absoluteTimeout = absoluteTimeout;
    return this;
  }

  /**
   * Setzen der Wartezeit zwischen den Retries
   * @param timeBetweenRetries
   * @return
   */
  public RetryExecutor sleep(long timeBetweenRetries) {
    if( timeBetweenRetries < 0 ) {
      throw new IllegalArgumentException("timeBetweenRetries must not be negative");
    }
    this.timeBetweenRetries = timeBetweenRetries;
    return this;
  }
  
  /**
   * Soll RetryExecutor durch eine InterruptedException abgebrochen werden?
   * @return
   */
  public RetryExecutor interruptable(boolean interruptable) {
    this.interruptable = interruptable;
    return this;
  }
  
  /**
   * Anzahl der maximal durchgef�hrten Retries
   * @param maxRetries
   * @return
   */
  public RetryExecutor maxRetries(int maxRetries) {
    this.maxRetries = maxRetries;
    return this;
  }
}
