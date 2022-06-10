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
 * GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.WS.Standortgruppenbaum.Messages;

public class GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost  implements java.io.Serializable {
    private java.lang.String staticHostID;  // attribute

    private java.lang.String cpe_mac;  // attribute

    private java.lang.String dns;  // attribute

    private java.lang.String remoteId;  // attribute

    private java.lang.String deployed2;  // attribute

    private java.lang.String deployed1;  // attribute

    private java.lang.String subnetID;  // attribute

    private java.lang.String subnet;  // attribute

    private java.lang.String ip;  // attribute

    private java.lang.String hostname;  // attribute

    private java.lang.String configDescr;  // attribute

    private java.lang.String label;  // attribute

    public GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost() {
    }

    public GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost(
           java.lang.String staticHostID,
           java.lang.String cpe_mac,
           java.lang.String dns,
           java.lang.String remoteId,
           java.lang.String deployed2,
           java.lang.String deployed1,
           java.lang.String subnetID,
           java.lang.String subnet,
           java.lang.String ip,
           java.lang.String hostname,
           java.lang.String configDescr,
           java.lang.String label) {
           this.staticHostID = staticHostID;
           this.cpe_mac = cpe_mac;
           this.dns = dns;
           this.remoteId = remoteId;
           this.deployed2 = deployed2;
           this.deployed1 = deployed1;
           this.subnetID = subnetID;
           this.subnet = subnet;
           this.ip = ip;
           this.hostname = hostname;
           this.configDescr = configDescr;
           this.label = label;
    }


    /**
     * Gets the staticHostID value for this GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost.
     * 
     * @return staticHostID
     */
    public java.lang.String getStaticHostID() {
        return staticHostID;
    }


    /**
     * Sets the staticHostID value for this GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost.
     * 
     * @param staticHostID
     */
    public void setStaticHostID(java.lang.String staticHostID) {
        this.staticHostID = staticHostID;
    }


    /**
     * Gets the cpe_mac value for this GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost.
     * 
     * @return cpe_mac
     */
    public java.lang.String getCpe_mac() {
        return cpe_mac;
    }


    /**
     * Sets the cpe_mac value for this GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost.
     * 
     * @param cpe_mac
     */
    public void setCpe_mac(java.lang.String cpe_mac) {
        this.cpe_mac = cpe_mac;
    }


    /**
     * Gets the dns value for this GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost.
     * 
     * @return dns
     */
    public java.lang.String getDns() {
        return dns;
    }


    /**
     * Sets the dns value for this GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost.
     * 
     * @param dns
     */
    public void setDns(java.lang.String dns) {
        this.dns = dns;
    }


    /**
     * Gets the remoteId value for this GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost.
     * 
     * @return remoteId
     */
    public java.lang.String getRemoteId() {
        return remoteId;
    }


    /**
     * Sets the remoteId value for this GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost.
     * 
     * @param remoteId
     */
    public void setRemoteId(java.lang.String remoteId) {
        this.remoteId = remoteId;
    }


    /**
     * Gets the deployed2 value for this GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost.
     * 
     * @return deployed2
     */
    public java.lang.String getDeployed2() {
        return deployed2;
    }


    /**
     * Sets the deployed2 value for this GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost.
     * 
     * @param deployed2
     */
    public void setDeployed2(java.lang.String deployed2) {
        this.deployed2 = deployed2;
    }


    /**
     * Gets the deployed1 value for this GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost.
     * 
     * @return deployed1
     */
    public java.lang.String getDeployed1() {
        return deployed1;
    }


    /**
     * Sets the deployed1 value for this GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost.
     * 
     * @param deployed1
     */
    public void setDeployed1(java.lang.String deployed1) {
        this.deployed1 = deployed1;
    }


    /**
     * Gets the subnetID value for this GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost.
     * 
     * @return subnetID
     */
    public java.lang.String getSubnetID() {
        return subnetID;
    }


    /**
     * Sets the subnetID value for this GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost.
     * 
     * @param subnetID
     */
    public void setSubnetID(java.lang.String subnetID) {
        this.subnetID = subnetID;
    }


    /**
     * Gets the subnet value for this GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost.
     * 
     * @return subnet
     */
    public java.lang.String getSubnet() {
        return subnet;
    }


    /**
     * Sets the subnet value for this GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost.
     * 
     * @param subnet
     */
    public void setSubnet(java.lang.String subnet) {
        this.subnet = subnet;
    }


    /**
     * Gets the ip value for this GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost.
     * 
     * @return ip
     */
    public java.lang.String getIp() {
        return ip;
    }


    /**
     * Sets the ip value for this GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost.
     * 
     * @param ip
     */
    public void setIp(java.lang.String ip) {
        this.ip = ip;
    }


    /**
     * Gets the hostname value for this GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost.
     * 
     * @return hostname
     */
    public java.lang.String getHostname() {
        return hostname;
    }


    /**
     * Sets the hostname value for this GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost.
     * 
     * @param hostname
     */
    public void setHostname(java.lang.String hostname) {
        this.hostname = hostname;
    }


    /**
     * Gets the configDescr value for this GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost.
     * 
     * @return configDescr
     */
    public java.lang.String getConfigDescr() {
        return configDescr;
    }


    /**
     * Sets the configDescr value for this GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost.
     * 
     * @param configDescr
     */
    public void setConfigDescr(java.lang.String configDescr) {
        this.configDescr = configDescr;
    }


    /**
     * Gets the label value for this GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost.
     * 
     * @return label
     */
    public java.lang.String getLabel() {
        return label;
    }


    /**
     * Sets the label value for this GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost.
     * 
     * @param label
     */
    public void setLabel(java.lang.String label) {
        this.label = label;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost)) return false;
        GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost other = (GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost) obj;
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
            ((this.cpe_mac==null && other.getCpe_mac()==null) || 
             (this.cpe_mac!=null &&
              this.cpe_mac.equals(other.getCpe_mac()))) &&
            ((this.dns==null && other.getDns()==null) || 
             (this.dns!=null &&
              this.dns.equals(other.getDns()))) &&
            ((this.remoteId==null && other.getRemoteId()==null) || 
             (this.remoteId!=null &&
              this.remoteId.equals(other.getRemoteId()))) &&
            ((this.deployed2==null && other.getDeployed2()==null) || 
             (this.deployed2!=null &&
              this.deployed2.equals(other.getDeployed2()))) &&
            ((this.deployed1==null && other.getDeployed1()==null) || 
             (this.deployed1!=null &&
              this.deployed1.equals(other.getDeployed1()))) &&
            ((this.subnetID==null && other.getSubnetID()==null) || 
             (this.subnetID!=null &&
              this.subnetID.equals(other.getSubnetID()))) &&
            ((this.subnet==null && other.getSubnet()==null) || 
             (this.subnet!=null &&
              this.subnet.equals(other.getSubnet()))) &&
            ((this.ip==null && other.getIp()==null) || 
             (this.ip!=null &&
              this.ip.equals(other.getIp()))) &&
            ((this.hostname==null && other.getHostname()==null) || 
             (this.hostname!=null &&
              this.hostname.equals(other.getHostname()))) &&
            ((this.configDescr==null && other.getConfigDescr()==null) || 
             (this.configDescr!=null &&
              this.configDescr.equals(other.getConfigDescr()))) &&
            ((this.label==null && other.getLabel()==null) || 
             (this.label!=null &&
              this.label.equals(other.getLabel())));
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
        if (getCpe_mac() != null) {
            _hashCode += getCpe_mac().hashCode();
        }
        if (getDns() != null) {
            _hashCode += getDns().hashCode();
        }
        if (getRemoteId() != null) {
            _hashCode += getRemoteId().hashCode();
        }
        if (getDeployed2() != null) {
            _hashCode += getDeployed2().hashCode();
        }
        if (getDeployed1() != null) {
            _hashCode += getDeployed1().hashCode();
        }
        if (getSubnetID() != null) {
            _hashCode += getSubnetID().hashCode();
        }
        if (getSubnet() != null) {
            _hashCode += getSubnet().hashCode();
        }
        if (getIp() != null) {
            _hashCode += getIp().hashCode();
        }
        if (getHostname() != null) {
            _hashCode += getHostname().hashCode();
        }
        if (getConfigDescr() != null) {
            _hashCode += getConfigDescr().hashCode();
        }
        if (getLabel() != null) {
            _hashCode += getLabel().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", ">>>>>GetTreeOutput>standortGruppe>sharedNetwork>subnet>staticHost"));
        org.apache.axis.description.AttributeDesc attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("staticHostID");
        attrField.setXmlName(new javax.xml.namespace.QName("", "StaticHostID"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("cpe_mac");
        attrField.setXmlName(new javax.xml.namespace.QName("", "Cpe_mac"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("dns");
        attrField.setXmlName(new javax.xml.namespace.QName("", "Dns"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("remoteId");
        attrField.setXmlName(new javax.xml.namespace.QName("", "RemoteId"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("deployed2");
        attrField.setXmlName(new javax.xml.namespace.QName("", "Deployed2"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("deployed1");
        attrField.setXmlName(new javax.xml.namespace.QName("", "Deployed1"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("subnetID");
        attrField.setXmlName(new javax.xml.namespace.QName("", "SubnetID"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("subnet");
        attrField.setXmlName(new javax.xml.namespace.QName("", "Subnet"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("ip");
        attrField.setXmlName(new javax.xml.namespace.QName("", "Ip"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("hostname");
        attrField.setXmlName(new javax.xml.namespace.QName("", "Hostname"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("configDescr");
        attrField.setXmlName(new javax.xml.namespace.QName("", "ConfigDescr"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("label");
        attrField.setXmlName(new javax.xml.namespace.QName("", "label"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
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
