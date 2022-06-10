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

package com.gip.www.juno.Auditv4Memory.WS.Dhcpv4Packets.Messages;

public class Row_ctype  implements java.io.Serializable {
    private java.lang.String host;

    private java.lang.String ip;

    private java.lang.String inTime;

    private java.lang.String discover;

    private java.lang.String offer;

    public Row_ctype() {
    }

    public Row_ctype(
           java.lang.String host,
           java.lang.String ip,
           java.lang.String inTime,
           java.lang.String discover,
           java.lang.String offer) {
           this.host = host;
           this.ip = ip;
           this.inTime = inTime;
           this.discover = discover;
           this.offer = offer;
    }


    /**
     * Gets the host value for this Row_ctype.
     * 
     * @return host
     */
    public java.lang.String getHost() {
        return host;
    }


    /**
     * Sets the host value for this Row_ctype.
     * 
     * @param host
     */
    public void setHost(java.lang.String host) {
        this.host = host;
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
     * Gets the inTime value for this Row_ctype.
     * 
     * @return inTime
     */
    public java.lang.String getInTime() {
        return inTime;
    }


    /**
     * Sets the inTime value for this Row_ctype.
     * 
     * @param inTime
     */
    public void setInTime(java.lang.String inTime) {
        this.inTime = inTime;
    }


    /**
     * Gets the discover value for this Row_ctype.
     * 
     * @return discover
     */
    public java.lang.String getDiscover() {
        return discover;
    }


    /**
     * Sets the discover value for this Row_ctype.
     * 
     * @param discover
     */
    public void setDiscover(java.lang.String discover) {
        this.discover = discover;
    }


    /**
     * Gets the offer value for this Row_ctype.
     * 
     * @return offer
     */
    public java.lang.String getOffer() {
        return offer;
    }


    /**
     * Sets the offer value for this Row_ctype.
     * 
     * @param offer
     */
    public void setOffer(java.lang.String offer) {
        this.offer = offer;
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
            ((this.host==null && other.getHost()==null) || 
             (this.host!=null &&
              this.host.equals(other.getHost()))) &&
            ((this.ip==null && other.getIp()==null) || 
             (this.ip!=null &&
              this.ip.equals(other.getIp()))) &&
            ((this.inTime==null && other.getInTime()==null) || 
             (this.inTime!=null &&
              this.inTime.equals(other.getInTime()))) &&
            ((this.discover==null && other.getDiscover()==null) || 
             (this.discover!=null &&
              this.discover.equals(other.getDiscover()))) &&
            ((this.offer==null && other.getOffer()==null) || 
             (this.offer!=null &&
              this.offer.equals(other.getOffer())));
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
        if (getHost() != null) {
            _hashCode += getHost().hashCode();
        }
        if (getIp() != null) {
            _hashCode += getIp().hashCode();
        }
        if (getInTime() != null) {
            _hashCode += getInTime().hashCode();
        }
        if (getDiscover() != null) {
            _hashCode += getDiscover().hashCode();
        }
        if (getOffer() != null) {
            _hashCode += getOffer().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Row_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Auditv4Memory/WS/Dhcpv4Packets/Messages", "Row_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("host");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Auditv4Memory/WS/Dhcpv4Packets/Messages", "Host"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("ip");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Auditv4Memory/WS/Dhcpv4Packets/Messages", "Ip"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("inTime");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Auditv4Memory/WS/Dhcpv4Packets/Messages", "InTime"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("discover");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Auditv4Memory/WS/Dhcpv4Packets/Messages", "Discover"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("offer");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Auditv4Memory/WS/Dhcpv4Packets/Messages", "Offer"));
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
