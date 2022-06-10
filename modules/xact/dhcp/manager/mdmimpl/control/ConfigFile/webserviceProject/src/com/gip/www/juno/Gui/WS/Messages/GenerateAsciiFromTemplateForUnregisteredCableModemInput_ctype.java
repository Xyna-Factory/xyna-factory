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
 * GenerateAsciiFromTemplateForUnregisteredCableModemInput_ctype.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.Gui.WS.Messages;

public class GenerateAsciiFromTemplateForUnregisteredCableModemInput_ctype  implements java.io.Serializable {
    private com.gip.www.juno.Gui.WS.Messages.TextConfigGeneratorParameters_ctype textConfigGeneratorParameters;

    private com.gip.www.juno.Gui.WS.Messages.CableModemRequest_ctype cableModemRequest;

    private com.gip.www.juno.Gui.WS.Messages.UnregisteredCableModem_ctype unregisteredCableModem;

    public GenerateAsciiFromTemplateForUnregisteredCableModemInput_ctype() {
    }

    public GenerateAsciiFromTemplateForUnregisteredCableModemInput_ctype(
           com.gip.www.juno.Gui.WS.Messages.TextConfigGeneratorParameters_ctype textConfigGeneratorParameters,
           com.gip.www.juno.Gui.WS.Messages.CableModemRequest_ctype cableModemRequest,
           com.gip.www.juno.Gui.WS.Messages.UnregisteredCableModem_ctype unregisteredCableModem) {
           this.textConfigGeneratorParameters = textConfigGeneratorParameters;
           this.cableModemRequest = cableModemRequest;
           this.unregisteredCableModem = unregisteredCableModem;
    }


    /**
     * Gets the textConfigGeneratorParameters value for this GenerateAsciiFromTemplateForUnregisteredCableModemInput_ctype.
     * 
     * @return textConfigGeneratorParameters
     */
    public com.gip.www.juno.Gui.WS.Messages.TextConfigGeneratorParameters_ctype getTextConfigGeneratorParameters() {
        return textConfigGeneratorParameters;
    }


    /**
     * Sets the textConfigGeneratorParameters value for this GenerateAsciiFromTemplateForUnregisteredCableModemInput_ctype.
     * 
     * @param textConfigGeneratorParameters
     */
    public void setTextConfigGeneratorParameters(com.gip.www.juno.Gui.WS.Messages.TextConfigGeneratorParameters_ctype textConfigGeneratorParameters) {
        this.textConfigGeneratorParameters = textConfigGeneratorParameters;
    }


    /**
     * Gets the cableModemRequest value for this GenerateAsciiFromTemplateForUnregisteredCableModemInput_ctype.
     * 
     * @return cableModemRequest
     */
    public com.gip.www.juno.Gui.WS.Messages.CableModemRequest_ctype getCableModemRequest() {
        return cableModemRequest;
    }


    /**
     * Sets the cableModemRequest value for this GenerateAsciiFromTemplateForUnregisteredCableModemInput_ctype.
     * 
     * @param cableModemRequest
     */
    public void setCableModemRequest(com.gip.www.juno.Gui.WS.Messages.CableModemRequest_ctype cableModemRequest) {
        this.cableModemRequest = cableModemRequest;
    }


    /**
     * Gets the unregisteredCableModem value for this GenerateAsciiFromTemplateForUnregisteredCableModemInput_ctype.
     * 
     * @return unregisteredCableModem
     */
    public com.gip.www.juno.Gui.WS.Messages.UnregisteredCableModem_ctype getUnregisteredCableModem() {
        return unregisteredCableModem;
    }


    /**
     * Sets the unregisteredCableModem value for this GenerateAsciiFromTemplateForUnregisteredCableModemInput_ctype.
     * 
     * @param unregisteredCableModem
     */
    public void setUnregisteredCableModem(com.gip.www.juno.Gui.WS.Messages.UnregisteredCableModem_ctype unregisteredCableModem) {
        this.unregisteredCableModem = unregisteredCableModem;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof GenerateAsciiFromTemplateForUnregisteredCableModemInput_ctype)) return false;
        GenerateAsciiFromTemplateForUnregisteredCableModemInput_ctype other = (GenerateAsciiFromTemplateForUnregisteredCableModemInput_ctype) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.textConfigGeneratorParameters==null && other.getTextConfigGeneratorParameters()==null) || 
             (this.textConfigGeneratorParameters!=null &&
              this.textConfigGeneratorParameters.equals(other.getTextConfigGeneratorParameters()))) &&
            ((this.cableModemRequest==null && other.getCableModemRequest()==null) || 
             (this.cableModemRequest!=null &&
              this.cableModemRequest.equals(other.getCableModemRequest()))) &&
            ((this.unregisteredCableModem==null && other.getUnregisteredCableModem()==null) || 
             (this.unregisteredCableModem!=null &&
              this.unregisteredCableModem.equals(other.getUnregisteredCableModem())));
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
        if (getTextConfigGeneratorParameters() != null) {
            _hashCode += getTextConfigGeneratorParameters().hashCode();
        }
        if (getCableModemRequest() != null) {
            _hashCode += getCableModemRequest().hashCode();
        }
        if (getUnregisteredCableModem() != null) {
            _hashCode += getUnregisteredCableModem().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(GenerateAsciiFromTemplateForUnregisteredCableModemInput_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForUnregisteredCableModemInput_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("textConfigGeneratorParameters");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TextConfigGeneratorParameters"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TextConfigGeneratorParameters_ctype"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("cableModemRequest");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CableModemRequest"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CableModemRequest_ctype"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("unregisteredCableModem");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UnregisteredCableModem"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UnregisteredCableModem_ctype"));
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
