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
 * Sharednetwork_ctype.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.WS.Standortgruppenbaum.Messages;

public class Sharednetwork_ctype  implements java.io.Serializable {
    private java.lang.String sharedNetworkID;

    private java.lang.String cpeDns;

    private java.lang.String sharedNetwork;

    private java.lang.String cpeDnsID;

    private java.lang.String standort;

    private java.lang.String standortID;

    private java.lang.String label;

    private java.lang.String standortGruppeID;

    private com.gip.www.juno.WS.Standortgruppenbaum.Messages.Subnet_ctype[] subnet;

    public Sharednetwork_ctype() {
    }

    public Sharednetwork_ctype(
           java.lang.String sharedNetworkID,
           java.lang.String cpeDns,
           java.lang.String sharedNetwork,
           java.lang.String cpeDnsID,
           java.lang.String standort,
           java.lang.String standortID,
           java.lang.String label,
           java.lang.String standortGruppeID,
           com.gip.www.juno.WS.Standortgruppenbaum.Messages.Subnet_ctype[] subnet) {
           this.sharedNetworkID = sharedNetworkID;
           this.cpeDns = cpeDns;
           this.sharedNetwork = sharedNetwork;
           this.cpeDnsID = cpeDnsID;
           this.standort = standort;
           this.standortID = standortID;
           this.label = label;
           this.standortGruppeID = standortGruppeID;
           this.subnet = subnet;
    }


    /**
     * Gets the sharedNetworkID value for this Sharednetwork_ctype.
     * 
     * @return sharedNetworkID
     */
    public java.lang.String getSharedNetworkID() {
        return sharedNetworkID;
    }


    /**
     * Sets the sharedNetworkID value for this Sharednetwork_ctype.
     * 
     * @param sharedNetworkID
     */
    public void setSharedNetworkID(java.lang.String sharedNetworkID) {
        this.sharedNetworkID = sharedNetworkID;
    }


    /**
     * Gets the cpeDns value for this Sharednetwork_ctype.
     * 
     * @return cpeDns
     */
    public java.lang.String getCpeDns() {
        return cpeDns;
    }


    /**
     * Sets the cpeDns value for this Sharednetwork_ctype.
     * 
     * @param cpeDns
     */
    public void setCpeDns(java.lang.String cpeDns) {
        this.cpeDns = cpeDns;
    }


    /**
     * Gets the sharedNetwork value for this Sharednetwork_ctype.
     * 
     * @return sharedNetwork
     */
    public java.lang.String getSharedNetwork() {
        return sharedNetwork;
    }


    /**
     * Sets the sharedNetwork value for this Sharednetwork_ctype.
     * 
     * @param sharedNetwork
     */
    public void setSharedNetwork(java.lang.String sharedNetwork) {
        this.sharedNetwork = sharedNetwork;
    }


    /**
     * Gets the cpeDnsID value for this Sharednetwork_ctype.
     * 
     * @return cpeDnsID
     */
    public java.lang.String getCpeDnsID() {
        return cpeDnsID;
    }


    /**
     * Sets the cpeDnsID value for this Sharednetwork_ctype.
     * 
     * @param cpeDnsID
     */
    public void setCpeDnsID(java.lang.String cpeDnsID) {
        this.cpeDnsID = cpeDnsID;
    }


    /**
     * Gets the standort value for this Sharednetwork_ctype.
     * 
     * @return standort
     */
    public java.lang.String getStandort() {
        return standort;
    }


    /**
     * Sets the standort value for this Sharednetwork_ctype.
     * 
     * @param standort
     */
    public void setStandort(java.lang.String standort) {
        this.standort = standort;
    }


    /**
     * Gets the standortID value for this Sharednetwork_ctype.
     * 
     * @return standortID
     */
    public java.lang.String getStandortID() {
        return standortID;
    }


    /**
     * Sets the standortID value for this Sharednetwork_ctype.
     * 
     * @param standortID
     */
    public void setStandortID(java.lang.String standortID) {
        this.standortID = standortID;
    }


    /**
     * Gets the label value for this Sharednetwork_ctype.
     * 
     * @return label
     */
    public java.lang.String getLabel() {
        return label;
    }


    /**
     * Sets the label value for this Sharednetwork_ctype.
     * 
     * @param label
     */
    public void setLabel(java.lang.String label) {
        this.label = label;
    }


    /**
     * Gets the standortGruppeID value for this Sharednetwork_ctype.
     * 
     * @return standortGruppeID
     */
    public java.lang.String getStandortGruppeID() {
        return standortGruppeID;
    }


    /**
     * Sets the standortGruppeID value for this Sharednetwork_ctype.
     * 
     * @param standortGruppeID
     */
    public void setStandortGruppeID(java.lang.String standortGruppeID) {
        this.standortGruppeID = standortGruppeID;
    }


    /**
     * Gets the subnet value for this Sharednetwork_ctype.
     * 
     * @return subnet
     */
    public com.gip.www.juno.WS.Standortgruppenbaum.Messages.Subnet_ctype[] getSubnet() {
        return subnet;
    }


    /**
     * Sets the subnet value for this Sharednetwork_ctype.
     * 
     * @param subnet
     */
    public void setSubnet(com.gip.www.juno.WS.Standortgruppenbaum.Messages.Subnet_ctype[] subnet) {
        this.subnet = subnet;
    }

    public com.gip.www.juno.WS.Standortgruppenbaum.Messages.Subnet_ctype getSubnet(int i) {
        return this.subnet[i];
    }

    public void setSubnet(int i, com.gip.www.juno.WS.Standortgruppenbaum.Messages.Subnet_ctype _value) {
        this.subnet[i] = _value;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof Sharednetwork_ctype)) return false;
        Sharednetwork_ctype other = (Sharednetwork_ctype) obj;
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
            ((this.cpeDns==null && other.getCpeDns()==null) || 
             (this.cpeDns!=null &&
              this.cpeDns.equals(other.getCpeDns()))) &&
            ((this.sharedNetwork==null && other.getSharedNetwork()==null) || 
             (this.sharedNetwork!=null &&
              this.sharedNetwork.equals(other.getSharedNetwork()))) &&
            ((this.cpeDnsID==null && other.getCpeDnsID()==null) || 
             (this.cpeDnsID!=null &&
              this.cpeDnsID.equals(other.getCpeDnsID()))) &&
            ((this.standort==null && other.getStandort()==null) || 
             (this.standort!=null &&
              this.standort.equals(other.getStandort()))) &&
            ((this.standortID==null && other.getStandortID()==null) || 
             (this.standortID!=null &&
              this.standortID.equals(other.getStandortID()))) &&
            ((this.label==null && other.getLabel()==null) || 
             (this.label!=null &&
              this.label.equals(other.getLabel()))) &&
            ((this.standortGruppeID==null && other.getStandortGruppeID()==null) || 
             (this.standortGruppeID!=null &&
              this.standortGruppeID.equals(other.getStandortGruppeID()))) &&
            ((this.subnet==null && other.getSubnet()==null) || 
             (this.subnet!=null &&
              java.util.Arrays.equals(this.subnet, other.getSubnet())));
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
        if (getCpeDns() != null) {
            _hashCode += getCpeDns().hashCode();
        }
        if (getSharedNetwork() != null) {
            _hashCode += getSharedNetwork().hashCode();
        }
        if (getCpeDnsID() != null) {
            _hashCode += getCpeDnsID().hashCode();
        }
        if (getStandort() != null) {
            _hashCode += getStandort().hashCode();
        }
        if (getStandortID() != null) {
            _hashCode += getStandortID().hashCode();
        }
        if (getLabel() != null) {
            _hashCode += getLabel().hashCode();
        }
        if (getStandortGruppeID() != null) {
            _hashCode += getStandortGruppeID().hashCode();
        }
        if (getSubnet() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getSubnet());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getSubnet(), i);
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
        new org.apache.axis.description.TypeDesc(Sharednetwork_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "sharednetwork_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("sharedNetworkID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "SharedNetworkID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("cpeDns");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "CpeDns"));
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
        elemField.setFieldName("cpeDnsID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "CpeDnsID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("standort");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "Standort"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("standortID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "StandortID"));
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
        elemField.setFieldName("standortGruppeID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "StandortGruppeID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("subnet");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "subnet"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "subnet"));
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
