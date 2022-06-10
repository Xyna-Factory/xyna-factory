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

package com.gip.www.juno.SnmpTrap.WS.PoolUsage.Messages;

public class Row_ctype  implements java.io.Serializable {
    private java.lang.String poolID;

    private java.lang.String sharedNetworkID;

    private java.lang.String poolTypeID;

    private java.lang.String standortGruppeID;

    private java.lang.String size;

    private java.lang.String used;

    private java.lang.String usedFraction;

    public Row_ctype() {
    }

    public Row_ctype(
           java.lang.String poolID,
           java.lang.String sharedNetworkID,
           java.lang.String poolTypeID,
           java.lang.String standortGruppeID,
           java.lang.String size,
           java.lang.String used,
           java.lang.String usedFraction) {
           this.poolID = poolID;
           this.sharedNetworkID = sharedNetworkID;
           this.poolTypeID = poolTypeID;
           this.standortGruppeID = standortGruppeID;
           this.size = size;
           this.used = used;
           this.usedFraction = usedFraction;
    }


    /**
     * Gets the poolID value for this Row_ctype.
     * 
     * @return poolID
     */
    public java.lang.String getPoolID() {
        return poolID;
    }


    /**
     * Sets the poolID value for this Row_ctype.
     * 
     * @param poolID
     */
    public void setPoolID(java.lang.String poolID) {
        this.poolID = poolID;
    }


    /**
     * Gets the sharedNetworkID value for this Row_ctype.
     * 
     * @return sharedNetworkID
     */
    public java.lang.String getSharedNetworkID() {
        return sharedNetworkID;
    }


    /**
     * Sets the sharedNetworkID value for this Row_ctype.
     * 
     * @param sharedNetworkID
     */
    public void setSharedNetworkID(java.lang.String sharedNetworkID) {
        this.sharedNetworkID = sharedNetworkID;
    }


    /**
     * Gets the poolTypeID value for this Row_ctype.
     * 
     * @return poolTypeID
     */
    public java.lang.String getPoolTypeID() {
        return poolTypeID;
    }


    /**
     * Sets the poolTypeID value for this Row_ctype.
     * 
     * @param poolTypeID
     */
    public void setPoolTypeID(java.lang.String poolTypeID) {
        this.poolTypeID = poolTypeID;
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
     * Gets the size value for this Row_ctype.
     * 
     * @return size
     */
    public java.lang.String getSize() {
        return size;
    }


    /**
     * Sets the size value for this Row_ctype.
     * 
     * @param size
     */
    public void setSize(java.lang.String size) {
        this.size = size;
    }


    /**
     * Gets the used value for this Row_ctype.
     * 
     * @return used
     */
    public java.lang.String getUsed() {
        return used;
    }


    /**
     * Sets the used value for this Row_ctype.
     * 
     * @param used
     */
    public void setUsed(java.lang.String used) {
        this.used = used;
    }


    /**
     * Gets the usedFraction value for this Row_ctype.
     * 
     * @return usedFraction
     */
    public java.lang.String getUsedFraction() {
        return usedFraction;
    }


    /**
     * Sets the usedFraction value for this Row_ctype.
     * 
     * @param usedFraction
     */
    public void setUsedFraction(java.lang.String usedFraction) {
        this.usedFraction = usedFraction;
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
            ((this.poolID==null && other.getPoolID()==null) || 
             (this.poolID!=null &&
              this.poolID.equals(other.getPoolID()))) &&
            ((this.sharedNetworkID==null && other.getSharedNetworkID()==null) || 
             (this.sharedNetworkID!=null &&
              this.sharedNetworkID.equals(other.getSharedNetworkID()))) &&
            ((this.poolTypeID==null && other.getPoolTypeID()==null) || 
             (this.poolTypeID!=null &&
              this.poolTypeID.equals(other.getPoolTypeID()))) &&
            ((this.standortGruppeID==null && other.getStandortGruppeID()==null) || 
             (this.standortGruppeID!=null &&
              this.standortGruppeID.equals(other.getStandortGruppeID()))) &&
            ((this.size==null && other.getSize()==null) || 
             (this.size!=null &&
              this.size.equals(other.getSize()))) &&
            ((this.used==null && other.getUsed()==null) || 
             (this.used!=null &&
              this.used.equals(other.getUsed()))) &&
            ((this.usedFraction==null && other.getUsedFraction()==null) || 
             (this.usedFraction!=null &&
              this.usedFraction.equals(other.getUsedFraction())));
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
        if (getPoolID() != null) {
            _hashCode += getPoolID().hashCode();
        }
        if (getSharedNetworkID() != null) {
            _hashCode += getSharedNetworkID().hashCode();
        }
        if (getPoolTypeID() != null) {
            _hashCode += getPoolTypeID().hashCode();
        }
        if (getStandortGruppeID() != null) {
            _hashCode += getStandortGruppeID().hashCode();
        }
        if (getSize() != null) {
            _hashCode += getSize().hashCode();
        }
        if (getUsed() != null) {
            _hashCode += getUsed().hashCode();
        }
        if (getUsedFraction() != null) {
            _hashCode += getUsedFraction().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Row_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/SnmpTrap/WS/PoolUsage/Messages", "Row_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("poolID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/SnmpTrap/WS/PoolUsage/Messages", "PoolID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("sharedNetworkID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/SnmpTrap/WS/PoolUsage/Messages", "SharedNetworkID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("poolTypeID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/SnmpTrap/WS/PoolUsage/Messages", "PoolTypeID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("standortGruppeID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/SnmpTrap/WS/PoolUsage/Messages", "StandortGruppeID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("size");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/SnmpTrap/WS/PoolUsage/Messages", "Size"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("used");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/SnmpTrap/WS/PoolUsage/Messages", "Used"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("usedFraction");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/SnmpTrap/WS/PoolUsage/Messages", "UsedFraction"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
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
