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
package com.gip.xyna.xprc.xbatchmgmt.beans;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.utils.db.types.StringSerializable;
import com.gip.xyna.utils.timing.ExecutionPeriod;
import com.gip.xyna.utils.timing.ExecutionPeriod.FixedDate_CatchUpNotTooLate;
import com.gip.xyna.utils.timing.ExecutionPeriod.Type;


/**
 * SlaveExecutionPeriod berechnet die wiederholten Startzeitpunkte der Slaves, damit die Slaves nicht 
 * mit maximaler Rate vom Scheduler gestartet werden, sondern mit einer vorgebbaren Rate, konfiguriert 
 * über den Kehrwert der Rate <code>interval</code>.
 * <br>
 * Wie nun die Rate eingehalten wird und wie auf unvorhersehbare Verzögerungen reagiert wird, ist derzeit
 * über folgende Typen konfigurierbar:
 * 
 * <ul>
 * <li>FixedInterval  </li>
 * <li>FixedDate_*
 *   <br>
 *   mit den Untertypen
 *   <ul>
 *   <li>FixedDate_CatchUpInPast</li>
 *   <li>FixedDate_CatchUpImmediately</li>
 *   <li>FixedDate_CatchUpNotTooLate</li>
 *   <li>FixedDate_IgnoreAllMissed</li>
 *   </ul>
 * </li>
 * </ul>
 * FixedInterval nimmt <code>interval</code> immer als Mindestabstand zwischen zwei Startzeitpunkten, 
 * alle Verzögerungen addieren sich.<br>
 * FixedDate_* versucht die Slaves immer in einem festen Takt (konstanten Abstände zwischen den Startzeitpunkten)
 * nach dem Anfangszeitpunkt zu starten. Der Anfangszeitpunkt wird dabei durch das Öffnen des Zeitfensters oder 
 * den Start des Master vorgegeben. Bei einem sich mehrfach öffnenden Zeitfenster wird der Anfangszeitpunkt
 * immer wieder umgesetzt, dies kann dazu führen, dass der Takt nicht exakt fortgesetzt wird.
 * Da durch Verzögerungen im Scheduler (fehlende Capacities, geschlossene Zeitfenster, Factory-Neustart) 
 * die konstanten Abstände nicht immer eingehalten werden kann, muss noch eine Reaktion auf die 
 * Verzögerungen mitangegeben werden, dies ist die Angabe "*":
 * <ul>
 * <li>Von FixedDate_CatchUpInPast und FixedDate_CatchUpImmediately werden alle verpasste Startzeitpunkte
 *     nachgeholt, was bei längeren Ausfällen evtl. viel Last verursacht. Bei CatchUpInPast werden die 
 *     Startzeitpunkte so berechnet, als ob es keine Verzögerung gegeben hätte. Damit liegen sie in der 
 *     Vergangenheit, und die Slaves werden daher evtl. mit ScheudlingTimeout abgebrochen.
 *     Bei CatchUpImmediately werden die Slaves direkt gestartet.</li>
 * <li>Von FixedDate_CatchUpNotTooLate wird der letzte verpasste Startzeitpunkt nachgeholt, wenn er nicht 
 *     zu lange zurückliegt. Der Schwellwert, was als "zu lange" gilt, kann daher noch als zusätzlicher 
 *     Parameter angegeben werden, bei fehlender Angabe wird als Default das halbe Intervall genommen.</li>
 * <li>Von FixedDate_IgnoreAllMissed werden alle verpassten Startzeitpunkte ignoriert, der nächste Slave 
 *     wird erst wieder im normalen Takt ausgeführt.</li>
 * </ul>
 * 
 *
 */
public class SlaveExecutionPeriod implements Serializable, StringSerializable<SlaveExecutionPeriod> {
  private static final long serialVersionUID = 1L;
  
  private static final String PATTERN_STRING = "ExecutionPeriod\\((\\w+),(\\d+),?(\\d+)?\\)";
  private static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);
  
  private Type type;
  private long interval;
  private long additional;
  private transient ExecutionPeriod executionPeriod;
  private transient AtomicInteger counter = new AtomicInteger(0);
  
  public SlaveExecutionPeriod(Type type, long interval) {
    this.type = type;
    this.interval = interval;
    if( type == null ) {
      throw new IllegalArgumentException("Type must not be null");
    }
    if( interval <= 0 ) {
      throw new IllegalArgumentException("Interval must be positive");
    }
  }
  
  public SlaveExecutionPeriod(Type type, long interval, long additional) {
    this(type,interval);
    this.additional = additional;
  }
  
  @Override
  public String toString() {
    return serializeToString();
  }
  
  
  public Type getType() {
    return type;
  }
  
  public long getInterval() {
    return interval;
  }
  
  public long getAdditional() {
    return additional;
  }
  
  public SlaveExecutionPeriod deserializeFromString(String string) {
    return valueOf(string);
  }

  public String serializeToString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ExecutionPeriod(").append(type).append(",").append(interval);
    if( additional != 0 ) {
      sb.append(",").append(additional);
    }
    sb.append(")");
    return sb.toString();
  }

  public static SlaveExecutionPeriod valueOf(String string) {
    if( string == null ) {
      return null;
    }
    Matcher m = PATTERN.matcher(string);
    if( m.matches() ) {
      try {
        Type type = Type.valueOf(m.group(1));
        long interval = Long.parseLong(m.group(2));
        long additional = ( m.group(3) != null ) ? Long.parseLong(m.group(3)) : 0;
        return new SlaveExecutionPeriod(type, interval, additional);
      } catch( Exception e ) {
        throw new IllegalArgumentException("Input \""+string+"\" could not be parsed successfully: "+e.getClass().getSimpleName()+" "+e.getMessage(), e );
      }
    } else {
      throw new IllegalArgumentException("Input \""+string+"\" does not match regexp \""+PATTERN_STRING+"\"");
    }
  }

  /**
   * Bau der eigentlichen ExecutionPeriod aus den Parametern
   * @param start
   * @return
   */
  public ExecutionPeriod createExecutionPeriod(long start) {
    ExecutionPeriod ep = new ExecutionPeriod(type, interval, start);
    if( additional != 0 ) {
      if( type == Type.FixedDate_CatchUpNotTooLate ) {
        ((FixedDate_CatchUpNotTooLate) ep.getCalculation() ).setTooLateThreshold(additional);
      }
    }
    return ep;
  }

  /**
   * Initialiserung bzw. Reinitialiserung der ExecutionPeriod.
   * Bei Reinitialisierung wird die ExecutionPeriod resettet, der neue Startzeitpunkt übernommen
   * und der Counter auf 1 gesetzt. 
   * <br>
   * Der Counter wird auf 1 gesetzt, für das folgende Szenario: Das Zeitfenster öffnet sich wieder, nachemd bereits in 
   * einer früherenPhase Slaves gestartet wurden. Nun wird der erste Slave direkt nach dem Öffnen gestartet, dies 
   * entspricht bereits Counter 0. Danach wird der Timeconstraint für die nächste Wiederholung gesetzt, 
   * der Counter muss dann bereits erhöht werden -&gt; Counter 1. Anders ausgedrückt: eigentlich hätte der Counter 
   * auf 0 gesetzt werden müssen mit dem Wiederöffnen des Zeitfensters, dann wäre die neue Startzeit zu berechnen gewesen 
   * und erst anch einem weiteren Scheduling hätte dann der Slave gestartet werden können. 
   * 
   */
  public void reInit( long start) {
    if( executionPeriod == null ) {
      executionPeriod = createExecutionPeriod(start);
      counter = new AtomicInteger(0);
    }
    if( executionPeriod.getStart() != start) {
      executionPeriod.reset(start);
      counter.set(1);
    }
  }

  /**
   * Berechnung des nächsten Startzeitpunkts
   * @param now
   * @return
   */
  public long next(long now) {
    return executionPeriod.next(now,counter.get() );
  }

  /**
   * Nach jedem Slave-Start muss der Counter erhöht werden, damit die Berechnung des 
   * nächsten Startzeitpunkts korrekt durchgeführt werden kann.
   */
  public void incrementCounter() {
    counter.getAndIncrement();
  }

  public int getCounter() {
    return counter.get();
  }

  /*
  public static void main(String[] args) {
    SlaveExecutionPeriod rd1 = new SlaveExecutionPeriod(Type.FixedInterval, 1000L);
    String ser1 = rd1.serializeToString();
    System.out.println( ser1);
    SlaveExecutionPeriod rd1d = SlaveExecutionPeriod.valueOf(ser1);
    System.out.println( rd1d );
    
    SlaveExecutionPeriod rd2 = new SlaveExecutionPeriod(Type.FixedDate_CatchUpNotTooLate, 1000L, 200L);
    String ser2 = rd2.serializeToString();
    System.out.println( ser2);
    SlaveExecutionPeriod rd2d = SlaveExecutionPeriod.valueOf(ser2);
    System.out.println( rd2d );
    
    SlaveExecutionPeriod rd3d = SlaveExecutionPeriod.valueOf("ExecutionPeriod(FixedInterval,1000)");
    System.out.println( rd3d );
 
  }*/
  
}
