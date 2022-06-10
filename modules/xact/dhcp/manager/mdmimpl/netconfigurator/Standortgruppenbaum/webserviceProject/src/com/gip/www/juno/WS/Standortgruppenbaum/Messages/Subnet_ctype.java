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
 * Subnet_ctype.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.WS.Standortgruppenbaum.Messages;

public class Subnet_ctype  implements java.io.Serializable {
    private java.lang.String subnetID;

    private java.lang.String mask;

    private java.lang.String sharedNetwork;

    private java.lang.String sharedNetworkID;

    private java.lang.String subnet;

    private java.lang.String fixedAttributes;

    private java.lang.String label;

    private com.gip.www.juno.WS.Standortgruppenbaum.Messages.Pool_ctype[] pool;

    private com.gip.www.juno.WS.Standortgruppenbaum.Messages.StaticHost_ctype[] staticHost;

    public Subnet_ctype() {
    }

    public Subnet_ctype(
           java.lang.String subnetID,
           java.lang.String mask,
           java.lang.String sharedNetwork,
           java.lang.String sharedNetworkID,
           java.lang.String subnet,
           java.lang.String fixedAttributes,
           java.lang.String label,
           com.gip.www.juno.WS.Standortgruppenbaum.Messages.Pool_ctype[] pool,
           com.gip.www.juno.WS.Standortgruppenbaum.Messages.StaticHost_ctype[] staticHost) {
           this.subnetID = subnetID;
           this.mask = mask;
           this.sharedNetwork = sharedNetwork;
           this.sharedNetworkID = sharedNetworkID;
           this.subnet = subnet;
           this.fixedAttributes = fixedAttributes;
           this.label = label;
           this.pool = pool;
           this.staticHost = staticHost;
    }


    /**
     * Gets the subnetID value for this Subnet_ctype.
     * 
     * @return subnetID
     */
    public java.lang.String getSubnetID() {
        return subnetID;
    }


    /**
     * Sets the subnetID value for this Subnet_ctype.
     * 
     * @param subnetID
     */
    public void setSubnetID(java.lang.String subnetID) {
        this.subnetID = subnetID;
    }


    /**
     * Gets the mask value for this Subnet_ctype.
     * 
     * @return mask
     */
    public java.lang.String getMask() {
        return mask;
    }


    /**
     * Sets the mask value for this Subnet_ctype.
     * 
     * @param mask
     */
    public void setMask(java.lang.String mask) {
        this.mask = mask;
    }


    /**
     * Gets the sharedNetwork value for this Subnet_ctype.
     * 
     * @return sharedNetwork
     */
    public java.lang.String getSharedNetwork() {
        return sharedNetwork;
    }


    /**
     * Sets the sharedNetwork value for this Subnet_ctype.
     * 
     * @param sharedNetwork
     */
    public void setSharedNetwork(java.lang.String sharedNetwork) {
        this.sharedNetwork = sharedNetwork;
    }


    /**
     * Gets the sharedNetworkID value for this Subnet_ctype.
     * 
     * @return sharedNetworkID
     */
    public java.lang.String getSharedNetworkID() {
        return sharedNetworkID;
    }


    /**
     * Sets the sharedNetworkID value for this Subnet_ctype.
     * 
     * @param sharedNetworkID
     */
    public void setSharedNetworkID(java.lang.String sharedNetworkID) {
        this.sharedNetworkID = sharedNetworkID;
    }


    /**
     * Gets the subnet value for this Subnet_ctype.
     * 
     * @return subnet
     */
    public java.lang.String getSubnet() {
        return subnet;
    }


    /**
     * Sets the subnet value for this Subnet_ctype.
     * 
     * @param subnet
     */
    public void setSubnet(java.lang.String subnet) {
        this.subnet = subnet;
    }


    /**
     * Gets the fixedAttributes value for this Subnet_ctype.
     * 
     * @return fixedAttributes
     */
    public java.lang.String getFixedAttributes() {
        return fixedAttributes;
    }


    /**
     * Sets the fixedAttributes value for this Subnet_ctype.
     * 
     * @param fixedAttributes
     */
    public void setFixedAttributes(java.lang.String fixedAttributes) {
        this.fixedAttributes = fixedAttributes;
    }


    /**
     * Gets the label value for this Subnet_ctype.
     * 
     * @return label
     */
    public java.lang.String getLabel() {
        return label;
    }


    /**
     * Sets the label value for this Subnet_ctype.
     * 
     * @param label
     */
    public void setLabel(java.lang.String label) {
        this.label = label;
    }


    /**
     * Gets the pool value for this Subnet_ctype.
     * 
     * @return pool
     */
    public com.gip.www.juno.WS.Standortgruppenbaum.Messages.Pool_ctype[] getPool() {
        return pool;
    }


    /**
     * Sets the pool value for this Subnet_ctype.
     * 
     * @param pool
     */
    public void setPool(com.gip.www.juno.WS.Standortgruppenbaum.Messages.Pool_ctype[] pool) {
        this.pool = pool;
    }

    public com.gip.www.juno.WS.Standortgruppenbaum.Messages.Pool_ctype getPool(int i) {
        return this.pool[i];
    }

    public void setPool(int i, com.gip.www.juno.WS.Standortgruppenbaum.Messages.Pool_ctype _value) {
        this.pool[i] = _value;
    }


    /**
     * Gets the staticHost value for this Subnet_ctype.
     * 
     * @return staticHost
     */
    public com.gip.www.juno.WS.Standortgruppenbaum.Messages.StaticHost_ctype[] getStaticHost() {
        return staticHost;
    }


    /**
     * Sets the staticHost value for this Subnet_ctype.
     * 
     * @param staticHost
     */
    public void setStaticHost(com.gip.www.juno.WS.Standortgruppenbaum.Messages.StaticHost_ctype[] staticHost) {
        this.staticHost = staticHost;
    }

    public com.gip.www.juno.WS.Standortgruppenbaum.Messages.StaticHost_ctype getStaticHost(int i) {
        return this.staticHost[i];
    }

    public void setStaticHost(int i, com.gip.www.juno.WS.Standortgruppenbaum.Messages.StaticHost_ctype _value) {
        this.staticHost[i] = _value;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof Subnet_ctype)) return false;
        Subnet_ctype other = (Subnet_ctype) obj;
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
            ((this.mask==null && other.getMask()==null) || 
             (this.mask!=null &&
              this.mask.equals(other.getMask()))) &&
            ((this.sharedNetwork==null && other.getSharedNetwork()==null) || 
             (this.sharedNetwork!=null &&
              this.sharedNetwork.equals(other.getSharedNetwork()))) &&
            ((this.sharedNetworkID==null && other.getSharedNetworkID()==null) || 
             (this.sharedNetworkID!=null &&
              this.sharedNetworkID.equals(other.getSharedNetworkID()))) &&
            ((this.subnet==null && other.getSubnet()==null) || 
             (this.subnet!=null &&
              this.subnet.equals(other.getSubnet()))) &&
            ((this.fixedAttributes==null && other.getFixedAttributes()==null) || 
             (this.fixedAttributes!=null &&
              this.fixedAttributes.equals(other.getFixedAttributes()))) &&
            ((this.label==null && other.getLabel()==null) || 
             (this.label!=null &&
              this.label.equals(other.getLabel()))) &&
            ((this.pool==null && other.getPool()==null) || 
             (this.pool!=null &&
              java.util.Arrays.equals(this.pool, other.getPool()))) &&
            ((this.staticHost==null && other.getStaticHost()==null) || 
             (this.staticHost!=null &&
              java.util.Arrays.equals(this.staticHost, other.getStaticHost())));
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
        if (getMask() != null) {
            _hashCode += getMask().hashCode();
        }
        if (getSharedNetwork() != null) {
            _hashCode += getSharedNetwork().hashCode();
        }
        if (getSharedNetworkID() != null) {
            _hashCode += getSharedNetworkID().hashCode();
        }
        if (getSubnet() != null) {
            _hashCode += getSubnet().hashCode();
        }
        if (getFixedAttributes() != null) {
            _hashCode += getFixedAttributes().hashCode();
        }
        if (getLabel() != null) {
            _hashCode += getLabel().hashCode();
        }
        if (getPool() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getPool());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getPool(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getStaticHost() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getStaticHost());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getStaticHost(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Subnet_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "subnet_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("subnetID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "SubnetID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("mask");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "Mask"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("sharedNetwork");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "SharedNetwork"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("sharedNetworkID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "SharedNetworkID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("subnet");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "Subnet"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("fixedAttributes");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "FixedAttributes"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("label");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "label"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("pool");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "pool"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "pool"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        elemField.setMaxOccursUnbounded(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("staticHost");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "staticHost"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "staticHost"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        elemField.setMaxOccursUnbounded(true);
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
