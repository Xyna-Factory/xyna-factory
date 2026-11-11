/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 */

package com.gip.xyna.xfmg.xfctrl.classloading;

import java.util.IdentityHashMap;
import java.util.Set;

import com.gip.xyna.utils.misc.DataRangeCollection;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.DeploymentTask;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;


public class MDMClassLoaderTestObject extends XynaObject implements DeploymentTask {

  private static final long serialVersionUID = 1L;
  private static boolean deployed = false;

  @Override
  public XynaObject clone() {
    return null;
  }

  public Object get(String path) throws InvalidObjectPathException {
    return null;
  }

  public void set(String name, Object value) {
  }

  @Override
  public String toXml(String varName, boolean onlyContent) {
    return null;
  }

  public void onDeployment() {
    if (deployed) {
      throw new IllegalStateException("Fixture is already deployed");
    }
    deployed = true;
  }

  public void onUndeployment() {
    if (!deployed) {
      throw new IllegalStateException("Fixture is not deployed");
    }
    deployed = false;
  }

  public String toXml(String varName, boolean onlyContent, long version, XMLReferenceCache cache) {
    return null;
  }

  public boolean supportsObjectVersioning() {
    return false;
  }

  public void collectChanges(long start, long end, IdentityHashMap<GeneralXynaObject, DataRangeCollection> changeSetsOfMembers,
                             Set<Long> datapoints) {
  }
}
