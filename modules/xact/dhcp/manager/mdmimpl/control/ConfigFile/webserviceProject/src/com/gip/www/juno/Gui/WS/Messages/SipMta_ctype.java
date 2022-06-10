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
 * SipMta_ctype.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.Gui.WS.Messages;

public class SipMta_ctype  implements java.io.Serializable {
    private java.lang.String mac;

    private java.lang.String softswitchType;

    private java.lang.String softswitch;

    private com.gip.www.juno.Gui.WS.Messages.SipMtaPortList_ctype sipPorts;

    private java.lang.String xml;

    public SipMta_ctype() {
    }

    public SipMta_ctype(
           java.lang.String mac,
           java.lang.String softswitchType,
           java.lang.String softswitch,
           com.gip.www.juno.Gui.WS.Messages.SipMtaPortList_ctype sipPorts,
           java.lang.String xml) {
           this.mac = mac;
           this.softswitchType = softswitchType;
           this.softswitch = softswitch;
           this.sipPorts = sipPorts;
           this.xml = xml;
    }


    /**
     * Gets the mac value for this SipMta_ctype.
     * 
     * @return mac
     */
    public java.lang.String getMac() {
        return mac;
    }


    /**
     * Sets the mac value for this SipMta_ctype.
     * 
     * @param mac
     */
    public void setMac(java.lang.String mac) {
        this.mac = mac;
    }


    /**
     * Gets the softswitchType value for this SipMta_ctype.
     * 
     * @return softswitchType
     */
    public java.lang.String getSoftswitchType() {
        return softswitchType;
    }


    /**
     * Sets the softswitchType value for this SipMta_ctype.
     * 
     * @param softswitchType
     */
    public void setSoftswitchType(java.lang.String softswitchType) {
        this.softswitchType = softswitchType;
    }


    /**
     * Gets the softswitch value for this SipMta_ctype.
     * 
     * @return softswitch
     */
    public java.lang.String getSoftswitch() {
        return softswitch;
    }


    /**
     * Sets the softswitch value for this SipMta_ctype.
     * 
     * @param softswitch
     */
    public void setSoftswitch(java.lang.String softswitch) {
        this.softswitch = softswitch;
    }


    /**
     * Gets the sipPorts value for this SipMta_ctype.
     * 
     * @return sipPorts
     */
    public com.gip.www.juno.Gui.WS.Messages.SipMtaPortList_ctype getSipPorts() {
        return sipPorts;
    }


    /**
     * Sets the sipPorts value for this SipMta_ctype.
     * 
     * @param sipPorts
     */
    public void setSipPorts(com.gip.www.juno.Gui.WS.Messages.SipMtaPortList_ctype sipPorts) {
        this.sipPorts = sipPorts;
    }


    /**
     * Gets the xml value for this SipMta_ctype.
     * 
     * @return xml
     */
    public java.lang.String getXml() {
        return xml;
    }


    /**
     * Sets the xml value for this SipMta_ctype.
     * 
     * @param xml
     */
    public void setXml(java.lang.String xml) {
        this.xml = xml;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof SipMta_ctype)) return false;
        SipMta_ctype other = (SipMta_ctype) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.mac==null && other.getMac()==null) || 
             (this.mac!=null &&
              this.mac.equals(other.getMac()))) &&
            ((this.softswitchType==null && other.getSoftswitchType()==null) || 
             (this.softswitchType!=null &&
              this.softswitchType.equals(other.getSoftswitchType()))) &&
            ((this.softswitch==null && other.getSoftswitch()==null) || 
             (this.softswitch!=null &&
              this.softswitch.equals(other.getSoftswitch()))) &&
            ((this.sipPorts==null && other.getSipPorts()==null) || 
             (this.sipPorts!=null &&
              this.sipPorts.equals(other.getSipPorts()))) &&
            ((this.xml==null && other.getXml()==null) || 
             (this.xml!=null &&
              this.xml.equals(other.getXml())));
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
        if (getMac() != null) {
            _hashCode += getMac().hashCode();
        }
        if (getSoftswitchType() != null) {
            _hashCode += getSoftswitchType().hashCode();
        }
        if (getSoftswitch() != null) {
            _hashCode += getSoftswitch().hashCode();
        }
        if (getSipPorts() != null) {
            _hashCode += getSipPorts().hashCode();
        }
        if (getXml() != null) {
            _hashCode += getXml().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(SipMta_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "SipMta_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("mac");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Mac"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("softswitchType");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "SoftswitchType"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("softswitch");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Softswitch"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("sipPorts");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "SipPorts"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "SipMtaPortList_ctype"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("xml");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "xml"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
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
