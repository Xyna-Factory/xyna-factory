/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package com.gip.xyna.xdev.xfractmod.xmdm;



import java.util.IdentityHashMap;
import java.util.Set;

import com.gip.xyna.utils.misc.DataRangeCollection;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils.ObjectVersionBase;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xprcods.orderarchive.XynaExceptionInformation;



public class XynaExceptionContainer implements GeneralXynaObject {

  private static final long serialVersionUID = -1975105422152425658L;

  private Throwable t;


  public XynaExceptionContainer(Throwable t) {
    this.t = t;
  }


  public String toXml() {
    return toXml(null);
  }


  public String toXml(String varName) {
    return toXml(varName, false);
  }


  public String toXml(String varName, boolean onlyContent) {
    return toXml(varName, onlyContent, -1, null);
  }


  public GeneralXynaObject clone() {
    return new XynaExceptionContainer(t);
  }

  public GeneralXynaObject clone(boolean deep) {
    return new XynaExceptionContainer(t);
  }


  public Throwable getException() {
    return t;
  }


  public String toXml(String varName, boolean onlyContent, long version, XMLReferenceCache cache) {
    return XynaExceptionInformation.getEmptyXML(t); //FIXME 
  }


  public boolean supportsObjectVersioning() {
    return false;
  }


  public ObjectVersionBase createObjectVersion(long version, IdentityHashMap<GeneralXynaObject, DataRangeCollection> changeSetsOfMembers) {
    return null;
  }


  public void collectChanges(long start, long end, IdentityHashMap<GeneralXynaObject, DataRangeCollection> changeSetsOfMembers,
                             Set<Long> datapoints) {
    
  }


  public Object get(String path) throws InvalidObjectPathException {
    return null;
  }


  public void set(String name, Object value) throws XDEV_PARAMETER_NAME_NOT_FOUND {
  }
}
