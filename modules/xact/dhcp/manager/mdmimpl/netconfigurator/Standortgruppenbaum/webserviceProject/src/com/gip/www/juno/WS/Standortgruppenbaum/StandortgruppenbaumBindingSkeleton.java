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
 * StandortgruppenbaumBindingSkeleton.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.WS.Standortgruppenbaum;

public class StandortgruppenbaumBindingSkeleton implements com.gip.www.juno.WS.Standortgruppenbaum.Standortgruppenbaum_PortType, org.apache.axis.wsdl.Skeleton {
    private com.gip.www.juno.WS.Standortgruppenbaum.Standortgruppenbaum_PortType impl;
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
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "GetTreeStringRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "GetTreeStringRequest_ctype"), com.gip.www.juno.WS.Standortgruppenbaum.Messages.GetTreeStringRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("getTreeString", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "GetTreeStringOutput"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/WS/Standortgruppenbaum/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "GetTreeString"));
        _oper.setSoapAction("GetTreeString");
        _myOperationsList.add(_oper);
        if (_myOperations.get("getTreeString") == null) {
            _myOperations.put("getTreeString", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("getTreeString")).add(_oper);
    }

    public StandortgruppenbaumBindingSkeleton() {
        this.impl = new com.gip.www.juno.WS.Standortgruppenbaum.StandortgruppenbaumBindingImpl();
    }

    public StandortgruppenbaumBindingSkeleton(com.gip.www.juno.WS.Standortgruppenbaum.Standortgruppenbaum_PortType impl) {
        this.impl = impl;
    }
    public com.gip.www.juno.WS.Standortgruppenbaum.Messages.Response_ctype getTreeString(com.gip.www.juno.WS.Standortgruppenbaum.Messages.GetTreeStringRequest_ctype getTreeStringRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.WS.Standortgruppenbaum.Messages.Response_ctype ret = impl.getTreeString(getTreeStringRequest);
        return ret;
    }

}
