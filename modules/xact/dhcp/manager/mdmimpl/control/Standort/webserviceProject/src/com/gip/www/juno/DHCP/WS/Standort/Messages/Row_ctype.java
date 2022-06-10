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

package com.gip.www.juno.DHCP.WS.Standort.Messages;

public class Row_ctype  implements java.io.Serializable {
    private java.lang.String standortID;

    private java.lang.String standortGruppeID;

    private java.lang.String standortGruppe;

    private java.lang.String name;

    public Row_ctype() {
    }

    public Row_ctype(
           java.lang.String standortID,
           java.lang.String standortGruppeID,
           java.lang.String standortGruppe,
           java.lang.String name) {
           this.standortID = standortID;
           this.standortGruppeID = standortGruppeID;
           this.standortGruppe = standortGruppe;
           this.name = name;
    }


    /**
     * Gets the standortID value for this Row_ctype.
     * 
     * @return standortID
     */
    public java.lang.String getStandortID() {
        return standortID;
    }


    /**
     * Sets the standortID value for this Row_ctype.
     * 
     * @param standortID
     */
    public void setStandortID(java.lang.String standortID) {
        this.standortID = standortID;
    }


    /**
     * Gets the standortGruppeID value for this Row_ctype.
     * 
     * @return standortGruppeID
     */
    public java.lang.String getStandortGruppeID() {
        return standortGruppeID;
    }


    /**
     * Sets the standortGruppeID value for this Row_ctype.
     * 
     * @param standortGruppeID
     */
    public void setStandortGruppeID(java.lang.String standortGruppeID) {
        this.standortGruppeID = standortGruppeID;
    }


    /**
     * Gets the standortGruppe value for this Row_ctype.
     * 
     * @return standortGruppe
     */
    public java.lang.String getStandortGruppe() {
        return standortGruppe;
    }


    /**
     * Sets the standortGruppe value for this Row_ctype.
     * 
     * @param standortGruppe
     */
    public void setStandortGruppe(java.lang.String standortGruppe) {
        this.standortGruppe = standortGruppe;
    }


    /**
     * Gets the name value for this Row_ctype.
     * 
     * @return name
     */
    public java.lang.String getName() {
        return name;
    }


    /**
     * Sets the name value for this Row_ctype.
     * 
     * @param name
     */
    public void setName(java.lang.String name) {
        this.name = name;
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
            ((this.standortID==null && other.getStandortID()==null) || 
             (this.standortID!=null &&
              this.standortID.equals(other.getStandortID()))) &&
            ((this.standortGruppeID==null && other.getStandortGruppeID()==null) || 
             (this.standortGruppeID!=null &&
              this.standortGruppeID.equals(other.getStandortGruppeID()))) &&
            ((this.standortGruppe==null && other.getStandortGruppe()==null) || 
             (this.standortGruppe!=null &&
              this.standortGruppe.equals(other.getStandortGruppe()))) &&
            ((this.name==null && other.getName()==null) || 
             (this.name!=null &&
              this.name.equals(other.getName())));
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
        if (getStandortID() != null) {
            _hashCode += getStandortID().hashCode();
        }
        if (getStandortGruppeID() != null) {
            _hashCode += getStandortGruppeID().hashCode();
        }
        if (getStandortGruppe() != null) {
            _hashCode += getStandortGruppe().hashCode();
        }
        if (getName() != null) {
            _hashCode += getName().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Row_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Standort/Messages", "Row_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("standortID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Standort/Messages", "StandortID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("standortGruppeID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Standort/Messages", "StandortGruppeID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("standortGruppe");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Standort/Messages", "StandortGruppe"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("name");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Standort/Messages", "Name"));
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
