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
package com.gip.xyna.utils.ssh;

// import org.apache.commons.codec.binary.Base64;
import java.util.Base64;
import org.apache.log4j.Logger;

import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class Ssh {
  static Logger logger = Logger.getLogger(Ssh.class.getName());

  private String user;
  private String host;
  private UserInfoImpl userInfo;
  protected Session session;
  private boolean connected;
  private String hostRsaKey;
  private String password;
  private String privKey;
  private String pubKey;
  private String passPhrase;
    
  public Ssh(String user, String host, final String password) {
    this.user = user;
    this.host = host;
    this.userInfo = new UserInfoImpl(password); 
    this.password=password;
  }
  
  /**
   * Dieser Konstruktor legt eine flache Kopie an, falls die �bergebene SSH-Instanz connected ist.
   * Der Sinn ist, dass mit einer SSH-Session mehrere SExec als auch SCP verwendet werden k�nnen,
   * ohne das ein erneuter Login n�tig ist.
   * @param ssh
   * @throws IllegalStateException wenn �bergebene SSH-Instanz nicht connected ist.
   */
  public Ssh(Ssh ssh) {
    if( ! ssh.isConnected() ) {
      throw new IllegalStateException( "Ssh is not conntected" );
    }
    this.user = ssh.user;
    this.host = ssh.host;
    this.userInfo = ssh.userInfo; 
    this.session = ssh.session;
    this.hostRsaKey = ssh.hostRsaKey;
  }

  public boolean isConnected() {
    return connected;
  }

  public void setHostRsaKey(String hostRsaKey) {
    if( hostRsaKey == null || hostRsaKey.length() ==  0 ) {
      throw new IllegalArgumentException("hostRsaKey is invalid");
    }
    this.hostRsaKey = hostRsaKey;
  }

  public void setPrivKey(String privKey) {
    if( privKey == null || privKey.length() ==  0 ) {
      throw new IllegalArgumentException("privKey is invalid");
    }
    this.privKey = privKey;
  }

  public void setPubKey(String pubKey) {
    if( pubKey == null || pubKey.length() ==  0 ) {
      throw new IllegalArgumentException("pubKey is invalid");
    }
    this.pubKey = pubKey;
  }
  
  public void setPassPhrase(String passphrase) {
    if( passphrase == null || passphrase.length() ==  0 ) {
      throw new IllegalArgumentException("PassPhrase is invalid");
    }
    this.passPhrase = passphrase;
  }
  

  
  public void connect() throws JSchException {
    connect(0);
  }
  
  /**
   * @param timeout
   * @throws JSchException
   */
  public void connect(int timeout) throws JSchException {
    
    JSch jsch=new JSch();
    JSch.setLogger( new JschLogger(logger) );
    
    if( hostRsaKey != null ) {
      //Base64 base64 = new Base64();
      jsch.getHostKeyRepository().add( new HostKey(host,Base64.getDecoder().decode(hostRsaKey) ), userInfo );// jsch.getHostKeyRepository().add( new HostKey(host,base64.decode(hostRsaKey) ), userInfo );
    }
    
    if (privKey != null && pubKey!=null)
    {
      if(passPhrase!=null)
      {
        jsch.addIdentity("verwaltung",privKey.getBytes(), pubKey.getBytes(), passPhrase.getBytes());
      }
      else
      {
        jsch.addIdentity("verwaltung",privKey.getBytes(), pubKey.getBytes(), null);
      }
    }
    
    session=jsch.getSession(user, host, 22);

    session.setUserInfo( userInfo );
   
    session.setConfig("StrictHostKeyChecking", "no");
    
    
    if(privKey!=null && privKey.length()>0)
    {
      session.setConfig("PreferredAuthentications","publickey");
    }
    else
    {
      session.setConfig("PreferredAuthentications","password,gssapi-with-mic,publickey,keyboard-interactive");
      session.setPassword(password);    
    }
    

    session.connect(timeout);
    
    if( hostRsaKey == null ) {
      HostKey h = jsch.getHostKeyRepository().getHostKey()[0];
      hostRsaKey =  h.getKey(); 
    }
    connected = true;
    
    
    
    logger.info("Public Hostkey used:"+session.getHostKey().getKey());
  }
  
  public String getHostRsaKey() {
    return hostRsaKey;
  }

  public String getPrivKey() {
    return privKey;
  }

  
  public void disconnect() {
    if( session != null ) {
      session.disconnect();
      session.disconnect();
      connected = false;
    }
  }
  
  public void close() {
    disconnect();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()+"("+user+"@"+host+")";
  }

}
