/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package com.gip.xyna.xact.filter.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.CallStatistics;
import com.gip.xyna.xact.filter.CallStatistics.StatisticsEntry;
import com.gip.xyna.xact.filter.FilterAction;
import com.gip.xyna.xact.filter.HTMLBuilder;
import com.gip.xyna.xact.filter.HTMLBuilder.FormBuilder;
import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.filter.HTMLBuilder.TableBuilder;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xprc.xsched.scheduling.FilteredCapacityReservation.TimestampFormat;

/**
 *
 */
public class StatisticsAction implements FilterAction {

  public static final String REQUEST_NAME = "requestName";
  public static final String ORDER_ID = "orderId";
  public static final String FAILURE = "failure";

  private static TimestampFormat timestampFormatter = new TimestampFormat("yyyy-MM-dd HH:mm:ss");
  
  private String applicationVersion;
  private String name;
  private CallStatistics callStatistics;
  private long startTS;

  public StatisticsAction(String applicationVersion, String name, CallStatistics callStatistics) {
    this.applicationVersion = applicationVersion;
    this.name = name;
    this.callStatistics = callStatistics;
    this.startTS = System.currentTimeMillis();
  }

  public boolean match(URLPath url, Method method) {
    return url.getPath().startsWith("/statistics") 
        && Method.GET == method;
  }

  public String getTitle() {
    return "Statistics";
  }

  public FilterActionInstance act(URLPath url, HTTPTriggerConnection tc) throws XynaException {
    
    //, StatisticsEntry statisticsEntry
    DefaultFilterActionInstance dfai = new DefaultFilterActionInstance();
    StatisticsType type = null;
    try {
      type = StatisticsType.valueOf( (String)tc.getParameters().get("type").get(0) );
    } catch( Exception e ) {
      type = StatisticsType.all;
    }
    dfai.sendHtml(tc, createStatisticsHtml(type));
    return dfai;
  }

  private enum StatisticsType {
    orders("requests with xyna orders", "Order statistics") {
      public List<StatisticsEntry> getStatisticsEntries(CallStatistics callStatistics) {
        return copyWithRetries(3, callStatistics.getOrderStatisticsEntries());
      }
      public int getCounter(CallStatistics callStatistics) {
        return callStatistics.getOrderCounter();
      }
      public void setHeader(TableBuilder tb) {
        tb.header("", "Timestamp", "Request", "OrderId", "Failed");
      }
      public void fillStatisticsTableEntry(TableBuilder tb, int idx, StatisticsEntry se) {
        tb.row(
            String.valueOf(idx),
            timestampFormatter.toString(se.getTimestamp()),
            se.getAdditional(REQUEST_NAME),
            se.getAdditional(ORDER_ID),
            se.getAdditional(FAILURE)
            );
      }

    },
    all("all requests", "All requests statistics" ) {
      public List<StatisticsEntry> getStatisticsEntries(CallStatistics callStatistics) {
        return copyWithRetries(3, callStatistics.getAllStatisticsEntries());
      }
      public int getCounter(CallStatistics callStatistics) {
        return callStatistics.getAllCounter();
      }
      public void setHeader(TableBuilder tb) {
        tb.header("", "Timestamp","Method", "URI", "Caller", "Status");
      }
      public void fillStatisticsTableEntry(TableBuilder tb, int idx, StatisticsEntry se) {
        tb.row(
            String.valueOf(idx),
            timestampFormatter.toString(se.getTimestamp()),
            String.valueOf(se.getMethod()),
            se.getUri(),
            se.getCaller(),
            se.getStatus()
            );
      }

    };

    private String description;
    private String heading;

    private StatisticsType(String description, String heading) {
      this.description = description;
      this.heading = heading;
    }
    
    public String getDescription() {
      return description;
    }

    public abstract List<StatisticsEntry> getStatisticsEntries(CallStatistics callStatistics);
    public abstract int getCounter(CallStatistics callStatistics);
    public abstract void setHeader(TableBuilder tb);
    public abstract void fillStatisticsTableEntry(TableBuilder tb, int c, StatisticsEntry se);
    
    public String getHeading() {
      return heading;
    }

  }
    

  public void appendIndexPage(HTMLPart body) {
    FormBuilder fb = null;
    for( StatisticsType type : StatisticsType.values() ) {
      fb = body.form("statistics").method("get").action("statistics");
      fb.hidden("type", type.name() );
      fb.submit(type.getDescription());
    }
  }

  public boolean hasIndexPageChanged() {
    return false;
  }

  private String createStatisticsHtml(StatisticsType type) {
    HTMLBuilder html = new HTMLBuilder(name+" Statistics");
    html.head().css("gipstyle.css");
    
    HTMLPart body = html.body();
    body.heading(1, type.getHeading()+" for "+name );
    body.heading(2, name +" in " + applicationVersion);
    body.paragraph().append("started on "+timestampFormatter.toString(startTS)).lineBreak();
    body.paragraph().append(type.getCounter(callStatistics) ).append(" requests so far");
    
    body.heading(2, " last calls");
    
    TableBuilder tb = body.table();
    type.setHeader(tb);
    List<StatisticsEntry> ses = type.getStatisticsEntries(callStatistics); 
    Collections.sort(ses, new StatisticsEntryComparator() );
    int c = 1;
    for( StatisticsEntry se : ses ) {
      type.fillStatisticsTableEntry(tb, c, se);
      ++c;
    }
    
    return html.toHTML();
  }


  private static List<StatisticsEntry> copyWithRetries(int retry,
      Collection<StatisticsEntry> statisticsEntries) {
    try {
      return new ArrayList<StatisticsEntry>(statisticsEntries);
    } catch (ConcurrentModificationException cme ) {
      if( retry > 0 ) {
        return copyWithRetries(retry-1, statisticsEntries);
      } else {
        throw cme;
      }
    }
  }

  
  private class StatisticsEntryComparator implements Comparator<StatisticsEntry> {

    public int compare(StatisticsEntry o1, StatisticsEntry o2) {
      return (int)(o2.getTimestamp() - o1.getTimestamp()); //rückwärts
    }
    
  }
  

}
