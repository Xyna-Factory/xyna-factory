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
 * DhcpdConfBindingSkeleton.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.WS.DhcpdConf;

public class DhcpdConfBindingSkeleton implements com.gip.www.juno.WS.DhcpdConf.DhcpdConf_PortType, org.apache.axis.wsdl.Skeleton {
    private com.gip.www.juno.WS.DhcpdConf.DhcpdConf_PortType impl;
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
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CheckDhcpdConfRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CheckDhcpdConfRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.CheckDhcpdConfRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("checkDhcpdConf", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CheckDhcpdConfResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "CheckDhcpdConf"));
        _oper.setSoapAction("CheckDhcpdConf");
        _myOperationsList.add(_oper);
        if (_myOperations.get("checkDhcpdConf") == null) {
            _myOperations.put("checkDhcpdConf", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("checkDhcpdConf")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CheckDhcpdConfNewFormatRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CheckDhcpdConfRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.CheckDhcpdConfRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("checkDhcpdConfNewFormat", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CheckDhcpdConfNewFormatResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "CheckDhcpdConfNewFormat"));
        _oper.setSoapAction("CheckDhcpdConfNewFormat");
        _myOperationsList.add(_oper);
        if (_myOperations.get("checkDhcpdConfNewFormat") == null) {
            _myOperations.put("checkDhcpdConfNewFormat", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("checkDhcpdConfNewFormat")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployDhcpdConfRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployDhcpdConfRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.DeployDhcpdConfRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("deployDhcpdConf", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployDhcpdConfResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "DeployDhcpdConf"));
        _oper.setSoapAction("DeployDhcpdConf");
        _myOperationsList.add(_oper);
        if (_myOperations.get("deployDhcpdConf") == null) {
            _myOperations.put("deployDhcpdConf", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("deployDhcpdConf")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployDhcpdConfNewFormatRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployDhcpdConfRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.DeployDhcpdConfRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("deployDhcpdConfNewFormat", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployDhcpdConfNewFormatResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "DeployDhcpdConfNewFormat"));
        _oper.setSoapAction("DeployDhcpdConfNewFormat");
        _myOperationsList.add(_oper);
        if (_myOperations.get("deployDhcpdConfNewFormat") == null) {
            _myOperations.put("deployDhcpdConfNewFormat", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("deployDhcpdConfNewFormat")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployStaticHostRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployStaticHostRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.DeployStaticHostRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("deployStaticHost", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployStaticHostResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "DeployStaticHost"));
        _oper.setSoapAction("DeployStaticHost");
        _myOperationsList.add(_oper);
        if (_myOperations.get("deployStaticHost") == null) {
            _myOperations.put("deployStaticHost", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("deployStaticHost")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployStaticHostNewFormatRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployStaticHostRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.DeployStaticHostRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("deployStaticHostNewFormat", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployStaticHostNewFormatResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "DeployStaticHostNewFormat"));
        _oper.setSoapAction("DeployStaticHostNewFormat");
        _myOperationsList.add(_oper);
        if (_myOperations.get("deployStaticHostNewFormat") == null) {
            _myOperations.put("deployStaticHostNewFormat", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("deployStaticHostNewFormat")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UndeployStaticHostRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UndeployStaticHostRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.UndeployStaticHostRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("undeployStaticHost", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UndeployStaticHostResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "UndeployStaticHost"));
        _oper.setSoapAction("UndeployStaticHost");
        _myOperationsList.add(_oper);
        if (_myOperations.get("undeployStaticHost") == null) {
            _myOperations.put("undeployStaticHost", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("undeployStaticHost")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UndeployStaticHostNewFormatRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UndeployStaticHostRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.UndeployStaticHostRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("undeployStaticHostNewFormat", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UndeployStaticHostNewFormatResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "UndeployStaticHostNewFormat"));
        _oper.setSoapAction("UndeployStaticHostNewFormat");
        _myOperationsList.add(_oper);
        if (_myOperations.get("undeployStaticHostNewFormat") == null) {
            _myOperations.put("undeployStaticHostNewFormat", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("undeployStaticHostNewFormat")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployCPERequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployCPERequest_ctype"), com.gip.www.juno.Gui.WS.Messages.DeployCPERequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("deployCPE", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployCPEResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployCPEResponse_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "DeployCPE"));
        _oper.setSoapAction("DeployCPE");
        _myOperationsList.add(_oper);
        if (_myOperations.get("deployCPE") == null) {
            _myOperations.put("deployCPE", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("deployCPE")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UndeployCPERequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UndeployCPERequest_ctype"), com.gip.www.juno.Gui.WS.Messages.UndeployCPERequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("undeployCPE", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UndeployCPEResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UndeployCPEResponse_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "UndeployCPE"));
        _oper.setSoapAction("UndeployCPE");
        _myOperationsList.add(_oper);
        if (_myOperations.get("undeployCPE") == null) {
            _myOperations.put("undeployCPE", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("undeployCPE")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DuplicateForMigrationRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DuplicateForMigrationRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.DuplicateForMigrationRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("duplicateForMigration", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DuplicateForMigrationResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "DuplicateForMigration"));
        _oper.setSoapAction("DuplicateForMigration");
        _myOperationsList.add(_oper);
        if (_myOperations.get("duplicateForMigration") == null) {
            _myOperations.put("duplicateForMigration", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("duplicateForMigration")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeactivateForMigrationRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "MigrationTargetIdentifier_ctype"), com.gip.www.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("deactivateForMigration", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeactivateForMigrationResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "DeactivateForMigration"));
        _oper.setSoapAction("DeactivateForMigration");
        _myOperationsList.add(_oper);
        if (_myOperations.get("deactivateForMigration") == null) {
            _myOperations.put("deactivateForMigration", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("deactivateForMigration")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ActivateForMigrationRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "MigrationTargetIdentifier_ctype"), com.gip.www.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("activateForMigration", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ActivateForMigrationResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "ActivateForMigration"));
        _oper.setSoapAction("ActivateForMigration");
        _myOperationsList.add(_oper);
        if (_myOperations.get("activateForMigration") == null) {
            _myOperations.put("activateForMigration", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("activateForMigration")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeleteForMigrationRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "MigrationTargetIdentifier_ctype"), com.gip.www.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("deleteForMigration", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeleteForMigrationResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "DeleteForMigration"));
        _oper.setSoapAction("DeleteForMigration");
        _myOperationsList.add(_oper);
        if (_myOperations.get("deleteForMigration") == null) {
            _myOperations.put("deleteForMigration", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("deleteForMigration")).add(_oper);
    }

    public DhcpdConfBindingSkeleton() {
        this.impl = new com.gip.www.juno.WS.DhcpdConf.DhcpdConfBindingImpl();
    }

    public DhcpdConfBindingSkeleton(com.gip.www.juno.WS.DhcpdConf.DhcpdConf_PortType impl) {
        this.impl = impl;
    }
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype checkDhcpdConf(com.gip.www.juno.Gui.WS.Messages.CheckDhcpdConfRequest_ctype checkDhcpdConfRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.checkDhcpdConf(checkDhcpdConfRequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype checkDhcpdConfNewFormat(com.gip.www.juno.Gui.WS.Messages.CheckDhcpdConfRequest_ctype checkDhcpdConfNewFormatRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.checkDhcpdConfNewFormat(checkDhcpdConfNewFormatRequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype deployDhcpdConf(com.gip.www.juno.Gui.WS.Messages.DeployDhcpdConfRequest_ctype deployDhcpdConfRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.deployDhcpdConf(deployDhcpdConfRequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype deployDhcpdConfNewFormat(com.gip.www.juno.Gui.WS.Messages.DeployDhcpdConfRequest_ctype deployDhcpdConfNewFormatRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.deployDhcpdConfNewFormat(deployDhcpdConfNewFormatRequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype deployStaticHost(com.gip.www.juno.Gui.WS.Messages.DeployStaticHostRequest_ctype deployStaticHostRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.deployStaticHost(deployStaticHostRequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype deployStaticHostNewFormat(com.gip.www.juno.Gui.WS.Messages.DeployStaticHostRequest_ctype deployStaticHostNewFormatRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.deployStaticHostNewFormat(deployStaticHostNewFormatRequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype undeployStaticHost(com.gip.www.juno.Gui.WS.Messages.UndeployStaticHostRequest_ctype undeployStaticHostRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.undeployStaticHost(undeployStaticHostRequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype undeployStaticHostNewFormat(com.gip.www.juno.Gui.WS.Messages.UndeployStaticHostRequest_ctype undeployStaticHostNewFormatRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.undeployStaticHostNewFormat(undeployStaticHostNewFormatRequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.DeployCPEResponse_ctype deployCPE(com.gip.www.juno.Gui.WS.Messages.DeployCPERequest_ctype deployCPERequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.DeployCPEResponse_ctype ret = impl.deployCPE(deployCPERequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.UndeployCPEResponse_ctype undeployCPE(com.gip.www.juno.Gui.WS.Messages.UndeployCPERequest_ctype undeployCPERequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.UndeployCPEResponse_ctype ret = impl.undeployCPE(undeployCPERequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype duplicateForMigration(com.gip.www.juno.Gui.WS.Messages.DuplicateForMigrationRequest_ctype duplicateForMigrationRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.duplicateForMigration(duplicateForMigrationRequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype deactivateForMigration(com.gip.www.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype deactivateForMigrationRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.deactivateForMigration(deactivateForMigrationRequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype activateForMigration(com.gip.www.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype activateForMigrationRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.activateForMigration(activateForMigrationRequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype deleteForMigration(com.gip.www.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype deleteForMigrationRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.deleteForMigration(deleteForMigrationRequest);
        return ret;
    }

}
