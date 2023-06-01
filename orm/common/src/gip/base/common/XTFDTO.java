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

import java.util.Vector;


/**
 * OBDTO
 */
@SuppressWarnings("serial")
public class XTFDTO extends OBObject implements OBDTOInterface {

  protected String formName = "";//$NON-NLS-1$
  protected String objectName = "";//$NON-NLS-1$
  protected String correlationId = "";//$NON-NLS-1$
  protected Vector<String> keys = null;
  protected Vector<String> values = null;

  /**
   * Standard-Konstruktor 
   */
  public XTFDTO() {
    super();
    keys = new Vector<String>();
    values = new Vector<String>();
  }

  /**
   * @see gip.base.common.OBDTOInterface#getCapabilityId()
   */
  public long getCapabilityId() throws OBException {
    return 0;
  }

  /**
   * @see gip.base.common.OBDTOInterface#setCapabilityId(long)
   */
  public void setCapabilityId(long _capabilityId) throws OBException {
    // ntbd
  }

  
  /**
   * @return Returns the formName.
   */
  public String getFormName() {
    return formName;
  }

  
  /**
   * @param formName The formName to set.
   */
  public void setFormName(String formName) {
    this.formName = formName;
  }

  
  /**
   * @return Returns the keys.
   */
  public Vector<String> getKeys() {
    return keys;
  }

  
  /**
   * @param keys The keys to set.
   */
  public void setKeys(Vector<String> keys) {
    this.keys = keys;
  }

  
  /**
   * @return Returns the values.
   */
  public Vector<String> getValues() {
    return values;
  }

  
  /**
   * @param values The values to set.
   */
  public void setValues(Vector<String> values) {
    this.values = values;
  }

  
  /**
   * @return Returns the correlationId.
   */
  public String getCorrelationId() {
    return correlationId;
  }

  
  /**
   * @param correlationId The correlationId to set.
   */
  public void setCorrelationId(String correlationId) {
    this.correlationId = correlationId;
  }
  
  public String getValueForKey(String key) {
    int keyPos = keys.indexOf(key);
    if (keyPos < 0) {
      return null;
    }
    return values.get(keyPos);
  }

  
  /**
   * @return Returns the objectName.
   */
  public String getObjectName() {
    return objectName;
  }

  
  /**
   * @param objectName The objectName to set.
   */
  public void setObjectName(String objectName) {
    this.objectName = objectName;
  }

}
