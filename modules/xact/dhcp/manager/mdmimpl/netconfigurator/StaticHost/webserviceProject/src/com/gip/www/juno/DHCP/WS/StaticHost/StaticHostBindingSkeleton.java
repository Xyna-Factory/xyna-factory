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
 * StaticHostBindingSkeleton.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.DHCP.WS.StaticHost;

public class StaticHostBindingSkeleton implements com.gip.www.juno.DHCP.WS.StaticHost.StaticHost_PortType, org.apache.axis.wsdl.Skeleton {
    private com.gip.www.juno.DHCP.WS.StaticHost.StaticHost_PortType impl;
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
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GetMetaInfoRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GetMetaInfoRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GetMetaInfoRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getMetaInfo", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "MetaInfoOutput"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "GetMetaInfo"));
        _oper.setSoapAction("GetMetaInfo");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getMetaInfo") == null) {
            _myOperations.put("getMetaInfo", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getMetaInfo")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "GetAllRowsRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GetAllRowsRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GetAllRowsRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getAllRows", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "GetAllRowsOutput"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "GetAllRows"));
        _oper.setSoapAction("GetAllRows");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getAllRows") == null) {
            _myOperations.put("getAllRows", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getAllRows")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "GetAllRowsWithSessionAuthentificationRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GetAllRowsRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GetAllRowsRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getAllRowsWithSessionAuthentification", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "GetAllRowsOutput"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "GetAllRowsWithSessionAuthentification"));
        _oper.setSoapAction("GetAllRowsWithSessionAuthentification");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getAllRowsWithSessionAuthentification") == null) {
            _myOperations.put("getAllRowsWithSessionAuthentification", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getAllRowsWithSessionAuthentification")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "SearchRowsRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "SearchRowsRequest_ctype"), com.gip.www.juno.DHCP.WS.StaticHost.Messages.SearchRowsRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("searchRows", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "SearchRowsOutput"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "SearchRows"));
        _oper.setSoapAction("SearchRows");
        _myOperationsList.add(_oper);
        if (_myOperations.get("searchRows") == null) {
            _myOperations.put("searchRows", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("searchRows")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "UpdateRowRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "UpdateRowRequest_ctype"), com.gip.www.juno.DHCP.WS.StaticHost.Messages.UpdateRowRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("updateRow", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "UpdateRowOutput"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "UpdateRow"));
        _oper.setSoapAction("UpdateRow");
        _myOperationsList.add(_oper);
        if (_myOperations.get("updateRow") == null) {
            _myOperations.put("updateRow", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("updateRow")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "InsertRowRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "InsertRowRequest_ctype"), com.gip.www.juno.DHCP.WS.StaticHost.Messages.InsertRowRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("insertRow", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "InsertRowOutput"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "InsertRow"));
        _oper.setSoapAction("InsertRow");
        _myOperationsList.add(_oper);
        if (_myOperations.get("insertRow") == null) {
            _myOperations.put("insertRow", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("insertRow")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "DeleteRowsRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "DeleteRowsRequest_ctype"), com.gip.www.juno.DHCP.WS.StaticHost.Messages.DeleteRowsRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("deleteRows", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "DeleteRowsOutput"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "DeleteRows"));
        _oper.setSoapAction("DeleteRows");
        _myOperationsList.add(_oper);
        if (_myOperations.get("deleteRows") == null) {
            _myOperations.put("deleteRows", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("deleteRows")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "GetFreeReservedIPsRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "GetFreeReservedIPsRequest_ctype"), com.gip.www.juno.DHCP.WS.StaticHost.Messages.GetFreeReservedIPsRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getFreeReservedIPs", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "GetFreeReservedIPsResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "GetFreeReservedIPsResponse_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "GetFreeReservedIPs"));
        _oper.setSoapAction("GetFreeReservedIPs");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getFreeReservedIPs") == null) {
            _myOperations.put("getFreeReservedIPs", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getFreeReservedIPs")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "SetIPforCPERequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "SetIPforCPERequest_ctype"), com.gip.www.juno.DHCP.WS.StaticHost.Messages.SetIPforCPERequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("setIPforCPE", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "SetIPforCPEResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "SetIPforCPEResponse_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "SetIPforCPE"));
        _oper.setSoapAction("SetIPforCPE");
        _myOperationsList.add(_oper);
        if (_myOperations.get("setIPforCPE") == null) {
            _myOperations.put("setIPforCPE", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("setIPforCPE")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "DeleteIPforCPERequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "DeleteIPforCPERequest_ctype"), com.gip.www.juno.DHCP.WS.StaticHost.Messages.DeleteIPforCPERequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("deleteIPforCPE", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "DeleteIPforCPEResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/StaticHost/Messages", "DeleteIPforCPEResponse_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "DeleteIPforCPE"));
        _oper.setSoapAction("DeleteIPforCPE");
        _myOperationsList.add(_oper);
        if (_myOperations.get("deleteIPforCPE") == null) {
            _myOperations.put("deleteIPforCPE", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("deleteIPforCPE")).add(_oper);
    }

    public StaticHostBindingSkeleton() {
        this.impl = new com.gip.www.juno.DHCP.WS.StaticHost.StaticHostBindingImpl();
    }

    public StaticHostBindingSkeleton(com.gip.www.juno.DHCP.WS.StaticHost.StaticHost_PortType impl) {
        this.impl = impl;
    }
    public com.gip.www.juno.DHCP.WS.StaticHost.Messages.Response_ctype getMetaInfo(com.gip.www.juno.Gui.WS.Messages.GetMetaInfoRequest_ctype metaInfoRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.DHCP.WS.StaticHost.Messages.Response_ctype ret = impl.getMetaInfo(metaInfoRequest);
        return ret;
    }

    public com.gip.www.juno.DHCP.WS.StaticHost.Messages.Response_ctype getAllRows(com.gip.www.juno.Gui.WS.Messages.GetAllRowsRequest_ctype getAllRowsRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.DHCP.WS.StaticHost.Messages.Response_ctype ret = impl.getAllRows(getAllRowsRequest);
        return ret;
    }

    public com.gip.www.juno.DHCP.WS.StaticHost.Messages.Response_ctype getAllRowsWithSessionAuthentification(com.gip.www.juno.Gui.WS.Messages.GetAllRowsRequest_ctype getAllRowsWithSessionAuthentificationRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.DHCP.WS.StaticHost.Messages.Response_ctype ret = impl.getAllRowsWithSessionAuthentification(getAllRowsWithSessionAuthentificationRequest);
        return ret;
    }

    public com.gip.www.juno.DHCP.WS.StaticHost.Messages.Response_ctype searchRows(com.gip.www.juno.DHCP.WS.StaticHost.Messages.SearchRowsRequest_ctype searchRowsRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.DHCP.WS.StaticHost.Messages.Response_ctype ret = impl.searchRows(searchRowsRequest);
        return ret;
    }

    public com.gip.www.juno.DHCP.WS.StaticHost.Messages.Response_ctype updateRow(com.gip.www.juno.DHCP.WS.StaticHost.Messages.UpdateRowRequest_ctype updateRowRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.DHCP.WS.StaticHost.Messages.Response_ctype ret = impl.updateRow(updateRowRequest);
        return ret;
    }

    public com.gip.www.juno.DHCP.WS.StaticHost.Messages.Response_ctype insertRow(com.gip.www.juno.DHCP.WS.StaticHost.Messages.InsertRowRequest_ctype insertRowRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.DHCP.WS.StaticHost.Messages.Response_ctype ret = impl.insertRow(insertRowRequest);
        return ret;
    }

    public com.gip.www.juno.DHCP.WS.StaticHost.Messages.Response_ctype deleteRows(com.gip.www.juno.DHCP.WS.StaticHost.Messages.DeleteRowsRequest_ctype deleteRowsRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.DHCP.WS.StaticHost.Messages.Response_ctype ret = impl.deleteRows(deleteRowsRequest);
        return ret;
    }

    public com.gip.www.juno.DHCP.WS.StaticHost.Messages.GetFreeReservedIPsResponse_ctype getFreeReservedIPs(com.gip.www.juno.DHCP.WS.StaticHost.Messages.GetFreeReservedIPsRequest_ctype getFreeReservedIPsRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.DHCP.WS.StaticHost.Messages.GetFreeReservedIPsResponse_ctype ret = impl.getFreeReservedIPs(getFreeReservedIPsRequest);
        return ret;
    }

    public com.gip.www.juno.DHCP.WS.StaticHost.Messages.SetIPforCPEResponse_ctype setIPforCPE(com.gip.www.juno.DHCP.WS.StaticHost.Messages.SetIPforCPERequest_ctype setIPforCPERequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.DHCP.WS.StaticHost.Messages.SetIPforCPEResponse_ctype ret = impl.setIPforCPE(setIPforCPERequest);
        return ret;
    }

    public com.gip.www.juno.DHCP.WS.StaticHost.Messages.DeleteIPforCPEResponse_ctype deleteIPforCPE(com.gip.www.juno.DHCP.WS.StaticHost.Messages.DeleteIPforCPERequest_ctype deleteIPforCPERequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.DHCP.WS.StaticHost.Messages.DeleteIPforCPEResponse_ctype ret = impl.deleteIPforCPE(deleteIPforCPERequest);
        return ret;
    }

}
