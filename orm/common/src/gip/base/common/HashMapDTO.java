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
package gip.base.common;

import java.util.HashMap;



/**
 * HashMapDTO kapselt eine HashMap,
 * die enthaltene HashMap kann nie 'null' sein.
 * 
 * Bitte dabei beachten, was man in die HashMap reinsteckt,
 * da diese ja serialisiert wird.
 */
@SuppressWarnings("serial")
public class HashMapDTO extends OBDTO implements OBDTOInterface {

  
  @SuppressWarnings("rawtypes")
  private HashMap _hm = new HashMap();

  
  /**
   * @see gip.base.common.OBDTOInterface#getCapabilityId()
   */
  public long getCapabilityId() {
    return OBAttribute.NULL;
  }


  /**
   * @see gip.base.common.OBDTOInterface#setCapabilityId(long)
   */
  public void setCapabilityId(long _capabilityId) throws OBException {
    throw new OBException(OBException.OBErrorNumber.capabilityUnknown);
  }


  /**
   * @return HashMap-Wert
   */
  @SuppressWarnings("rawtypes")
  public HashMap getHashMap() {
    return _hm;
  }


  /**
   * Setzt den Wert des HashMapDTOs, 
   * dabei wird ein 'null' auf eine leere HashMap geaendert.
   * @param hm HashMap
   */
  @SuppressWarnings("rawtypes")
  public void setHashMap(HashMap hm) {
    if (hm==null) {
      _hm = new HashMap();
    }
    else {
      _hm = hm;
    }
  }


  /**
   * Liefert einfach nur ein HashMap.toString zurueck.
   * @see gip.base.common.OBObject#toString()
   */
  public String toString() {
    return _hm.toString();
  }

  
  /**
   * Liefert die Anzahl der enthaltenen Werte.
   * @return Anzahl der Elemente in der HashMap
   */
  public int size() {
    if (_hm==null) {
      return 0;
    }
    return _hm.size();
  }

  
  /**
   * Liefert zurueck, ob das HashMapDTO leer ist.
   * @see gip.base.common.OBObject#isEmpty()
   */
  public boolean isEmpty() {
    return size()==0;
  }

  /**
   * Wandelt ein das Objekt in eine HashMap um
   * 
   * @return HashMap
   */
  public HashMap<String, Object> convertToHashMap(String key) {
    HashMap<String, Object> retVal = new HashMap<String, Object>();
    retVal.put(key, _hm);
    return retVal;
  }
  
}


