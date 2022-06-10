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
 * GetTreeOutput.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.WS.Standortgruppenbaum.Messages;

public class GetTreeOutput  implements java.io.Serializable {
    private com.gip.www.juno.WS.Standortgruppenbaum.Messages.GetTreeOutputStandortGruppeSharedNetworkSubnet[][] standortGruppe;

    public GetTreeOutput() {
    }

    public GetTreeOutput(
           com.gip.www.juno.WS.Standortgruppenbaum.Messages.GetTreeOutputStandortGruppeSharedNetworkSubnet[][] standortGruppe) {
           this.standortGruppe = standortGruppe;
    }


    /**
     * Gets the standortGruppe value for this GetTreeOutput.
     * 
     * @return standortGruppe
     */
    public com.gip.www.juno.WS.Standortgruppenbaum.Messages.GetTreeOutputStandortGruppeSharedNetworkSubnet[][] getStandortGruppe() {
        return standortGruppe;
    }


    /**
     * Sets the standortGruppe value for this GetTreeOutput.
     * 
     * @param standortGruppe
     */
    public void setStandortGruppe(com.gip.www.juno.WS.Standortgruppenbaum.Messages.GetTreeOutputStandortGruppeSharedNetworkSubnet[][] standortGruppe) {
        this.standortGruppe = standortGruppe;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof GetTreeOutput)) return false;
        GetTreeOutput other = (GetTreeOutput) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.standortGruppe==null && other.getStandortGruppe()==null) || 
             (this.standortGruppe!=null &&
              java.util.Arrays.equals(this.standortGruppe, other.getStandortGruppe())));
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
        if (getStandortGruppe() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getStandortGruppe());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getStandortGruppe(), i);
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
        new org.apache.axis.description.TypeDesc(GetTreeOutput.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", ">GetTreeOutput"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("standortGruppe");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "standortGruppe"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", ">>>GetTreeOutput>standortGruppe>sharedNetwork"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        elemField.setItemQName(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "sharedNetwork"));
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
