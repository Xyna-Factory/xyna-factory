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

package com.gip.www.juno.DHCP.WS.Subnet.Messages;

public class Row_ctype  implements java.io.Serializable {
    private java.lang.String subnetID;

    private java.lang.String sharedNetworkID;

    private java.lang.String sharedNetwork;

    private java.lang.String subnet;

    private java.lang.String mask;

    private java.lang.String attributes;

    private java.lang.String fixedAttributes;

    private java.lang.String migrationState;

    public Row_ctype() {
    }

    public Row_ctype(
           java.lang.String subnetID,
           java.lang.String sharedNetworkID,
           java.lang.String sharedNetwork,
           java.lang.String subnet,
           java.lang.String mask,
           java.lang.String attributes,
           java.lang.String fixedAttributes,
           java.lang.String migrationState) {
           this.subnetID = subnetID;
           this.sharedNetworkID = sharedNetworkID;
           this.sharedNetwork = sharedNetwork;
           this.subnet = subnet;
           this.mask = mask;
           this.attributes = attributes;
           this.fixedAttributes = fixedAttributes;
           this.migrationState = migrationState;
    }


    /**
     * Gets the subnetID value for this Row_ctype.
     * 
     * @return subnetID
     */
    public java.lang.String getSubnetID() {
        return subnetID;
    }


    /**
     * Sets the subnetID value for this Row_ctype.
     * 
     * @param subnetID
     */
    public void setSubnetID(java.lang.String subnetID) {
        this.subnetID = subnetID;
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
     * Gets the sharedNetwork value for this Row_ctype.
     * 
     * @return sharedNetwork
     */
    public java.lang.String getSharedNetwork() {
        return sharedNetwork;
    }


    /**
     * Sets the sharedNetwork value for this Row_ctype.
     * 
     * @param sharedNetwork
     */
    public void setSharedNetwork(java.lang.String sharedNetwork) {
        this.sharedNetwork = sharedNetwork;
    }


    /**
     * Gets the subnet value for this Row_ctype.
     * 
     * @return subnet
     */
    public java.lang.String getSubnet() {
        return subnet;
    }


    /**
     * Sets the subnet value for this Row_ctype.
     * 
     * @param subnet
     */
    public void setSubnet(java.lang.String subnet) {
        this.subnet = subnet;
    }


    /**
     * Gets the mask value for this Row_ctype.
     * 
     * @return mask
     */
    public java.lang.String getMask() {
        return mask;
    }


    /**
     * Sets the mask value for this Row_ctype.
     * 
     * @param mask
     */
    public void setMask(java.lang.String mask) {
        this.mask = mask;
    }


    /**
     * Gets the attributes value for this Row_ctype.
     * 
     * @return attributes
     */
    public java.lang.String getAttributes() {
        return attributes;
    }


    /**
     * Sets the attributes value for this Row_ctype.
     * 
     * @param attributes
     */
    public void setAttributes(java.lang.String attributes) {
        this.attributes = attributes;
    }


    /**
     * Gets the fixedAttributes value for this Row_ctype.
     * 
     * @return fixedAttributes
     */
    public java.lang.String getFixedAttributes() {
        return fixedAttributes;
    }


    /**
     * Sets the fixedAttributes value for this Row_ctype.
     * 
     * @param fixedAttributes
     */
    public void setFixedAttributes(java.lang.String fixedAttributes) {
        this.fixedAttributes = fixedAttributes;
    }


    /**
     * Gets the migrationState value for this Row_ctype.
     * 
     * @return migrationState
     */
    public java.lang.String getMigrationState() {
        return migrationState;
    }


    /**
     * Sets the migrationState value for this Row_ctype.
     * 
     * @param migrationState
     */
    public void setMigrationState(java.lang.String migrationState) {
        this.migrationState = migrationState;
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
            ((this.subnetID==null && other.getSubnetID()==null) || 
             (this.subnetID!=null &&
              this.subnetID.equals(other.getSubnetID()))) &&
            ((this.sharedNetworkID==null && other.getSharedNetworkID()==null) || 
             (this.sharedNetworkID!=null &&
              this.sharedNetworkID.equals(other.getSharedNetworkID()))) &&
            ((this.sharedNetwork==null && other.getSharedNetwork()==null) || 
             (this.sharedNetwork!=null &&
              this.sharedNetwork.equals(other.getSharedNetwork()))) &&
            ((this.subnet==null && other.getSubnet()==null) || 
             (this.subnet!=null &&
              this.subnet.equals(other.getSubnet()))) &&
            ((this.mask==null && other.getMask()==null) || 
             (this.mask!=null &&
              this.mask.equals(other.getMask()))) &&
            ((this.attributes==null && other.getAttributes()==null) || 
             (this.attributes!=null &&
              this.attributes.equals(other.getAttributes()))) &&
            ((this.fixedAttributes==null && other.getFixedAttributes()==null) || 
             (this.fixedAttributes!=null &&
              this.fixedAttributes.equals(other.getFixedAttributes()))) &&
            ((this.migrationState==null && other.getMigrationState()==null) || 
             (this.migrationState!=null &&
              this.migrationState.equals(other.getMigrationState())));
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
        if (getSubnetID() != null) {
            _hashCode += getSubnetID().hashCode();
        }
        if (getSharedNetworkID() != null) {
            _hashCode += getSharedNetworkID().hashCode();
        }
        if (getSharedNetwork() != null) {
            _hashCode += getSharedNetwork().hashCode();
        }
        if (getSubnet() != null) {
            _hashCode += getSubnet().hashCode();
        }
        if (getMask() != null) {
            _hashCode += getMask().hashCode();
        }
        if (getAttributes() != null) {
            _hashCode += getAttributes().hashCode();
        }
        if (getFixedAttributes() != null) {
            _hashCode += getFixedAttributes().hashCode();
        }
        if (getMigrationState() != null) {
            _hashCode += getMigrationState().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Row_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Subnet/Messages", "Row_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("subnetID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Subnet/Messages", "SubnetID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("sharedNetworkID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Subnet/Messages", "SharedNetworkID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("sharedNetwork");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Subnet/Messages", "SharedNetwork"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("subnet");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Subnet/Messages", "Subnet"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("mask");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Subnet/Messages", "Mask"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("attributes");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Subnet/Messages", "Attributes"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("fixedAttributes");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Subnet/Messages", "FixedAttributes"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("migrationState");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Subnet/Messages", "MigrationState"));
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
