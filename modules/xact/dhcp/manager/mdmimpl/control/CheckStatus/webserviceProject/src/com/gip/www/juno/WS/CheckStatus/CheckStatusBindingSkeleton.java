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
 * CheckStatusBindingSkeleton.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.WS.CheckStatus;

public class CheckStatusBindingSkeleton implements com.gip.www.juno.WS.CheckStatus.CheckStatus_PortType, org.apache.axis.wsdl.Skeleton {
    private com.gip.www.juno.WS.CheckStatus.CheckStatus_PortType impl;
    private static java.util.Map _myOperations = new java.util.Hashtable();
    private static java.util.Collection _myOperationsList = new java.util.ArrayList();

    /**
    * Returns List of OperationDesc objects with this name
    */
    public static java.util.List getOperationDescByName(java.lang.String methodName) {
        return (java.util.List)_myOperations.get(methodName);
    }

    /**
    * Returns Collection of OperationDescs
    */
    public static java.util.Collection getOperationDescs() {
        return _myOperationsList;
    }

    static {
        org.apache.axis.description.OperationDesc _oper;
        org.apache.axis.description.FaultDesc _fault;
        org.apache.axis.description.ParameterDesc [] _params;
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CheckStatusForIpRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CheckStatusForIpRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.CheckStatusForIpRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("checkStatusForIp", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CheckStatusForIpResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "CheckStatusForIp"));
        _oper.setSoapAction("CheckStatusForIp");
        _myOperationsList.add(_oper);
        if (_myOperations.get("checkStatusForIp") == null) {
            _myOperations.put("checkStatusForIp", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("checkStatusForIp")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CheckStatusForIpv6Request"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CheckStatusForIpRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.CheckStatusForIpRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("checkStatusForIpv6", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CheckStatusForIpv6Response"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "CheckStatusForIpv6"));
        _oper.setSoapAction("CheckStatusForIpv6");
        _myOperationsList.add(_oper);
        if (_myOperations.get("checkStatusForIpv6") == null) {
            _myOperations.put("checkStatusForIpv6", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("checkStatusForIpv6")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GetInstanceInfoListRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GetInstanceInfoListRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GetInstanceInfoListRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getInstanceInfoList", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GetInstanceInfoListResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "GetInstanceInfoList"));
        _oper.setSoapAction("GetInstanceInfoList");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getInstanceInfoList") == null) {
            _myOperations.put("getInstanceInfoList", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getInstanceInfoList")).add(_oper);
    }

    public CheckStatusBindingSkeleton() {
        this.impl = new com.gip.www.juno.WS.CheckStatus.CheckStatusBindingImpl();
    }

    public CheckStatusBindingSkeleton(com.gip.www.juno.WS.CheckStatus.CheckStatus_PortType impl) {
        this.impl = impl;
    }
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype checkStatusForIp(com.gip.www.juno.Gui.WS.Messages.CheckStatusForIpRequest_ctype checkStatusForIpRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.checkStatusForIp(checkStatusForIpRequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype checkStatusForIpv6(com.gip.www.juno.Gui.WS.Messages.CheckStatusForIpRequest_ctype checkStatusForIpv6Request) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.checkStatusForIpv6(checkStatusForIpv6Request);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype getInstanceInfoList(com.gip.www.juno.Gui.WS.Messages.GetInstanceInfoListRequest_ctype getInstanceInfoListRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.getInstanceInfoList(getInstanceInfoListRequest);
        return ret;
    }

}
