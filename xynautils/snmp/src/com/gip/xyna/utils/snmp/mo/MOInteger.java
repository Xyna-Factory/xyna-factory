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
package com.gip.xyna.utils.snmp.mo;

import java.util.Map;


public class MOInteger extends MOBase {

  public MOInteger(MO mo) {
    super(mo);
  }

  private int value;

  public int getValue() {
    return value;
  }

  @Override
  public String toString() {
    if( mo.isTypeString() ) {
      return mo.map(value);
    } else {
      return String.valueOf(value);
    }
  }

  /* (non-Javadoc)
   * @see com.huawei.oss.prov.mib.MOBase#getIntValue()
   */
  @Override
  protected int getIntValue() {
    return value;
  }
  /* (non-Javadoc)
   * @see com.huawei.oss.prov.mib.MOBase#setIntValue(int)
   */
  @Override
  protected void setIntValue(int value) {
    this.value=value;
  }

  /* (non-Javadoc)
   * @see com.huawei.oss.prov.mib.MOBase#getStringValue()
   */
  @Override
  protected String getStringValue() {
    return mo.map( value );
  }
  /* (non-Javadoc)
   * @see com.huawei.oss.prov.mib.MOBase#setStringValue(java.lang.String)
   */
  @Override
  protected void setStringValue(String value) {
    this.value=mo.map(value);
  }

  /* (non-Javadoc)
   * @see com.huawei.oss.prov.mib.MOBase#read(java.util.Map, java.lang.String)
   */
  @Override
  public void read(final Map<String,Object> data, final String key) {
    value = mo.readInt(data, key);
  }

}
