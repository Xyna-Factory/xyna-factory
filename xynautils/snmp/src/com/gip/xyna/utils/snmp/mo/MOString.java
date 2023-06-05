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
package com.gip.xyna.utils.snmp.mo;

import java.util.Map;


public class MOString extends MOBase {

  private String value;

  public MOString(MO mo) {
    super(mo);
  }

  @Override
  public String toString() {
    return value;
  }

  public int map() {
    return mo.map(value);
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.mo.MOBase#getIntValue()
   */
  @Override
  protected int getIntValue() {
    return mo.map(value);
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.mo.MOBase#setIntValue(int)
   */
  @Override
  protected void setIntValue(int value) {
    this.value=mo.map(value);
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.mo.MOBase#getStringValue()
   */
  @Override
  protected String getStringValue() {
    return value;
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.mo.MOBase#setStringValue(java.lang.String)
   */
  @Override
  protected void setStringValue(String value) {
    this.value=value;
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.snmp.mo.MOBase#read(java.util.Map, java.lang.String)
   */
  @Override
  public void read(final Map<String,Object> data, final String key) {
    value = mo.readString(data, key);
  }

}
