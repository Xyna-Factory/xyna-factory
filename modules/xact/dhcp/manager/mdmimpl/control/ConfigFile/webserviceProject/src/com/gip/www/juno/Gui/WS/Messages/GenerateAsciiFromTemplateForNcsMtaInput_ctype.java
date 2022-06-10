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
 * GenerateAsciiFromTemplateForNcsMtaInput_ctype.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.Gui.WS.Messages;

public class GenerateAsciiFromTemplateForNcsMtaInput_ctype  implements java.io.Serializable {
    private com.gip.www.juno.Gui.WS.Messages.TextConfigGeneratorParameters_ctype textConfigGeneratorParameters;

    private com.gip.www.juno.Gui.WS.Messages.MtaRequest_ctype mtaRequest;

    private com.gip.www.juno.Gui.WS.Messages.NcsMta_ctype ncsMta;

    public GenerateAsciiFromTemplateForNcsMtaInput_ctype() {
    }

    public GenerateAsciiFromTemplateForNcsMtaInput_ctype(
           com.gip.www.juno.Gui.WS.Messages.TextConfigGeneratorParameters_ctype textConfigGeneratorParameters,
           com.gip.www.juno.Gui.WS.Messages.MtaRequest_ctype mtaRequest,
           com.gip.www.juno.Gui.WS.Messages.NcsMta_ctype ncsMta) {
           this.textConfigGeneratorParameters = textConfigGeneratorParameters;
           this.mtaRequest = mtaRequest;
           this.ncsMta = ncsMta;
    }


    /**
     * Gets the textConfigGeneratorParameters value for this GenerateAsciiFromTemplateForNcsMtaInput_ctype.
     * 
     * @return textConfigGeneratorParameters
     */
    public com.gip.www.juno.Gui.WS.Messages.TextConfigGeneratorParameters_ctype getTextConfigGeneratorParameters() {
        return textConfigGeneratorParameters;
    }


    /**
     * Sets the textConfigGeneratorParameters value for this GenerateAsciiFromTemplateForNcsMtaInput_ctype.
     * 
     * @param textConfigGeneratorParameters
     */
    public void setTextConfigGeneratorParameters(com.gip.www.juno.Gui.WS.Messages.TextConfigGeneratorParameters_ctype textConfigGeneratorParameters) {
        this.textConfigGeneratorParameters = textConfigGeneratorParameters;
    }


    /**
     * Gets the mtaRequest value for this GenerateAsciiFromTemplateForNcsMtaInput_ctype.
     * 
     * @return mtaRequest
     */
    public com.gip.www.juno.Gui.WS.Messages.MtaRequest_ctype getMtaRequest() {
        return mtaRequest;
    }


    /**
     * Sets the mtaRequest value for this GenerateAsciiFromTemplateForNcsMtaInput_ctype.
     * 
     * @param mtaRequest
     */
    public void setMtaRequest(com.gip.www.juno.Gui.WS.Messages.MtaRequest_ctype mtaRequest) {
        this.mtaRequest = mtaRequest;
    }


    /**
     * Gets the ncsMta value for this GenerateAsciiFromTemplateForNcsMtaInput_ctype.
     * 
     * @return ncsMta
     */
    public com.gip.www.juno.Gui.WS.Messages.NcsMta_ctype getNcsMta() {
        return ncsMta;
    }


    /**
     * Sets the ncsMta value for this GenerateAsciiFromTemplateForNcsMtaInput_ctype.
     * 
     * @param ncsMta
     */
    public void setNcsMta(com.gip.www.juno.Gui.WS.Messages.NcsMta_ctype ncsMta) {
        this.ncsMta = ncsMta;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof GenerateAsciiFromTemplateForNcsMtaInput_ctype)) return false;
        GenerateAsciiFromTemplateForNcsMtaInput_ctype other = (GenerateAsciiFromTemplateForNcsMtaInput_ctype) obj;
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
            ((this.mtaRequest==null && other.getMtaRequest()==null) || 
             (this.mtaRequest!=null &&
              this.mtaRequest.equals(other.getMtaRequest()))) &&
            ((this.ncsMta==null && other.getNcsMta()==null) || 
             (this.ncsMta!=null &&
              this.ncsMta.equals(other.getNcsMta())));
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
        if (getMtaRequest() != null) {
            _hashCode += getMtaRequest().hashCode();
        }
        if (getNcsMta() != null) {
            _hashCode += getNcsMta().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(GenerateAsciiFromTemplateForNcsMtaInput_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForNcsMtaInput_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("textConfigGeneratorParameters");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TextConfigGeneratorParameters"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TextConfigGeneratorParameters_ctype"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("mtaRequest");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "MtaRequest"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "MtaRequest_ctype"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("ncsMta");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "NcsMta"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "NcsMta_ctype"));
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
