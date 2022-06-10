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
 * Condition_ServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.DHCP.WS.Condition;

public class Condition_ServiceLocator extends org.apache.axis.client.Service implements com.gip.www.juno.DHCP.WS.Condition.Condition_Service {

    public Condition_ServiceLocator() {
    }


    public Condition_ServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public Condition_ServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for ConditionPort
    private java.lang.String ConditionPort_address = "http://tempuri.org/Condition/services/ConditionPort";

    public java.lang.String getConditionPortAddress() {
        return ConditionPort_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String ConditionPortWSDDServiceName = "ConditionPort";

    public java.lang.String getConditionPortWSDDServiceName() {
        return ConditionPortWSDDServiceName;
    }

    public void setConditionPortWSDDServiceName(java.lang.String name) {
        ConditionPortWSDDServiceName = name;
    }

    public com.gip.www.juno.DHCP.WS.Condition.Condition_PortType getConditionPort() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(ConditionPort_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getConditionPort(endpoint);
    }

    public com.gip.www.juno.DHCP.WS.Condition.Condition_PortType getConditionPort(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            com.gip.www.juno.DHCP.WS.Condition.ConditionBindingStub _stub = new com.gip.www.juno.DHCP.WS.Condition.ConditionBindingStub(portAddress, this);
            _stub.setPortName(getConditionPortWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setConditionPortEndpointAddress(java.lang.String address) {
        ConditionPort_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (com.gip.www.juno.DHCP.WS.Condition.Condition_PortType.class.isAssignableFrom(serviceEndpointInterface)) {
                com.gip.www.juno.DHCP.WS.Condition.ConditionBindingStub _stub = new com.gip.www.juno.DHCP.WS.Condition.ConditionBindingStub(new java.net.URL(ConditionPort_address), this);
                _stub.setPortName(getConditionPortWSDDServiceName());
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
        if ("ConditionPort".equals(inputPortName)) {
            return getConditionPort();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Condition", "Condition");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Condition", "ConditionPort"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("ConditionPort".equals(portName)) {
            setConditionPortEndpointAddress(address);
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
