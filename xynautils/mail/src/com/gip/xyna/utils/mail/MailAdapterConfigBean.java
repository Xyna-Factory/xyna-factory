/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.utils.mail;

/**
 * define variables used within the ConfigBean
 * 
 * 
 */
public class MailAdapterConfigBean extends ConfigBean {

   protected String mailHost;
   protected String mailPort;
   protected String mailUser;
   protected String mailUserPwd;
   protected String mailUserAddr;
   protected String smtpPort;
   protected String mailFolder;
   protected String mailDebug;
   protected String mailAuth;
   protected String popPort;
   protected String mailDummy;
   protected String sSLSocketFactory;
   protected String socketFactoryfallback;
   protected String mailTimeOut;
   protected String enableTLS;
   protected String transportProtocoll;

   /**
    * Required filling variables used within the ConfigBean hardcode default
    * properties here if you wish
    */
   public MailAdapterConfigBean() {

      mailHost = "";
      mailPort = "";
      mailUser = "";
      mailUserPwd = "";
      mailUserAddr = "";
      smtpPort = "";
      mailFolder = "";
      mailDebug = "";
      mailAuth = "";
      popPort = "";
      sSLSocketFactory = "javax.net.ssl.SSLSocketFactory";
      socketFactoryfallback = "";
      transportProtocoll = "";
      mailTimeOut = "";
      enableTLS = "";
      mailDummy = "";
   }

   /**
    * 
    * toString extends the variables used within this class to their final
    * lenght: VariableName: Variable is required to fill the properties object
    * used in Emailer.java correctly.
    * 
    * @returns String
    */
   public String toString() {

      return "mailHost: " + mailHost + "\n" + "mailPort: " + mailPort + "\n"
            + "mailUser: " + mailUser + "\n" + "mailUserPwd: " + mailUserPwd
            + "\n" + "mailUserAddr: " + mailUserAddr + "\n" + "mailSmptPort: "
            + smtpPort + "\n" + "mailFolder: " + mailFolder + "\n"
            + "mailDebug: " + mailDebug + "\n" + "mailAuth: " + mailAuth + "\n"
            + "popPort: " + popPort + "\n" + "sSLSocketFactory: "
            + sSLSocketFactory + "\n" + "socketFactoryfallback: "
            + socketFactoryfallback + "\n" + "mailTimeOut: " + mailTimeOut
            + "\n" + "enableTLS: " + enableTLS + "\n" + "transportProtocoll: "
            + transportProtocoll + "\n" + "mailDummy: " + mailDummy;
   }

   public String getMailAuth() {
      return mailAuth;
   }

   public void setMailAuth(String mailAuth) {
      this.mailAuth = mailAuth;
   }

   public String getMailDebug() {
      return mailDebug;
   }

   public void setMailDebug(String mailDebug) {
      this.mailDebug = mailDebug;
   }

   public String getMailFolder() {
      return mailFolder;
   }

   public void setMailFolder(String mailFolder) {
      this.mailFolder = mailFolder;
   }

   public String getTransportProtocoll() {
      return transportProtocoll;
   }

   public void setTransportProtocoll(String transportProtocoll) {
      this.transportProtocoll = transportProtocoll;
   }

   public String getSocketFactoryFallback() {
      return socketFactoryfallback;
   }

   public void setSocketFactoryFallback(String socketFactoryfallback) {
      this.socketFactoryfallback = socketFactoryfallback;
   }

   public String getEnableTLS() {
      return enableTLS;
   }

   public void setEnableTLS(String enableTLS) {
      this.enableTLS = enableTLS;
   }

   public String getMailTimeOut() {
      return mailTimeOut;
   }

   public void setMailTimeOut(String mailTimeOut) {
      this.mailTimeOut = mailTimeOut;
   }

   public String getsSLSocketFactory() {
      return sSLSocketFactory;
   }

   public void setsSLSocketFactory(String sSLSocketFactory) {
      this.sSLSocketFactory = sSLSocketFactory;
   }

   public String getMailDummy() {
      return mailDummy;
   }

   public void setMailDummy(String mailDummy) {
      this.mailDummy = mailDummy;
   }

   public String getpopPort() {
      return popPort;
   }

   public void setpopPort(String popPort) {
      this.popPort = popPort;
   }

   public String getMailHost() {
      return mailHost;
   }

   public void setMailHost(String mailHost) {
      this.mailHost = mailHost;
   }

   public String getMailPort() {
      return mailPort;
   }

   public void setMailPort(String mailPort) {
      this.mailPort = mailPort;
   }

   public String getMailUser() {
      return mailUser;
   }

   public void setMailUser(String mailUser) {
      this.mailUser = mailUser;
   }

   public String getMailUserAddr() {
      return mailUserAddr;
   }

   public String getsmtpPort() {
      return smtpPort;
   }

   public void setsmtpPort(String mailSmtpPort) {
      this.smtpPort = mailSmtpPort;
   }

   public void setMailUserAddr(String mailUserAddr) {
      this.mailUserAddr = mailUserAddr;
   }

   public String getMailUserPwd() {
      return mailUserPwd;
   }

   public void setMailUserPwd(String mailUserPwd) {
      this.mailUserPwd = mailUserPwd;
   }

}
