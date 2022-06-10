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
 * DhcpdConfResponse_ctype.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.Gui.WS.Messages;

public class DhcpdConfResponse_ctype  implements java.io.Serializable {
    private com.gip.www.juno.Gui.WS.Messages.OutputHeaderContent_ctype outputHeader;

    private java.lang.String outputContent;

    public DhcpdConfResponse_ctype() {
    }

    public DhcpdConfResponse_ctype(
           com.gip.www.juno.Gui.WS.Messages.OutputHeaderContent_ctype outputHeader,
           java.lang.String outputContent) {
           this.outputHeader = outputHeader;
           this.outputContent = outputContent;
    }


    /**
     * Gets the outputHeader value for this DhcpdConfResponse_ctype.
     * 
     * @return outputHeader
     */
    public com.gip.www.juno.Gui.WS.Messages.OutputHeaderContent_ctype getOutputHeader() {
        return outputHeader;
    }


    /**
     * Sets the outputHeader value for this DhcpdConfResponse_ctype.
     * 
     * @param outputHeader
     */
    public void setOutputHeader(com.gip.www.juno.Gui.WS.Messages.OutputHeaderContent_ctype outputHeader) {
        this.outputHeader = outputHeader;
    }


    /**
     * Gets the outputContent value for this DhcpdConfResponse_ctype.
     * 
     * @return outputContent
     */
    public java.lang.String getOutputContent() {
        return outputContent;
    }


    /**
     * Sets the outputContent value for this DhcpdConfResponse_ctype.
     * 
     * @param outputContent
     */
    public void setOutputContent(java.lang.String outputContent) {
        this.outputContent = outputContent;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof DhcpdConfResponse_ctype)) return false;
        DhcpdConfResponse_ctype other = (DhcpdConfResponse_ctype) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.outputHeader==null && other.getOutputHeader()==null) || 
             (this.outputHeader!=null &&
              this.outputHeader.equals(other.getOutputHeader()))) &&
            ((this.outputContent==null && other.getOutputContent()==null) || 
             (this.outputContent!=null &&
              this.outputContent.equals(other.getOutputContent())));
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
        if (getOutputHeader() != null) {
            _hashCode += getOutputHeader().hashCode();
        }
        if (getOutputContent() != null) {
            _hashCode += getOutputContent().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(DhcpdConfResponse_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DhcpdConfResponse_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("outputHeader");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "OutputHeader"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "OutputHeaderContent_ctype"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("outputContent");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "OutputContent"));
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
