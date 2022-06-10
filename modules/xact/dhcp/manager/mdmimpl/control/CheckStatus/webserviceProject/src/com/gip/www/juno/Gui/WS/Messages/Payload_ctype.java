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
 * Payload_ctype.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.Gui.WS.Messages;

public class Payload_ctype  implements java.io.Serializable {
    private com.gip.www.juno.Gui.WS.Messages.InstanceInfo_ctype[] getInstanceInfoListResponseOutput;

    private com.gip.www.juno.Gui.WS.Messages.StatusElement_ctype[] checkStatusForIpResponseOutput;

    public Payload_ctype() {
    }

    public Payload_ctype(
           com.gip.www.juno.Gui.WS.Messages.InstanceInfo_ctype[] getInstanceInfoListResponseOutput,
           com.gip.www.juno.Gui.WS.Messages.StatusElement_ctype[] checkStatusForIpResponseOutput) {
           this.getInstanceInfoListResponseOutput = getInstanceInfoListResponseOutput;
           this.checkStatusForIpResponseOutput = checkStatusForIpResponseOutput;
    }


    /**
     * Gets the getInstanceInfoListResponseOutput value for this Payload_ctype.
     * 
     * @return getInstanceInfoListResponseOutput
     */
    public com.gip.www.juno.Gui.WS.Messages.InstanceInfo_ctype[] getGetInstanceInfoListResponseOutput() {
        return getInstanceInfoListResponseOutput;
    }


    /**
     * Sets the getInstanceInfoListResponseOutput value for this Payload_ctype.
     * 
     * @param getInstanceInfoListResponseOutput
     */
    public void setGetInstanceInfoListResponseOutput(com.gip.www.juno.Gui.WS.Messages.InstanceInfo_ctype[] getInstanceInfoListResponseOutput) {
        this.getInstanceInfoListResponseOutput = getInstanceInfoListResponseOutput;
    }


    /**
     * Gets the checkStatusForIpResponseOutput value for this Payload_ctype.
     * 
     * @return checkStatusForIpResponseOutput
     */
    public com.gip.www.juno.Gui.WS.Messages.StatusElement_ctype[] getCheckStatusForIpResponseOutput() {
        return checkStatusForIpResponseOutput;
    }


    /**
     * Sets the checkStatusForIpResponseOutput value for this Payload_ctype.
     * 
     * @param checkStatusForIpResponseOutput
     */
    public void setCheckStatusForIpResponseOutput(com.gip.www.juno.Gui.WS.Messages.StatusElement_ctype[] checkStatusForIpResponseOutput) {
        this.checkStatusForIpResponseOutput = checkStatusForIpResponseOutput;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof Payload_ctype)) return false;
        Payload_ctype other = (Payload_ctype) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.getInstanceInfoListResponseOutput==null && other.getGetInstanceInfoListResponseOutput()==null) || 
             (this.getInstanceInfoListResponseOutput!=null &&
              java.util.Arrays.equals(this.getInstanceInfoListResponseOutput, other.getGetInstanceInfoListResponseOutput()))) &&
            ((this.checkStatusForIpResponseOutput==null && other.getCheckStatusForIpResponseOutput()==null) || 
             (this.checkStatusForIpResponseOutput!=null &&
              java.util.Arrays.equals(this.checkStatusForIpResponseOutput, other.getCheckStatusForIpResponseOutput())));
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
        if (getGetInstanceInfoListResponseOutput() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getGetInstanceInfoListResponseOutput());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getGetInstanceInfoListResponseOutput(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getCheckStatusForIpResponseOutput() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getCheckStatusForIpResponseOutput());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getCheckStatusForIpResponseOutput(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Payload_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Payload_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("getInstanceInfoListResponseOutput");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GetInstanceInfoListResponseOutput"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "InstanceInfo_ctype"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        elemField.setItemQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "InstanceInfo"));
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("checkStatusForIpResponseOutput");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CheckStatusForIpResponseOutput"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "StatusElement_ctype"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        elemField.setItemQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "StatusElement"));
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
