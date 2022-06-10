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
 * ConfigFile_PortType.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.WS.ConfigFile;

public interface ConfigFile_PortType extends java.rmi.Remote {
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype tlvToAscii(com.gip.www.juno.Gui.WS.Messages.TlvToAsciiRequest_ctype tlvToAsciiRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromString(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromStringRequest_ctype generateAsciiFromStringRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromStringV4(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromStringV4Request_ctype generateAsciiFromStringV4Request) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype showPacketsAsAscii(com.gip.www.juno.Gui.WS.Messages.ShowPacketsAsAsciiRequest_ctype showPacketsAsAsciiRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype showV4PacketsAsAscii(com.gip.www.juno.Gui.WS.Messages.ShowV4PacketsAsAsciiRequest_ctype showV4PacketsAsAsciiRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromTemplateForInitializedCableModem(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForInitializedCableModemRequest_ctype generateAsciiFromTemplateForInitializedCableModemRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromTemplateForUnregisteredCableModem(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForUnregisteredCableModemRequest_ctype generateAsciiFromTemplateForUnregisteredCableModemRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromTemplateForSipMta(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForSipMtaRequest_ctype generateAsciiFromTemplateForSipMtaRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromTemplateForNcsMta(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForNcsMtaRequest_ctype generateAsciiFromTemplateForNcsMtaRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromTemplateForIsdnMta(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForIsdnMtaRequest_ctype generateAsciiFromTemplateForIsdnMtaRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromTemplateForUninitializedMta(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForUninitializedMtaRequest_ctype generateAsciiFromTemplateForUninitializedMtaRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateAsciiFromTemplateForUnregisteredMta(com.gip.www.juno.Gui.WS.Messages.GenerateAsciiFromTemplateForUnregisteredMtaRequest_ctype generateAsciiFromTemplateForUnregisteredMtaRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromString(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromStringRequest_ctype generateTlvFromStringRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromStringV4(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromStringV4Request_ctype generateTlvFromStringV4Request) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromTemplateForInitializedCableModem(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForInitializedCableModemRequest_ctype generateTlvFromTemplateForInitializedCableModemRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromTemplateForUnregisteredCableModem(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForUnregisteredCableModemRequest_ctype generateTlvFromTemplateForUnregisteredCableModemRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromTemplateForSipMta(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForSipMtaRequest_ctype generateTlvFromTemplateForSipMtaRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromTemplateForNcsMta(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForNcsMtaRequest_ctype generateTlvFromTemplateForNcsMtaRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromTemplateForIsdnMta(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForIsdnMtaRequest_ctype generateTlvFromTemplateForIsdnMtaRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromTemplateForUninitializedMta(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForUninitializedMtaRequest_ctype generateTlvFromTemplateForUninitializedMtaRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype generateTlvFromTemplateForUnregisteredMta(com.gip.www.juno.Gui.WS.Messages.GenerateTlvFromTemplateForUnregisteredMtaRequest_ctype generateTlvFromTemplateForUnregisteredMtaRequest) throws java.rmi.RemoteException;
}
