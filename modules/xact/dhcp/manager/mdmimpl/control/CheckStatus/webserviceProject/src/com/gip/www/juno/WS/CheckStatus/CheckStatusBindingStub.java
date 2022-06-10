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
 * CheckStatusBindingStub.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.WS.CheckStatus;

public class CheckStatusBindingStub extends org.apache.axis.client.Stub implements com.gip.www.juno.WS.CheckStatus.CheckStatus_PortType {
    private java.util.Vector cachedSerClasses = new java.util.Vector();
    private java.util.Vector cachedSerQNames = new java.util.Vector();
    private java.util.Vector cachedSerFactories = new java.util.Vector();
    private java.util.Vector cachedDeserFactories = new java.util.Vector();

    static org.apache.axis.description.OperationDesc [] _operations;

    static {
        _operations = new org.apache.axis.description.OperationDesc[3];
        _initOperationDesc1();
    }

    private static void _initOperationDesc1(){
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("CheckStatusForIp");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CheckStatusForIpRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CheckStatusForIpRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.CheckStatusForIpRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CheckStatusForIpResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[0] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("CheckStatusForIpv6");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CheckStatusForIpv6Request"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CheckStatusForIpRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.CheckStatusForIpRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CheckStatusForIpv6Response"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[1] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("GetInstanceInfoList");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GetInstanceInfoListRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GetInstanceInfoListRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GetInstanceInfoListRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GetInstanceInfoListResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[2] = oper;

    }

    public CheckStatusBindingStub() throws org.apache.axis.AxisFault {
         this(null);
    }

    public CheckStatusBindingStub(java.net.URL endpointURL, javax.xml.rpc.Service service) throws org.apache.axis.AxisFault {
         this(service);
         super.cachedEndpoint = endpointURL;
    }

    public CheckStatusBindingStub(javax.xml.rpc.Service service) throws org.apache.axis.AxisFault {
        if (service == null) {
            super.service = new org.apache.axis.client.Service();
        } else {
            super.service = service;
        }
        ((org.apache.axis.client.Service)super.service).setTypeMappingVersion("1.2");
            java.lang.Class cls;
            javax.xml.namespace.QName qName;
            javax.xml.namespace.QName qName2;
            java.lang.Class beansf = org.apache.axis.encoding.ser.BeanSerializerFactory.class;
            java.lang.Class beandf = org.apache.axis.encoding.ser.BeanDeserializerFactory.class;
            java.lang.Class enumsf = org.apache.axis.encoding.ser.EnumSerializerFactory.class;
            java.lang.Class enumdf = org.apache.axis.encoding.ser.EnumDeserializerFactory.class;
            java.lang.Class arraysf = org.apache.axis.encoding.ser.ArraySerializerFactory.class;
            java.lang.Class arraydf = org.apache.axis.encoding.ser.ArrayDeserializerFactory.class;
            java.lang.Class simplesf = org.apache.axis.encoding.ser.SimpleSerializerFactory.class;
            java.lang.Class simpledf = org.apache.axis.encoding.ser.SimpleDeserializerFactory.class;
            java.lang.Class simplelistsf = org.apache.axis.encoding.ser.SimpleListSerializerFactory.class;
            java.lang.Class simplelistdf = org.apache.axis.encoding.ser.SimpleListDeserializerFactory.class;
            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CheckStatusForIpInput_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.CheckStatusForIpInput_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CheckStatusForIpRequest_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.CheckStatusForIpRequest_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CheckStatusForIpResponse_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.StatusElement_ctype[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "StatusElement_ctype");
            qName2 = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "StatusElement");
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ErrorParameter_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ErrorParameterList_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.ErrorParameter_ctype[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ErrorParameter_ctype");
            qName2 = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Parameter");
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GetInstanceInfoListRequest_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GetInstanceInfoListRequest_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GetInstanceInfoListResponse_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.InstanceInfo_ctype[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "InstanceInfo_ctype");
            qName2 = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "InstanceInfo");
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "InputHeaderContent_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "InstanceInfo_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.InstanceInfo_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Payload_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.Payload_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.Response_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ResponseHeader_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.ResponseHeader_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "StatusElement_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.StatusElement_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

    }

    protected org.apache.axis.client.Call createCall() throws java.rmi.RemoteException {
        try {
            org.apache.axis.client.Call _call = super._createCall();
            if (super.maintainSessionSet) {
                _call.setMaintainSession(super.maintainSession);
            }
            if (super.cachedUsername != null) {
                _call.setUsername(super.cachedUsername);
            }
            if (super.cachedPassword != null) {
                _call.setPassword(super.cachedPassword);
            }
            if (super.cachedEndpoint != null) {
                _call.setTargetEndpointAddress(super.cachedEndpoint);
            }
            if (super.cachedTimeout != null) {
                _call.setTimeout(super.cachedTimeout);
            }
            if (super.cachedPortName != null) {
                _call.setPortName(super.cachedPortName);
            }
            java.util.Enumeration keys = super.cachedProperties.keys();
            while (keys.hasMoreElements()) {
                java.lang.String key = (java.lang.String) keys.nextElement();
                _call.setProperty(key, super.cachedProperties.get(key));
            }
            // All the type mapping information is registered
            // when the first call is made.
            // The type mapping information is actually registered in
            // the TypeMappingRegistry of the service, which
            // is the reason why registration is only needed for the first call.
            synchronized (this) {
                if (firstCall()) {
                    // must set encoding style before registering serializers
                    _call.setEncodingStyle(null);
                    for (int i = 0; i < cachedSerFactories.size(); ++i) {
                        java.lang.Class cls = (java.lang.Class) cachedSerClasses.get(i);
                        javax.xml.namespace.QName qName =
                                (javax.xml.namespace.QName) cachedSerQNames.get(i);
                        java.lang.Object x = cachedSerFactories.get(i);
                        if (x instanceof Class) {
                            java.lang.Class sf = (java.lang.Class)
                                 cachedSerFactories.get(i);
                            java.lang.Class df = (java.lang.Class)
                                 cachedDeserFactories.get(i);
                            _call.registerTypeMapping(cls, qName, sf, df, false);
                        }
                        else if (x instanceof javax.xml.rpc.encoding.SerializerFactory) {
                            org.apache.axis.encoding.SerializerFactory sf = (org.apache.axis.encoding.SerializerFactory)
                                 cachedSerFactories.get(i);
                            org.apache.axis.encoding.DeserializerFactory df = (org.apache.axis.encoding.DeserializerFactory)
                                 cachedDeserFactories.get(i);
                            _call.registerTypeMapping(cls, qName, sf, df, false);
                        }
                    }
                }
            }
            return _call;
        }
        catch (java.lang.Throwable _t) {
            throw new org.apache.axis.AxisFault("Failure trying to get the Call object", _t);
        }
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype checkStatusForIp(com.gip.www.juno.Gui.WS.Messages.CheckStatusForIpRequest_ctype checkStatusForIpRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[0]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("CheckStatusForIp");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "CheckStatusForIp"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {checkStatusForIpRequest});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.gip.www.juno.Gui.WS.Messages.Response_ctype) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.gip.www.juno.Gui.WS.Messages.Response_ctype) org.apache.axis.utils.JavaUtils.convert(_resp, com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
  throw axisFaultException;
}
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype checkStatusForIpv6(com.gip.www.juno.Gui.WS.Messages.CheckStatusForIpRequest_ctype checkStatusForIpv6Request) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[1]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("CheckStatusForIpv6");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "CheckStatusForIpv6"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {checkStatusForIpv6Request});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.gip.www.juno.Gui.WS.Messages.Response_ctype) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.gip.www.juno.Gui.WS.Messages.Response_ctype) org.apache.axis.utils.JavaUtils.convert(_resp, com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
  throw axisFaultException;
}
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype getInstanceInfoList(com.gip.www.juno.Gui.WS.Messages.GetInstanceInfoListRequest_ctype getInstanceInfoListRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[2]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("GetInstanceInfoList");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "GetInstanceInfoList"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {getInstanceInfoListRequest});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.gip.www.juno.Gui.WS.Messages.Response_ctype) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.gip.www.juno.Gui.WS.Messages.Response_ctype) org.apache.axis.utils.JavaUtils.convert(_resp, com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
  throw axisFaultException;
}
    }

}
