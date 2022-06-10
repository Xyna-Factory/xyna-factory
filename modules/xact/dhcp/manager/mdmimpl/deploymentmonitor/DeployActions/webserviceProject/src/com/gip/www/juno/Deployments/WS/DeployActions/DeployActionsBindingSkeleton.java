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
 * DeployActionsBindingSkeleton.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.Deployments.WS.DeployActions;

public class DeployActionsBindingSkeleton implements com.gip.www.juno.Deployments.WS.DeployActions.DeployActions_PortType, org.apache.axis.wsdl.Skeleton {
    private com.gip.www.juno.Deployments.WS.DeployActions.DeployActions_PortType impl;
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
        _oper = new org.apache.axis.description.OperationDesc("getMetaInfo", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "MetaInfo"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "MetaInfo_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "GetMetaInfo"));
        _oper.setSoapAction("GetMetaInfo");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getMetaInfo") == null) {
            _myOperations.put("getMetaInfo", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getMetaInfo")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Deployments/WS/DeployActions/Messages", "GetAllRowsRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GetAllRowsRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GetAllRowsRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getAllRows", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Deployments/WS/DeployActions/Messages", "GetAllRowsOutput"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Deployments/WS/DeployActions/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "GetAllRows"));
        _oper.setSoapAction("GetAllRows");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getAllRows") == null) {
            _myOperations.put("getAllRows", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getAllRows")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Deployments/WS/DeployActions/Messages", "SearchRowsRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Deployments/WS/DeployActions/Messages", "SearchRowsRequest_ctype"), com.gip.www.juno.Deployments.WS.DeployActions.Messages.SearchRowsRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("searchRows", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Deployments/WS/DeployActions/Messages", "SearchRowsOutput"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Deployments/WS/DeployActions/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "SearchRows"));
        _oper.setSoapAction("SearchRows");
        _myOperationsList.add(_oper);
        if (_myOperations.get("searchRows") == null) {
            _myOperations.put("searchRows", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("searchRows")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Deployments/WS/DeployActions/Messages", "InsertRowRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Deployments/WS/DeployActions/Messages", "InsertRowRequest_ctype"), com.gip.www.juno.Deployments.WS.DeployActions.Messages.InsertRowRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("insertRow", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Deployments/WS/DeployActions/Messages", "InsertRowOutput"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Deployments/WS/DeployActions/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "InsertRow"));
        _oper.setSoapAction("InsertRow");
        _myOperationsList.add(_oper);
        if (_myOperations.get("insertRow") == null) {
            _myOperations.put("insertRow", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("insertRow")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Deployments/WS/DeployActions/Messages", "DeleteRowsRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Deployments/WS/DeployActions/Messages", "DeleteRowsRequest_ctype"), com.gip.www.juno.Deployments.WS.DeployActions.Messages.DeleteRowsRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("deleteRows", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Deployments/WS/DeployActions/Messages", "DeleteRowsOutput"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Deployments/WS/DeployActions/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "DeleteRows"));
        _oper.setSoapAction("DeleteRows");
        _myOperationsList.add(_oper);
        if (_myOperations.get("deleteRows") == null) {
            _myOperations.put("deleteRows", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("deleteRows")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Deployments/WS/DeployActions/Messages", "UpdateRowPkRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Deployments/WS/DeployActions/Messages", "UpdateRowPkRequest_ctype"), com.gip.www.juno.Deployments.WS.DeployActions.Messages.UpdateRowPkRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("updateRowPk", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Deployments/WS/DeployActions/Messages", "UpdateRowPkOutput"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Deployments/WS/DeployActions/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "UpdateRowPk"));
        _oper.setSoapAction("UpdateRowPk");
        _myOperationsList.add(_oper);
        if (_myOperations.get("updateRowPk") == null) {
            _myOperations.put("updateRowPk", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("updateRowPk")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Deployments/WS/DeployActions/Messages", "CountAllRowsRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Deployments/WS/DeployActions/Messages", "CountAllRowsRequest_ctype"), com.gip.www.juno.Deployments.WS.DeployActions.Messages.CountAllRowsRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("countAllRows", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Deployments/WS/DeployActions/Messages", "CountAllRowsOutput"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Deployments/WS/DeployActions/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "CountAllRows"));
        _oper.setSoapAction("CountAllRows");
        _myOperationsList.add(_oper);
        if (_myOperations.get("countAllRows") == null) {
            _myOperations.put("countAllRows", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("countAllRows")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Deployments/WS/DeployActions/Messages", "CountRowsWithConditionRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Deployments/WS/DeployActions/Messages", "CountRowsWithConditionRequest_ctype"), com.gip.www.juno.Deployments.WS.DeployActions.Messages.CountRowsWithConditionRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("countRowsWithCondition", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Deployments/WS/DeployActions/Messages", "CountRowsWithConditionOutput"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Deployments/WS/DeployActions/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "CountRowsWithCondition"));
        _oper.setSoapAction("CountRowsWithCondition");
        _myOperationsList.add(_oper);
        if (_myOperations.get("countRowsWithCondition") == null) {
            _myOperations.put("countRowsWithCondition", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("countRowsWithCondition")).add(_oper);
    }

    public DeployActionsBindingSkeleton() {
        this.impl = new com.gip.www.juno.Deployments.WS.DeployActions.DeployActionsBindingImpl();
    }

    public DeployActionsBindingSkeleton(com.gip.www.juno.Deployments.WS.DeployActions.DeployActions_PortType impl) {
        this.impl = impl;
    }
    public com.gip.www.juno.Gui.WS.Messages.MetaInfoRow_ctype[] getMetaInfo(com.gip.www.juno.Gui.WS.Messages.GetMetaInfoRequest_ctype metaInfoRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.MetaInfoRow_ctype[] ret = impl.getMetaInfo(metaInfoRequest);
        return ret;
    }

    public com.gip.www.juno.Deployments.WS.DeployActions.Messages.Response_ctype getAllRows(com.gip.www.juno.Gui.WS.Messages.GetAllRowsRequest_ctype getAllRowsRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Deployments.WS.DeployActions.Messages.Response_ctype ret = impl.getAllRows(getAllRowsRequest);
        return ret;
    }

    public com.gip.www.juno.Deployments.WS.DeployActions.Messages.Response_ctype searchRows(com.gip.www.juno.Deployments.WS.DeployActions.Messages.SearchRowsRequest_ctype searchRowsRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Deployments.WS.DeployActions.Messages.Response_ctype ret = impl.searchRows(searchRowsRequest);
        return ret;
    }

    public com.gip.www.juno.Deployments.WS.DeployActions.Messages.Response_ctype insertRow(com.gip.www.juno.Deployments.WS.DeployActions.Messages.InsertRowRequest_ctype insertRowRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Deployments.WS.DeployActions.Messages.Response_ctype ret = impl.insertRow(insertRowRequest);
        return ret;
    }

    public com.gip.www.juno.Deployments.WS.DeployActions.Messages.Response_ctype deleteRows(com.gip.www.juno.Deployments.WS.DeployActions.Messages.DeleteRowsRequest_ctype deleteRowsRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Deployments.WS.DeployActions.Messages.Response_ctype ret = impl.deleteRows(deleteRowsRequest);
        return ret;
    }

    public com.gip.www.juno.Deployments.WS.DeployActions.Messages.Response_ctype updateRowPk(com.gip.www.juno.Deployments.WS.DeployActions.Messages.UpdateRowPkRequest_ctype updateRowPkRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Deployments.WS.DeployActions.Messages.Response_ctype ret = impl.updateRowPk(updateRowPkRequest);
        return ret;
    }

    public com.gip.www.juno.Deployments.WS.DeployActions.Messages.Response_ctype countAllRows(com.gip.www.juno.Deployments.WS.DeployActions.Messages.CountAllRowsRequest_ctype countAllRowsRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Deployments.WS.DeployActions.Messages.Response_ctype ret = impl.countAllRows(countAllRowsRequest);
        return ret;
    }

    public com.gip.www.juno.Deployments.WS.DeployActions.Messages.Response_ctype countRowsWithCondition(com.gip.www.juno.Deployments.WS.DeployActions.Messages.CountRowsWithConditionRequest_ctype countRowsWithConditionRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Deployments.WS.DeployActions.Messages.Response_ctype ret = impl.countRowsWithCondition(countRowsWithConditionRequest);
        return ret;
    }

}
