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
package com.gip.xyna.utils.timing;


/**
 * ExecutionPeriod berechnet die Zeitpunkte, zu denen Operationen wiederholt ausgef�hrt werden sollen.
 * 
 * �ber die Methode {@link #next(long, int)} kann der n�chste geplante Zeitpunkt nach dem 
 * aktuellen Zeitpunkt "<code>now</code>" berechnet werden.<br>
 * 
 * Die Zeitpunkte sind durch ein Intervall voneinander getrennt, welches je nach ExecutionPeriod-Typ und 
 * dem aktuellen Zeitpunkt mehr oder weniger einem Soll-Intervall entspricht. Die Abweichungen vom 
 * Soll-Intervall dienen dazu, dass Verz�gerungen des aktuellen Zeitpunkts gegen�ber dem letzen 
 * geplanten Zeitpunkt ausgeglichen werden k�nnen oder ausgefallene Zeitpunkte nachgeholt werden k�nnen.
 *
 */
public class ExecutionPeriod {

  public abstract static class Calculation {
    protected long start;
    protected long interval;
    
    protected Calculation(long start, long interval) {
      this.start = start;
      this.interval = interval;
    }
    public abstract long next(long now, int counter);
    public void reset(long start) {
      this.start = start;
    }
  }
  
  /**
   * Das Intervall wird immer fest eingehalten, die Termine verschieben sich dadurch
   */
  public static class FixedInterval extends Calculation {
    public FixedInterval(long start, long interval) {
      super(start, interval);
    }
    public long next(long now, int counter) {
      if( counter == 0 ) {
        return now;
      } else {
        return now+interval;
      }
    }
  }
   
  /**
   * Die Termine sind in gleichm��igen Abst�nden zum Startzeitpunkt, 
   * verpasste Termine werden nicht nachgeholt.
   */
  public static class FixedDate_IgnoreAllMissed extends Calculation {
    public FixedDate_IgnoreAllMissed(long start, long interval) {
      super(start, interval);
    }
    public long next(long now, int counter) {
      if( counter == 0 ) {
        return calc(now,interval-1);
      }
      return calc(now,interval);
    }
    private long calc(long now, long add) {
      long cnt = (now -start +add)/interval;
      return start +cnt*interval;
    }
    
  }
  
  /**
   * Die Termine sind in gleichm��igen Abst�nden zum Startzeitpunkt, 
   * verpasste Termine werden alle nachgeholt, da Termine auch in der 
   * Vergangenheit liegen.
   */
  public static class FixedDate_CatchUpInPast extends Calculation {
    public FixedDate_CatchUpInPast(long start, long interval) {
      super(start, interval);
    }
    public long next(long now, int counter) {
      return start +counter*interval;
    }
  }
  
  /**
   * Die Termine sind bevorzugt in gleichm��igen Abst�nden zum Startzeitpunkt, 
   * verpasste Termine werden zum aktuellen Zeitpunkt nachgeholt.
   */
  public static class FixedDate_CatchUpImmediately extends Calculation {
    public FixedDate_CatchUpImmediately(long start, long interval) {
      super(start, interval);
    }
    public long next(long now, int counter) {
      return Math.max( now, start +counter*interval );
    }
  }
  
  /**
   * Die Termine sind bevorzugt in gleichm��igen Abst�nden zum Startzeitpunkt, 
   * verpasste Termine werden zum aktuellen Zeitpunkt nachgeholt, falls sie nicht zu alt sind.
   */
  public static class FixedDate_CatchUpNotTooLate extends Calculation {
    private long skipped = 0;
    private long tooLateThreshold = 0;
    public FixedDate_CatchUpNotTooLate(long start, long interval) {
      super(start, interval);
      tooLateThreshold = interval/2; //nachholen, wenn der Abstand zu n�chsten Termin gr��er ist als der Anstand zum verpassten
    }
    public FixedDate_CatchUpNotTooLate(long start, long interval, long tooLateThreshold) {
      super(start, interval);
      this.tooLateThreshold = tooLateThreshold;
    }
    public void setTooLateThreshold(long tooLateThreshold) {
      this.tooLateThreshold = tooLateThreshold;
    }
    @Override
    public void reset(long start) {
      super.reset(start);
      skipped = 0;
    }
    public long next(long now, int counter) {
      long cnt = (now -start)/interval+1;
      //gibt es einen Versatz zwischen erwartetem cnt und tats�chlichem counter? bisheriger Versatz skipped
      long skipInc = cnt-counter-skipped;
      long next = start +cnt*interval;
      if( skipInc == 0 ) {
        return next; //kein Versatz
      } else {
        //wie gro� ist Versp�tung gegen�ber regul�rem Termin?
        long tooLate = now +interval - next;
        if( tooLate < tooLateThreshold ) {
          skipped += skipInc-1; //Versatz hinzuf�gen, -1 da nachgeholt
          return now;
        } else {
          skipped += skipInc;
          return next;
        }
      }
    }
  }
  
 
  
  
  
  public static enum Type {
    FixedInterval() {
      public FixedInterval getInstance(long start, long interval) {
        return new FixedInterval(start, interval);
      }
    },
    FixedDate_IgnoreAllMissed() {
      public FixedDate_IgnoreAllMissed getInstance(long start, long interval) {
        return new FixedDate_IgnoreAllMissed(start, interval);
      }
    },
    FixedDate_CatchUpInPast() {
      public FixedDate_CatchUpInPast getInstance(long start, long interval) {
        return new FixedDate_CatchUpInPast(start, interval);
      }
    },
    FixedDate_CatchUpImmediately() {
      public FixedDate_CatchUpImmediately getInstance(long start, long interval) {
        return new FixedDate_CatchUpImmediately(start, interval);
      }
    },
    FixedDate_CatchUpNotTooLate() {
      public FixedDate_CatchUpNotTooLate getInstance(long start, long interval) {
        return new FixedDate_CatchUpNotTooLate(start, interval);
      }
    };
    
    public abstract Calculation getInstance(long start, long interval);
    
    
  }
  
  private Calculation calculation;
  private int counter;
  private long start;
  
  public ExecutionPeriod(Type type, long interval, long start) {
    this.calculation = type.getInstance(start,interval);
    this.start = start;
  }
  
  public ExecutionPeriod(Calculation calculation) {
    this.calculation = calculation;
  }
  
  public Calculation getCalculation() {
    return calculation;
  }
  
  public void setCounter(int counter) {
    this.counter = counter;
  }

  public long getStart() {
    return start;
  }
  
  /**
   * Berechnung des n�chsten Zeitpunktes, interner Counter wird erh�ht
   * @param now
   * @return
   */
  public long nextAndInc(long now) {
    return calculation.next(now,counter++);
  }
  
  /**
   * Berechnung des n�chsten Zeitpunktes, externer Counter wird verwendet
   * @param now
   * @param counter
   * @return
   */
  public long next(long now, int counter) {
    return calculation.next(now,counter);
  }

  /**
   * Reset der ExecutionPeriod: Counter wird auf 0 gesetzt, Startzeitpunkt wird beibehalten
   */
  public void reset() {
    counter = 0;
    calculation.reset(start);
  }
  
  /**
   * Reset der ExecutionPeriod: Counter wird auf 0 gesetzt, Startzeitpunkt wird ge�ndert.
   * @param start
   */
  public void reset(long start) {
    counter = 0;
    this.start = start;
    calculation.reset(start);
  }

}
