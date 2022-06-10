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
 * CheckStatus_PortType.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.WS.CheckStatus;

public interface CheckStatus_PortType extends java.rmi.Remote {
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype checkStatusForIp(com.gip.www.juno.Gui.WS.Messages.CheckStatusForIpRequest_ctype checkStatusForIpRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype checkStatusForIpv6(com.gip.www.juno.Gui.WS.Messages.CheckStatusForIpRequest_ctype checkStatusForIpv6Request) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype getInstanceInfoList(com.gip.www.juno.Gui.WS.Messages.GetInstanceInfoListRequest_ctype getInstanceInfoListRequest) throws java.rmi.RemoteException;
}
