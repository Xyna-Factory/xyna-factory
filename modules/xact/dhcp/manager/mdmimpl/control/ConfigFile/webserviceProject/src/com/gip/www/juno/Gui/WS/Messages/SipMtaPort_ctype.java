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
 * SipMtaPort_ctype.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.Gui.WS.Messages;

public class SipMtaPort_ctype  implements java.io.Serializable {
    private java.lang.String portNumber;

    private java.lang.String directoryNumber;

    private java.lang.String userName;

    private java.lang.String password;

    private java.lang.String registrarServer;

    private java.lang.String proxyServer;

    private java.lang.String xml;

    private java.lang.String localNumber;

    private java.lang.String areaCode;

    public SipMtaPort_ctype() {
    }

    public SipMtaPort_ctype(
           java.lang.String portNumber,
           java.lang.String directoryNumber,
           java.lang.String userName,
           java.lang.String password,
           java.lang.String registrarServer,
           java.lang.String proxyServer,
           java.lang.String xml,
           java.lang.String localNumber,
           java.lang.String areaCode) {
           this.portNumber = portNumber;
           this.directoryNumber = directoryNumber;
           this.userName = userName;
           this.password = password;
           this.registrarServer = registrarServer;
           this.proxyServer = proxyServer;
           this.xml = xml;
           this.localNumber = localNumber;
           this.areaCode = areaCode;
    }


    /**
     * Gets the portNumber value for this SipMtaPort_ctype.
     * 
     * @return portNumber
     */
    public java.lang.String getPortNumber() {
        return portNumber;
    }


    /**
     * Sets the portNumber value for this SipMtaPort_ctype.
     * 
     * @param portNumber
     */
    public void setPortNumber(java.lang.String portNumber) {
        this.portNumber = portNumber;
    }


    /**
     * Gets the directoryNumber value for this SipMtaPort_ctype.
     * 
     * @return directoryNumber
     */
    public java.lang.String getDirectoryNumber() {
        return directoryNumber;
    }


    /**
     * Sets the directoryNumber value for this SipMtaPort_ctype.
     * 
     * @param directoryNumber
     */
    public void setDirectoryNumber(java.lang.String directoryNumber) {
        this.directoryNumber = directoryNumber;
    }


    /**
     * Gets the userName value for this SipMtaPort_ctype.
     * 
     * @return userName
     */
    public java.lang.String getUserName() {
        return userName;
    }


    /**
     * Sets the userName value for this SipMtaPort_ctype.
     * 
     * @param userName
     */
    public void setUserName(java.lang.String userName) {
        this.userName = userName;
    }


    /**
     * Gets the password value for this SipMtaPort_ctype.
     * 
     * @return password
     */
    public java.lang.String getPassword() {
        return password;
    }


    /**
     * Sets the password value for this SipMtaPort_ctype.
     * 
     * @param password
     */
    public void setPassword(java.lang.String password) {
        this.password = password;
    }


    /**
     * Gets the registrarServer value for this SipMtaPort_ctype.
     * 
     * @return registrarServer
     */
    public java.lang.String getRegistrarServer() {
        return registrarServer;
    }


    /**
     * Sets the registrarServer value for this SipMtaPort_ctype.
     * 
     * @param registrarServer
     */
    public void setRegistrarServer(java.lang.String registrarServer) {
        this.registrarServer = registrarServer;
    }


    /**
     * Gets the proxyServer value for this SipMtaPort_ctype.
     * 
     * @return proxyServer
     */
    public java.lang.String getProxyServer() {
        return proxyServer;
    }


    /**
     * Sets the proxyServer value for this SipMtaPort_ctype.
     * 
     * @param proxyServer
     */
    public void setProxyServer(java.lang.String proxyServer) {
        this.proxyServer = proxyServer;
    }


    /**
     * Gets the xml value for this SipMtaPort_ctype.
     * 
     * @return xml
     */
    public java.lang.String getXml() {
        return xml;
    }


    /**
     * Sets the xml value for this SipMtaPort_ctype.
     * 
     * @param xml
     */
    public void setXml(java.lang.String xml) {
        this.xml = xml;
    }


    /**
     * Gets the localNumber value for this SipMtaPort_ctype.
     * 
     * @return localNumber
     */
    public java.lang.String getLocalNumber() {
        return localNumber;
    }


    /**
     * Sets the localNumber value for this SipMtaPort_ctype.
     * 
     * @param localNumber
     */
    public void setLocalNumber(java.lang.String localNumber) {
        this.localNumber = localNumber;
    }


    /**
     * Gets the areaCode value for this SipMtaPort_ctype.
     * 
     * @return areaCode
     */
    public java.lang.String getAreaCode() {
        return areaCode;
    }


    /**
     * Sets the areaCode value for this SipMtaPort_ctype.
     * 
     * @param areaCode
     */
    public void setAreaCode(java.lang.String areaCode) {
        this.areaCode = areaCode;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof SipMtaPort_ctype)) return false;
        SipMtaPort_ctype other = (SipMtaPort_ctype) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.portNumber==null && other.getPortNumber()==null) || 
             (this.portNumber!=null &&
              this.portNumber.equals(other.getPortNumber()))) &&
            ((this.directoryNumber==null && other.getDirectoryNumber()==null) || 
             (this.directoryNumber!=null &&
              this.directoryNumber.equals(other.getDirectoryNumber()))) &&
            ((this.userName==null && other.getUserName()==null) || 
             (this.userName!=null &&
              this.userName.equals(other.getUserName()))) &&
            ((this.password==null && other.getPassword()==null) || 
             (this.password!=null &&
              this.password.equals(other.getPassword()))) &&
            ((this.registrarServer==null && other.getRegistrarServer()==null) || 
             (this.registrarServer!=null &&
              this.registrarServer.equals(other.getRegistrarServer()))) &&
            ((this.proxyServer==null && other.getProxyServer()==null) || 
             (this.proxyServer!=null &&
              this.proxyServer.equals(other.getProxyServer()))) &&
            ((this.xml==null && other.getXml()==null) || 
             (this.xml!=null &&
              this.xml.equals(other.getXml()))) &&
            ((this.localNumber==null && other.getLocalNumber()==null) || 
             (this.localNumber!=null &&
              this.localNumber.equals(other.getLocalNumber()))) &&
            ((this.areaCode==null && other.getAreaCode()==null) || 
             (this.areaCode!=null &&
              this.areaCode.equals(other.getAreaCode())));
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
        if (getPortNumber() != null) {
            _hashCode += getPortNumber().hashCode();
        }
        if (getDirectoryNumber() != null) {
            _hashCode += getDirectoryNumber().hashCode();
        }
        if (getUserName() != null) {
            _hashCode += getUserName().hashCode();
        }
        if (getPassword() != null) {
            _hashCode += getPassword().hashCode();
        }
        if (getRegistrarServer() != null) {
            _hashCode += getRegistrarServer().hashCode();
        }
        if (getProxyServer() != null) {
            _hashCode += getProxyServer().hashCode();
        }
        if (getXml() != null) {
            _hashCode += getXml().hashCode();
        }
        if (getLocalNumber() != null) {
            _hashCode += getLocalNumber().hashCode();
        }
        if (getAreaCode() != null) {
            _hashCode += getAreaCode().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(SipMtaPort_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "SipMtaPort_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("portNumber");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "PortNumber"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("directoryNumber");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DirectoryNumber"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("userName");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UserName"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("password");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Password"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("registrarServer");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "RegistrarServer"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("proxyServer");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ProxyServer"));
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
        elemField.setFieldName("localNumber");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "localNumber"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(true);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("areaCode");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "areaCode"));
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
