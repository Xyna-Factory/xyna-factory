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
 * StandortGruppe_ctype.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.WS.Standortgruppenbaum.Messages;

public class StandortGruppe_ctype  implements java.io.Serializable {
    private java.lang.String standortGruppeID;

    private java.lang.String name;

    private java.lang.String label;

    private com.gip.www.juno.WS.Standortgruppenbaum.Messages.Sharednetwork_ctype[] sharedNetwork;

    public StandortGruppe_ctype() {
    }

    public StandortGruppe_ctype(
           java.lang.String standortGruppeID,
           java.lang.String name,
           java.lang.String label,
           com.gip.www.juno.WS.Standortgruppenbaum.Messages.Sharednetwork_ctype[] sharedNetwork) {
           this.standortGruppeID = standortGruppeID;
           this.name = name;
           this.label = label;
           this.sharedNetwork = sharedNetwork;
    }


    /**
     * Gets the standortGruppeID value for this StandortGruppe_ctype.
     * 
     * @return standortGruppeID
     */
    public java.lang.String getStandortGruppeID() {
        return standortGruppeID;
    }


    /**
     * Sets the standortGruppeID value for this StandortGruppe_ctype.
     * 
     * @param standortGruppeID
     */
    public void setStandortGruppeID(java.lang.String standortGruppeID) {
        this.standortGruppeID = standortGruppeID;
    }


    /**
     * Gets the name value for this StandortGruppe_ctype.
     * 
     * @return name
     */
    public java.lang.String getName() {
        return name;
    }


    /**
     * Sets the name value for this StandortGruppe_ctype.
     * 
     * @param name
     */
    public void setName(java.lang.String name) {
        this.name = name;
    }


    /**
     * Gets the label value for this StandortGruppe_ctype.
     * 
     * @return label
     */
    public java.lang.String getLabel() {
        return label;
    }


    /**
     * Sets the label value for this StandortGruppe_ctype.
     * 
     * @param label
     */
    public void setLabel(java.lang.String label) {
        this.label = label;
    }


    /**
     * Gets the sharedNetwork value for this StandortGruppe_ctype.
     * 
     * @return sharedNetwork
     */
    public com.gip.www.juno.WS.Standortgruppenbaum.Messages.Sharednetwork_ctype[] getSharedNetwork() {
        return sharedNetwork;
    }


    /**
     * Sets the sharedNetwork value for this StandortGruppe_ctype.
     * 
     * @param sharedNetwork
     */
    public void setSharedNetwork(com.gip.www.juno.WS.Standortgruppenbaum.Messages.Sharednetwork_ctype[] sharedNetwork) {
        this.sharedNetwork = sharedNetwork;
    }

    public com.gip.www.juno.WS.Standortgruppenbaum.Messages.Sharednetwork_ctype getSharedNetwork(int i) {
        return this.sharedNetwork[i];
    }

    public void setSharedNetwork(int i, com.gip.www.juno.WS.Standortgruppenbaum.Messages.Sharednetwork_ctype _value) {
        this.sharedNetwork[i] = _value;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof StandortGruppe_ctype)) return false;
        StandortGruppe_ctype other = (StandortGruppe_ctype) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.standortGruppeID==null && other.getStandortGruppeID()==null) || 
             (this.standortGruppeID!=null &&
              this.standortGruppeID.equals(other.getStandortGruppeID()))) &&
            ((this.name==null && other.getName()==null) || 
             (this.name!=null &&
              this.name.equals(other.getName()))) &&
            ((this.label==null && other.getLabel()==null) || 
             (this.label!=null &&
              this.label.equals(other.getLabel()))) &&
            ((this.sharedNetwork==null && other.getSharedNetwork()==null) || 
             (this.sharedNetwork!=null &&
              java.util.Arrays.equals(this.sharedNetwork, other.getSharedNetwork())));
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
        if (getStandortGruppeID() != null) {
            _hashCode += getStandortGruppeID().hashCode();
        }
        if (getName() != null) {
            _hashCode += getName().hashCode();
        }
        if (getLabel() != null) {
            _hashCode += getLabel().hashCode();
        }
        if (getSharedNetwork() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getSharedNetwork());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getSharedNetwork(), i);
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
        new org.apache.axis.description.TypeDesc(StandortGruppe_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "standortGruppe_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("standortGruppeID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "StandortGruppeID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("name");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "Name"));
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
        elemField.setFieldName("sharedNetwork");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "sharedNetwork"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "sharedNetwork"));
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
