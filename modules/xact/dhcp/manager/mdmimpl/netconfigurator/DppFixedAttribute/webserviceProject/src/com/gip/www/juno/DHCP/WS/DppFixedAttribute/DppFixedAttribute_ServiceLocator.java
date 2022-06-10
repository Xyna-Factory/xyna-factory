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
 * DppFixedAttribute_ServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.DHCP.WS.DppFixedAttribute;

public class DppFixedAttribute_ServiceLocator extends org.apache.axis.client.Service implements com.gip.www.juno.DHCP.WS.DppFixedAttribute.DppFixedAttribute_Service {

    public DppFixedAttribute_ServiceLocator() {
    }


    public DppFixedAttribute_ServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public DppFixedAttribute_ServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for DppFixedAttributePort
    private java.lang.String DppFixedAttributePort_address = "http://local.de";

    public java.lang.String getDppFixedAttributePortAddress() {
        return DppFixedAttributePort_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String DppFixedAttributePortWSDDServiceName = "DppFixedAttributePort";

    public java.lang.String getDppFixedAttributePortWSDDServiceName() {
        return DppFixedAttributePortWSDDServiceName;
    }

    public void setDppFixedAttributePortWSDDServiceName(java.lang.String name) {
        DppFixedAttributePortWSDDServiceName = name;
    }

    public com.gip.www.juno.DHCP.WS.DppFixedAttribute.DppFixedAttribute_PortType getDppFixedAttributePort() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(DppFixedAttributePort_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getDppFixedAttributePort(endpoint);
    }

    public com.gip.www.juno.DHCP.WS.DppFixedAttribute.DppFixedAttribute_PortType getDppFixedAttributePort(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            com.gip.www.juno.DHCP.WS.DppFixedAttribute.DppFixedAttributeBindingStub _stub = new com.gip.www.juno.DHCP.WS.DppFixedAttribute.DppFixedAttributeBindingStub(portAddress, this);
            _stub.setPortName(getDppFixedAttributePortWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setDppFixedAttributePortEndpointAddress(java.lang.String address) {
        DppFixedAttributePort_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (com.gip.www.juno.DHCP.WS.DppFixedAttribute.DppFixedAttribute_PortType.class.isAssignableFrom(serviceEndpointInterface)) {
                com.gip.www.juno.DHCP.WS.DppFixedAttribute.DppFixedAttributeBindingStub _stub = new com.gip.www.juno.DHCP.WS.DppFixedAttribute.DppFixedAttributeBindingStub(new java.net.URL(DppFixedAttributePort_address), this);
                _stub.setPortName(getDppFixedAttributePortWSDDServiceName());
                return _stub;
            }
        }
        catch (java.lang.Throwable t) {
            throw new javax.xml.rpc.ServiceException(t);
        }
        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        if (portName == null) {
            return getPort(serviceEndpointInterface);
        }
        java.lang.String inputPortName = portName.getLocalPart();
        if ("DppFixedAttributePort".equals(inputPortName)) {
            return getDppFixedAttributePort();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/DppFixedAttribute", "DppFixedAttribute");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/DppFixedAttribute", "DppFixedAttributePort"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("DppFixedAttributePort".equals(portName)) {
            setDppFixedAttributePortEndpointAddress(address);
        }
        else 
{ // Unknown Port Name
            throw new javax.xml.rpc.ServiceException(" Cannot set Endpoint Address for Unknown Port" + portName);
        }
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(javax.xml.namespace.QName portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        setEndpointAddress(portName.getLocalPart(), address);
    }

}
