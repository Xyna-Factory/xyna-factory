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
 * SharedNetwork_ServiceLocator.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.DHCP.WS.SharedNetwork;

public class SharedNetwork_ServiceLocator extends org.apache.axis.client.Service implements com.gip.www.juno.DHCP.WS.SharedNetwork.SharedNetwork_Service {

    public SharedNetwork_ServiceLocator() {
    }


    public SharedNetwork_ServiceLocator(org.apache.axis.EngineConfiguration config) {
        super(config);
    }

    public SharedNetwork_ServiceLocator(java.lang.String wsdlLoc, javax.xml.namespace.QName sName) throws javax.xml.rpc.ServiceException {
        super(wsdlLoc, sName);
    }

    // Use to get a proxy class for SharedNetworkPort
    private java.lang.String SharedNetworkPort_address = "http://localhost:8080/SharedNetwork/services/SharedNetworkPort";

    public java.lang.String getSharedNetworkPortAddress() {
        return SharedNetworkPort_address;
    }

    // The WSDD service name defaults to the port name.
    private java.lang.String SharedNetworkPortWSDDServiceName = "SharedNetworkPort";

    public java.lang.String getSharedNetworkPortWSDDServiceName() {
        return SharedNetworkPortWSDDServiceName;
    }

    public void setSharedNetworkPortWSDDServiceName(java.lang.String name) {
        SharedNetworkPortWSDDServiceName = name;
    }

    public com.gip.www.juno.DHCP.WS.SharedNetwork.SharedNetwork_PortType getSharedNetworkPort() throws javax.xml.rpc.ServiceException {
       java.net.URL endpoint;
        try {
            endpoint = new java.net.URL(SharedNetworkPort_address);
        }
        catch (java.net.MalformedURLException e) {
            throw new javax.xml.rpc.ServiceException(e);
        }
        return getSharedNetworkPort(endpoint);
    }

    public com.gip.www.juno.DHCP.WS.SharedNetwork.SharedNetwork_PortType getSharedNetworkPort(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
        try {
            com.gip.www.juno.DHCP.WS.SharedNetwork.SharedNetworkBindingStub _stub = new com.gip.www.juno.DHCP.WS.SharedNetwork.SharedNetworkBindingStub(portAddress, this);
            _stub.setPortName(getSharedNetworkPortWSDDServiceName());
            return _stub;
        }
        catch (org.apache.axis.AxisFault e) {
            return null;
        }
    }

    public void setSharedNetworkPortEndpointAddress(java.lang.String address) {
        SharedNetworkPort_address = address;
    }

    /**
     * For the given interface, get the stub implementation.
     * If this service has no port for the given interface,
     * then ServiceException is thrown.
     */
    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
        try {
            if (com.gip.www.juno.DHCP.WS.SharedNetwork.SharedNetwork_PortType.class.isAssignableFrom(serviceEndpointInterface)) {
                com.gip.www.juno.DHCP.WS.SharedNetwork.SharedNetworkBindingStub _stub = new com.gip.www.juno.DHCP.WS.SharedNetwork.SharedNetworkBindingStub(new java.net.URL(SharedNetworkPort_address), this);
                _stub.setPortName(getSharedNetworkPortWSDDServiceName());
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
        if ("SharedNetworkPort".equals(inputPortName)) {
            return getSharedNetworkPort();
        }
        else  {
            java.rmi.Remote _stub = getPort(serviceEndpointInterface);
            ((org.apache.axis.client.Stub) _stub).setPortName(portName);
            return _stub;
        }
    }

    public javax.xml.namespace.QName getServiceName() {
        return new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/SharedNetwork", "SharedNetwork");
    }

    private java.util.HashSet ports = null;

    public java.util.Iterator getPorts() {
        if (ports == null) {
            ports = new java.util.HashSet();
            ports.add(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/SharedNetwork", "SharedNetworkPort"));
        }
        return ports.iterator();
    }

    /**
    * Set the endpoint address for the specified port name.
    */
    public void setEndpointAddress(java.lang.String portName, java.lang.String address) throws javax.xml.rpc.ServiceException {
        
if ("SharedNetworkPort".equals(portName)) {
            setSharedNetworkPortEndpointAddress(address);
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
