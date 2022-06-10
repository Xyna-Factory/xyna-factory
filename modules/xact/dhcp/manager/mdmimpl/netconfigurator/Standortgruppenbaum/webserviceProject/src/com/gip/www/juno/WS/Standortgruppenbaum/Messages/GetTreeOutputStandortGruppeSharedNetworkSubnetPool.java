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
 * GetTreeOutputStandortGruppeSharedNetworkSubnetPool.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.WS.Standortgruppenbaum.Messages;

public class GetTreeOutputStandortGruppeSharedNetworkSubnetPool  implements java.io.Serializable {
    private java.lang.String subnetID;  // attribute

    private java.lang.String poolTypeID;  // attribute

    private java.lang.String poolID;  // attribute

    private java.lang.String rangeStop;  // attribute

    private java.lang.String rangeStart;  // attribute

    private java.lang.String subnet;  // attribute

    private java.lang.String poolType;  // attribute

    private java.lang.String label;  // attribute

    public GetTreeOutputStandortGruppeSharedNetworkSubnetPool() {
    }

    public GetTreeOutputStandortGruppeSharedNetworkSubnetPool(
           java.lang.String subnetID,
           java.lang.String poolTypeID,
           java.lang.String poolID,
           java.lang.String rangeStop,
           java.lang.String rangeStart,
           java.lang.String subnet,
           java.lang.String poolType,
           java.lang.String label) {
           this.subnetID = subnetID;
           this.poolTypeID = poolTypeID;
           this.poolID = poolID;
           this.rangeStop = rangeStop;
           this.rangeStart = rangeStart;
           this.subnet = subnet;
           this.poolType = poolType;
           this.label = label;
    }


    /**
     * Gets the subnetID value for this GetTreeOutputStandortGruppeSharedNetworkSubnetPool.
     * 
     * @return subnetID
     */
    public java.lang.String getSubnetID() {
        return subnetID;
    }


    /**
     * Sets the subnetID value for this GetTreeOutputStandortGruppeSharedNetworkSubnetPool.
     * 
     * @param subnetID
     */
    public void setSubnetID(java.lang.String subnetID) {
        this.subnetID = subnetID;
    }


    /**
     * Gets the poolTypeID value for this GetTreeOutputStandortGruppeSharedNetworkSubnetPool.
     * 
     * @return poolTypeID
     */
    public java.lang.String getPoolTypeID() {
        return poolTypeID;
    }


    /**
     * Sets the poolTypeID value for this GetTreeOutputStandortGruppeSharedNetworkSubnetPool.
     * 
     * @param poolTypeID
     */
    public void setPoolTypeID(java.lang.String poolTypeID) {
        this.poolTypeID = poolTypeID;
    }


    /**
     * Gets the poolID value for this GetTreeOutputStandortGruppeSharedNetworkSubnetPool.
     * 
     * @return poolID
     */
    public java.lang.String getPoolID() {
        return poolID;
    }


    /**
     * Sets the poolID value for this GetTreeOutputStandortGruppeSharedNetworkSubnetPool.
     * 
     * @param poolID
     */
    public void setPoolID(java.lang.String poolID) {
        this.poolID = poolID;
    }


    /**
     * Gets the rangeStop value for this GetTreeOutputStandortGruppeSharedNetworkSubnetPool.
     * 
     * @return rangeStop
     */
    public java.lang.String getRangeStop() {
        return rangeStop;
    }


    /**
     * Sets the rangeStop value for this GetTreeOutputStandortGruppeSharedNetworkSubnetPool.
     * 
     * @param rangeStop
     */
    public void setRangeStop(java.lang.String rangeStop) {
        this.rangeStop = rangeStop;
    }


    /**
     * Gets the rangeStart value for this GetTreeOutputStandortGruppeSharedNetworkSubnetPool.
     * 
     * @return rangeStart
     */
    public java.lang.String getRangeStart() {
        return rangeStart;
    }


    /**
     * Sets the rangeStart value for this GetTreeOutputStandortGruppeSharedNetworkSubnetPool.
     * 
     * @param rangeStart
     */
    public void setRangeStart(java.lang.String rangeStart) {
        this.rangeStart = rangeStart;
    }


    /**
     * Gets the subnet value for this GetTreeOutputStandortGruppeSharedNetworkSubnetPool.
     * 
     * @return subnet
     */
    public java.lang.String getSubnet() {
        return subnet;
    }


    /**
     * Sets the subnet value for this GetTreeOutputStandortGruppeSharedNetworkSubnetPool.
     * 
     * @param subnet
     */
    public void setSubnet(java.lang.String subnet) {
        this.subnet = subnet;
    }


    /**
     * Gets the poolType value for this GetTreeOutputStandortGruppeSharedNetworkSubnetPool.
     * 
     * @return poolType
     */
    public java.lang.String getPoolType() {
        return poolType;
    }


    /**
     * Sets the poolType value for this GetTreeOutputStandortGruppeSharedNetworkSubnetPool.
     * 
     * @param poolType
     */
    public void setPoolType(java.lang.String poolType) {
        this.poolType = poolType;
    }


    /**
     * Gets the label value for this GetTreeOutputStandortGruppeSharedNetworkSubnetPool.
     * 
     * @return label
     */
    public java.lang.String getLabel() {
        return label;
    }


    /**
     * Sets the label value for this GetTreeOutputStandortGruppeSharedNetworkSubnetPool.
     * 
     * @param label
     */
    public void setLabel(java.lang.String label) {
        this.label = label;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof GetTreeOutputStandortGruppeSharedNetworkSubnetPool)) return false;
        GetTreeOutputStandortGruppeSharedNetworkSubnetPool other = (GetTreeOutputStandortGruppeSharedNetworkSubnetPool) obj;
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
            ((this.poolTypeID==null && other.getPoolTypeID()==null) || 
             (this.poolTypeID!=null &&
              this.poolTypeID.equals(other.getPoolTypeID()))) &&
            ((this.poolID==null && other.getPoolID()==null) || 
             (this.poolID!=null &&
              this.poolID.equals(other.getPoolID()))) &&
            ((this.rangeStop==null && other.getRangeStop()==null) || 
             (this.rangeStop!=null &&
              this.rangeStop.equals(other.getRangeStop()))) &&
            ((this.rangeStart==null && other.getRangeStart()==null) || 
             (this.rangeStart!=null &&
              this.rangeStart.equals(other.getRangeStart()))) &&
            ((this.subnet==null && other.getSubnet()==null) || 
             (this.subnet!=null &&
              this.subnet.equals(other.getSubnet()))) &&
            ((this.poolType==null && other.getPoolType()==null) || 
             (this.poolType!=null &&
              this.poolType.equals(other.getPoolType()))) &&
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
        if (getSubnetID() != null) {
            _hashCode += getSubnetID().hashCode();
        }
        if (getPoolTypeID() != null) {
            _hashCode += getPoolTypeID().hashCode();
        }
        if (getPoolID() != null) {
            _hashCode += getPoolID().hashCode();
        }
        if (getRangeStop() != null) {
            _hashCode += getRangeStop().hashCode();
        }
        if (getRangeStart() != null) {
            _hashCode += getRangeStart().hashCode();
        }
        if (getSubnet() != null) {
            _hashCode += getSubnet().hashCode();
        }
        if (getPoolType() != null) {
            _hashCode += getPoolType().hashCode();
        }
        if (getLabel() != null) {
            _hashCode += getLabel().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(GetTreeOutputStandortGruppeSharedNetworkSubnetPool.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", ">>>>>GetTreeOutput>standortGruppe>sharedNetwork>subnet>pool"));
        org.apache.axis.description.AttributeDesc attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("subnetID");
        attrField.setXmlName(new javax.xml.namespace.QName("", "SubnetID"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("poolTypeID");
        attrField.setXmlName(new javax.xml.namespace.QName("", "PoolTypeID"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("poolID");
        attrField.setXmlName(new javax.xml.namespace.QName("", "PoolID"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("rangeStop");
        attrField.setXmlName(new javax.xml.namespace.QName("", "RangeStop"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("rangeStart");
        attrField.setXmlName(new javax.xml.namespace.QName("", "RangeStart"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("subnet");
        attrField.setXmlName(new javax.xml.namespace.QName("", "Subnet"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("poolType");
        attrField.setXmlName(new javax.xml.namespace.QName("", "PoolType"));
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
