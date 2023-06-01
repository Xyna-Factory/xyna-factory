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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;


/**
 * MultiTimeWindow fasst mehrere {@link TimeWindow}-Implementierungen zusammen, um damit 
 * komplexere Zeitfenster zu ermöglichen.
 * Das MultiTimeWindow gilt als offen, wenn mindestens eines der Zeitfenster offen ist.
 * Da sich die zusammengefassten Zeitfenster überlappen können, sind die berechnete Zeitpunkte 
 * des nächsten Öffnens und Schließens evtl. nicht richtig. In diesem Fall wird in TimeWindowData
 * das Flag onlyEstimated gesetzt.
 */
public class MultiTimeWindow extends TimeWindow {

  private static Logger logger = CentralFactoryLogging.getLogger(MultiTimeWindow.class);
  
  private ArrayList<TimeWindow> timeWindows;
  private MultiTimeWindowDefinition definition;
  
  public MultiTimeWindow(TimeWindow ... timeWindows ) {
    this.timeWindows = new ArrayList<TimeWindow>( Arrays.asList(timeWindows) );
    this.definition = new MultiTimeWindowDefinition(this.timeWindows);
  }
  
  @Override
  public String toString() {
    return "MultiTimeWindow("+timeWindows.size()+" subWindows)";
  }
  
  public void add(TimeWindow timeWindow) {
    timeWindows.add(timeWindow);
    this.definition = new MultiTimeWindowDefinition(this.timeWindows);
  }
 
  @Override
  protected TimeWindowData recalculateInternal(long now) {
    if( timeWindows.size() == 0 ) {
      return new TimeWindowData(false,Long.MAX_VALUE,Long.MAX_VALUE,0);
    }
    
    ArrayList<TimeWindowData> data = new ArrayList<TimeWindowData>();
    int openCnt = recalculateAll( data, now );
    
    boolean isOpen = openCnt > 0;
    long nextOpen;
    long nextClose;
    long since;
    boolean estimated = false;
    if( isOpen ) {
      //derzeit offen -> nextClose muss geschätzt werden, daraus dann nextOpen berechnen
      nextClose = getNextClose(getMaxNextCloseForOpen(data));
      if( nextClose < 0 ) {
        estimated = true;
        nextClose = - nextClose;
      }
      since = getOpenSince(getMinSinceForOpen(data));
      if( since < 0 ) {
        estimated = true;
        since = - since;
      }
      data.clear();
      recalculateAll( data, nextClose );
      nextOpen = getMinNextOpen(data);
    } else {
      //derzeit geschlossen -> nextOpen ist einfach ermittelbar, daraus dann nextClose schätzen
      nextOpen = getMinNextOpen(data);
      nextClose = getNextClose(nextOpen);
      since = getMaxSince(data);
    }
    return new TimeWindowData(isOpen, nextOpen, nextClose, since, estimated);
  }
  
  private int recalculateAll(ArrayList<TimeWindowData> data, long now) {
    int openCnt = 0;
    for( TimeWindow tw : timeWindows ) {
      TimeWindowData twd = tw.recalculateInternal(now);
      data.add( twd );
      if( twd.isOpen() ) {
        ++openCnt;
      }
    }
    return openCnt;
  }

  /**
   * Achtung: wenn nextClose nur geschätzt wird, ist Ausgabe negativ
   * @param now
   * @return
   */
  private long getNextClose(long now) {
    //Zeitfenster ist nur geschlossen, wenn alle Sub-Zeitfenster geschlossen sind. 
    //Aufgrund von Überlappungen der Offen-Phasen muss hier iterativ nach dem 
    //nächsten Schließen gesucht werden
    long estimatedNextClose = now;
    ArrayList<TimeWindowData> data = new ArrayList<TimeWindowData>();
    for( int i=0; i<10; ++i ) { //FIXME Grenze konfigurieren, hilft gegen Endlosschleife
      data.clear();
      int openCnt = recalculateAll(data, estimatedNextClose);
      if( openCnt == 0 ) {
        //alles geschlossen, d.h. nextClose ist gefunden
        return estimatedNextClose;
      } else {
        //mindestens ein Zeitfenster ist offen, d.h. nach max. nextClose dieser offenen Zeitfenster suchen
        estimatedNextClose = getMaxNextCloseForOpen(data); 
      }
    }
    return - estimatedNextClose;
  }
  
  /**
   * Achtung: wenn since nur geschätzt wird, ist Ausgabe negativ
   * @param now
   * @return
   */
  private long getOpenSince(long now) {
    //Zeitfenster ist offen, wenn mindestens ein Sub-Zeitfenster offen ist. 
    //Aufgrund von Überlappungen der Offen-Phasen muss hier iterativ nach dem 
    //esten Öffnen gesucht werden
    long estimatedOpenSince = now;
    ArrayList<TimeWindowData> data = new ArrayList<TimeWindowData>();
    for( int i=0; i<10; ++i ) { //FIXME Grenze konfigurieren, hilft gegen Endlosschleife
      data.clear();
      int openCnt = recalculateAll(data, estimatedOpenSince-1 );
      if( openCnt == 0 ) {
        //alle Zeitfenster geschlossen, daher ist openSince gefunden
        return estimatedOpenSince;
      }
      long openSince = getMinSinceForOpen(data);
      if( openSince == estimatedOpenSince ) {
        //alle Zeitfenster starten gleichzeitig: openSince ist gefunden
        return openSince;
      } else {
        //zu diesem Zeitpunkt gibt es bereits offene Zeitfenster: nach früherem OpenSince weitersuchen
        estimatedOpenSince = openSince;
      }
    }
    return - estimatedOpenSince;
  }
    
  private long getMinNextOpen(ArrayList<TimeWindowData> data) {
    long nextOpen = Long.MAX_VALUE;
    for( TimeWindowData twd : data ) {
      nextOpen = Math.min(nextOpen, twd.getNextOpen() );
    }
    return nextOpen;
  }
  
  private long getMaxNextCloseForOpen(ArrayList<TimeWindowData> data) {
    long nextClose = Long.MIN_VALUE;
    for( TimeWindowData twd : data ) {
      if( twd.isOpen() ) {
        nextClose = Math.max(nextClose, twd.getNextClose() );
      }
    }
    return nextClose;
  }
  
  private long getMinSinceForOpen(ArrayList<TimeWindowData> data) {
    long since = Long.MAX_VALUE;
    for( TimeWindowData twd : data ) {
      if( twd.isOpen() ) {
        since = Math.min(since, twd.getSince() );
      }
    }
    return since;
  }
  
  private long getMaxSince(ArrayList<TimeWindowData> data) {
    long since = Long.MIN_VALUE;
    for( TimeWindowData twd : data ) {
      since = Math.max(since, twd.getSince() );
    }
    return since;
  }

  //zum Testen package private
  int getOpened(long now) {
    ArrayList<TimeWindowData> data = new ArrayList<TimeWindowData>();
    int openCnt = recalculateAll( data, now );
    return openCnt;
  }

  @Override
  public TimeWindowDefinition getDefinition() {
    return definition;
  }
  
  public static class MultiTimeWindowDefinition extends TimeWindowDefinition {
    private static final long serialVersionUID = 1L;
    private static final String TYPE = "Multi";
    private static final String P_LONG = "(\\d+)";
    private static final String P_DEFINITION = TYPE+"\\("+P_LONG+",(.*)\\)";
    private static final Pattern PATTERN_DEFINITION = Pattern.compile(P_DEFINITION);
  
    
    private List<TimeWindowDefinition> definitions = new ArrayList<TimeWindowDefinition>();
    private transient String serialized;
    
    public MultiTimeWindowDefinition(ArrayList<TimeWindow> timeWindows) {
      definitions = new ArrayList<TimeWindowDefinition>();
      for( TimeWindow tw : timeWindows ) {
        definitions.add(tw.getDefinition()); //FIXME prüfen, dass kein MultiTimeWindow
      }
    }
    
    public static MultiTimeWindowDefinition construct(List<? extends TimeWindowDefinition> timeWindowDefinitions) {
      MultiTimeWindowDefinition multi = new MultiTimeWindowDefinition();
    //FIXME prüfen, dass kein MultiTimeWindowDefinition in timeWindowDefinitions enthalten
      multi.definitions = new ArrayList<TimeWindowDefinition>(timeWindowDefinitions);
      return multi;
    }

    private MultiTimeWindowDefinition() {
    }

    @Override
    public String toString() {
      return "TimeWindowDefinition("+TYPE+","+definitions.size()+")";
    }
    
    @Override
    public TimeWindow constructTimeWindow() {
      MultiTimeWindow mtw = new MultiTimeWindow();
      for( TimeWindowDefinition twd : definitions ) {
        mtw.add( twd.constructTimeWindow() );
      }
      return mtw;
    }

    @Override
    public TimeWindowDefinition deserializeFromString(String string) {
      Matcher m = PATTERN_DEFINITION.matcher(string);
      if( ! m.matches() ) {
        throw new IllegalArgumentException("\""+string +"\" does not match pattern \""+P_DEFINITION+"\"");
      }
      int size = Integer.parseInt(m.group(1));
      String[] parts = m.group(2).split("\\),");
      
      ArrayList<TimeWindowDefinition> defs = new ArrayList<TimeWindowDefinition>();
      for( int i=0; i<parts.length; ++i ) {
        String def = parts[i];
        if( i < parts.length-1 ) {
          def = def+")";
        }
        defs.add( TimeWindowDefinition.valueOf(def) );
      }
      if( defs.size() != size ) {
        logger.warn( "Mismatch in number of TimeWindowDefinitions");
        System.err.println("Mismatch in number of TimeWindowDefinitions" );
      }
      MultiTimeWindowDefinition multi = new MultiTimeWindowDefinition();
      multi.definitions = defs;
      return multi;
    }

    @Override
    public String serializeToString() {
      if( serialized == null ) {
        StringBuilder sb = new StringBuilder();
        sb.append("Multi(").append(definitions.size());
        for( TimeWindowDefinition twd : definitions ) {
          sb.append(",").append(twd.serializeToString());
        }
        sb.append(")");
        serialized = sb.toString();
      }
      return serialized;
    }

    @Override
    public String getType() {
      return TYPE;
    }
 
    public int size() {
      return definitions.size();
    }
    
    public TimeWindowDefinition getDefinition(int index) {
      return definitions.get(index);
    }
    
  }

}
