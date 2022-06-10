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

package com.gip.www.juno.DHCP.WS.SharedNetwork.Messages;

public class Row_ctype  implements java.io.Serializable {
    private java.lang.String sharedNetworkID;

    private java.lang.String standortID;

    private java.lang.String standort;

    private java.lang.String sharedNetwork;

    private java.lang.String cpeDnsID;

    private java.lang.String cpeDns;

    private java.lang.String linkAddresses;

    private java.lang.String migrationState;

    public Row_ctype() {
    }

    public Row_ctype(
           java.lang.String sharedNetworkID,
           java.lang.String standortID,
           java.lang.String standort,
           java.lang.String sharedNetwork,
           java.lang.String cpeDnsID,
           java.lang.String cpeDns,
           java.lang.String linkAddresses,
           java.lang.String migrationState) {
           this.sharedNetworkID = sharedNetworkID;
           this.standortID = standortID;
           this.standort = standort;
           this.sharedNetwork = sharedNetwork;
           this.cpeDnsID = cpeDnsID;
           this.cpeDns = cpeDns;
           this.linkAddresses = linkAddresses;
           this.migrationState = migrationState;
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
     * Gets the standort value for this Row_ctype.
     * 
     * @return standort
     */
    public java.lang.String getStandort() {
        return standort;
    }


    /**
     * Sets the standort value for this Row_ctype.
     * 
     * @param standort
     */
    public void setStandort(java.lang.String standort) {
        this.standort = standort;
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


    /**
     * Gets the linkAddresses value for this Row_ctype.
     * 
     * @return linkAddresses
     */
    public java.lang.String getLinkAddresses() {
        return linkAddresses;
    }


    /**
     * Sets the linkAddresses value for this Row_ctype.
     * 
     * @param linkAddresses
     */
    public void setLinkAddresses(java.lang.String linkAddresses) {
        this.linkAddresses = linkAddresses;
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
            ((this.sharedNetworkID==null && other.getSharedNetworkID()==null) || 
             (this.sharedNetworkID!=null &&
              this.sharedNetworkID.equals(other.getSharedNetworkID()))) &&
            ((this.standortID==null && other.getStandortID()==null) || 
             (this.standortID!=null &&
              this.standortID.equals(other.getStandortID()))) &&
            ((this.standort==null && other.getStandort()==null) || 
             (this.standort!=null &&
              this.standort.equals(other.getStandort()))) &&
            ((this.sharedNetwork==null && other.getSharedNetwork()==null) || 
             (this.sharedNetwork!=null &&
              this.sharedNetwork.equals(other.getSharedNetwork()))) &&
            ((this.cpeDnsID==null && other.getCpeDnsID()==null) || 
             (this.cpeDnsID!=null &&
              this.cpeDnsID.equals(other.getCpeDnsID()))) &&
            ((this.cpeDns==null && other.getCpeDns()==null) || 
             (this.cpeDns!=null &&
              this.cpeDns.equals(other.getCpeDns()))) &&
            ((this.linkAddresses==null && other.getLinkAddresses()==null) || 
             (this.linkAddresses!=null &&
              this.linkAddresses.equals(other.getLinkAddresses()))) &&
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
        if (getSharedNetworkID() != null) {
            _hashCode += getSharedNetworkID().hashCode();
        }
        if (getStandortID() != null) {
            _hashCode += getStandortID().hashCode();
        }
        if (getStandort() != null) {
            _hashCode += getStandort().hashCode();
        }
        if (getSharedNetwork() != null) {
            _hashCode += getSharedNetwork().hashCode();
        }
        if (getCpeDnsID() != null) {
            _hashCode += getCpeDnsID().hashCode();
        }
        if (getCpeDns() != null) {
            _hashCode += getCpeDns().hashCode();
        }
        if (getLinkAddresses() != null) {
            _hashCode += getLinkAddresses().hashCode();
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
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/SharedNetwork/Messages", "Row_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("sharedNetworkID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/SharedNetwork/Messages", "SharedNetworkID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("standortID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/SharedNetwork/Messages", "StandortID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("standort");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/SharedNetwork/Messages", "Standort"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("sharedNetwork");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/SharedNetwork/Messages", "SharedNetwork"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("cpeDnsID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/SharedNetwork/Messages", "CpeDnsID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("cpeDns");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/SharedNetwork/Messages", "CpeDns"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("linkAddresses");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/SharedNetwork/Messages", "LinkAddresses"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("migrationState");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/SharedNetwork/Messages", "MigrationState"));
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
