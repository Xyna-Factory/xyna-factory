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
package gip.base.common;

import java.util.HashMap;



/**
 * StringDTO kapselt einen String, 
 * der enthaltene String kann nie 'null' sein.
 */
@SuppressWarnings("serial")
public class StringDTO extends OBDTO implements OBDTOInterface {

  
  private String _string="";//$NON-NLS-1$

  
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
   * @return String-Wert
   */
  public String getString() {
    return _string;
  }


  /**
   * Setzt den Wert des StringDTOs, 
   * dabei wird ein 'null' auf einen Leerstring geaendert.
   * @param str zu setztender Wert
   */
  public void setString(String str) {
    if (str==null) {
      _string = "";//$NON-NLS-1$
    }
    else {
      _string = str;
    }
  }


  /**
   * Liefert einfach nur den enthaltenen Wert zurueck.
   * @see gip.base.common.OBObject#toString()
   */
  public String toString() {
    return _string;
  }

  
  /**
   * Liefert die Länge des enthaltenen Wertes.
   * @return String-Laenge
   */
  public int length() {
    if (_string==null) {
      return 0;
    }
    return _string.length();
  }

  
  /**
   * Liefert zurueck, ob das StringDTO leer ist.
   * @see gip.base.common.OBObject#isEmpty()
   */
  public boolean isEmpty() {
    return length()==0;
  }
  
  /**
   * Wandelt ein das Objekt in eine HashMap um
   * 
   * @return HashMap
   */
  public HashMap<String, Object> convertToHashMap(String key) {
    HashMap<String, Object> retVal = new HashMap<String, Object>();
    retVal.put(key, _string);
    return retVal;
  }
}


