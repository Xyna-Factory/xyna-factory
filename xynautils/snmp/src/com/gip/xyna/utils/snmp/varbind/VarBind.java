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
package com.gip.xyna.utils.snmp.varbind;


/**
 * Variable binding base class.
 *
 */
public abstract class VarBind {

  // FIXME es sollte klar gestellt werden, dass hier das format mit fuehrendem punkt notwendig ist oder dass das format egal ist.
  //evtl besser als OID speichern?
  private final String objectIdentifier;

  // Constructor is package private since instances should not be created outside of this package.
  VarBind(final String objectIdentifier) {
    if (objectIdentifier == null) {
      throw new IllegalArgumentException("Object identifier may not be null.");
    }
    this.objectIdentifier = objectIdentifier;
  }

  /**
   * Gets object identifier.
   * @return object identifier.
   */
  public final String getObjectIdentifier() {
    return objectIdentifier;
  }

  /**
   * Gets value.
   * @return value.
   */
  public abstract Object getValue();

  // This method is not made final, like the two following, since StringVarBind needs to override it.
  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(this.objectIdentifier);
    sb.append(" := ");
    sb.append(this.getValue());
    return sb.toString();
  }

  @Override
  public final int hashCode() {
    final int prime = 31;
    Object value = getValue();
    int result = 1;
    result = prime * result + ((objectIdentifier == null) ? 0 : objectIdentifier.hashCode());
    result = prime * result + ((value == null) ? 0 : value.hashCode());
    return result;
  }

  @Override
  public final boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    VarBind other = (VarBind) obj;
    if (objectIdentifier == null) {
      if (other.objectIdentifier != null) {
        return false;
      }
    } else if (!objectIdentifier.equals(other.objectIdentifier)) {
      return false;
    }
    Object value = this.getValue();
    if (value == null) {
      if (other.getValue() != null) {
        return false;
      }
    } else if (!value.equals(other.getValue())) {
      return false;
    }
    return true;
  }

  /**
   * Creates a new VarBind of type depending on value. Note that any null value will be mapped to NullVarBind.
   * @param oid object identifier.
   * @param value value that may be null, String or Integer.
   * @return new VarBind.
   * @throws IllegalArgumentException if object identifier is null or value is not null, String or Integer. 
   */
  public static VarBind newVarBind(final String objectIdentifier, final Object value) {
    if (value instanceof String) {
      return new StringVarBind(objectIdentifier, (String) value);
    }
    if (value instanceof Integer) {
      return new IntegerVarBind(objectIdentifier, (Integer) value);
    }
    if (value == null) {
      return new NullVarBind(objectIdentifier);
    }
    throw new IllegalArgumentException("Value ist neither String, Integer nor null: <" + value + ">.");
  }

  /**
   * Converts this VarBind to type of given type converter.
   * @param typeConverter type converter.
   * @return V created by type converter.
   */
  public abstract <V> V convert( VarBindTypeConverter<V>  converter );


}
