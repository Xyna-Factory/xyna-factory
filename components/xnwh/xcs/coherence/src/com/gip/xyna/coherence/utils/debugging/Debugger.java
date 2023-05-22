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
package com.gip.xyna.coherence.utils.debugging;



import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.NDC;

import com.gip.xyna.coherence.utils.logging.LoggerFactory;
import com.gip.xyna.xact.rmi.GenericRMIAdapter;



public final class Debugger {
  //TODO benutzerdefiniertes filtern von logmeldungen.
  //variante1: beim speichern im ringbuffer
  //variante2: beim ausgeben
  //TODO konfigurierbarkeit

  private static String[] prefix;
  private static String[] suffix;
  private final static int prefixSize = 1000;

  private Logmessage[] buffer = new Logmessage[10000];
  private int currentPosition = 0;

  private static Debugger instance = new Debugger();
  private static final boolean enabled = true;
  private static final boolean printNDC = true; //ndc an logmeldungen anh�ngen ist unabh�ngig von ndc-erzeugung in cachecontrollerimpl
  private static final boolean printTimeStamps = false;
  private static final boolean printContext = false;
  private static final boolean forwardToLog4j = false;
  
  private String context;

  static {
    GenericRMIAdapter.setLogger(LoggerFactory.getLogger(GenericRMIAdapter.class)); //TODO ist das hier der richtige Ort?
    
    prefix = new String[prefixSize];
    suffix = new String[prefixSize];
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < prefixSize; i++) {
      prefix[i] = sb.toString();
      suffix[prefixSize - i - 1] = sb.toString();
      sb.append(";");
    }
  }


  public class Logmessage {

    private String threadid;
    private String ndc;
    private Object message;
    private long timeStamp;
    private int idx;
    private int duplicateCount = 1;


    public Logmessage(String threadid, String ndc, Object message) {
      this.threadid = threadid;
      this.ndc = ndc;
      this.message = message;
      if (printTimeStamps) {
        timeStamp = System.currentTimeMillis();
      }
    }
    
    public int hashCode() {
      int hc = 65489121;
      if (threadid != null) {
        hc += threadid.hashCode()*177;
      }
      if (ndc != null) {
        hc += ndc.hashCode() * 9422;
      }
      if (message != null) {
        hc += message.hashCode() * 52111;
      }
      return hc;
    }


    public boolean equals(Object otherObj) {
      if (!(otherObj instanceof Logmessage)) {
        return false;
      }
      Logmessage otherMsg = (Logmessage) otherObj;
      if (otherMsg == null) {
        return false;
      }
      if (otherMsg == this) {
        return true;
      }
      boolean sameThreadId = otherMsg.threadid.equals(threadid);
      boolean sameNdc = true;
      if (ndc == null) {
        if (otherMsg.ndc != null) {
          return false;
        }
      } else {
        sameNdc = ndc.equals(otherMsg.ndc);
      }
      boolean sameMessage = true;
      if (message == null) {
        if (otherMsg.message != null) {
          return false;
        }
      } else {
        sameMessage = message.equals(otherMsg.message);
      }
      return sameThreadId && sameNdc && sameMessage;
    }


    public String createCSVLine(List<String> threadidsAsList) {
      StringBuilder sb = new StringBuilder();
      sb.append(prefix[idx]);
      if (printNDC) {
        sb.append("[").append(ndc == null ? "" : ndc).append("] ");
      }
      if (printContext) {
        sb.append("[").append(context == null ? "" : context).append("] ");
      }
      if (duplicateCount > 1) {
        sb.append(duplicateCount).append(" x ");
      }
      sb.append(message).append(suffix[prefixSize - threadidsAsList.size() + idx]).append("\n");
      return sb.toString();
    }
    
    public String createNormalLine() {
      StringBuilder sb = new StringBuilder();
      sb.append(threadid);
      if (printNDC) {
        sb.append("[").append(ndc == null ? "" : ndc).append("] ");
      }
      if (printContext) {
        sb.append("[").append(context == null ? "" : context).append("] ");
      }
      if (duplicateCount > 1) {
        sb.append(duplicateCount).append(" x ");
      }
      sb.append(message).append("\n");
      return sb.toString();
    }
  }


  private Debugger() {

  }
  
  public interface LogFilter {
    /**
     * gibt true zur�ck, falls msg geloggt werden soll und false falls nicht.
     */
    public boolean checkMessage(Logmessage msg);
  }
  
  private List<LogFilter> filters = new ArrayList<LogFilter>();

  public void addFilter(LogFilter filter) {
    filters.add(filter);
  }

  public static Debugger getDebugger() {
    return instance;
  }
  
  public boolean isEnabled() {
    return enabled;
  }


  public synchronized void clear() {
    buffer = new Logmessage[buffer.length];
    currentPosition = 0;
  }

  
  private synchronized void addEntry(Logmessage s) {
    buffer[currentPosition] = s;
    if (++currentPosition >= buffer.length) {
      currentPosition = 0;
    }
  }

  private static final Logger internalLogger = LoggerFactory.getLogger(Debugger.class);
  private static final String classname = Debugger.class.getName();
  
  public void debug(Object message) {
    if (enabled) {
      if (message != null) {
        Thread th = Thread.currentThread();
        addEntry(new Logmessage(th.getName(), printNDC ? NDC.get() : null, message));
        if (forwardToLog4j) {
          internalLogger.log(classname, Level.TRACE, message, null);
        }
      }
    }
  }
  
  public void writeCSVToStream(OutputStream os) throws IOException {
    OutputStreamWriter osw = new OutputStreamWriter(os);
    BufferedWriter bw = new BufferedWriter(osw);
    //synchronized copy der objekte erstellen, die geloggt werden sollen, damit weiter geloggt werden kann.
    //damit I/O nicht weiteres logging verhindert
    Logmessage[] copyOfBuffer;
    int copyOfCurrentPosition;
    synchronized (this) {
      copyOfBuffer = buffer;
      copyOfCurrentPosition = currentPosition;
      clear();
    }
    

    Set<String> threadids;
    List<String> threadidsAsList;
    
    threadids = new HashSet<String>();
    for (Logmessage msg : copyOfBuffer) {
      if (msg != null) {
        threadids.add(msg.threadid);
      }
    }

    threadidsAsList = new ArrayList<String>(threadids);

    Logmessage lastMsg = null;
    for (int i = 0; i < copyOfBuffer.length; i++) {
      Logmessage msg = copyOfBuffer[i];
      if (msg != null) {
        String msgString = (String) msg.message.toString();
       /* if (msgString.startsWith("\n-------- ")) {
          buffer[i] = null;
        } else {*/
          if (lastMsg != null) {
            if (msg.equals(lastMsg)) {
              msg.duplicateCount++;
              copyOfBuffer[i] = null;
            }
          }
          lastMsg = msg;
      //  }
      }
    }

    for (Logmessage msg : copyOfBuffer) {
      if (msg != null) {
        int idx = threadidsAsList.indexOf(msg.threadid);
        if (idx > -1) {
          msg.idx = idx;
        } else {
          throw new RuntimeException("didnt find thread " + msg.threadid);
        }
      }
    }

    for (String header : threadidsAsList) {
      bw.append(header).append(";");
    }
    bw.append("\n\n");

    for (int i = copyOfCurrentPosition; i < copyOfBuffer.length; i++) {
      if (copyOfBuffer[i] != null) {
        bw.append(copyOfBuffer[i].createCSVLine(threadidsAsList));
      }
    }
    for (int i = 0; i < copyOfCurrentPosition; i++) {
      if (copyOfBuffer[i] != null) {
        bw.append(copyOfBuffer[i].createCSVLine(threadidsAsList));
      }
    }
    bw.flush();
  }

  public void setContext(Object context) {
    this.context = context.toString();
  }

}
