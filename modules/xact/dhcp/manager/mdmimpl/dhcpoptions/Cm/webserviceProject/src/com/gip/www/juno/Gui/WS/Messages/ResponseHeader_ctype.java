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
 * ResponseHeader_ctype.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.Gui.WS.Messages;

public class ResponseHeader_ctype  implements java.io.Serializable {
    private java.lang.String errorDomain;

    private java.lang.String errorNumber;

    private java.lang.String severity;

    private java.lang.String description;

    private java.lang.String stacktrace;

    private com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList;

    private java.lang.String status;

    public ResponseHeader_ctype() {
    }

    public ResponseHeader_ctype(
           java.lang.String errorDomain,
           java.lang.String errorNumber,
           java.lang.String severity,
           java.lang.String description,
           java.lang.String stacktrace,
           com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList,
           java.lang.String status) {
           this.errorDomain = errorDomain;
           this.errorNumber = errorNumber;
           this.severity = severity;
           this.description = description;
           this.stacktrace = stacktrace;
           this.parameterList = parameterList;
           this.status = status;
    }


    /**
     * Gets the errorDomain value for this ResponseHeader_ctype.
     * 
     * @return errorDomain
     */
    public java.lang.String getErrorDomain() {
        return errorDomain;
    }


    /**
     * Sets the errorDomain value for this ResponseHeader_ctype.
     * 
     * @param errorDomain
     */
    public void setErrorDomain(java.lang.String errorDomain) {
        this.errorDomain = errorDomain;
    }


    /**
     * Gets the errorNumber value for this ResponseHeader_ctype.
     * 
     * @return errorNumber
     */
    public java.lang.String getErrorNumber() {
        return errorNumber;
    }


    /**
     * Sets the errorNumber value for this ResponseHeader_ctype.
     * 
     * @param errorNumber
     */
    public void setErrorNumber(java.lang.String errorNumber) {
        this.errorNumber = errorNumber;
    }


    /**
     * Gets the severity value for this ResponseHeader_ctype.
     * 
     * @return severity
     */
    public java.lang.String getSeverity() {
        return severity;
    }


    /**
     * Sets the severity value for this ResponseHeader_ctype.
     * 
     * @param severity
     */
    public void setSeverity(java.lang.String severity) {
        this.severity = severity;
    }


    /**
     * Gets the description value for this ResponseHeader_ctype.
     * 
     * @return description
     */
    public java.lang.String getDescription() {
        return description;
    }


    /**
     * Sets the description value for this ResponseHeader_ctype.
     * 
     * @param description
     */
    public void setDescription(java.lang.String description) {
        this.description = description;
    }


    /**
     * Gets the stacktrace value for this ResponseHeader_ctype.
     * 
     * @return stacktrace
     */
    public java.lang.String getStacktrace() {
        return stacktrace;
    }


    /**
     * Sets the stacktrace value for this ResponseHeader_ctype.
     * 
     * @param stacktrace
     */
    public void setStacktrace(java.lang.String stacktrace) {
        this.stacktrace = stacktrace;
    }


    /**
     * Gets the parameterList value for this ResponseHeader_ctype.
     * 
     * @return parameterList
     */
    public com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] getParameterList() {
        return parameterList;
    }


    /**
     * Sets the parameterList value for this ResponseHeader_ctype.
     * 
     * @param parameterList
     */
    public void setParameterList(com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[] parameterList) {
        this.parameterList = parameterList;
    }


    /**
     * Gets the status value for this ResponseHeader_ctype.
     * 
     * @return status
     */
    public java.lang.String getStatus() {
        return status;
    }


    /**
     * Sets the status value for this ResponseHeader_ctype.
     * 
     * @param status
     */
    public void setStatus(java.lang.String status) {
        this.status = status;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof ResponseHeader_ctype)) return false;
        ResponseHeader_ctype other = (ResponseHeader_ctype) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.errorDomain==null && other.getErrorDomain()==null) || 
             (this.errorDomain!=null &&
              this.errorDomain.equals(other.getErrorDomain()))) &&
            ((this.errorNumber==null && other.getErrorNumber()==null) || 
             (this.errorNumber!=null &&
              this.errorNumber.equals(other.getErrorNumber()))) &&
            ((this.severity==null && other.getSeverity()==null) || 
             (this.severity!=null &&
              this.severity.equals(other.getSeverity()))) &&
            ((this.description==null && other.getDescription()==null) || 
             (this.description!=null &&
              this.description.equals(other.getDescription()))) &&
            ((this.stacktrace==null && other.getStacktrace()==null) || 
             (this.stacktrace!=null &&
              this.stacktrace.equals(other.getStacktrace()))) &&
            ((this.parameterList==null && other.getParameterList()==null) || 
             (this.parameterList!=null &&
              java.util.Arrays.equals(this.parameterList, other.getParameterList()))) &&
            ((this.status==null && other.getStatus()==null) || 
             (this.status!=null &&
              this.status.equals(other.getStatus())));
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
        if (getErrorDomain() != null) {
            _hashCode += getErrorDomain().hashCode();
        }
        if (getErrorNumber() != null) {
            _hashCode += getErrorNumber().hashCode();
        }
        if (getSeverity() != null) {
            _hashCode += getSeverity().hashCode();
        }
        if (getDescription() != null) {
            _hashCode += getDescription().hashCode();
        }
        if (getStacktrace() != null) {
            _hashCode += getStacktrace().hashCode();
        }
        if (getParameterList() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getParameterList());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getParameterList(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getStatus() != null) {
            _hashCode += getStatus().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(ResponseHeader_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ResponseHeader_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("errorDomain");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ErrorDomain"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("errorNumber");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ErrorNumber"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("severity");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Severity"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("description");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Description"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("stacktrace");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Stacktrace"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("parameterList");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ParameterList"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ErrorParameter_ctype"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        elemField.setItemQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Parameter"));
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("status");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Status"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
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
