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
 * DhcpdConfBindingStub.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.WS.DhcpdConf;

public class DhcpdConfBindingStub extends org.apache.axis.client.Stub implements com.gip.www.juno.WS.DhcpdConf.DhcpdConf_PortType {
    private java.util.Vector cachedSerClasses = new java.util.Vector();
    private java.util.Vector cachedSerQNames = new java.util.Vector();
    private java.util.Vector cachedSerFactories = new java.util.Vector();
    private java.util.Vector cachedDeserFactories = new java.util.Vector();

    static org.apache.axis.description.OperationDesc [] _operations;

    static {
        _operations = new org.apache.axis.description.OperationDesc[14];
        _initOperationDesc1();
        _initOperationDesc2();
    }

    private static void _initOperationDesc1(){
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("CheckDhcpdConf");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CheckDhcpdConfRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CheckDhcpdConfRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.CheckDhcpdConfRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CheckDhcpdConfResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[0] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("CheckDhcpdConfNewFormat");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CheckDhcpdConfNewFormatRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CheckDhcpdConfRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.CheckDhcpdConfRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CheckDhcpdConfNewFormatResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[1] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("DeployDhcpdConf");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployDhcpdConfRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployDhcpdConfRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.DeployDhcpdConfRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployDhcpdConfResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[2] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("DeployDhcpdConfNewFormat");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployDhcpdConfNewFormatRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployDhcpdConfRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.DeployDhcpdConfRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployDhcpdConfNewFormatResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[3] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("DeployStaticHost");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployStaticHostRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployStaticHostRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.DeployStaticHostRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployStaticHostResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[4] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("DeployStaticHostNewFormat");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployStaticHostNewFormatRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployStaticHostRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.DeployStaticHostRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployStaticHostNewFormatResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[5] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("UndeployStaticHost");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UndeployStaticHostRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UndeployStaticHostRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.UndeployStaticHostRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UndeployStaticHostResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[6] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("UndeployStaticHostNewFormat");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UndeployStaticHostNewFormatRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UndeployStaticHostRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.UndeployStaticHostRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UndeployStaticHostNewFormatResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[7] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("DeployCPE");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployCPERequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployCPERequest_ctype"), com.gip.www.juno.Gui.WS.Messages.DeployCPERequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployCPEResponse_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.DeployCPEResponse_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployCPEResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[8] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("UndeployCPE");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UndeployCPERequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UndeployCPERequest_ctype"), com.gip.www.juno.Gui.WS.Messages.UndeployCPERequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UndeployCPEResponse_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.UndeployCPEResponse_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UndeployCPEResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[9] = oper;

    }

    private static void _initOperationDesc2(){
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("DuplicateForMigration");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DuplicateForMigrationRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DuplicateForMigrationRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.DuplicateForMigrationRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DuplicateForMigrationResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[10] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("DeactivateForMigration");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeactivateForMigrationRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "MigrationTargetIdentifier_ctype"), com.gip.www.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeactivateForMigrationResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[11] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("ActivateForMigration");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ActivateForMigrationRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "MigrationTargetIdentifier_ctype"), com.gip.www.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ActivateForMigrationResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[12] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("DeleteForMigration");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeleteForMigrationRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "MigrationTargetIdentifier_ctype"), com.gip.www.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeleteForMigrationResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[13] = oper;

    }

    public DhcpdConfBindingStub() throws org.apache.axis.AxisFault {
         this(null);
    }

    public DhcpdConfBindingStub(java.net.URL endpointURL, javax.xml.rpc.Service service) throws org.apache.axis.AxisFault {
         this(service);
         super.cachedEndpoint = endpointURL;
    }

    public DhcpdConfBindingStub(javax.xml.rpc.Service service) throws org.apache.axis.AxisFault {
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
            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CheckDhcpdConfInput_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.CheckDhcpdConfInput_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CheckDhcpdConfRequest_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.CheckDhcpdConfRequest_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CPEIdentification_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.CPEIdentification_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployCPERequest_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.DeployCPERequest_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployCPEResponse_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.DeployCPEResponse_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployDhcpdConfInput_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.DeployDhcpdConfInput_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployDhcpdConfRequest_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.DeployDhcpdConfRequest_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployStaticHostInput_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.DeployStaticHostInput_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeployStaticHostRequest_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.DeployStaticHostRequest_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DhcpdConfResponse_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.DhcpdConfResponse_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DuplicateForMigrationRequest_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.DuplicateForMigrationRequest_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

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

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "InputHeaderContent_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "MigrationTargetIdentifier_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "OutputHeaderContent_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.OutputHeaderContent_ctype.class;
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

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UndeployCPERequest_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.UndeployCPERequest_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UndeployCPEResponse_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.UndeployCPEResponse_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UndeployStaticHostInput_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.UndeployStaticHostInput_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UndeployStaticHostRequest_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.UndeployStaticHostRequest_ctype.class;
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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype checkDhcpdConf(com.gip.www.juno.Gui.WS.Messages.CheckDhcpdConfRequest_ctype checkDhcpdConfRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[0]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("CheckDhcpdConf");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "CheckDhcpdConf"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {checkDhcpdConfRequest});

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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype checkDhcpdConfNewFormat(com.gip.www.juno.Gui.WS.Messages.CheckDhcpdConfRequest_ctype checkDhcpdConfNewFormatRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[1]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("CheckDhcpdConfNewFormat");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "CheckDhcpdConfNewFormat"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {checkDhcpdConfNewFormatRequest});

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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype deployDhcpdConf(com.gip.www.juno.Gui.WS.Messages.DeployDhcpdConfRequest_ctype deployDhcpdConfRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[2]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("DeployDhcpdConf");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "DeployDhcpdConf"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {deployDhcpdConfRequest});

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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype deployDhcpdConfNewFormat(com.gip.www.juno.Gui.WS.Messages.DeployDhcpdConfRequest_ctype deployDhcpdConfNewFormatRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[3]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("DeployDhcpdConfNewFormat");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "DeployDhcpdConfNewFormat"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {deployDhcpdConfNewFormatRequest});

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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype deployStaticHost(com.gip.www.juno.Gui.WS.Messages.DeployStaticHostRequest_ctype deployStaticHostRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[4]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("DeployStaticHost");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "DeployStaticHost"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {deployStaticHostRequest});

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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype deployStaticHostNewFormat(com.gip.www.juno.Gui.WS.Messages.DeployStaticHostRequest_ctype deployStaticHostNewFormatRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[5]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("DeployStaticHostNewFormat");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "DeployStaticHostNewFormat"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {deployStaticHostNewFormatRequest});

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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype undeployStaticHost(com.gip.www.juno.Gui.WS.Messages.UndeployStaticHostRequest_ctype undeployStaticHostRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[6]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("UndeployStaticHost");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "UndeployStaticHost"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {undeployStaticHostRequest});

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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype undeployStaticHostNewFormat(com.gip.www.juno.Gui.WS.Messages.UndeployStaticHostRequest_ctype undeployStaticHostNewFormatRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[7]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("UndeployStaticHostNewFormat");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "UndeployStaticHostNewFormat"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {undeployStaticHostNewFormatRequest});

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

    public com.gip.www.juno.Gui.WS.Messages.DeployCPEResponse_ctype deployCPE(com.gip.www.juno.Gui.WS.Messages.DeployCPERequest_ctype deployCPERequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[8]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("DeployCPE");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "DeployCPE"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {deployCPERequest});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.gip.www.juno.Gui.WS.Messages.DeployCPEResponse_ctype) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.gip.www.juno.Gui.WS.Messages.DeployCPEResponse_ctype) org.apache.axis.utils.JavaUtils.convert(_resp, com.gip.www.juno.Gui.WS.Messages.DeployCPEResponse_ctype.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
  throw axisFaultException;
}
    }

    public com.gip.www.juno.Gui.WS.Messages.UndeployCPEResponse_ctype undeployCPE(com.gip.www.juno.Gui.WS.Messages.UndeployCPERequest_ctype undeployCPERequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[9]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("UndeployCPE");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "UndeployCPE"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {undeployCPERequest});

        if (_resp instanceof java.rmi.RemoteException) {
            throw (java.rmi.RemoteException)_resp;
        }
        else {
            extractAttachments(_call);
            try {
                return (com.gip.www.juno.Gui.WS.Messages.UndeployCPEResponse_ctype) _resp;
            } catch (java.lang.Exception _exception) {
                return (com.gip.www.juno.Gui.WS.Messages.UndeployCPEResponse_ctype) org.apache.axis.utils.JavaUtils.convert(_resp, com.gip.www.juno.Gui.WS.Messages.UndeployCPEResponse_ctype.class);
            }
        }
  } catch (org.apache.axis.AxisFault axisFaultException) {
  throw axisFaultException;
}
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype duplicateForMigration(com.gip.www.juno.Gui.WS.Messages.DuplicateForMigrationRequest_ctype duplicateForMigrationRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[10]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("DuplicateForMigration");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "DuplicateForMigration"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {duplicateForMigrationRequest});

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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype deactivateForMigration(com.gip.www.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype deactivateForMigrationRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[11]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("DeactivateForMigration");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "DeactivateForMigration"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {deactivateForMigrationRequest});

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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype activateForMigration(com.gip.www.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype activateForMigrationRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[12]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("ActivateForMigration");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "ActivateForMigration"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {activateForMigrationRequest});

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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype deleteForMigration(com.gip.www.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype deleteForMigrationRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[13]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("DeleteForMigration");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "DeleteForMigration"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {deleteForMigrationRequest});

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
