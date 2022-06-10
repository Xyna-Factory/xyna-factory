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
 * ConfigFileBindingStub.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.WS.ConfigFile;

public class ConfigFileBindingStub extends org.apache.axis.client.Stub implements com.gip.www.juno.WS.ConfigFile.ConfigFile_PortType {
    private java.util.Vector cachedSerClasses = new java.util.Vector();
    private java.util.Vector cachedSerQNames = new java.util.Vector();
    private java.util.Vector cachedSerFactories = new java.util.Vector();
    private java.util.Vector cachedDeserFactories = new java.util.Vector();

    static org.apache.axis.description.OperationDesc [] _operations;

    static {
        _operations = new org.apache.axis.description.OperationDesc[21];
        _initOperationDesc1();
        _initOperationDesc2();
        _initOperationDesc3();
    }

    private static void _initOperationDesc1(){
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("TlvToAscii");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TlvToAsciiRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TlvToAsciiRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.TlvToAsciiRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TlvToAsciiResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[0] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("GenerateAsciiFromString");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromStringRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromStringRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromStringRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromStringResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[1] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("GenerateAsciiFromStringV4");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromStringV4Request"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromStringV4Request_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromStringV4Request_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromStringV4Response"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[2] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("ShowPacketsAsAscii");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ShowPacketsAsAsciiRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ShowPacketsAsAsciiRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.ShowPacketsAsAsciiRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ShowPacketsAsAsciiResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[3] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("ShowV4PacketsAsAscii");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ShowV4PacketsAsAsciiRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ShowV4PacketsAsAsciiRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.ShowV4PacketsAsAsciiRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ShowV4PacketsAsAsciiResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[4] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("GenerateAsciiFromTemplateForInitializedCableModem");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForInitializedCableModemRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForInitializedCableModemRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForInitializedCableModemRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForInitializedCableModemResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[5] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("GenerateAsciiFromTemplateForUnregisteredCableModem");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForUnregisteredCableModemRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForUnregisteredCableModemRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForUnregisteredCableModemRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForUnregisteredCableModemResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[6] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("GenerateAsciiFromTemplateForSipMta");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForSipMtaRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForSipMtaRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForSipMtaRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForSipMtaResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[7] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("GenerateAsciiFromTemplateForNcsMta");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForNcsMtaRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForNcsMtaRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForNcsMtaRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForNcsMtaResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[8] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("GenerateAsciiFromTemplateForIsdnMta");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForIsdnMtaRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForIsdnMtaRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForIsdnMtaRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForIsdnMtaResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[9] = oper;

    }

    private static void _initOperationDesc2(){
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("GenerateAsciiFromTemplateForUninitializedMta");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForUninitializedMtaRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForUninitializedMtaRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForUninitializedMtaRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForUninitializedMtaResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[10] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("GenerateAsciiFromTemplateForUnregisteredMta");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForUnregisteredMtaRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForUnregisteredMtaRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForUnregisteredMtaRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForUnregisteredMtaResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[11] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("GenerateTlvFromString");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromStringRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromStringRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromStringRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromStringResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[12] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("GenerateTlvFromStringV4");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromStringV4Request"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromStringV4Request_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromStringV4Request_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromStringV4Response"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[13] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("GenerateTlvFromTemplateForInitializedCableModem");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForInitializedCableModemRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForInitializedCableModemRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForInitializedCableModemRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForInitializedCableModemResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[14] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("GenerateTlvFromTemplateForUnregisteredCableModem");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForUnregisteredCableModemRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForUnregisteredCableModemRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForUnregisteredCableModemRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForUnregisteredCableModemResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[15] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("GenerateTlvFromTemplateForSipMta");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForSipMtaRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForSipMtaRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForSipMtaRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForSipMtaResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[16] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("GenerateTlvFromTemplateForNcsMta");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForNcsMtaRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForNcsMtaRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForNcsMtaRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForNcsMtaResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[17] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("GenerateTlvFromTemplateForIsdnMta");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForIsdnMtaRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForIsdnMtaRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForIsdnMtaRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForIsdnMtaResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[18] = oper;

        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("GenerateTlvFromTemplateForUninitializedMta");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForUninitializedMtaRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForUninitializedMtaRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForUninitializedMtaRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForUninitializedMtaResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[19] = oper;

    }

    private static void _initOperationDesc3(){
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName("GenerateTlvFromTemplateForUnregisteredMta");
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForUnregisteredMtaRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForUnregisteredMtaRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForUnregisteredMtaRequest_ctype.class, false, false);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        oper.setReturnClass(com.gip.www.juno.Gui.WS.Messages.Response_ctype.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForUnregisteredMtaResponse"));
        oper.setStyle(org.apache.axis.constants.Style.DOCUMENT);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[20] = oper;

    }

    public ConfigFileBindingStub() throws org.apache.axis.AxisFault {
         this(null);
    }

    public ConfigFileBindingStub(java.net.URL endpointURL, javax.xml.rpc.Service service) throws org.apache.axis.AxisFault {
         this(service);
         super.cachedEndpoint = endpointURL;
    }

    public ConfigFileBindingStub(javax.xml.rpc.Service service) throws org.apache.axis.AxisFault {
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
            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "CableModemRequest_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.CableModemRequest_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ConfigFileGeneratorParameters_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.ConfigFileGeneratorParameters_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "DeviceDetails_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.DeviceDetails_ctype.class;
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

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromStringInput_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromStringInput_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromStringRequest_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromStringRequest_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromStringV4Input_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromStringV4Input_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromStringV4Request_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromStringV4Request_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForInitializedCableModemInput_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForInitializedCableModemInput_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForInitializedCableModemRequest_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForInitializedCableModemRequest_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForIsdnMtaInput_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForIsdnMtaInput_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForIsdnMtaRequest_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForIsdnMtaRequest_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForNcsMtaInput_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForNcsMtaInput_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForNcsMtaRequest_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForNcsMtaRequest_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForSipMtaInput_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForSipMtaInput_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForSipMtaRequest_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForSipMtaRequest_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForUninitializedMtaInput_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForUninitializedMtaInput_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForUninitializedMtaRequest_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForUninitializedMtaRequest_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForUnregisteredCableModemInput_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForUnregisteredCableModemInput_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForUnregisteredCableModemRequest_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForUnregisteredCableModemRequest_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForUnregisteredMtaInput_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForUnregisteredMtaInput_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForUnregisteredMtaRequest_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForUnregisteredMtaRequest_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromStringInput_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromStringInput_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromStringRequest_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromStringRequest_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromStringV4Input_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromStringV4Input_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromStringV4Request_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromStringV4Request_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForInitializedCableModemInput_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForInitializedCableModemInput_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForInitializedCableModemRequest_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForInitializedCableModemRequest_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForIsdnMtaInput_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForIsdnMtaInput_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForIsdnMtaRequest_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForIsdnMtaRequest_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForNcsMtaInput_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForNcsMtaInput_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForNcsMtaRequest_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForNcsMtaRequest_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForSipMtaInput_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForSipMtaInput_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForSipMtaRequest_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForSipMtaRequest_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForUninitializedMtaInput_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForUninitializedMtaInput_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForUninitializedMtaRequest_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForUninitializedMtaRequest_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForUnregisteredCableModemInput_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForUnregisteredCableModemInput_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForUnregisteredCableModemRequest_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForUnregisteredCableModemRequest_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForUnregisteredMtaInput_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForUnregisteredMtaInput_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForUnregisteredMtaRequest_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForUnregisteredMtaRequest_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "InitializedCableModem_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.InitializedCableModem_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "InputHeaderContent_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.InputHeaderContent_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "IsdnMta_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.IsdnMta_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "MtaRequest_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.MtaRequest_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "NcsMta_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.NcsMta_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Payload_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.Payload_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "PortNumberList_ctype");
            cachedSerQNames.add(qName);
            cls = int[].class;
            cachedSerClasses.add(cls);
            qName = new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int");
            qName2 = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "PortNumber");
            cachedSerFactories.add(new org.apache.axis.encoding.ser.ArraySerializerFactory(qName, qName2));
            cachedDeserFactories.add(new org.apache.axis.encoding.ser.ArrayDeserializerFactory());

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

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ShowPacketsAsAsciiInput_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.ShowPacketsAsAsciiInput_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ShowPacketsAsAsciiRequest_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.ShowPacketsAsAsciiRequest_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ShowV4PacketsAsAsciiInput_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.ShowV4PacketsAsAsciiInput_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ShowV4PacketsAsAsciiRequest_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.ShowV4PacketsAsAsciiRequest_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "SipMta_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.SipMta_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "SipMtaPort_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.SipMtaPort_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "SipMtaPortList_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.SipMtaPortList_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TextConfigGeneratorParameters_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.TextConfigGeneratorParameters_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TextResponse_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.TextResponse_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TlvToAsciiInput_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.TlvToAsciiInput_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TlvToAsciiRequest_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.TlvToAsciiRequest_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TlvToAsciiResponse_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.TlvToAsciiResponse_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UninitializedMta_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.UninitializedMta_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UnregisteredCableModem_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.UnregisteredCableModem_ctype.class;
            cachedSerClasses.add(cls);
            cachedSerFactories.add(beansf);
            cachedDeserFactories.add(beandf);

            qName = new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "UnregisteredMta_ctype");
            cachedSerQNames.add(qName);
            cls = com.gip.www.juno.Gui.WS.Messages.UnregisteredMta_ctype.class;
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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype tlvToAscii(com.gip.www.juno.Gui.WS.Messages.TlvToAsciiRequest_ctype tlvToAsciiRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[0]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("TlvToAscii");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "TlvToAscii"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {tlvToAsciiRequest});

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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromString(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromStringRequest_ctype generateAsciiFromStringRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[1]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("GenerateAsciiFromString");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "GenerateAsciiFromString"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {generateAsciiFromStringRequest});

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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromStringV4(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromStringV4Request_ctype generateAsciiFromStringV4Request) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[2]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("GenerateAsciiFromStringV4");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "GenerateAsciiFromStringV4"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {generateAsciiFromStringV4Request});

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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype showPacketsAsAscii(com.gip.www.juno.Gui.WS.Messages.ShowPacketsAsAsciiRequest_ctype showPacketsAsAsciiRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[3]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("ShowPacketsAsAscii");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "ShowPacketsAsAscii"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {showPacketsAsAsciiRequest});

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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype showV4PacketsAsAscii(com.gip.www.juno.Gui.WS.Messages.ShowV4PacketsAsAsciiRequest_ctype showV4PacketsAsAsciiRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[4]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("ShowV4PacketsAsAscii");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "ShowV4PacketsAsAscii"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {showV4PacketsAsAsciiRequest});

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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromTemplateForInitializedCableModem(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForInitializedCableModemRequest_ctype generateAsciiFromTemplateForInitializedCableModemRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[5]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("GenerateAsciiFromTemplateForInitializedCableModem");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "GenerateAsciiFromTemplateForInitializedCableModem"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {generateAsciiFromTemplateForInitializedCableModemRequest});

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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromTemplateForUnregisteredCableModem(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForUnregisteredCableModemRequest_ctype generateAsciiFromTemplateForUnregisteredCableModemRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[6]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("GenerateAsciiFromTemplateForUnregisteredCableModem");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "GenerateAsciiFromTemplateForUnregisteredCableModem"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {generateAsciiFromTemplateForUnregisteredCableModemRequest});

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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromTemplateForSipMta(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForSipMtaRequest_ctype generateAsciiFromTemplateForSipMtaRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[7]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("GenerateAsciiFromTemplateForSipMta");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "GenerateAsciiFromTemplateForSipMta"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {generateAsciiFromTemplateForSipMtaRequest});

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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromTemplateForNcsMta(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForNcsMtaRequest_ctype generateAsciiFromTemplateForNcsMtaRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[8]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("GenerateAsciiFromTemplateForNcsMta");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "GenerateAsciiFromTemplateForNcsMta"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {generateAsciiFromTemplateForNcsMtaRequest});

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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromTemplateForIsdnMta(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForIsdnMtaRequest_ctype generateAsciiFromTemplateForIsdnMtaRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[9]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("GenerateAsciiFromTemplateForIsdnMta");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "GenerateAsciiFromTemplateForIsdnMta"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {generateAsciiFromTemplateForIsdnMtaRequest});

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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromTemplateForUninitializedMta(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForUninitializedMtaRequest_ctype generateAsciiFromTemplateForUninitializedMtaRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[10]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("GenerateAsciiFromTemplateForUninitializedMta");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "GenerateAsciiFromTemplateForUninitializedMta"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {generateAsciiFromTemplateForUninitializedMtaRequest});

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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromTemplateForUnregisteredMta(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForUnregisteredMtaRequest_ctype generateAsciiFromTemplateForUnregisteredMtaRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[11]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("GenerateAsciiFromTemplateForUnregisteredMta");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "GenerateAsciiFromTemplateForUnregisteredMta"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {generateAsciiFromTemplateForUnregisteredMtaRequest});

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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromString(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromStringRequest_ctype generateTlvFromStringRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[12]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("GenerateTlvFromString");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "GenerateTlvFromString"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {generateTlvFromStringRequest});

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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromStringV4(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromStringV4Request_ctype generateTlvFromStringV4Request) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[13]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("GenerateTlvFromStringV4");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "GenerateTlvFromStringV4"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {generateTlvFromStringV4Request});

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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromTemplateForInitializedCableModem(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForInitializedCableModemRequest_ctype generateTlvFromTemplateForInitializedCableModemRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[14]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("GenerateTlvFromTemplateForInitializedCableModem");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "GenerateTlvFromTemplateForInitializedCableModem"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {generateTlvFromTemplateForInitializedCableModemRequest});

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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromTemplateForUnregisteredCableModem(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForUnregisteredCableModemRequest_ctype generateTlvFromTemplateForUnregisteredCableModemRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[15]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("GenerateTlvFromTemplateForUnregisteredCableModem");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "GenerateTlvFromTemplateForUnregisteredCableModem"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {generateTlvFromTemplateForUnregisteredCableModemRequest});

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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromTemplateForSipMta(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForSipMtaRequest_ctype generateTlvFromTemplateForSipMtaRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[16]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("GenerateTlvFromTemplateForSipMta");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "GenerateTlvFromTemplateForSipMta"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {generateTlvFromTemplateForSipMtaRequest});

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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromTemplateForNcsMta(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForNcsMtaRequest_ctype generateTlvFromTemplateForNcsMtaRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[17]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("GenerateTlvFromTemplateForNcsMta");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "GenerateTlvFromTemplateForNcsMta"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {generateTlvFromTemplateForNcsMtaRequest});

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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromTemplateForIsdnMta(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForIsdnMtaRequest_ctype generateTlvFromTemplateForIsdnMtaRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[18]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("GenerateTlvFromTemplateForIsdnMta");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "GenerateTlvFromTemplateForIsdnMta"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {generateTlvFromTemplateForIsdnMtaRequest});

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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromTemplateForUninitializedMta(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForUninitializedMtaRequest_ctype generateTlvFromTemplateForUninitializedMtaRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[19]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("GenerateTlvFromTemplateForUninitializedMta");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "GenerateTlvFromTemplateForUninitializedMta"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {generateTlvFromTemplateForUninitializedMtaRequest});

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

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromTemplateForUnregisteredMta(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForUnregisteredMtaRequest_ctype generateTlvFromTemplateForUnregisteredMtaRequest) throws java.rmi.RemoteException {
        if (super.cachedEndpoint == null) {
            throw new org.apache.axis.NoEndPointException();
        }
        org.apache.axis.client.Call _call = createCall();
        _call.setOperation(_operations[20]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("GenerateTlvFromTemplateForUnregisteredMta");
        _call.setEncodingStyle(null);
        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("", "GenerateTlvFromTemplateForUnregisteredMta"));

        setRequestHeaders(_call);
        setAttachments(_call);
 try {        java.lang.Object _resp = _call.invoke(new java.lang.Object[] {generateTlvFromTemplateForUnregisteredMtaRequest});

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
