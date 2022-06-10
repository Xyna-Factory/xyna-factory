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
 * InitializedCableModem_ctype.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.Gui.WS.Messages;

public class InitializedCableModem_ctype  implements java.io.Serializable {
    private java.lang.String macAddress;

    private java.lang.String downstreamSpeed;

    private java.lang.String upstreamSpeed;

    private java.lang.String mode;

    private java.lang.String numberOfCPEs;

    private java.lang.String xml;

    private java.lang.String cpeIPs;

    private java.lang.String ipMode;

    private java.lang.String mtaEnable;

    public InitializedCableModem_ctype() {
    }

    public InitializedCableModem_ctype(
           java.lang.String macAddress,
           java.lang.String downstreamSpeed,
           java.lang.String upstreamSpeed,
           java.lang.String mode,
           java.lang.String numberOfCPEs,
           java.lang.String xml,
           java.lang.String cpeIPs,
           java.lang.String ipMode,
           java.lang.String mtaEnable) {
           this.macAddress = macAddress;
           this.downstreamSpeed = downstreamSpeed;
           this.upstreamSpeed = upstreamSpeed;
           this.mode = mode;
           this.numberOfCPEs = numberOfCPEs;
           this.xml = xml;
           this.cpeIPs = cpeIPs;
           this.ipMode = ipMode;
           this.mtaEnable = mtaEnable;
    }


    /**
     * Gets the macAddress value for this InitializedCableModem_ctype.
     * 
     * @return macAddress
     */
    public java.lang.String getMacAddress() {
        return macAddress;
    }


    /**
     * Sets the macAddress value for this InitializedCableModem_ctype.
     * 
     * @param macAddress
     */
    public void setMacAddress(java.lang.String macAddress) {
        this.macAddress = macAddress;
    }


    /**
     * Gets the downstreamSpeed value for this InitializedCableModem_ctype.
     * 
     * @return downstreamSpeed
     */
    public java.lang.String getDownstreamSpeed() {
        return downstreamSpeed;
    }


    /**
     * Sets the downstreamSpeed value for this InitializedCableModem_ctype.
     * 
     * @param downstreamSpeed
     */
    public void setDownstreamSpeed(java.lang.String downstreamSpeed) {
        this.downstreamSpeed = downstreamSpeed;
    }


    /**
     * Gets the upstreamSpeed value for this InitializedCableModem_ctype.
     * 
     * @return upstreamSpeed
     */
    public java.lang.String getUpstreamSpeed() {
        return upstreamSpeed;
    }


    /**
     * Sets the upstreamSpeed value for this InitializedCableModem_ctype.
     * 
     * @param upstreamSpeed
     */
    public void setUpstreamSpeed(java.lang.String upstreamSpeed) {
        this.upstreamSpeed = upstreamSpeed;
    }


    /**
     * Gets the mode value for this InitializedCableModem_ctype.
     * 
     * @return mode
     */
    public java.lang.String getMode() {
        return mode;
    }


    /**
     * Sets the mode value for this InitializedCableModem_ctype.
     * 
     * @param mode
     */
    public void setMode(java.lang.String mode) {
        this.mode = mode;
    }


    /**
     * Gets the numberOfCPEs value for this InitializedCableModem_ctype.
     * 
     * @return numberOfCPEs
     */
    public java.lang.String getNumberOfCPEs() {
        return numberOfCPEs;
    }


    /**
     * Sets the numberOfCPEs value for this InitializedCableModem_ctype.
     * 
     * @param numberOfCPEs
     */
    public void setNumberOfCPEs(java.lang.String numberOfCPEs) {
        this.numberOfCPEs = numberOfCPEs;
    }


    /**
     * Gets the xml value for this InitializedCableModem_ctype.
     * 
     * @return xml
     */
    public java.lang.String getXml() {
        return xml;
    }


    /**
     * Sets the xml value for this InitializedCableModem_ctype.
     * 
     * @param xml
     */
    public void setXml(java.lang.String xml) {
        this.xml = xml;
    }


    /**
     * Gets the cpeIPs value for this InitializedCableModem_ctype.
     * 
     * @return cpeIPs
     */
    public java.lang.String getCpeIPs() {
        return cpeIPs;
    }


    /**
     * Sets the cpeIPs value for this InitializedCableModem_ctype.
     * 
     * @param cpeIPs
     */
    public void setCpeIPs(java.lang.String cpeIPs) {
        this.cpeIPs = cpeIPs;
    }


    /**
     * Gets the ipMode value for this InitializedCableModem_ctype.
     * 
     * @return ipMode
     */
    public java.lang.String getIpMode() {
        return ipMode;
    }


    /**
     * Sets the ipMode value for this InitializedCableModem_ctype.
     * 
     * @param ipMode
     */
    public void setIpMode(java.lang.String ipMode) {
        this.ipMode = ipMode;
    }


    /**
     * Gets the mtaEnable value for this InitializedCableModem_ctype.
     * 
     * @return mtaEnable
     */
    public java.lang.String getMtaEnable() {
        return mtaEnable;
    }


    /**
     * Sets the mtaEnable value for this InitializedCableModem_ctype.
     * 
     * @param mtaEnable
     */
    public void setMtaEnable(java.lang.String mtaEnable) {
        this.mtaEnable = mtaEnable;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof InitializedCableModem_ctype)) return false;
        InitializedCableModem_ctype other = (InitializedCableModem_ctype) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.macAddress==null && other.getMacAddress()==null) || 
             (this.macAddress!=null &&
              this.macAddress.equals(other.getMacAddress()))) &&
            ((this.downstreamSpeed==null && other.getDownstreamSpeed()==null) || 
             (this.downstreamSpeed!=null &&
              this.downstreamSpeed.equals(other.getDownstreamSpeed()))) &&
            ((this.upstreamSpeed==null && other.getUpstreamSpeed()==null) || 
             (this.upstreamSpeed!=null &&
              this.upstreamSpeed.equals(other.getUpstreamSpeed()))) &&
            ((this.mode==null && other.getMode()==null) || 
             (this.mode!=null &&
              this.mode.equals(other.getMode()))) &&
            ((this.numberOfCPEs==null && other.getNumberOfCPEs()==null) || 
             (this.numberOfCPEs!=null &&
              this.numberOfCPEs.equals(other.getNumberOfCPEs()))) &&
            ((this.xml==null && other.getXml()==null) || 
             (this.xml!=null &&
              this.xml.equals(other.getXml()))) &&
            ((this.cpeIPs==null && other.getCpeIPs()==null) || 
             (this.cpeIPs!=null &&
              this.cpeIPs.equals(other.getCpeIPs()))) &&
            ((this.ipMode==null && other.getIpMode()==null) || 
             (this.ipMode!=null &&
              this.ipMode.equals(other.getIpMode()))) &&
            ((this.mtaEnable==null && other.getMtaEnable()==null) || 
             (this.mtaEnable!=null &&
              this.mtaEnable.equals(other.getMtaEnable())));
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
        if (getMacAddress() != null) {
            _hashCode += getMacAddress().hashCode();
        }
        if (getDownstreamSpeed() != null) {
            _hashCode += getDownstreamSpeed().hashCode();
        }
        if (getUpstreamSpeed() != null) {
            _hashCode += getUpstreamSpeed().hashCode();
        }
        if (getMode() != null) {
            _hashCode += getMode().hashCode();
        }
        if (getNumberOfCPEs() != null) {
            _hashCode += getNumberOfCPEs().hashCode();
        }
        if (getXml() != null) {
            _hashCode += getXml().hashCode();
        }
        if (getCpeIPs() != null) {
            _hashCode += getCpeIPs().hashCode();
        }
        if (getIpMode() != null) {
            _hashCode += getIpMode().hashCode();
        }
        if (getMtaEnable() != null) {
            _hashCode += getMtaEnable().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(InitializedCableModem_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "InitializedCableModem_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("macAddress");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "MacAddress"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("downstreamSpeed");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DownstreamSpeed"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("upstreamSpeed");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UpstreamSpeed"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("mode");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Mode"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("numberOfCPEs");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "NumberOfCPEs"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("xml");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "xml"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("cpeIPs");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CpeIPs"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("ipMode");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ipMode"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("mtaEnable");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "mtaEnable"));
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
