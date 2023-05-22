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

package com.gip.xyna;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import com.gip.xyna.utils.exceptions.XynaException;



public abstract class Section extends XynaFactoryComponent {

  protected HashMap<String, FunctionGroup> functionGroups;


  public Section() throws XynaException {
    super();
    functionGroups = new HashMap<String, FunctionGroup>();
    if (getDependencies(this).size() == 0 || !XynaFactory.getInstance().isStartingUp()) { // the !isStartingUp is needed for the RMIChannel-Redeployment
      if (logger.isDebugEnabled()) {                                                      // the addComponentToBeInitializedLater would fail
        logger.debug(new StringBuilder("Initializing Section ").append(getDefaultName()).append(".").toString());
      }
      initInternally();
    } else
      XynaFactory.getInstance().addComponentToBeInitializedLater(this);
  }


  protected void shutdown() throws XynaException {
    for (FunctionGroup fg : getFunctionGroupsAsList()) {
      try {
        fg.shutDownInternally();
      } catch (Throwable t) {
        Department.handleThrowable(t);
        logger.error("Error while shutting down FunctionGroup <" + fg.getDefaultName() + "> of Section <"
                        + getDefaultName() + ">", t);
      }
    }
  }


  public void deployFunctionGroup(FunctionGroup fg) {
    functionGroups.put(fg.getDefaultName(), fg);
    fg.setParentSection(this);
  }


  public void undeployFunctionGroup(FunctionGroup fg) {
    // TODO implement check
    functionGroups.remove(fg.getDefaultName());
    fg.setParentSection(null);
  }


  public FunctionGroup getFunctionGroup(String name) {
    FunctionGroup s = functionGroups.get(name);
    if (s == null)
      logger.warn("Tried to access unknown FunctionGroup " + name + " in Section " + getDefaultName());
    return s;
  }


  public ArrayList<FunctionGroup> getFunctionGroupsAsList() {

    if (functionGroups == null)
      return null;

    ArrayList<FunctionGroup> list = new ArrayList<FunctionGroup>();

    Set<Entry<String, FunctionGroup>> set = functionGroups.entrySet();
    if (set == null)
      return null;

    Iterator<Entry<String, FunctionGroup>> iter = set.iterator();
    while (iter.hasNext())
      list.add(iter.next().getValue());

    return list;

  }


}
