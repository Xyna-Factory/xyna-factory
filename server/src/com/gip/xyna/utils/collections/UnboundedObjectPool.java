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
package com.gip.xyna.utils.collections;



import java.io.Closeable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.Constants;



/**
 * Wiederverwendbare Objekte cachen<p>
 * 
 * Es werden nur Objekte verwaltet, die gerade frei sind, d.h. mit get() herausgegeben können. sobald sie herausgegeben wurden, kann der Verwender
 * mit den Objekten machen was er will.<p>
 * 
 * Objekte im Pool werden aufgeräumt, wenn sie so lange wie der angegebene Timeout nicht verwendet wurden.
 * Dabei wird nicht garantiert, dass die Identität der Objekte berücksichtigt wird, d.h. alle Objekte sind gleichwertig. 
 * Es kann also sein, dass ein Objekt aufgeräumt wird, welches kurze Zeit vorher noch verwendet worden ist.
 * <p>
 * 
 * Intendierte Verwendung:
 * 
 * <pre>
 * CachedObject cached = pool.get();
    if (cached == null) {
      InnerCon con = ...
      cached = new CachedObject(con);
    }
    boolean success = false;
    try {
      cached.doSomething();
      success = true;
    } finally {
      if (!success || !pool.addIfNeeded(cached)) {
        try {
          cached.close();
        } catch (IOException e) {
          _logger.warn("Could not close ...", e);
        }
      } //else success &amp;&amp; addIfNeeded=true
    }
    </pre>
    Das success-Flag ist optional - ist hier nur beispielhaft als Schutz vor kaputt gegangenen Objekten im Cache.
    Alternativ kann man es natürlich auch jedes Mal testen oder sowas
 *
 */
public class UnboundedObjectPool<T extends Closeable> {

  private static final Logger logger = CentralFactoryLogging.getLogger(UnboundedObjectPool.class);

  private final LinkedHashSet<T> objects = new LinkedHashSet<>();
  /*
   * angenommener usecase: addIfNeeded wird aufgerufen, nachdem ein objekt in benutzung war.
   * 
   * 3 objekte in pool:
   * [0] -> vor 15 min benutzt
   * [1] -> vor 10 min benutzt
   * [2] -> vor 5 min benutzt
   * falls timeout nun 18 min ist, wird in 3 min das älteste objekt aufgeräumt, d.h. danach ist der stand dann:
   * [0] -> vor 13 min genutzt
   * [1] -> vor 8 min genutzt
   * 
   * beim get() wird immer der niedrigste eintrag (höchster index) entfernt
   * beim addIfNeeded() wird ein neuer eintrag angelegt (höchster index) mit (vor 0 min genutzt)
   */
  private final List<Long> lastAccess = new ArrayList<>();
  private final int timeoutClose; //wenn ein objekt solange nicht benutzt wurde, wird es aufgeräumt
  private final int minSize; //wenn weniger objekte im pool sind, wird beim addIfNeeded sicher hinzugefügt.
  //objekte beim addIfNeeded ablehnen, falls es ein objekt gibt, was so lange nicht in verwendung ist. muss <= timeoutClose sein
  //idee: wieso neues objekt hinzufügen, wenn es noch so lange nicht verwendete objekte gibt...
  private final int timeoutDenyNew;
  private static final Timer timer = new Timer("UnboundedObjectPool-" + Constants.defaultUTCSimpleDateFormat().format(new Date()), true);
  private TimerTask timertask;
  private boolean closed = false;


  /**
   * 
   * @param timeoutCloseMs wenn ein Objekt so lange nicht in Verwendung war, wird es geclosed
   * @param timeoutDenyNewMs beim add werden Objekte nicht angenommen, wenn es Objekte gibt, die mindestens so lange nicht in Verwendung waren.
   * @param minSize beim add werden Objekte angenommen, falls nicht mindestens soviele Objekte im Pool sind
   */
  public UnboundedObjectPool(int timeoutCloseMs, int timeoutDenyNewMs, int minSize) {
    if (timeoutCloseMs <= 10) {
      throw new RuntimeException("Timeout too low");
    }
    this.timeoutClose = timeoutCloseMs;
    this.timeoutDenyNew = timeoutDenyNewMs;
    this.minSize = minSize;
  }


  /**
   * returns null, falls kein objekt vorhanden
   */
  public T get() {
    synchronized (objects) {
      checkClosed();
      if (objects.isEmpty()) {
        return null;
      }
      lastAccess.remove(lastAccess.size() - 1);
      return removeLast();
    }
  }


  private T removeLast() {
    Iterator<T> it = objects.iterator();
    T t = it.next();
    it.remove();
    return t;
  }


  private class MyTimerTask extends TimerTask {

    @Override
    public void run() {
      try {
        cleanup(false);
      } finally {
        synchronized (objects) {
          addTimerTask();
        }
      }
    }
  }


  /**
   * Fügt Objekt zu Pool hinzu, wenn minSize nicht erreicht ist, oder wenn das älteste Objekt weniger lange als timeoutDenyNew wartet.
   * @return gibt false zurück, falls Objekt nicht zum Pool hinzugefügt worden ist. In diesem Fall ist der Aufrufer für das Objekt zuständig (close aufrufen)
   */
  public boolean addIfNeeded(T t) {
    long time = System.currentTimeMillis();
    synchronized (objects) {
      checkClosed();
      if (objects.contains(t)) {
        throw new IllegalArgumentException("Object already in Pool.");
      }
      int s = objects.size();
      if (s > 0 && s >= minSize && time - lastAccess.get(0) >= timeoutDenyNew) {
        return false;
      }
      objects.add(t);
      lastAccess.add(time);
      if (timertask == null) {
        addTimerTask();
      }
      return true;
    }
  }


  private void addTimerTask() {
    if (objects.size() > 0) {
      timertask = new MyTimerTask();
      long delay = timeoutClose - (System.currentTimeMillis() - lastAccess.get(0));
      if (delay < 10) {
        delay = 10;
      }
      timer.schedule(timertask, delay);
    } else {
      timertask = null;
    }
  }


  private void checkClosed() {
    if (closed) {
      throw new RuntimeException("Pool is closed");
    }
  }


  private void cleanup(boolean force) {
    List<T> removed = new ArrayList<T>();
    long timeoutTS = System.currentTimeMillis() - timeoutClose;
    synchronized (objects) {
      while (lastAccess.size() > 0 && (force || lastAccess.get(0) <= timeoutTS)) {
        removed.add(removeLast());
        lastAccess.remove(0);
      }
    }
    for (T t : removed) {
      try {
        t.close();
      } catch (Throwable e) {
        logger.warn("Could not close " + t + ".", e);
      }
    }
  }


  public int size() {
    synchronized (objects) {
      checkClosed();
      return objects.size();
    }
  }


  public boolean isClosed() {
    synchronized (objects) {
      return closed;
    }
  }


  /**
   * räumt alle objekte im pool auf
   */
  public void close() {
    synchronized (objects) {
      checkClosed();
      if (timertask != null) {
        timertask.cancel();
      }
      cleanup(true);
      closed = true;
    }
  }

}