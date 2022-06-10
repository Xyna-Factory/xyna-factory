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

package com.gip.www.juno.DHCP.WS.StaticHost.Messages;

public class Row_ctype  implements java.io.Serializable {
    private java.lang.String staticHostID;

    private java.lang.String subnetID;

    private java.lang.String subnet;

    private java.lang.String cpe_mac;

    private java.lang.String remoteId;

    private java.lang.String ip;

    private java.lang.String dns;

    private java.lang.String hostname;

    private java.lang.String deployed1;

    private java.lang.String deployed2;

    private java.lang.String dynamicDnsActive;

    private java.lang.String configDescr;

    private java.lang.String assignedPoolID;

    private java.lang.String pool;

    private java.lang.String desiredPoolType;

    private java.lang.String poolType;

    private java.lang.String cmtsip;

    public Row_ctype() {
    }

    public Row_ctype(
           java.lang.String staticHostID,
           java.lang.String subnetID,
           java.lang.String subnet,
           java.lang.String cpe_mac,
           java.lang.String remoteId,
           java.lang.String ip,
           java.lang.String dns,
           java.lang.String hostname,
           java.lang.String deployed1,
           java.lang.String deployed2,
           java.lang.String dynamicDnsActive,
           java.lang.String configDescr,
           java.lang.String assignedPoolID,
           java.lang.String pool,
           java.lang.String desiredPoolType,
           java.lang.String poolType,
           java.lang.String cmtsip) {
           this.staticHostID = staticHostID;
           this.subnetID = subnetID;
           this.subnet = subnet;
           this.cpe_mac = cpe_mac;
           this.remoteId = remoteId;
           this.ip = ip;
           this.dns = dns;
           this.hostname = hostname;
           this.deployed1 = deployed1;
           this.deployed2 = deployed2;
           this.dynamicDnsActive = dynamicDnsActive;
           this.configDescr = configDescr;
           this.assignedPoolID = assignedPoolID;
           this.pool = pool;
           this.desiredPoolType = desiredPoolType;
           this.poolType = poolType;
           this.cmtsip = cmtsip;
    }


    /**
     * Gets the staticHostID value for this Row_ctype.
     * 
     * @return staticHostID
     */
    public java.lang.String getStaticHostID() {
        return staticHostID;
    }


    /**
     * Sets the staticHostID value for this Row_ctype.
     * 
     * @param staticHostID
     */
    public void setStaticHostID(java.lang.String staticHostID) {
        this.staticHostID = staticHostID;
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
     * Gets the cpe_mac value for this Row_ctype.
     * 
     * @return cpe_mac
     */
    public java.lang.String getCpe_mac() {
        return cpe_mac;
    }


    /**
     * Sets the cpe_mac value for this Row_ctype.
     * 
     * @param cpe_mac
     */
    public void setCpe_mac(java.lang.String cpe_mac) {
        this.cpe_mac = cpe_mac;
    }


    /**
     * Gets the remoteId value for this Row_ctype.
     * 
     * @return remoteId
     */
    public java.lang.String getRemoteId() {
        return remoteId;
    }


    /**
     * Sets the remoteId value for this Row_ctype.
     * 
     * @param remoteId
     */
    public void setRemoteId(java.lang.String remoteId) {
        this.remoteId = remoteId;
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
     * Gets the dns value for this Row_ctype.
     * 
     * @return dns
     */
    public java.lang.String getDns() {
        return dns;
    }


    /**
     * Sets the dns value for this Row_ctype.
     * 
     * @param dns
     */
    public void setDns(java.lang.String dns) {
        this.dns = dns;
    }


    /**
     * Gets the hostname value for this Row_ctype.
     * 
     * @return hostname
     */
    public java.lang.String getHostname() {
        return hostname;
    }


    /**
     * Sets the hostname value for this Row_ctype.
     * 
     * @param hostname
     */
    public void setHostname(java.lang.String hostname) {
        this.hostname = hostname;
    }


    /**
     * Gets the deployed1 value for this Row_ctype.
     * 
     * @return deployed1
     */
    public java.lang.String getDeployed1() {
        return deployed1;
    }


    /**
     * Sets the deployed1 value for this Row_ctype.
     * 
     * @param deployed1
     */
    public void setDeployed1(java.lang.String deployed1) {
        this.deployed1 = deployed1;
    }


    /**
     * Gets the deployed2 value for this Row_ctype.
     * 
     * @return deployed2
     */
    public java.lang.String getDeployed2() {
        return deployed2;
    }


    /**
     * Sets the deployed2 value for this Row_ctype.
     * 
     * @param deployed2
     */
    public void setDeployed2(java.lang.String deployed2) {
        this.deployed2 = deployed2;
    }


    /**
     * Gets the dynamicDnsActive value for this Row_ctype.
     * 
     * @return dynamicDnsActive
     */
    public java.lang.String getDynamicDnsActive() {
        return dynamicDnsActive;
    }


    /**
     * Sets the dynamicDnsActive value for this Row_ctype.
     * 
     * @param dynamicDnsActive
     */
    public void setDynamicDnsActive(java.lang.String dynamicDnsActive) {
        this.dynamicDnsActive = dynamicDnsActive;
    }


    /**
     * Gets the configDescr value for this Row_ctype.
     * 
     * @return configDescr
     */
    public java.lang.String getConfigDescr() {
        return configDescr;
    }


    /**
     * Sets the configDescr value for this Row_ctype.
     * 
     * @param configDescr
     */
    public void setConfigDescr(java.lang.String configDescr) {
        this.configDescr = configDescr;
    }


    /**
     * Gets the assignedPoolID value for this Row_ctype.
     * 
     * @return assignedPoolID
     */
    public java.lang.String getAssignedPoolID() {
        return assignedPoolID;
    }


    /**
     * Sets the assignedPoolID value for this Row_ctype.
     * 
     * @param assignedPoolID
     */
    public void setAssignedPoolID(java.lang.String assignedPoolID) {
        this.assignedPoolID = assignedPoolID;
    }


    /**
     * Gets the pool value for this Row_ctype.
     * 
     * @return pool
     */
    public java.lang.String getPool() {
        return pool;
    }


    /**
     * Sets the pool value for this Row_ctype.
     * 
     * @param pool
     */
    public void setPool(java.lang.String pool) {
        this.pool = pool;
    }


    /**
     * Gets the desiredPoolType value for this Row_ctype.
     * 
     * @return desiredPoolType
     */
    public java.lang.String getDesiredPoolType() {
        return desiredPoolType;
    }


    /**
     * Sets the desiredPoolType value for this Row_ctype.
     * 
     * @param desiredPoolType
     */
    public void setDesiredPoolType(java.lang.String desiredPoolType) {
        this.desiredPoolType = desiredPoolType;
    }


    /**
     * Gets the poolType value for this Row_ctype.
     * 
     * @return poolType
     */
    public java.lang.String getPoolType() {
        return poolType;
    }


    /**
     * Sets the poolType value for this Row_ctype.
     * 
     * @param poolType
     */
    public void setPoolType(java.lang.String poolType) {
        this.poolType = poolType;
    }


    /**
     * Gets the cmtsip value for this Row_ctype.
     * 
     * @return cmtsip
     */
    public java.lang.String getCmtsip() {
        return cmtsip;
    }


    /**
     * Sets the cmtsip value for this Row_ctype.
     * 
     * @param cmtsip
     */
    public void setCmtsip(java.lang.String cmtsip) {
        this.cmtsip = cmtsip;
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
            ((this.staticHostID==null && other.getStaticHostID()==null) || 
             (this.staticHostID!=null &&
              this.staticHostID.equals(other.getStaticHostID()))) &&
            ((this.subnetID==null && other.getSubnetID()==null) || 
             (this.subnetID!=null &&
              this.subnetID.equals(other.getSubnetID()))) &&
            ((this.subnet==null && other.getSubnet()==null) || 
             (this.subnet!=null &&
              this.subnet.equals(other.getSubnet()))) &&
            ((this.cpe_mac==null && other.getCpe_mac()==null) || 
             (this.cpe_mac!=null &&
              this.cpe_mac.equals(other.getCpe_mac()))) &&
            ((this.remoteId==null && other.getRemoteId()==null) || 
             (this.remoteId!=null &&
              this.remoteId.equals(other.getRemoteId()))) &&
            ((this.ip==null && other.getIp()==null) || 
             (this.ip!=null &&
              this.ip.equals(other.getIp()))) &&
            ((this.dns==null && other.getDns()==null) || 
             (this.dns!=null &&
              this.dns.equals(other.getDns()))) &&
            ((this.hostname==null && other.getHostname()==null) || 
             (this.hostname!=null &&
              this.hostname.equals(other.getHostname()))) &&
            ((this.deployed1==null && other.getDeployed1()==null) || 
             (this.deployed1!=null &&
              this.deployed1.equals(other.getDeployed1()))) &&
            ((this.deployed2==null && other.getDeployed2()==null) || 
             (this.deployed2!=null &&
              this.deployed2.equals(other.getDeployed2()))) &&
            ((this.dynamicDnsActive==null && other.getDynamicDnsActive()==null) || 
             (this.dynamicDnsActive!=null &&
              this.dynamicDnsActive.equals(other.getDynamicDnsActive()))) &&
            ((this.configDescr==null && other.getConfigDescr()==null) || 
             (this.configDescr!=null &&
              this.configDescr.equals(other.getConfigDescr()))) &&
            ((this.assignedPoolID==null && other.getAssignedPoolID()==null) || 
             (this.assignedPoolID!=null &&
              this.assignedPoolID.equals(other.getAssignedPoolID()))) &&
            ((this.pool==null && other.getPool()==null) || 
             (this.pool!=null &&
              this.pool.equals(other.getPool()))) &&
            ((this.desiredPoolType==null && other.getDesiredPoolType()==null) || 
             (this.desiredPoolType!=null &&
              this.desiredPoolType.equals(other.getDesiredPoolType()))) &&
            ((this.poolType==null && other.getPoolType()==null) || 
             (this.poolType!=null &&
              this.poolType.equals(other.getPoolType()))) &&
            ((this.cmtsip==null && other.getCmtsip()==null) || 
             (this.cmtsip!=null &&
              this.cmtsip.equals(other.getCmtsip())));
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
        if (getStaticHostID() != null) {
            _hashCode += getStaticHostID().hashCode();
        }
        if (getSubnetID() != null) {
            _hashCode += getSubnetID().hashCode();
        }
        if (getSubnet() != null) {
            _hashCode += getSubnet().hashCode();
        }
        if (getCpe_mac() != null) {
            _hashCode += getCpe_mac().hashCode();
        }
        if (getRemoteId() != null) {
            _hashCode += getRemoteId().hashCode();
        }
        if (getIp() != null) {
            _hashCode += getIp().hashCode();
        }
        if (getDns() != null) {
            _hashCode += getDns().hashCode();
        }
        if (getHostname() != null) {
            _hashCode += getHostname().hashCode();
        }
        if (getDeployed1() != null) {
            _hashCode += getDeployed1().hashCode();
        }
        if (getDeployed2() != null) {
            _hashCode += getDeployed2().hashCode();
        }
        if (getDynamicDnsActive() != null) {
            _hashCode += getDynamicDnsActive().hashCode();
        }
        if (getConfigDescr() != null) {
            _hashCode += getConfigDescr().hashCode();
        }
        if (getAssignedPoolID() != null) {
            _hashCode += getAssignedPoolID().hashCode();
        }
        if (getPool() != null) {
            _hashCode += getPool().hashCode();
        }
        if (getDesiredPoolType() != null) {
            _hashCode += getDesiredPoolType().hashCode();
        }
        if (getPoolType() != null) {
            _hashCode += getPoolType().hashCode();
        }
        if (getCmtsip() != null) {
            _hashCode += getCmtsip().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Row_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "Row_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("staticHostID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "StaticHostID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("subnetID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "SubnetID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("subnet");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "Subnet"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("cpe_mac");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "Cpe_mac"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("remoteId");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "RemoteId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("ip");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "Ip"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("dns");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "Dns"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("hostname");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "Hostname"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("deployed1");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "Deployed1"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("deployed2");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "Deployed2"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("dynamicDnsActive");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "DynamicDnsActive"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("configDescr");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "ConfigDescr"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("assignedPoolID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "AssignedPoolID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("pool");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "Pool"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("desiredPoolType");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "DesiredPoolType"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("poolType");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "PoolType"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("cmtsip");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "Cmtsip"));
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
