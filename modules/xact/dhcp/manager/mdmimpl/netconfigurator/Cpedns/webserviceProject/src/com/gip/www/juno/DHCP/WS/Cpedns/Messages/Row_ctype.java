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
 * Row_ctype.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.DHCP.WS.Cpedns.Messages;

public class Row_ctype  implements java.io.Serializable {
    private java.lang.String cpeDnsID;

    private java.lang.String cpeDns;

    public Row_ctype() {
    }

    public Row_ctype(
           java.lang.String cpeDnsID,
           java.lang.String cpeDns) {
           this.cpeDnsID = cpeDnsID;
           this.cpeDns = cpeDns;
    }


    /**
     * Gets the cpeDnsID value for this Row_ctype.
     * 
     * @return cpeDnsID
     */
    public java.lang.String getCpeDnsID() {
        return cpeDnsID;
    }


    /**
     * Sets the cpeDnsID value for this Row_ctype.
     * 
     * @param cpeDnsID
     */
    public void setCpeDnsID(java.lang.String cpeDnsID) {
        this.cpeDnsID = cpeDnsID;
    }


    /**
     * Gets the cpeDns value for this Row_ctype.
     * 
     * @return cpeDns
     */
    public java.lang.String getCpeDns() {
        return cpeDns;
    }


    /**
     * Sets the cpeDns value for this Row_ctype.
     * 
     * @param cpeDns
     */
    public void setCpeDns(java.lang.String cpeDns) {
        this.cpeDns = cpeDns;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof Row_ctype)) return false;
        Row_ctype other = (Row_ctype) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.cpeDnsID==null && other.getCpeDnsID()==null) || 
             (this.cpeDnsID!=null &&
              this.cpeDnsID.equals(other.getCpeDnsID()))) &&
            ((this.cpeDns==null && other.getCpeDns()==null) || 
             (this.cpeDns!=null &&
              this.cpeDns.equals(other.getCpeDns())));
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
        if (getCpeDnsID() != null) {
            _hashCode += getCpeDnsID().hashCode();
        }
        if (getCpeDns() != null) {
            _hashCode += getCpeDns().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Row_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Cpedns/Messages", "Row_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("cpeDnsID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Cpedns/Messages", "CpeDnsID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("cpeDns");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Cpedns/Messages", "CpeDns"));
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
