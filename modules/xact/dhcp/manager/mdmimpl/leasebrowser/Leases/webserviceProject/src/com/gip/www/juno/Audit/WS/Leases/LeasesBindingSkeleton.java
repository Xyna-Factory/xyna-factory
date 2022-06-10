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
 * LeasesBindingSkeleton.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.Audit.WS.Leases;

public class LeasesBindingSkeleton implements com.gip.www.juno.Audit.WS.Leases.Leases_PortType, org.apache.axis.wsdl.Skeleton {
    private com.gip.www.juno.Audit.WS.Leases.Leases_PortType impl;
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
        _oper = new org.apache.axis.description.OperationDesc("getMetaInfo", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "MetaInfoOutput"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "GetMetaInfo"));
        _oper.setSoapAction("GetMetaInfo");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getMetaInfo") == null) {
            _myOperations.put("getMetaInfo", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getMetaInfo")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "GetAllRowsRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GetAllRowsRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GetAllRowsRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getAllRows", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "GetAllRowsOutput"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "GetAllRows"));
        _oper.setSoapAction("GetAllRows");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getAllRows") == null) {
            _myOperations.put("getAllRows", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getAllRows")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "SearchRowsRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "SearchRowsRequest_ctype"), com.gip.www.juno.Audit.WS.Leases.Messages.SearchRowsRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("searchRows", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "SearchRowsOutput"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "SearchRows"));
        _oper.setSoapAction("SearchRows");
        _myOperationsList.add(_oper);
        if (_myOperations.get("searchRows") == null) {
            _myOperations.put("searchRows", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("searchRows")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "UpdateRowRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "UpdateRowRequest_ctype"), com.gip.www.juno.Audit.WS.Leases.Messages.UpdateRowRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("updateRow", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "UpdateRowOutput"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "UpdateRow"));
        _oper.setSoapAction("UpdateRow");
        _myOperationsList.add(_oper);
        if (_myOperations.get("updateRow") == null) {
            _myOperations.put("updateRow", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("updateRow")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "InsertRowRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "InsertRowRequest_ctype"), com.gip.www.juno.Audit.WS.Leases.Messages.InsertRowRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("insertRow", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "InsertRowOutput"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "InsertRow"));
        _oper.setSoapAction("InsertRow");
        _myOperationsList.add(_oper);
        if (_myOperations.get("insertRow") == null) {
            _myOperations.put("insertRow", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("insertRow")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "DeleteRowsRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "DeleteRowsRequest_ctype"), com.gip.www.juno.Audit.WS.Leases.Messages.DeleteRowsRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("deleteRows", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "DeleteRowsOutput"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "DeleteRows"));
        _oper.setSoapAction("DeleteRows");
        _myOperationsList.add(_oper);
        if (_myOperations.get("deleteRows") == null) {
            _myOperations.put("deleteRows", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("deleteRows")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "SearchLeasesRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "SearchLeasesRequest_ctype"), com.gip.www.juno.Audit.WS.Leases.Messages.SearchLeasesRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("searchLeases", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "SearchLeasesOutput"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "SearchLeases"));
        _oper.setSoapAction("SearchLeases");
        _myOperationsList.add(_oper);
        if (_myOperations.get("searchLeases") == null) {
            _myOperations.put("searchLeases", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("searchLeases")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "CountRowsWithConditionRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "CountRowsWithConditionRequest_ctype"), com.gip.www.juno.Audit.WS.Leases.Messages.CountRowsWithConditionRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("countRowsWithCondition", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "CountRowsWithConditionOutput"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "CountRowsWithCondition"));
        _oper.setSoapAction("CountRowsWithCondition");
        _myOperationsList.add(_oper);
        if (_myOperations.get("countRowsWithCondition") == null) {
            _myOperations.put("countRowsWithCondition", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("countRowsWithCondition")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "CountAllRowsRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "CountAllRowsRequest_ctype"), com.gip.www.juno.Audit.WS.Leases.Messages.CountAllRowsRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("countAllRows", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "CountAllRowsOutput"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "CountAllRows"));
        _oper.setSoapAction("CountAllRows");
        _myOperationsList.add(_oper);
        if (_myOperations.get("countAllRows") == null) {
            _myOperations.put("countAllRows", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("countAllRows")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "CountLeasesRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "CountLeasesRequest_ctype"), com.gip.www.juno.Audit.WS.Leases.Messages.CountLeasesRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("countLeases", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "CountLeasesOutput"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Audit/WS/Leases/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "CountLeases"));
        _oper.setSoapAction("CountLeases");
        _myOperationsList.add(_oper);
        if (_myOperations.get("countLeases") == null) {
            _myOperations.put("countLeases", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("countLeases")).add(_oper);
    }

    public LeasesBindingSkeleton() {
        this.impl = new com.gip.www.juno.Audit.WS.Leases.LeasesBindingImpl();
    }

    public LeasesBindingSkeleton(com.gip.www.juno.Audit.WS.Leases.Leases_PortType impl) {
        this.impl = impl;
    }
    public com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype getMetaInfo(com.gip.www.juno.Gui.WS.Messages.GetMetaInfoRequest_ctype metaInfoRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype ret = impl.getMetaInfo(metaInfoRequest);
        return ret;
    }

    public com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype getAllRows(com.gip.www.juno.Gui.WS.Messages.GetAllRowsRequest_ctype getAllRowsRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype ret = impl.getAllRows(getAllRowsRequest);
        return ret;
    }

    public com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype searchRows(com.gip.www.juno.Audit.WS.Leases.Messages.SearchRowsRequest_ctype searchRowsRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype ret = impl.searchRows(searchRowsRequest);
        return ret;
    }

    public com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype updateRow(com.gip.www.juno.Audit.WS.Leases.Messages.UpdateRowRequest_ctype updateRowRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype ret = impl.updateRow(updateRowRequest);
        return ret;
    }

    public com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype insertRow(com.gip.www.juno.Audit.WS.Leases.Messages.InsertRowRequest_ctype insertRowRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype ret = impl.insertRow(insertRowRequest);
        return ret;
    }

    public com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype deleteRows(com.gip.www.juno.Audit.WS.Leases.Messages.DeleteRowsRequest_ctype deleteRowsRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype ret = impl.deleteRows(deleteRowsRequest);
        return ret;
    }

    public com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype searchLeases(com.gip.www.juno.Audit.WS.Leases.Messages.SearchLeasesRequest_ctype searchLeasesRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype ret = impl.searchLeases(searchLeasesRequest);
        return ret;
    }

    public com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype countRowsWithCondition(com.gip.www.juno.Audit.WS.Leases.Messages.CountRowsWithConditionRequest_ctype countRowsWithConditionRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype ret = impl.countRowsWithCondition(countRowsWithConditionRequest);
        return ret;
    }

    public com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype countAllRows(com.gip.www.juno.Audit.WS.Leases.Messages.CountAllRowsRequest_ctype countAllRowsRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype ret = impl.countAllRows(countAllRowsRequest);
        return ret;
    }

    public com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype countLeases(com.gip.www.juno.Audit.WS.Leases.Messages.CountLeasesRequest_ctype countLeasesRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Audit.WS.Leases.Messages.Response_ctype ret = impl.countLeases(countLeasesRequest);
        return ret;
    }

}
