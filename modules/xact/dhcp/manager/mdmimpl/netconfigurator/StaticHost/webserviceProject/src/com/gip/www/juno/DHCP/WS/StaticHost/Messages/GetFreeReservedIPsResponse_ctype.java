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
 * GetFreeReservedIPsResponse_ctype.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.DHCP.WS.StaticHost.Messages;

public class GetFreeReservedIPsResponse_ctype  implements java.io.Serializable {
    private com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader;

    private java.lang.String[] getFreeReservedIPsOutput;

    public GetFreeReservedIPsResponse_ctype() {
    }

    public GetFreeReservedIPsResponse_ctype(
           com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader,
           java.lang.String[] getFreeReservedIPsOutput) {
           this.responseHeader = responseHeader;
           this.getFreeReservedIPsOutput = getFreeReservedIPsOutput;
    }


    /**
     * Gets the responseHeader value for this GetFreeReservedIPsResponse_ctype.
     * 
     * @return responseHeader
     */
    public com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype getResponseHeader() {
        return responseHeader;
    }


    /**
     * Sets the responseHeader value for this GetFreeReservedIPsResponse_ctype.
     * 
     * @param responseHeader
     */
    public void setResponseHeader(com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader) {
        this.responseHeader = responseHeader;
    }


    /**
     * Gets the getFreeReservedIPsOutput value for this GetFreeReservedIPsResponse_ctype.
     * 
     * @return getFreeReservedIPsOutput
     */
    public java.lang.String[] getGetFreeReservedIPsOutput() {
        return getFreeReservedIPsOutput;
    }


    /**
     * Sets the getFreeReservedIPsOutput value for this GetFreeReservedIPsResponse_ctype.
     * 
     * @param getFreeReservedIPsOutput
     */
    public void setGetFreeReservedIPsOutput(java.lang.String[] getFreeReservedIPsOutput) {
        this.getFreeReservedIPsOutput = getFreeReservedIPsOutput;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof GetFreeReservedIPsResponse_ctype)) return false;
        GetFreeReservedIPsResponse_ctype other = (GetFreeReservedIPsResponse_ctype) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.responseHeader==null && other.getResponseHeader()==null) || 
             (this.responseHeader!=null &&
              this.responseHeader.equals(other.getResponseHeader()))) &&
            ((this.getFreeReservedIPsOutput==null && other.getGetFreeReservedIPsOutput()==null) || 
             (this.getFreeReservedIPsOutput!=null &&
              java.util.Arrays.equals(this.getFreeReservedIPsOutput, other.getGetFreeReservedIPsOutput())));
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
        if (getResponseHeader() != null) {
            _hashCode += getResponseHeader().hashCode();
        }
        if (getGetFreeReservedIPsOutput() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getGetFreeReservedIPsOutput());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getGetFreeReservedIPsOutput(), i);
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
        new org.apache.axis.description.TypeDesc(GetFreeReservedIPsResponse_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "GetFreeReservedIPsResponse_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("responseHeader");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "ResponseHeader"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ResponseHeader_ctype"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("getFreeReservedIPsOutput");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "GetFreeReservedIPsOutput"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        elemField.setItemQName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "Ip"));
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
