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
 * Optionsv4_PortType.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.DHCP.WS.Optionsv4;

public interface Optionsv4_PortType extends java.rmi.Remote {
    public com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Response_ctype getMetaInfo(com.gip.www.juno.DHCP.WS.Optionsv4.Messages.GetMetaInfoRequest_ctype getMetaInfoRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Response_ctype getAllRows(com.gip.www.juno.DHCP.WS.Optionsv4.Messages.GetAllRowsRequest_ctype getAllRowsRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Response_ctype searchRows(com.gip.www.juno.DHCP.WS.Optionsv4.Messages.SearchRowsRequest_ctype searchRowsRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Response_ctype updateRow(com.gip.www.juno.DHCP.WS.Optionsv4.Messages.UpdateRowRequest_ctype updateRowRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Response_ctype insertRow(com.gip.www.juno.DHCP.WS.Optionsv4.Messages.InsertRowRequest_ctype insertRowRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Response_ctype deleteRows(com.gip.www.juno.DHCP.WS.Optionsv4.Messages.DeleteRowsRequest_ctype deleteRowsRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Response_ctype countRowsWithCondition(com.gip.www.juno.DHCP.WS.Optionsv4.Messages.CountRowsWithConditionRequest_ctype countRowsWithConditionRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Response_ctype countAllRows(com.gip.www.juno.DHCP.WS.Optionsv4.Messages.CountAllRowsRequest_ctype countAllRowsRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.DHCP.WS.Optionsv4.Messages.Response_ctype deployOnDPP(com.gip.www.juno.DHCP.WS.Optionsv4.Messages.DeployOnDPPRequest_ctype deployOnDPPRequest) throws java.rmi.RemoteException;
}
