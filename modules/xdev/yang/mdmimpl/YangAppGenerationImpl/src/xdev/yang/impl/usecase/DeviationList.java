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

package xdev.yang.impl.usecase;

import java.util.ArrayList;
import java.util.List;

import org.yangcentral.yangkit.model.api.stmt.Deviation;
import org.yangcentral.yangkit.model.api.stmt.Module;


public class DeviationList {

  private List<Deviation> deviations = new ArrayList<>();
  
  private DeviationList() {}
  
  public DeviationList(List<Deviation> deviations) {
    if (deviations != null) {
      this.deviations.addAll(deviations);
    }
  }
  
  public DeviationList(Module mod) {
    add(mod);
  }
  
  public static DeviationList build(List<Module> modules) {
    DeviationList ret = new DeviationList();
    if (modules == null) { return ret; }
    for (Module mod : modules) {
      ret.add(mod);
    }
    return ret;
  }
  
  
  private void add(Module mod) {
    if (mod != null) {
      add(mod.getDeviations());
    }
  }
  
  private void add(List<Deviation> deviations) {
    if (deviations != null) {
      this.deviations.addAll(deviations);
    }
  }

  public List<Deviation> getDeviations() {
    return deviations;
  }

}
