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
 * GenerateTlvFromTemplateForIsdnMtaInput_ctype.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.Gui.WS.Messages;

public class GenerateTlvFromTemplateForIsdnMtaInput_ctype  implements java.io.Serializable {
    private com.gip.www.juno.Gui.WS.Messages.ConfigFileGeneratorParameters_ctype configFileGeneratorParameters;

    private com.gip.www.juno.Gui.WS.Messages.MtaRequest_ctype mtaRequest;

    private com.gip.www.juno.Gui.WS.Messages.IsdnMta_ctype isdnMta;

    public GenerateTlvFromTemplateForIsdnMtaInput_ctype() {
    }

    public GenerateTlvFromTemplateForIsdnMtaInput_ctype(
           com.gip.www.juno.Gui.WS.Messages.ConfigFileGeneratorParameters_ctype configFileGeneratorParameters,
           com.gip.www.juno.Gui.WS.Messages.MtaRequest_ctype mtaRequest,
           com.gip.www.juno.Gui.WS.Messages.IsdnMta_ctype isdnMta) {
           this.configFileGeneratorParameters = configFileGeneratorParameters;
           this.mtaRequest = mtaRequest;
           this.isdnMta = isdnMta;
    }


    /**
     * Gets the configFileGeneratorParameters value for this GenerateTlvFromTemplateForIsdnMtaInput_ctype.
     * 
     * @return configFileGeneratorParameters
     */
    public com.gip.www.juno.Gui.WS.Messages.ConfigFileGeneratorParameters_ctype getConfigFileGeneratorParameters() {
        return configFileGeneratorParameters;
    }


    /**
     * Sets the configFileGeneratorParameters value for this GenerateTlvFromTemplateForIsdnMtaInput_ctype.
     * 
     * @param configFileGeneratorParameters
     */
    public void setConfigFileGeneratorParameters(com.gip.www.juno.Gui.WS.Messages.ConfigFileGeneratorParameters_ctype configFileGeneratorParameters) {
        this.configFileGeneratorParameters = configFileGeneratorParameters;
    }


    /**
     * Gets the mtaRequest value for this GenerateTlvFromTemplateForIsdnMtaInput_ctype.
     * 
     * @return mtaRequest
     */
    public com.gip.www.juno.Gui.WS.Messages.MtaRequest_ctype getMtaRequest() {
        return mtaRequest;
    }


    /**
     * Sets the mtaRequest value for this GenerateTlvFromTemplateForIsdnMtaInput_ctype.
     * 
     * @param mtaRequest
     */
    public void setMtaRequest(com.gip.www.juno.Gui.WS.Messages.MtaRequest_ctype mtaRequest) {
        this.mtaRequest = mtaRequest;
    }


    /**
     * Gets the isdnMta value for this GenerateTlvFromTemplateForIsdnMtaInput_ctype.
     * 
     * @return isdnMta
     */
    public com.gip.www.juno.Gui.WS.Messages.IsdnMta_ctype getIsdnMta() {
        return isdnMta;
    }


    /**
     * Sets the isdnMta value for this GenerateTlvFromTemplateForIsdnMtaInput_ctype.
     * 
     * @param isdnMta
     */
    public void setIsdnMta(com.gip.www.juno.Gui.WS.Messages.IsdnMta_ctype isdnMta) {
        this.isdnMta = isdnMta;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof GenerateTlvFromTemplateForIsdnMtaInput_ctype)) return false;
        GenerateTlvFromTemplateForIsdnMtaInput_ctype other = (GenerateTlvFromTemplateForIsdnMtaInput_ctype) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.configFileGeneratorParameters==null && other.getConfigFileGeneratorParameters()==null) || 
             (this.configFileGeneratorParameters!=null &&
              this.configFileGeneratorParameters.equals(other.getConfigFileGeneratorParameters()))) &&
            ((this.mtaRequest==null && other.getMtaRequest()==null) || 
             (this.mtaRequest!=null &&
              this.mtaRequest.equals(other.getMtaRequest()))) &&
            ((this.isdnMta==null && other.getIsdnMta()==null) || 
             (this.isdnMta!=null &&
              this.isdnMta.equals(other.getIsdnMta())));
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
        if (getConfigFileGeneratorParameters() != null) {
            _hashCode += getConfigFileGeneratorParameters().hashCode();
        }
        if (getMtaRequest() != null) {
            _hashCode += getMtaRequest().hashCode();
        }
        if (getIsdnMta() != null) {
            _hashCode += getIsdnMta().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(GenerateTlvFromTemplateForIsdnMtaInput_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForIsdnMtaInput_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("configFileGeneratorParameters");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ConfigFileGeneratorParameters"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ConfigFileGeneratorParameters_ctype"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("mtaRequest");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "MtaRequest"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "MtaRequest_ctype"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("isdnMta");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "IsdnMta"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "IsdnMta_ctype"));
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
