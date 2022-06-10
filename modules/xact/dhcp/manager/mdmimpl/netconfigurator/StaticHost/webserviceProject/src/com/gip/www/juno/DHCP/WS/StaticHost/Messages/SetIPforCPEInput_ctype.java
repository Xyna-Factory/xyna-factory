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
 * SetIPforCPEInput_ctype.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.DHCP.WS.StaticHost.Messages;

public class SetIPforCPEInput_ctype  implements java.io.Serializable {
    private java.lang.String cpe_mac;

    private java.lang.String ip;

    private java.lang.String remoteId;

    private java.lang.String dns;

    private java.lang.String hostname;

    private java.lang.String configDescr;

    public SetIPforCPEInput_ctype() {
    }

    public SetIPforCPEInput_ctype(
           java.lang.String cpe_mac,
           java.lang.String ip,
           java.lang.String remoteId,
           java.lang.String dns,
           java.lang.String hostname,
           java.lang.String configDescr) {
           this.cpe_mac = cpe_mac;
           this.ip = ip;
           this.remoteId = remoteId;
           this.dns = dns;
           this.hostname = hostname;
           this.configDescr = configDescr;
    }


    /**
     * Gets the cpe_mac value for this SetIPforCPEInput_ctype.
     * 
     * @return cpe_mac
     */
    public java.lang.String getCpe_mac() {
        return cpe_mac;
    }


    /**
     * Sets the cpe_mac value for this SetIPforCPEInput_ctype.
     * 
     * @param cpe_mac
     */
    public void setCpe_mac(java.lang.String cpe_mac) {
        this.cpe_mac = cpe_mac;
    }


    /**
     * Gets the ip value for this SetIPforCPEInput_ctype.
     * 
     * @return ip
     */
    public java.lang.String getIp() {
        return ip;
    }


    /**
     * Sets the ip value for this SetIPforCPEInput_ctype.
     * 
     * @param ip
     */
    public void setIp(java.lang.String ip) {
        this.ip = ip;
    }


    /**
     * Gets the remoteId value for this SetIPforCPEInput_ctype.
     * 
     * @return remoteId
     */
    public java.lang.String getRemoteId() {
        return remoteId;
    }


    /**
     * Sets the remoteId value for this SetIPforCPEInput_ctype.
     * 
     * @param remoteId
     */
    public void setRemoteId(java.lang.String remoteId) {
        this.remoteId = remoteId;
    }


    /**
     * Gets the dns value for this SetIPforCPEInput_ctype.
     * 
     * @return dns
     */
    public java.lang.String getDns() {
        return dns;
    }


    /**
     * Sets the dns value for this SetIPforCPEInput_ctype.
     * 
     * @param dns
     */
    public void setDns(java.lang.String dns) {
        this.dns = dns;
    }


    /**
     * Gets the hostname value for this SetIPforCPEInput_ctype.
     * 
     * @return hostname
     */
    public java.lang.String getHostname() {
        return hostname;
    }


    /**
     * Sets the hostname value for this SetIPforCPEInput_ctype.
     * 
     * @param hostname
     */
    public void setHostname(java.lang.String hostname) {
        this.hostname = hostname;
    }


    /**
     * Gets the configDescr value for this SetIPforCPEInput_ctype.
     * 
     * @return configDescr
     */
    public java.lang.String getConfigDescr() {
        return configDescr;
    }


    /**
     * Sets the configDescr value for this SetIPforCPEInput_ctype.
     * 
     * @param configDescr
     */
    public void setConfigDescr(java.lang.String configDescr) {
        this.configDescr = configDescr;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof SetIPforCPEInput_ctype)) return false;
        SetIPforCPEInput_ctype other = (SetIPforCPEInput_ctype) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.cpe_mac==null && other.getCpe_mac()==null) || 
             (this.cpe_mac!=null &&
              this.cpe_mac.equals(other.getCpe_mac()))) &&
            ((this.ip==null && other.getIp()==null) || 
             (this.ip!=null &&
              this.ip.equals(other.getIp()))) &&
            ((this.remoteId==null && other.getRemoteId()==null) || 
             (this.remoteId!=null &&
              this.remoteId.equals(other.getRemoteId()))) &&
            ((this.dns==null && other.getDns()==null) || 
             (this.dns!=null &&
              this.dns.equals(other.getDns()))) &&
            ((this.hostname==null && other.getHostname()==null) || 
             (this.hostname!=null &&
              this.hostname.equals(other.getHostname()))) &&
            ((this.configDescr==null && other.getConfigDescr()==null) || 
             (this.configDescr!=null &&
              this.configDescr.equals(other.getConfigDescr())));
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
        if (getCpe_mac() != null) {
            _hashCode += getCpe_mac().hashCode();
        }
        if (getIp() != null) {
            _hashCode += getIp().hashCode();
        }
        if (getRemoteId() != null) {
            _hashCode += getRemoteId().hashCode();
        }
        if (getDns() != null) {
            _hashCode += getDns().hashCode();
        }
        if (getHostname() != null) {
            _hashCode += getHostname().hashCode();
        }
        if (getConfigDescr() != null) {
            _hashCode += getConfigDescr().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(SetIPforCPEInput_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "SetIPforCPEInput_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("cpe_mac");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "Cpe_mac"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("ip");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "Ip"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("remoteId");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "RemoteId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("dns");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "Dns"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("hostname");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "Hostname"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("configDescr");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "ConfigDescr"));
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
