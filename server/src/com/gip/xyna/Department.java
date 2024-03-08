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

package com.gip.xyna;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.utils.collections.ArrayRingBuffer;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.XynaFactoryManagementBase;




public abstract class Department extends XynaFactoryComponent {

  private static Logger errorLogger = CentralFactoryLogging.getLogger(Department.class);
  
  protected HashMap<String, Section> sections;

  /**
   * Create a new department with the given name
   */
  public Department() throws XynaException {
    super();
    sections = new HashMap<String, Section>();
    if (logger.isDebugEnabled()) {
      logger.debug("Initializing " + getDefaultName());
    }
    register();
    if (getDependencies(this).isEmpty()) {
      initInternally();
    } else {
      XynaFactory.getInstance().addComponentToBeInitializedLater(this);
    }
  }


  protected Department(String cause) {
    
  }


  private final void register() {
    if (!(this instanceof XynaFactoryManagementBase)) {
      XynaFactory.getInstance().getFactoryManagement().getComponents().register(this);
    }
  }


  protected void shutdown() throws XynaException {
    for (Section s : getSectionsAsList()) {
      try {
        s.shutDownInternally();
      } catch (Throwable t) {
        Department.handleThrowable(t);
        logger.error("Error while shutting down section <" + s.getDefaultName() + "> of department <" + getDefaultName() + ">", t);
      }
    }
  }


  public void deploySection(Section s) {
    if (s != null) {
      sections.put(s.getDefaultName(), s);
      if (logger.isInfoEnabled()) {
        logger.info("Section " + s.getDefaultName() + " added to department " + getDefaultName());
      }
    }
  }


  /**
   * Remove the given section from the department
   */
  public void undeploySection(Section s) {
    if (s == null) {
      return;
    }
    if (sections.remove(s.getDefaultName()) == null) {
      logger.warn("Tried to remove section " + s.getDefaultName() + " from department " + getDefaultName()
                      + " but was not found.");
    } else if (logger.isInfoEnabled()) {
      logger.info("Section " + s.getDefaultName() + " removed from department " + getDefaultName());
    }
  }


  public Section getSection(String name) {
    Section s = sections.get(name);
    if (s == null)
      logger.warn("Tried to access unknown section " + name + " in department " + getDefaultName());
    return s;
  }


  public List<Section> getSectionsAsList() {
    ArrayList<Section> list = new ArrayList<>();
    Iterator<Entry<String, Section>> iter = sections.entrySet().iterator();
    while (iter.hasNext())
      list.add(iter.next().getValue());
    return list;
  }

  private static final ArrayRingBuffer<Long> lastGCs = new ArrayRingBuffer<>(2);
  static {
    lastGCs.add(0L);
  }

  /**
   * throwables sollten nicht einfach gefangen werden, ohne dass man sich bewusst ist, was da alles enthalten ist
   */
  public static void handleThrowable(Throwable t) {
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getOomManagement().handleThrowable(t);
  }


  public static synchronized void checkMassiveExceptionOccurrence(Set<Long> lastThrowableTimes, Throwable t,
                                                                  int max_count) {

    if (t instanceof XynaException)
      return;

    if (lastThrowableTimes == null)
      throw new IllegalArgumentException("Set of last throwable times may not be null");

    if (errorLogger.isDebugEnabled())
      errorLogger.debug("Currently " + lastThrowableTimes.size() + " throwables since last call, removing old entries...");

    long current = System.currentTimeMillis();
    Iterator<Long> iter = lastThrowableTimes.iterator();
    while (iter.hasNext()) {
      if (iter.next() < current - (long) 60000)
        iter.remove();
    }

    if (errorLogger.isDebugEnabled())
      errorLogger.debug("There are now " + lastThrowableTimes.size() + " throwables that have been captured during the last 60 seconds");

    if (lastThrowableTimes.size() > max_count)
      throw new TooManyThrowablesException("Encountering massive exception occurrence, throwing exception", t);

    lastThrowableTimes.add(current);

  }

}
