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
package com.gip.xyna.xprc.xfractwfe.generation.restriction;

import java.util.IdentityHashMap;
import java.util.Set;

import com.gip.xyna.utils.misc.DataRangeCollection;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;


public abstract class UnsupportedGeneralXynaObject implements GeneralXynaObject {

  private static final long serialVersionUID = 8696095667285482706L;


  @Override
  public String toXml() {
    throw new UnsupportedOperationException();
  }


  @Override
  public String toXml(String varName) {
    throw new UnsupportedOperationException();
  }


  @Override
  public String toXml(String varName, boolean onlyContent) {
    throw new UnsupportedOperationException();
  }


  @Override
  public String toXml(String varName, boolean onlyContent, long version, XMLReferenceCache cache) {
    throw new UnsupportedOperationException();
  }


  @Override
  public GeneralXynaObject clone() {
    throw new UnsupportedOperationException();
  }


  @Override
  public GeneralXynaObject clone(boolean deep) {
    throw new UnsupportedOperationException();
  }


  @Override
  public boolean supportsObjectVersioning() {
    throw new UnsupportedOperationException();
  }


  @Override
  public ObjectVersionBase createObjectVersion(long version,
                                               IdentityHashMap<GeneralXynaObject, DataRangeCollection> changeSetsOfMembers) {
    throw new UnsupportedOperationException();
  }


  @Override
  public void collectChanges(long start, long end,
                             IdentityHashMap<GeneralXynaObject, DataRangeCollection> changeSetsOfMembers,
                             Set<Long> datapoints) {
    throw new UnsupportedOperationException();
  }


  @Override
  public Object get(String path) throws InvalidObjectPathException {
    throw new UnsupportedOperationException();
  }


  @Override
  public void set(String name, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND {
    throw new UnsupportedOperationException();
  }

}
