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
 * LongDTO Kapselt einen long
 */
@SuppressWarnings("serial")
public class DoubleDTO extends OBDTO implements OBDTOInterface {

  private double value;
  
  
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
   * @return double-Wert
   */
  public double getDouble() {
    return value;
  }


  /**
   * @param d Zu setzender Wert
   */
  public void setDouble(double d) {
    this.value = d;
  }


  /**
   * Liefert einfach nur den enthaltenen Wert zurueck.
   * 
   * @see gip.base.common.OBObject#toString()
   */
  public String toString() {
    return String.valueOf(value);
  }

  /**
   * Wandelt ein das Objekt in eine HashMap um
   * 
   * @return HashMap
   */
  public HashMap<String, Object> convertToHashMap(String key) {
    HashMap<String, Object> retVal = new HashMap<String, Object>();
    retVal.put(key, String.valueOf(value));
    return retVal;
  }
  

}


