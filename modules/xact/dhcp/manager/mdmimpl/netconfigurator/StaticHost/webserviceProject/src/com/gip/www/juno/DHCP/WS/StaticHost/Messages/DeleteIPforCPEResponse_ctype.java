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
 * DeleteIPforCPEResponse_ctype.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.DHCP.WS.StaticHost.Messages;

public class DeleteIPforCPEResponse_ctype  implements java.io.Serializable {
    private com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader;

    private java.lang.String deleteIPforCPEOutput;

    public DeleteIPforCPEResponse_ctype() {
    }

    public DeleteIPforCPEResponse_ctype(
           com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader,
           java.lang.String deleteIPforCPEOutput) {
           this.responseHeader = responseHeader;
           this.deleteIPforCPEOutput = deleteIPforCPEOutput;
    }


    /**
     * Gets the responseHeader value for this DeleteIPforCPEResponse_ctype.
     * 
     * @return responseHeader
     */
    public com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype getResponseHeader() {
        return responseHeader;
    }


    /**
     * Sets the responseHeader value for this DeleteIPforCPEResponse_ctype.
     * 
     * @param responseHeader
     */
    public void setResponseHeader(com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype responseHeader) {
        this.responseHeader = responseHeader;
    }


    /**
     * Gets the deleteIPforCPEOutput value for this DeleteIPforCPEResponse_ctype.
     * 
     * @return deleteIPforCPEOutput
     */
    public java.lang.String getDeleteIPforCPEOutput() {
        return deleteIPforCPEOutput;
    }


    /**
     * Sets the deleteIPforCPEOutput value for this DeleteIPforCPEResponse_ctype.
     * 
     * @param deleteIPforCPEOutput
     */
    public void setDeleteIPforCPEOutput(java.lang.String deleteIPforCPEOutput) {
        this.deleteIPforCPEOutput = deleteIPforCPEOutput;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof DeleteIPforCPEResponse_ctype)) return false;
        DeleteIPforCPEResponse_ctype other = (DeleteIPforCPEResponse_ctype) obj;
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
            ((this.deleteIPforCPEOutput==null && other.getDeleteIPforCPEOutput()==null) || 
             (this.deleteIPforCPEOutput!=null &&
              this.deleteIPforCPEOutput.equals(other.getDeleteIPforCPEOutput())));
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
        if (getDeleteIPforCPEOutput() != null) {
            _hashCode += getDeleteIPforCPEOutput().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(DeleteIPforCPEResponse_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "DeleteIPforCPEResponse_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("responseHeader");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "ResponseHeader"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ResponseHeader_ctype"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("deleteIPforCPEOutput");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "DeleteIPforCPEOutput"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
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
