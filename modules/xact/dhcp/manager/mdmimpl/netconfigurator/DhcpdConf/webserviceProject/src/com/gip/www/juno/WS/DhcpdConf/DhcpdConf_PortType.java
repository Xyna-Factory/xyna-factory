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
 * DhcpdConf_PortType.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.WS.DhcpdConf;

public interface DhcpdConf_PortType extends java.rmi.Remote {
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype checkDhcpdConf(com.gip.www.juno.Gui.WS.Messages.CheckDhcpdConfRequest_ctype checkDhcpdConfRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype checkDhcpdConfNewFormat(com.gip.www.juno.Gui.WS.Messages.CheckDhcpdConfRequest_ctype checkDhcpdConfNewFormatRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype deployDhcpdConf(com.gip.www.juno.Gui.WS.Messages.DeployDhcpdConfRequest_ctype deployDhcpdConfRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype deployDhcpdConfNewFormat(com.gip.www.juno.Gui.WS.Messages.DeployDhcpdConfRequest_ctype deployDhcpdConfNewFormatRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype deployStaticHost(com.gip.www.juno.Gui.WS.Messages.DeployStaticHostRequest_ctype deployStaticHostRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype deployStaticHostNewFormat(com.gip.www.juno.Gui.WS.Messages.DeployStaticHostRequest_ctype deployStaticHostNewFormatRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype undeployStaticHost(com.gip.www.juno.Gui.WS.Messages.UndeployStaticHostRequest_ctype undeployStaticHostRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype undeployStaticHostNewFormat(com.gip.www.juno.Gui.WS.Messages.UndeployStaticHostRequest_ctype undeployStaticHostNewFormatRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.DeployCPEResponse_ctype deployCPE(com.gip.www.juno.Gui.WS.Messages.DeployCPERequest_ctype deployCPERequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.UndeployCPEResponse_ctype undeployCPE(com.gip.www.juno.Gui.WS.Messages.UndeployCPERequest_ctype undeployCPERequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype duplicateForMigration(com.gip.www.juno.Gui.WS.Messages.DuplicateForMigrationRequest_ctype duplicateForMigrationRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype deactivateForMigration(com.gip.www.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype deactivateForMigrationRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype activateForMigration(com.gip.www.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype activateForMigrationRequest) throws java.rmi.RemoteException;
    public com.gip.www.juno.Gui.WS.Messages.Response_ctype deleteForMigration(com.gip.www.juno.Gui.WS.Messages.MigrationTargetIdentifier_ctype deleteForMigrationRequest) throws java.rmi.RemoteException;
}
