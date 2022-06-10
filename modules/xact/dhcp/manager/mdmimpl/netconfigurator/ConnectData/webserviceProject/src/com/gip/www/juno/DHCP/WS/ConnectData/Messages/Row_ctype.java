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

package com.gip.www.juno.DHCP.WS.ConnectData.Messages;

public class Row_ctype  implements java.io.Serializable {
    private java.lang.String connectDataID;

    private java.lang.String ip;

    private java.lang.String osUser;

    private java.lang.String passwort;

    private java.lang.String rsaKey;

    public Row_ctype() {
    }

    public Row_ctype(
           java.lang.String connectDataID,
           java.lang.String ip,
           java.lang.String osUser,
           java.lang.String passwort,
           java.lang.String rsaKey) {
           this.connectDataID = connectDataID;
           this.ip = ip;
           this.osUser = osUser;
           this.passwort = passwort;
           this.rsaKey = rsaKey;
    }


    /**
     * Gets the connectDataID value for this Row_ctype.
     * 
     * @return connectDataID
     */
    public java.lang.String getConnectDataID() {
        return connectDataID;
    }


    /**
     * Sets the connectDataID value for this Row_ctype.
     * 
     * @param connectDataID
     */
    public void setConnectDataID(java.lang.String connectDataID) {
        this.connectDataID = connectDataID;
    }


    /**
     * Gets the ip value for this Row_ctype.
     * 
     * @return ip
     */
    public java.lang.String getIp() {
        return ip;
    }


    /**
     * Sets the ip value for this Row_ctype.
     * 
     * @param ip
     */
    public void setIp(java.lang.String ip) {
        this.ip = ip;
    }


    /**
     * Gets the osUser value for this Row_ctype.
     * 
     * @return osUser
     */
    public java.lang.String getOsUser() {
        return osUser;
    }


    /**
     * Sets the osUser value for this Row_ctype.
     * 
     * @param osUser
     */
    public void setOsUser(java.lang.String osUser) {
        this.osUser = osUser;
    }


    /**
     * Gets the passwort value for this Row_ctype.
     * 
     * @return passwort
     */
    public java.lang.String getPasswort() {
        return passwort;
    }


    /**
     * Sets the passwort value for this Row_ctype.
     * 
     * @param passwort
     */
    public void setPasswort(java.lang.String passwort) {
        this.passwort = passwort;
    }


    /**
     * Gets the rsaKey value for this Row_ctype.
     * 
     * @return rsaKey
     */
    public java.lang.String getRsaKey() {
        return rsaKey;
    }


    /**
     * Sets the rsaKey value for this Row_ctype.
     * 
     * @param rsaKey
     */
    public void setRsaKey(java.lang.String rsaKey) {
        this.rsaKey = rsaKey;
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
            ((this.connectDataID==null && other.getConnectDataID()==null) || 
             (this.connectDataID!=null &&
              this.connectDataID.equals(other.getConnectDataID()))) &&
            ((this.ip==null && other.getIp()==null) || 
             (this.ip!=null &&
              this.ip.equals(other.getIp()))) &&
            ((this.osUser==null && other.getOsUser()==null) || 
             (this.osUser!=null &&
              this.osUser.equals(other.getOsUser()))) &&
            ((this.passwort==null && other.getPasswort()==null) || 
             (this.passwort!=null &&
              this.passwort.equals(other.getPasswort()))) &&
            ((this.rsaKey==null && other.getRsaKey()==null) || 
             (this.rsaKey!=null &&
              this.rsaKey.equals(other.getRsaKey())));
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
        if (getConnectDataID() != null) {
            _hashCode += getConnectDataID().hashCode();
        }
        if (getIp() != null) {
            _hashCode += getIp().hashCode();
        }
        if (getOsUser() != null) {
            _hashCode += getOsUser().hashCode();
        }
        if (getPasswort() != null) {
            _hashCode += getPasswort().hashCode();
        }
        if (getRsaKey() != null) {
            _hashCode += getRsaKey().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Row_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/ConnectData/Messages", "Row_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("connectDataID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/ConnectData/Messages", "ConnectDataID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("ip");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/ConnectData/Messages", "Ip"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("osUser");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/ConnectData/Messages", "OsUser"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("passwort");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/ConnectData/Messages", "Passwort"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("rsaKey");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/ConnectData/Messages", "RsaKey"));
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
