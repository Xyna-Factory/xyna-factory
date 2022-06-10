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
 * UpdateRowPkWithNullConditionsRequest_ctype.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.SnmpTrap.WS.PoolUsageThreshold.Messages;

public class UpdateRowPkWithNullConditionsRequest_ctype  implements java.io.Serializable {
    private com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader;

    private com.gip.www.juno.SnmpTrap.WS.PoolUsageThreshold.Messages.UpdateRowPkWithNullConditionsInput_ctype updateRowPkWithNullConditionsInput;

    public UpdateRowPkWithNullConditionsRequest_ctype() {
    }

    public UpdateRowPkWithNullConditionsRequest_ctype(
           com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader,
           com.gip.www.juno.SnmpTrap.WS.PoolUsageThreshold.Messages.UpdateRowPkWithNullConditionsInput_ctype updateRowPkWithNullConditionsInput) {
           this.inputHeader = inputHeader;
           this.updateRowPkWithNullConditionsInput = updateRowPkWithNullConditionsInput;
    }


    /**
     * Gets the inputHeader value for this UpdateRowPkWithNullConditionsRequest_ctype.
     * 
     * @return inputHeader
     */
    public com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype getInputHeader() {
        return inputHeader;
    }


    /**
     * Sets the inputHeader value for this UpdateRowPkWithNullConditionsRequest_ctype.
     * 
     * @param inputHeader
     */
    public void setInputHeader(com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype inputHeader) {
        this.inputHeader = inputHeader;
    }


    /**
     * Gets the updateRowPkWithNullConditionsInput value for this UpdateRowPkWithNullConditionsRequest_ctype.
     * 
     * @return updateRowPkWithNullConditionsInput
     */
    public com.gip.www.juno.SnmpTrap.WS.PoolUsageThreshold.Messages.UpdateRowPkWithNullConditionsInput_ctype getUpdateRowPkWithNullConditionsInput() {
        return updateRowPkWithNullConditionsInput;
    }


    /**
     * Sets the updateRowPkWithNullConditionsInput value for this UpdateRowPkWithNullConditionsRequest_ctype.
     * 
     * @param updateRowPkWithNullConditionsInput
     */
    public void setUpdateRowPkWithNullConditionsInput(com.gip.www.juno.SnmpTrap.WS.PoolUsageThreshold.Messages.UpdateRowPkWithNullConditionsInput_ctype updateRowPkWithNullConditionsInput) {
        this.updateRowPkWithNullConditionsInput = updateRowPkWithNullConditionsInput;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof UpdateRowPkWithNullConditionsRequest_ctype)) return false;
        UpdateRowPkWithNullConditionsRequest_ctype other = (UpdateRowPkWithNullConditionsRequest_ctype) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.inputHeader==null && other.getInputHeader()==null) || 
             (this.inputHeader!=null &&
              this.inputHeader.equals(other.getInputHeader()))) &&
            ((this.updateRowPkWithNullConditionsInput==null && other.getUpdateRowPkWithNullConditionsInput()==null) || 
             (this.updateRowPkWithNullConditionsInput!=null &&
              this.updateRowPkWithNullConditionsInput.equals(other.getUpdateRowPkWithNullConditionsInput())));
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
        if (getInputHeader() != null) {
            _hashCode += getInputHeader().hashCode();
        }
        if (getUpdateRowPkWithNullConditionsInput() != null) {
            _hashCode += getUpdateRowPkWithNullConditionsInput().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(UpdateRowPkWithNullConditionsRequest_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/SnmpTrap/WS/PoolUsageThreshold/Messages", "UpdateRowPkWithNullConditionsRequest_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("inputHeader");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/SnmpTrap/WS/PoolUsageThreshold/Messages", "InputHeader"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "InputHeaderContent_ctype"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("updateRowPkWithNullConditionsInput");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/SnmpTrap/WS/PoolUsageThreshold/Messages", "UpdateRowPkWithNullConditionsInput"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/SnmpTrap/WS/PoolUsageThreshold/Messages", "UpdateRowPkWithNullConditionsInput_ctype"));
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
