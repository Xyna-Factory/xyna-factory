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

package com.gip.xyna.xprc.xpce.manualinteraction;

import java.io.Serializable;
import java.util.ArrayList;



public class WorkflowStacktrace implements Serializable {

  private static final long serialVersionUID = -348510230066348261L;
  private ArrayList<String> entries;


  public WorkflowStacktrace(String mostRecentTraceElement) {
    entries = new ArrayList<String>();
    entries.add(mostRecentTraceElement);
  }


  public ArrayList<String> getEntries() {
    return entries;
  }


  public String getRootOrderType() {
    if (entries.size() > 0)
      return entries.get(0);
    else
      return null;
  }


  synchronized public void push(String s) {
    if (entries.size() > 0)
      entries.add(s);
    else
      entries.add(s);
  }


  synchronized public String pop() {
    if (entries.size() > 0) {
      return entries.remove(entries.size() - 1);
    }
    else {
      return null;
    }
  }

}
