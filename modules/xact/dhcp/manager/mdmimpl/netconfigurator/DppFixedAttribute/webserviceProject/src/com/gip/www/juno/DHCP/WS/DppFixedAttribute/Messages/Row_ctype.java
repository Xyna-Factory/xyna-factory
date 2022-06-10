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

package com.gip.www.juno.DHCP.WS.DppFixedAttribute.Messages;

public class Row_ctype  implements java.io.Serializable {
    private java.lang.String dppFixedAttributeID;

    private java.lang.String name;

    private java.lang.String eth0;

    private java.lang.String eth1;

    private java.lang.String eth2;

    private java.lang.String eth3;

    private java.lang.String domainName;

    private java.lang.String failover;

    private java.lang.String eth1Peer;

    public Row_ctype() {
    }

    public Row_ctype(
           java.lang.String dppFixedAttributeID,
           java.lang.String name,
           java.lang.String eth0,
           java.lang.String eth1,
           java.lang.String eth2,
           java.lang.String eth3,
           java.lang.String domainName,
           java.lang.String failover,
           java.lang.String eth1Peer) {
           this.dppFixedAttributeID = dppFixedAttributeID;
           this.name = name;
           this.eth0 = eth0;
           this.eth1 = eth1;
           this.eth2 = eth2;
           this.eth3 = eth3;
           this.domainName = domainName;
           this.failover = failover;
           this.eth1Peer = eth1Peer;
    }


    /**
     * Gets the dppFixedAttributeID value for this Row_ctype.
     * 
     * @return dppFixedAttributeID
     */
    public java.lang.String getDppFixedAttributeID() {
        return dppFixedAttributeID;
    }


    /**
     * Sets the dppFixedAttributeID value for this Row_ctype.
     * 
     * @param dppFixedAttributeID
     */
    public void setDppFixedAttributeID(java.lang.String dppFixedAttributeID) {
        this.dppFixedAttributeID = dppFixedAttributeID;
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


    /**
     * Gets the eth0 value for this Row_ctype.
     * 
     * @return eth0
     */
    public java.lang.String getEth0() {
        return eth0;
    }


    /**
     * Sets the eth0 value for this Row_ctype.
     * 
     * @param eth0
     */
    public void setEth0(java.lang.String eth0) {
        this.eth0 = eth0;
    }


    /**
     * Gets the eth1 value for this Row_ctype.
     * 
     * @return eth1
     */
    public java.lang.String getEth1() {
        return eth1;
    }


    /**
     * Sets the eth1 value for this Row_ctype.
     * 
     * @param eth1
     */
    public void setEth1(java.lang.String eth1) {
        this.eth1 = eth1;
    }


    /**
     * Gets the eth2 value for this Row_ctype.
     * 
     * @return eth2
     */
    public java.lang.String getEth2() {
        return eth2;
    }


    /**
     * Sets the eth2 value for this Row_ctype.
     * 
     * @param eth2
     */
    public void setEth2(java.lang.String eth2) {
        this.eth2 = eth2;
    }


    /**
     * Gets the eth3 value for this Row_ctype.
     * 
     * @return eth3
     */
    public java.lang.String getEth3() {
        return eth3;
    }


    /**
     * Sets the eth3 value for this Row_ctype.
     * 
     * @param eth3
     */
    public void setEth3(java.lang.String eth3) {
        this.eth3 = eth3;
    }


    /**
     * Gets the domainName value for this Row_ctype.
     * 
     * @return domainName
     */
    public java.lang.String getDomainName() {
        return domainName;
    }


    /**
     * Sets the domainName value for this Row_ctype.
     * 
     * @param domainName
     */
    public void setDomainName(java.lang.String domainName) {
        this.domainName = domainName;
    }


    /**
     * Gets the failover value for this Row_ctype.
     * 
     * @return failover
     */
    public java.lang.String getFailover() {
        return failover;
    }


    /**
     * Sets the failover value for this Row_ctype.
     * 
     * @param failover
     */
    public void setFailover(java.lang.String failover) {
        this.failover = failover;
    }


    /**
     * Gets the eth1Peer value for this Row_ctype.
     * 
     * @return eth1Peer
     */
    public java.lang.String getEth1Peer() {
        return eth1Peer;
    }


    /**
     * Sets the eth1Peer value for this Row_ctype.
     * 
     * @param eth1Peer
     */
    public void setEth1Peer(java.lang.String eth1Peer) {
        this.eth1Peer = eth1Peer;
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
            ((this.dppFixedAttributeID==null && other.getDppFixedAttributeID()==null) || 
             (this.dppFixedAttributeID!=null &&
              this.dppFixedAttributeID.equals(other.getDppFixedAttributeID()))) &&
            ((this.name==null && other.getName()==null) || 
             (this.name!=null &&
              this.name.equals(other.getName()))) &&
            ((this.eth0==null && other.getEth0()==null) || 
             (this.eth0!=null &&
              this.eth0.equals(other.getEth0()))) &&
            ((this.eth1==null && other.getEth1()==null) || 
             (this.eth1!=null &&
              this.eth1.equals(other.getEth1()))) &&
            ((this.eth2==null && other.getEth2()==null) || 
             (this.eth2!=null &&
              this.eth2.equals(other.getEth2()))) &&
            ((this.eth3==null && other.getEth3()==null) || 
             (this.eth3!=null &&
              this.eth3.equals(other.getEth3()))) &&
            ((this.domainName==null && other.getDomainName()==null) || 
             (this.domainName!=null &&
              this.domainName.equals(other.getDomainName()))) &&
            ((this.failover==null && other.getFailover()==null) || 
             (this.failover!=null &&
              this.failover.equals(other.getFailover()))) &&
            ((this.eth1Peer==null && other.getEth1Peer()==null) || 
             (this.eth1Peer!=null &&
              this.eth1Peer.equals(other.getEth1Peer())));
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
        if (getDppFixedAttributeID() != null) {
            _hashCode += getDppFixedAttributeID().hashCode();
        }
        if (getName() != null) {
            _hashCode += getName().hashCode();
        }
        if (getEth0() != null) {
            _hashCode += getEth0().hashCode();
        }
        if (getEth1() != null) {
            _hashCode += getEth1().hashCode();
        }
        if (getEth2() != null) {
            _hashCode += getEth2().hashCode();
        }
        if (getEth3() != null) {
            _hashCode += getEth3().hashCode();
        }
        if (getDomainName() != null) {
            _hashCode += getDomainName().hashCode();
        }
        if (getFailover() != null) {
            _hashCode += getFailover().hashCode();
        }
        if (getEth1Peer() != null) {
            _hashCode += getEth1Peer().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Row_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/DppFixedAttribute/Messages", "Row_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("dppFixedAttributeID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/DppFixedAttribute/Messages", "DppFixedAttributeID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("name");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/DppFixedAttribute/Messages", "Name"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("eth0");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/DppFixedAttribute/Messages", "Eth0"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("eth1");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/DppFixedAttribute/Messages", "Eth1"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("eth2");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/DppFixedAttribute/Messages", "Eth2"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("eth3");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/DppFixedAttribute/Messages", "Eth3"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("domainName");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/DppFixedAttribute/Messages", "DomainName"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("failover");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/DppFixedAttribute/Messages", "Failover"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("eth1Peer");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/DppFixedAttribute/Messages", "Eth1peer"));
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
