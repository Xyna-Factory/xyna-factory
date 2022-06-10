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
 * ConfigFileBindingSkeleton.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.WS.ConfigFile;

public class ConfigFileBindingSkeleton implements com.gip.www.juno.WS.ConfigFile.ConfigFile_PortType, org.apache.axis.wsdl.Skeleton {
    private com.gip.www.juno.WS.ConfigFile.ConfigFile_PortType impl;
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
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TlvToAsciiRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TlvToAsciiRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.TlvToAsciiRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("tlvToAscii", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "TlvToAsciiResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "TlvToAscii"));
        _oper.setSoapAction("TlvToAscii");
        _myOperationsList.add(_oper);
        if (_myOperations.get("tlvToAscii") == null) {
            _myOperations.put("tlvToAscii", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("tlvToAscii")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromStringRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromStringRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromStringRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("generateAsciiFromString", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromStringResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "GenerateAsciiFromString"));
        _oper.setSoapAction("GenerateAsciiFromString");
        _myOperationsList.add(_oper);
        if (_myOperations.get("generateAsciiFromString") == null) {
            _myOperations.put("generateAsciiFromString", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("generateAsciiFromString")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromStringV4Request"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromStringV4Request_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromStringV4Request_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("generateAsciiFromStringV4", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromStringV4Response"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "GenerateAsciiFromStringV4"));
        _oper.setSoapAction("GenerateAsciiFromStringV4");
        _myOperationsList.add(_oper);
        if (_myOperations.get("generateAsciiFromStringV4") == null) {
            _myOperations.put("generateAsciiFromStringV4", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("generateAsciiFromStringV4")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ShowPacketsAsAsciiRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ShowPacketsAsAsciiRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.ShowPacketsAsAsciiRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("showPacketsAsAscii", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ShowPacketsAsAsciiResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "ShowPacketsAsAscii"));
        _oper.setSoapAction("ShowPacketsAsAscii");
        _myOperationsList.add(_oper);
        if (_myOperations.get("showPacketsAsAscii") == null) {
            _myOperations.put("showPacketsAsAscii", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("showPacketsAsAscii")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ShowV4PacketsAsAsciiRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ShowV4PacketsAsAsciiRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.ShowV4PacketsAsAsciiRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("showV4PacketsAsAscii", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "ShowV4PacketsAsAsciiResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "ShowV4PacketsAsAscii"));
        _oper.setSoapAction("ShowV4PacketsAsAscii");
        _myOperationsList.add(_oper);
        if (_myOperations.get("showV4PacketsAsAscii") == null) {
            _myOperations.put("showV4PacketsAsAscii", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("showV4PacketsAsAscii")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForInitializedCableModemRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForInitializedCableModemRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForInitializedCableModemRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("generateAsciiFromTemplateForInitializedCableModem", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForInitializedCableModemResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "GenerateAsciiFromTemplateForInitializedCableModem"));
        _oper.setSoapAction("GenerateAsciiFromTemplateForInitializedCableModem");
        _myOperationsList.add(_oper);
        if (_myOperations.get("generateAsciiFromTemplateForInitializedCableModem") == null) {
            _myOperations.put("generateAsciiFromTemplateForInitializedCableModem", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("generateAsciiFromTemplateForInitializedCableModem")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForUnregisteredCableModemRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForUnregisteredCableModemRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForUnregisteredCableModemRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("generateAsciiFromTemplateForUnregisteredCableModem", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForUnregisteredCableModemResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "GenerateAsciiFromTemplateForUnregisteredCableModem"));
        _oper.setSoapAction("GenerateAsciiFromTemplateForUnregisteredCableModem");
        _myOperationsList.add(_oper);
        if (_myOperations.get("generateAsciiFromTemplateForUnregisteredCableModem") == null) {
            _myOperations.put("generateAsciiFromTemplateForUnregisteredCableModem", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("generateAsciiFromTemplateForUnregisteredCableModem")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForSipMtaRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForSipMtaRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForSipMtaRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("generateAsciiFromTemplateForSipMta", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForSipMtaResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "GenerateAsciiFromTemplateForSipMta"));
        _oper.setSoapAction("GenerateAsciiFromTemplateForSipMta");
        _myOperationsList.add(_oper);
        if (_myOperations.get("generateAsciiFromTemplateForSipMta") == null) {
            _myOperations.put("generateAsciiFromTemplateForSipMta", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("generateAsciiFromTemplateForSipMta")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForNcsMtaRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForNcsMtaRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForNcsMtaRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("generateAsciiFromTemplateForNcsMta", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForNcsMtaResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "GenerateAsciiFromTemplateForNcsMta"));
        _oper.setSoapAction("GenerateAsciiFromTemplateForNcsMta");
        _myOperationsList.add(_oper);
        if (_myOperations.get("generateAsciiFromTemplateForNcsMta") == null) {
            _myOperations.put("generateAsciiFromTemplateForNcsMta", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("generateAsciiFromTemplateForNcsMta")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForIsdnMtaRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForIsdnMtaRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForIsdnMtaRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("generateAsciiFromTemplateForIsdnMta", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForIsdnMtaResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "GenerateAsciiFromTemplateForIsdnMta"));
        _oper.setSoapAction("GenerateAsciiFromTemplateForIsdnMta");
        _myOperationsList.add(_oper);
        if (_myOperations.get("generateAsciiFromTemplateForIsdnMta") == null) {
            _myOperations.put("generateAsciiFromTemplateForIsdnMta", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("generateAsciiFromTemplateForIsdnMta")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForUninitializedMtaRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForUninitializedMtaRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForUninitializedMtaRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("generateAsciiFromTemplateForUninitializedMta", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForUninitializedMtaResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "GenerateAsciiFromTemplateForUninitializedMta"));
        _oper.setSoapAction("GenerateAsciiFromTemplateForUninitializedMta");
        _myOperationsList.add(_oper);
        if (_myOperations.get("generateAsciiFromTemplateForUninitializedMta") == null) {
            _myOperations.put("generateAsciiFromTemplateForUninitializedMta", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("generateAsciiFromTemplateForUninitializedMta")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForUnregisteredMtaRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForUnregisteredMtaRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForUnregisteredMtaRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("generateAsciiFromTemplateForUnregisteredMta", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateAsciiFromTemplateForUnregisteredMtaResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "GenerateAsciiFromTemplateForUnregisteredMta"));
        _oper.setSoapAction("GenerateAsciiFromTemplateForUnregisteredMta");
        _myOperationsList.add(_oper);
        if (_myOperations.get("generateAsciiFromTemplateForUnregisteredMta") == null) {
            _myOperations.put("generateAsciiFromTemplateForUnregisteredMta", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("generateAsciiFromTemplateForUnregisteredMta")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromStringRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromStringRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromStringRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("generateTlvFromString", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromStringResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "GenerateTlvFromString"));
        _oper.setSoapAction("GenerateTlvFromString");
        _myOperationsList.add(_oper);
        if (_myOperations.get("generateTlvFromString") == null) {
            _myOperations.put("generateTlvFromString", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("generateTlvFromString")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromStringV4Request"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromStringV4Request_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromStringV4Request_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("generateTlvFromStringV4", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromStringV4Response"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "GenerateTlvFromStringV4"));
        _oper.setSoapAction("GenerateTlvFromStringV4");
        _myOperationsList.add(_oper);
        if (_myOperations.get("generateTlvFromStringV4") == null) {
            _myOperations.put("generateTlvFromStringV4", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("generateTlvFromStringV4")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForInitializedCableModemRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForInitializedCableModemRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForInitializedCableModemRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("generateTlvFromTemplateForInitializedCableModem", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForInitializedCableModemResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "GenerateTlvFromTemplateForInitializedCableModem"));
        _oper.setSoapAction("GenerateTlvFromTemplateForInitializedCableModem");
        _myOperationsList.add(_oper);
        if (_myOperations.get("generateTlvFromTemplateForInitializedCableModem") == null) {
            _myOperations.put("generateTlvFromTemplateForInitializedCableModem", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("generateTlvFromTemplateForInitializedCableModem")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForUnregisteredCableModemRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForUnregisteredCableModemRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForUnregisteredCableModemRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("generateTlvFromTemplateForUnregisteredCableModem", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForUnregisteredCableModemResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "GenerateTlvFromTemplateForUnregisteredCableModem"));
        _oper.setSoapAction("GenerateTlvFromTemplateForUnregisteredCableModem");
        _myOperationsList.add(_oper);
        if (_myOperations.get("generateTlvFromTemplateForUnregisteredCableModem") == null) {
            _myOperations.put("generateTlvFromTemplateForUnregisteredCableModem", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("generateTlvFromTemplateForUnregisteredCableModem")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForSipMtaRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForSipMtaRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForSipMtaRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("generateTlvFromTemplateForSipMta", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForSipMtaResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "GenerateTlvFromTemplateForSipMta"));
        _oper.setSoapAction("GenerateTlvFromTemplateForSipMta");
        _myOperationsList.add(_oper);
        if (_myOperations.get("generateTlvFromTemplateForSipMta") == null) {
            _myOperations.put("generateTlvFromTemplateForSipMta", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("generateTlvFromTemplateForSipMta")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForNcsMtaRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForNcsMtaRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForNcsMtaRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("generateTlvFromTemplateForNcsMta", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForNcsMtaResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "GenerateTlvFromTemplateForNcsMta"));
        _oper.setSoapAction("GenerateTlvFromTemplateForNcsMta");
        _myOperationsList.add(_oper);
        if (_myOperations.get("generateTlvFromTemplateForNcsMta") == null) {
            _myOperations.put("generateTlvFromTemplateForNcsMta", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("generateTlvFromTemplateForNcsMta")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForIsdnMtaRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForIsdnMtaRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForIsdnMtaRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("generateTlvFromTemplateForIsdnMta", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForIsdnMtaResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "GenerateTlvFromTemplateForIsdnMta"));
        _oper.setSoapAction("GenerateTlvFromTemplateForIsdnMta");
        _myOperationsList.add(_oper);
        if (_myOperations.get("generateTlvFromTemplateForIsdnMta") == null) {
            _myOperations.put("generateTlvFromTemplateForIsdnMta", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("generateTlvFromTemplateForIsdnMta")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForUninitializedMtaRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForUninitializedMtaRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForUninitializedMtaRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("generateTlvFromTemplateForUninitializedMta", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForUninitializedMtaResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "GenerateTlvFromTemplateForUninitializedMta"));
        _oper.setSoapAction("GenerateTlvFromTemplateForUninitializedMta");
        _myOperationsList.add(_oper);
        if (_myOperations.get("generateTlvFromTemplateForUninitializedMta") == null) {
            _myOperations.put("generateTlvFromTemplateForUninitializedMta", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("generateTlvFromTemplateForUninitializedMta")).add(_oper);
        _params = new org.apache.axis.description.ParameterDesc [] {
            new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForUnregisteredMtaRequest"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForUnregisteredMtaRequest_ctype"), com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForUnregisteredMtaRequest_ctype.class, false, false), 
        };
        _oper = new org.apache.axis.description.OperationDesc("generateTlvFromTemplateForUnregisteredMta", _params, new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "GenerateTlvFromTemplateForUnregisteredMtaResponse"));
        _oper.setReturnType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "Response_ctype"));
        _oper.setElementQName(new javax.xml.namespace.QName("", "GenerateTlvFromTemplateForUnregisteredMta"));
        _oper.setSoapAction("GenerateTlvFromTemplateForUnregisteredMta");
        _myOperationsList.add(_oper);
        if (_myOperations.get("generateTlvFromTemplateForUnregisteredMta") == null) {
            _myOperations.put("generateTlvFromTemplateForUnregisteredMta", new java.util.ArrayList());
        }
        ((java.util.List)_myOperations.get("generateTlvFromTemplateForUnregisteredMta")).add(_oper);
    }

    public ConfigFileBindingSkeleton() {
        this.impl = new com.gip.www.juno.WS.ConfigFile.ConfigFileBindingImpl();
    }

    public ConfigFileBindingSkeleton(com.gip.www.juno.WS.ConfigFile.ConfigFile_PortType impl) {
        this.impl = impl;
    }
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype tlvToAscii(com.gip.www.juno.Gui.WS.Messages.TlvToAsciiRequest_ctype tlvToAsciiRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.tlvToAscii(tlvToAsciiRequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromString(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromStringRequest_ctype generateAsciiFromStringRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.generateAsciiFromString(generateAsciiFromStringRequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromStringV4(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromStringV4Request_ctype generateAsciiFromStringV4Request) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.generateAsciiFromStringV4(generateAsciiFromStringV4Request);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype showPacketsAsAscii(com.gip.www.juno.Gui.WS.Messages.ShowPacketsAsAsciiRequest_ctype showPacketsAsAsciiRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.showPacketsAsAscii(showPacketsAsAsciiRequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype showV4PacketsAsAscii(com.gip.www.juno.Gui.WS.Messages.ShowV4PacketsAsAsciiRequest_ctype showV4PacketsAsAsciiRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.showV4PacketsAsAscii(showV4PacketsAsAsciiRequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromTemplateForInitializedCableModem(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForInitializedCableModemRequest_ctype generateAsciiFromTemplateForInitializedCableModemRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.generateAsciiFromTemplateForInitializedCableModem(generateAsciiFromTemplateForInitializedCableModemRequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromTemplateForUnregisteredCableModem(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForUnregisteredCableModemRequest_ctype generateAsciiFromTemplateForUnregisteredCableModemRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.generateAsciiFromTemplateForUnregisteredCableModem(generateAsciiFromTemplateForUnregisteredCableModemRequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromTemplateForSipMta(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForSipMtaRequest_ctype generateAsciiFromTemplateForSipMtaRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.generateAsciiFromTemplateForSipMta(generateAsciiFromTemplateForSipMtaRequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromTemplateForNcsMta(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForNcsMtaRequest_ctype generateAsciiFromTemplateForNcsMtaRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.generateAsciiFromTemplateForNcsMta(generateAsciiFromTemplateForNcsMtaRequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromTemplateForIsdnMta(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForIsdnMtaRequest_ctype generateAsciiFromTemplateForIsdnMtaRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.generateAsciiFromTemplateForIsdnMta(generateAsciiFromTemplateForIsdnMtaRequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromTemplateForUninitializedMta(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForUninitializedMtaRequest_ctype generateAsciiFromTemplateForUninitializedMtaRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.generateAsciiFromTemplateForUninitializedMta(generateAsciiFromTemplateForUninitializedMtaRequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromTemplateForUnregisteredMta(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForUnregisteredMtaRequest_ctype generateAsciiFromTemplateForUnregisteredMtaRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.generateAsciiFromTemplateForUnregisteredMta(generateAsciiFromTemplateForUnregisteredMtaRequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromString(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromStringRequest_ctype generateTlvFromStringRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.generateTlvFromString(generateTlvFromStringRequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromStringV4(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromStringV4Request_ctype generateTlvFromStringV4Request) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.generateTlvFromStringV4(generateTlvFromStringV4Request);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromTemplateForInitializedCableModem(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForInitializedCableModemRequest_ctype generateTlvFromTemplateForInitializedCableModemRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.generateTlvFromTemplateForInitializedCableModem(generateTlvFromTemplateForInitializedCableModemRequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromTemplateForUnregisteredCableModem(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForUnregisteredCableModemRequest_ctype generateTlvFromTemplateForUnregisteredCableModemRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.generateTlvFromTemplateForUnregisteredCableModem(generateTlvFromTemplateForUnregisteredCableModemRequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromTemplateForSipMta(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForSipMtaRequest_ctype generateTlvFromTemplateForSipMtaRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.generateTlvFromTemplateForSipMta(generateTlvFromTemplateForSipMtaRequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromTemplateForNcsMta(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForNcsMtaRequest_ctype generateTlvFromTemplateForNcsMtaRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.generateTlvFromTemplateForNcsMta(generateTlvFromTemplateForNcsMtaRequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromTemplateForIsdnMta(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForIsdnMtaRequest_ctype generateTlvFromTemplateForIsdnMtaRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.generateTlvFromTemplateForIsdnMta(generateTlvFromTemplateForIsdnMtaRequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromTemplateForUninitializedMta(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForUninitializedMtaRequest_ctype generateTlvFromTemplateForUninitializedMtaRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.generateTlvFromTemplateForUninitializedMta(generateTlvFromTemplateForUninitializedMtaRequest);
        return ret;
    }

    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromTemplateForUnregisteredMta(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForUnregisteredMtaRequest_ctype generateTlvFromTemplateForUnregisteredMtaRequest) throws java.rmi.RemoteException
    {
        com.gip.www.juno.Gui.WS.Messages.Response_ctype ret = impl.generateTlvFromTemplateForUnregisteredMta(generateTlvFromTemplateForUnregisteredMtaRequest);
        return ret;
    }

}
