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
package com.gip.xyna.xprc.xsched.timeconstraint.windows;



/**
 * TimeWindow ist die Basisklasse für verschiedene Zeitfenster-Implementierungen.
 * Nachdem ein {@link #recalculate(long) recalculate} mit dem aktuellen Timestamp aufgerufen wurde,
 * kann über {@link #isOpen()} der Zustand des Zeitfensters und über {@link #getNextChange()}
 * der Zeitpunkt des nächsten Zustandswechsel erfragt werden.
 */
public abstract class TimeWindow {
  
  private TimeWindowData timeWindowData;
  
  /**
   * TimeWindowData enthält die wesentlichen Daten, die das Zeitfenster derzeit charakterisieren.
   * Dazu gehören nicht die Daten, die Grundlage zur Neuberechnung sind.
   * <ul>
   * <li>isOpen: ist Zeitfenster derzeit offen (nextClose&lt;nextOpen)</li>
   * <li>nextOpen: nächster Zeitpunkt, zu dem Zeitfenster geöffnet wird</li>
   * <li>nextClose: nächster Zeitpunkt, zu dem Zeitfenster geschlossen wird</li>
   * </ul>
   * TimeWindowData ist immutable.
   */
  public static class TimeWindowData {
    
    private boolean isOpen;
    private long nextOpen;
    private long nextClose;
    private long since;
    private boolean onlyEstimated;
    
    public TimeWindowData(boolean isOpen, long nextOpen, long nextClose, long since) {
      this.isOpen = isOpen;
      this.nextOpen = nextOpen;
      this.nextClose = nextClose;
      this.since = since;
      this.onlyEstimated = false;
    }
    
    public TimeWindowData(boolean isOpen, long nextOpen, long nextClose, long since, boolean onlyEstimated) {
      this.isOpen = isOpen;
      this.nextOpen = nextOpen;
      this.nextClose = nextClose;
      this.since = since;
      this.onlyEstimated = onlyEstimated;
    }
    
    @Override
    public String toString() {
      return "TimeWindowData("+isOpen+","+nextOpen+","+nextClose+","+onlyEstimated+")";
    }

    public boolean isOpen() {
      return isOpen;
    }
    
    public long getNextClose() {
      return nextClose;
    }
    
    public long getNextOpen() {
      return nextOpen;
    }
    
    public long getSince() {
      return since;
    }
    
    public boolean isOnlyEstimated() {
      return onlyEstimated;
    }
  }
  
  
  
  /**
   * Neuberechnen der TimeWindowData
   * @param now
   * @return TimeWindowData
   */
  protected abstract TimeWindowData recalculateInternal(long now);
  
  public abstract TimeWindowDefinition getDefinition();

  
  /**
   * @param now aktueller Zeitstempel
   */
  public void recalculate(long now) {
    timeWindowData = recalculateInternal(now);
  }
  
  /**
   * Ist Zeitfenster derzeit offen?
   * @return
   */
  public boolean isOpen() {
    return timeWindowData.isOpen();
  }

  /**
   * Was ist der Zeitpunkt des nächsten Wechsel offen nach geschlossen oder umgekehrt?
   * @return
   */
  public long getNextChange() {
    return Math.min(getNextOpen(), getNextClose());
  } 

  /**
   * Was ist der Zeitpunkt des nächsten Wechsels offen nach geschlossen?
   * @return
   */
  public long getNextClose() {
    return timeWindowData.getNextClose();
  }
  
  /**
   * Was ist der Zeitpunkt des nächsten Wechsels geschlossen nach offen?
   * @return
   */
  public long getNextOpen() {
    return timeWindowData.getNextOpen();
  }
  
  /**
   * Seit wann ist das Zeitfenster offen?
   * @return
   */
  public long getSince() {
    return timeWindowData.getSince();
  }
  
  
  public boolean isOnlyEstimated() {
    return timeWindowData.isOnlyEstimated();
  }
  
}
