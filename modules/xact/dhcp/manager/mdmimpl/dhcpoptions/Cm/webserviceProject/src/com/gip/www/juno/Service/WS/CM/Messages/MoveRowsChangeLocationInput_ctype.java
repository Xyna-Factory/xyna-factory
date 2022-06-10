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
/**
 * MoveRowsChangeLocationInput_ctype.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.Service.WS.CM.Messages;

public class MoveRowsChangeLocationInput_ctype  implements java.io.Serializable {
    private java.lang.String newLocation;

    private com.gip.www.juno.Service.WS.CM.Messages.Row_ctype condition;

    public MoveRowsChangeLocationInput_ctype() {
    }

    public MoveRowsChangeLocationInput_ctype(
           java.lang.String newLocation,
           com.gip.www.juno.Service.WS.CM.Messages.Row_ctype condition) {
           this.newLocation = newLocation;
           this.condition = condition;
    }


    /**
     * Gets the newLocation value for this MoveRowsChangeLocationInput_ctype.
     * 
     * @return newLocation
     */
    public java.lang.String getNewLocation() {
        return newLocation;
    }


    /**
     * Sets the newLocation value for this MoveRowsChangeLocationInput_ctype.
     * 
     * @param newLocation
     */
    public void setNewLocation(java.lang.String newLocation) {
        this.newLocation = newLocation;
    }


    /**
     * Gets the condition value for this MoveRowsChangeLocationInput_ctype.
     * 
     * @return condition
     */
    public com.gip.www.juno.Service.WS.CM.Messages.Row_ctype getCondition() {
        return condition;
    }


    /**
     * Sets the condition value for this MoveRowsChangeLocationInput_ctype.
     * 
     * @param condition
     */
    public void setCondition(com.gip.www.juno.Service.WS.CM.Messages.Row_ctype condition) {
        this.condition = condition;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof MoveRowsChangeLocationInput_ctype)) return false;
        MoveRowsChangeLocationInput_ctype other = (MoveRowsChangeLocationInput_ctype) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.newLocation==null && other.getNewLocation()==null) || 
             (this.newLocation!=null &&
              this.newLocation.equals(other.getNewLocation()))) &&
            ((this.condition==null && other.getCondition()==null) || 
             (this.condition!=null &&
              this.condition.equals(other.getCondition())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        if (getNewLocation() != null) {
            _hashCode += getNewLocation().hashCode();
        }
        if (getCondition() != null) {
            _hashCode += getCondition().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(MoveRowsChangeLocationInput_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Service/WS/CM/Messages", "MoveRowsChangeLocationInput_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("newLocation");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Service/WS/CM/Messages", "NewLocation"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("condition");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Service/WS/CM/Messages", "Condition"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Service/WS/CM/Messages", "Row_ctype"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanSerializer(
            _javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}
