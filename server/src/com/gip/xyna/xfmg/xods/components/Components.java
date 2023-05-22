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
package com.gip.xyna.xfmg.xods.components;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import com.gip.xyna.Department;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;



public class Components extends FunctionGroup {

  private HashMap<String, Department> departments;
  private HashMap<String, XynaProcess> processes;

  public static final String DEFAULT_NAME = "Components";


  public Components() throws XynaException {
    super();
  }


  public void register(Department d) {
    departments.put(d.getDefaultName(), d);
    if (logger.isDebugEnabled()) {
      logger.debug("Department " + d.getDefaultName() + " registered.");
    }
  }


  public void unregister(Department d) {
    if (departments.remove(d.getDefaultName()) != null) {
      if (logger.isInfoEnabled()) {
        logger.info("tried to unregister " + Department.class.getSimpleName() + " " + d.getDefaultName()
                        + " but was not registered.");
      }
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("unregistered " + XynaProcess.class + " " + d.getClass().getName());
      }
    }
  }


  public void register(XynaProcess xp) {
    processes.put(xp.getClass().getName(), xp);
    logger.debug("XynaProcess " + xp.getClass().getName() + " registered.");
  }


  public void unregister(XynaProcess xp) {
    if (processes.remove(xp.getClass().getName()) != null) {
      if (logger.isInfoEnabled()) {
        logger.info("tried to unregister " + XynaProcess.class.getSimpleName() + " " + xp.getClass().getName()
                        + " but was not registered.");
      }
    } else {
      if (logger.isDebugEnabled()) {
        logger.debug("unregistered " + XynaProcess.class + " " + xp.getClass().getName());
      }
    }
  }


  public void init() throws XynaException {
    departments = new HashMap<String, Department>();
    processes = new HashMap<String, XynaProcess>();
  }


  public void shutdown() throws XynaException {
  }


  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  public Department getDepartment(String name) {
    Department d = departments.get(name);
    if (d == null) {
      logger.warn("Tried to access unknown department " + name);
    }
    return d;
  }


  public ArrayList<Department> getDepartmentsAsList() {
    ArrayList<Department> list = new ArrayList<Department>();
    Iterator<Entry<String, Department>> iter = departments.entrySet().iterator();
    while (iter.hasNext()) {
      list.add(iter.next().getValue());
    }
    return list;
  }

}
