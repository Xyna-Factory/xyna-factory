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
 * GetTreeOutputStandortGruppeSharedNetworkSubnet.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.WS.Standortgruppenbaum.Messages;

public class GetTreeOutputStandortGruppeSharedNetworkSubnet  implements java.io.Serializable {
    private com.gip.www.juno.WS.Standortgruppenbaum.Messages.GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost[] staticHost;

    private com.gip.www.juno.WS.Standortgruppenbaum.Messages.GetTreeOutputStandortGruppeSharedNetworkSubnetPool[] pool;

    private java.lang.String subnetID;  // attribute

    private java.lang.String mask;  // attribute

    private java.lang.String sharedNetwork;  // attribute

    private java.lang.String sharedNetworkID;  // attribute

    private java.lang.String subnet;  // attribute

    private java.lang.String fixedAttributes;  // attribute

    private java.lang.String label;  // attribute

    public GetTreeOutputStandortGruppeSharedNetworkSubnet() {
    }

    public GetTreeOutputStandortGruppeSharedNetworkSubnet(
           com.gip.www.juno.WS.Standortgruppenbaum.Messages.GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost[] staticHost,
           com.gip.www.juno.WS.Standortgruppenbaum.Messages.GetTreeOutputStandortGruppeSharedNetworkSubnetPool[] pool,
           java.lang.String subnetID,
           java.lang.String mask,
           java.lang.String sharedNetwork,
           java.lang.String sharedNetworkID,
           java.lang.String subnet,
           java.lang.String fixedAttributes,
           java.lang.String label) {
           this.staticHost = staticHost;
           this.pool = pool;
           this.subnetID = subnetID;
           this.mask = mask;
           this.sharedNetwork = sharedNetwork;
           this.sharedNetworkID = sharedNetworkID;
           this.subnet = subnet;
           this.fixedAttributes = fixedAttributes;
           this.label = label;
    }


    /**
     * Gets the staticHost value for this GetTreeOutputStandortGruppeSharedNetworkSubnet.
     * 
     * @return staticHost
     */
    public com.gip.www.juno.WS.Standortgruppenbaum.Messages.GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost[] getStaticHost() {
        return staticHost;
    }


    /**
     * Sets the staticHost value for this GetTreeOutputStandortGruppeSharedNetworkSubnet.
     * 
     * @param staticHost
     */
    public void setStaticHost(com.gip.www.juno.WS.Standortgruppenbaum.Messages.GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost[] staticHost) {
        this.staticHost = staticHost;
    }

    public com.gip.www.juno.WS.Standortgruppenbaum.Messages.GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost getStaticHost(int i) {
        return this.staticHost[i];
    }

    public void setStaticHost(int i, com.gip.www.juno.WS.Standortgruppenbaum.Messages.GetTreeOutputStandortGruppeSharedNetworkSubnetStaticHost _value) {
        this.staticHost[i] = _value;
    }


    /**
     * Gets the pool value for this GetTreeOutputStandortGruppeSharedNetworkSubnet.
     * 
     * @return pool
     */
    public com.gip.www.juno.WS.Standortgruppenbaum.Messages.GetTreeOutputStandortGruppeSharedNetworkSubnetPool[] getPool() {
        return pool;
    }


    /**
     * Sets the pool value for this GetTreeOutputStandortGruppeSharedNetworkSubnet.
     * 
     * @param pool
     */
    public void setPool(com.gip.www.juno.WS.Standortgruppenbaum.Messages.GetTreeOutputStandortGruppeSharedNetworkSubnetPool[] pool) {
        this.pool = pool;
    }

    public com.gip.www.juno.WS.Standortgruppenbaum.Messages.GetTreeOutputStandortGruppeSharedNetworkSubnetPool getPool(int i) {
        return this.pool[i];
    }

    public void setPool(int i, com.gip.www.juno.WS.Standortgruppenbaum.Messages.GetTreeOutputStandortGruppeSharedNetworkSubnetPool _value) {
        this.pool[i] = _value;
    }


    /**
     * Gets the subnetID value for this GetTreeOutputStandortGruppeSharedNetworkSubnet.
     * 
     * @return subnetID
     */
    public java.lang.String getSubnetID() {
        return subnetID;
    }


    /**
     * Sets the subnetID value for this GetTreeOutputStandortGruppeSharedNetworkSubnet.
     * 
     * @param subnetID
     */
    public void setSubnetID(java.lang.String subnetID) {
        this.subnetID = subnetID;
    }


    /**
     * Gets the mask value for this GetTreeOutputStandortGruppeSharedNetworkSubnet.
     * 
     * @return mask
     */
    public java.lang.String getMask() {
        return mask;
    }


    /**
     * Sets the mask value for this GetTreeOutputStandortGruppeSharedNetworkSubnet.
     * 
     * @param mask
     */
    public void setMask(java.lang.String mask) {
        this.mask = mask;
    }


    /**
     * Gets the sharedNetwork value for this GetTreeOutputStandortGruppeSharedNetworkSubnet.
     * 
     * @return sharedNetwork
     */
    public java.lang.String getSharedNetwork() {
        return sharedNetwork;
    }


    /**
     * Sets the sharedNetwork value for this GetTreeOutputStandortGruppeSharedNetworkSubnet.
     * 
     * @param sharedNetwork
     */
    public void setSharedNetwork(java.lang.String sharedNetwork) {
        this.sharedNetwork = sharedNetwork;
    }


    /**
     * Gets the sharedNetworkID value for this GetTreeOutputStandortGruppeSharedNetworkSubnet.
     * 
     * @return sharedNetworkID
     */
    public java.lang.String getSharedNetworkID() {
        return sharedNetworkID;
    }


    /**
     * Sets the sharedNetworkID value for this GetTreeOutputStandortGruppeSharedNetworkSubnet.
     * 
     * @param sharedNetworkID
     */
    public void setSharedNetworkID(java.lang.String sharedNetworkID) {
        this.sharedNetworkID = sharedNetworkID;
    }


    /**
     * Gets the subnet value for this GetTreeOutputStandortGruppeSharedNetworkSubnet.
     * 
     * @return subnet
     */
    public java.lang.String getSubnet() {
        return subnet;
    }


    /**
     * Sets the subnet value for this GetTreeOutputStandortGruppeSharedNetworkSubnet.
     * 
     * @param subnet
     */
    public void setSubnet(java.lang.String subnet) {
        this.subnet = subnet;
    }


    /**
     * Gets the fixedAttributes value for this GetTreeOutputStandortGruppeSharedNetworkSubnet.
     * 
     * @return fixedAttributes
     */
    public java.lang.String getFixedAttributes() {
        return fixedAttributes;
    }


    /**
     * Sets the fixedAttributes value for this GetTreeOutputStandortGruppeSharedNetworkSubnet.
     * 
     * @param fixedAttributes
     */
    public void setFixedAttributes(java.lang.String fixedAttributes) {
        this.fixedAttributes = fixedAttributes;
    }


    /**
     * Gets the label value for this GetTreeOutputStandortGruppeSharedNetworkSubnet.
     * 
     * @return label
     */
    public java.lang.String getLabel() {
        return label;
    }


    /**
     * Sets the label value for this GetTreeOutputStandortGruppeSharedNetworkSubnet.
     * 
     * @param label
     */
    public void setLabel(java.lang.String label) {
        this.label = label;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof GetTreeOutputStandortGruppeSharedNetworkSubnet)) return false;
        GetTreeOutputStandortGruppeSharedNetworkSubnet other = (GetTreeOutputStandortGruppeSharedNetworkSubnet) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.staticHost==null && other.getStaticHost()==null) || 
             (this.staticHost!=null &&
              java.util.Arrays.equals(this.staticHost, other.getStaticHost()))) &&
            ((this.pool==null && other.getPool()==null) || 
             (this.pool!=null &&
              java.util.Arrays.equals(this.pool, other.getPool()))) &&
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
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(GetTreeOutputStandortGruppeSharedNetworkSubnet.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", ">>>>GetTreeOutput>standortGruppe>sharedNetwork>subnet"));
        org.apache.axis.description.AttributeDesc attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("subnetID");
        attrField.setXmlName(new javax.xml.namespace.QName("", "SubnetID"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("mask");
        attrField.setXmlName(new javax.xml.namespace.QName("", "Mask"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("sharedNetwork");
        attrField.setXmlName(new javax.xml.namespace.QName("", "SharedNetwork"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("sharedNetworkID");
        attrField.setXmlName(new javax.xml.namespace.QName("", "SharedNetworkID"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("subnet");
        attrField.setXmlName(new javax.xml.namespace.QName("", "Subnet"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("fixedAttributes");
        attrField.setXmlName(new javax.xml.namespace.QName("", "FixedAttributes"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("label");
        attrField.setXmlName(new javax.xml.namespace.QName("", "label"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("staticHost");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "staticHost"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", ">>>>>GetTreeOutput>standortGruppe>sharedNetwork>subnet>staticHost"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        elemField.setMaxOccursUnbounded(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("pool");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "pool"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", ">>>>>GetTreeOutput>standortGruppe>sharedNetwork>subnet>pool"));
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
