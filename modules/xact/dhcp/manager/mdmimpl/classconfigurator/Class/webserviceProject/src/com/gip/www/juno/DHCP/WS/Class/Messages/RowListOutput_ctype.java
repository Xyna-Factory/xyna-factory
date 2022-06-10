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
 * RowListOutput_ctype.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.DHCP.WS.Class.Messages;

public class RowListOutput_ctype  implements java.io.Serializable {
    private com.gip.www.juno.Gui.WS.Messages.OutputHeaderContent_ctype outputHeader;

    private com.gip.www.juno.DHCP.WS.Class.Messages.Row_ctype[] content;

    public RowListOutput_ctype() {
    }

    public RowListOutput_ctype(
           com.gip.www.juno.Gui.WS.Messages.OutputHeaderContent_ctype outputHeader,
           com.gip.www.juno.DHCP.WS.Class.Messages.Row_ctype[] content) {
           this.outputHeader = outputHeader;
           this.content = content;
    }


    /**
     * Gets the outputHeader value for this RowListOutput_ctype.
     * 
     * @return outputHeader
     */
    public com.gip.www.juno.Gui.WS.Messages.OutputHeaderContent_ctype getOutputHeader() {
        return outputHeader;
    }


    /**
     * Sets the outputHeader value for this RowListOutput_ctype.
     * 
     * @param outputHeader
     */
    public void setOutputHeader(com.gip.www.juno.Gui.WS.Messages.OutputHeaderContent_ctype outputHeader) {
        this.outputHeader = outputHeader;
    }


    /**
     * Gets the content value for this RowListOutput_ctype.
     * 
     * @return content
     */
    public com.gip.www.juno.DHCP.WS.Class.Messages.Row_ctype[] getContent() {
        return content;
    }


    /**
     * Sets the content value for this RowListOutput_ctype.
     * 
     * @param content
     */
    public void setContent(com.gip.www.juno.DHCP.WS.Class.Messages.Row_ctype[] content) {
        this.content = content;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof RowListOutput_ctype)) return false;
        RowListOutput_ctype other = (RowListOutput_ctype) obj;
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
            ((this.content==null && other.getContent()==null) || 
             (this.content!=null &&
              java.util.Arrays.equals(this.content, other.getContent())));
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
        if (getContent() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getContent());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getContent(), i);
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
        new org.apache.axis.description.TypeDesc(RowListOutput_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Class/Messages", "RowListOutput_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("outputHeader");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Class/Messages", "OutputHeader"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "OutputHeaderContent_ctype"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("content");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Class/Messages", "Content"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Class/Messages", "Row"));
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
