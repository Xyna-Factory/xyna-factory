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
package xact.tacacs.tk;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.security.NoSuchAlgorithmException;
import java.util.StringTokenizer;


/**
 * Usage:
 * 
 * Tacacs tac = new Tacacs();  
 * tac.setHostname = "tacacsServer";
 * tac.setKey = "tacacsEncryptionKey";
 * boolean authResult = tac.isAuthenticated("user","pw");
 */

public class Tacacs {
    
  // Different status possibilities  
  private static final byte AUTHEN_PASS = (byte)0x01;
  private static final byte AUTHEN_FAIL = (byte)0x02;
  private static final byte AUTHEN_GETDATA = (byte)0x03;
  private static final byte AUTHEN_GETUSER = (byte)0x04;
  private static final byte AUTHEN_GETPASS = (byte)0x05;
  private static final byte AUTHEN_RESTART = (byte)0x06;
  private static final byte AUTHEN_ERROR = (byte)0x07;
  private static final byte AUTHEN_FOLLOW = (byte)0x21;
  private static final byte ZEROBYTE = (byte)0x00;
  static final byte HEADERFLAG_UNENCRYPT = (byte)0x01;
  private static final byte HEADERFLAG_SINGLECON = (byte)0x04;

  public static final byte VERSION_13_0 = (byte)0xc0;
  public static final byte VERSION_13_1 = (byte)0xc1;
  public static final int PORT_STANDARD = 49;

  private byte headerFlags;
  private byte[] sessionID;
  private Integer tacacsSequence;

  private byte[] secretkey;
  private Integer port;
  private Byte version;
  private String hostname;
  protected static String myIp = "";

private Socket socket = null;

  /**
   * Default Ctor
   */

  public Tacacs(){
    this.headerFlags = ZEROBYTE;
    this.port = new Integer(PORT_STANDARD);
    this.hostname = "";
    this.version = new Byte(VERSION_13_0);
    this.secretkey = "".getBytes();
  }
  
  /**
   * Sets the port number of the TACACS+ server (default: 49).
   * @param port The port number
   */
  public void setPortNumber(int port) {
      this.port = new Integer(port);
  }

  /**
   * Sets the Hostname of the TACACS+ server. 
   * 
   * @param tacHost the hostname of the TACACS+ server.
   */

  public void setHostname(String tacHost) {
      this.hostname = tacHost;
  }
  
  /**
   * Sets the secret encryption key.
   * @param encKey Key for the TACACS+ encryption.
   */
  public void setKey(String encKey) {
      this.secretkey = encKey.getBytes();
  }
  
  /**
   * Sets the Version of TACACS+.  
   */
  public void setVersion(byte ver) {
      this.version = new Byte(ver);
  }
  
  /**
   * Returns the IP of the client (=> own IP).  
   * @return
   */
  public static String getMyIp() {
    return myIp;
  }

  /**
   * Sets the IP of the client (=> own IP).  
   * @param myIp My own IP (client IP)
   */
  public static void setMyIp(String myIp) {
    Tacacs.myIp = myIp;
  }
  
  /** 
   * Opens the connection to the TACACS+ server.
   * @throws IOException If the server is not reachable
   */
  private void connect() throws IOException {

    // Start always with 1 as sequence!
    this.tacacsSequence = new Integer(1);
    this.sessionID = Header.generateSID();
    if (this.socket == null) {
      this.socket = new Socket (hostname,port.intValue());
    }
  }
  
  /**
   * Closes the connection to the TACACS+ server.
   * @throws IOException
   */
  private void closeConnection() throws IOException {
    if (this.socket != null) {
        this.socket.close();
        this.socket = null;
    }
    sessionID = null;
  }
  
  /**
   * "Main" Method:
   * 1) Connect to the Tacacs+ server 
   * 2) Check if user and password are correct. 
   * 3) Return the result as a boolean value
   * 
   * @param user the username to be authenticated
   * @param pw the corresponding password
   * @exception IOException In case of network problems
   * @exception NoSuchAlgorithmException If the MD5 algorithm can't be found 
   * @return true if the authentication was successful, otherwise false
   */
  public synchronized boolean isAuthenticated(String user, String pw) throws IOException, NoSuchAlgorithmException  {
      
    // Username is required!    
    if (user.equals("")) {
      return false;
    }
    connect();
    AuthSTART AS = new AuthSTART();
    AS.send("","");
    AuthREPLY AR = new AuthREPLY();
    AR.get();
    boolean exitLoop = false;
    while (AR.getStatus() != AUTHEN_PASS && AR.getStatus() != AUTHEN_FAIL && AR.getStatus() != AUTHEN_ERROR && AR.getStatus() != AUTHEN_FOLLOW && exitLoop != true) {
      synchronized (tacacsSequence) {
        int tmpSeqNum = tacacsSequence.intValue();
        tmpSeqNum++;tmpSeqNum++;
        tacacsSequence = new Integer(tmpSeqNum);
      }
      if (AR.REPLY_status == AUTHEN_GETDATA | AR.REPLY_status == AUTHEN_GETUSER) {
        AuthCONT AC = new AuthCONT();
        AC.send(user);
        AR.get();
      }
      else if (AR.REPLY_status == AUTHEN_GETPASS) {
        AuthCONT AC = new AuthCONT();

        // S/Key login...
        if (AR.isUseSkeyLogin()) {
            otp otpwd = new otp(AR.getSkeyChallengeSeq(), AR.getSkeyChallengeSeed(), pw, 4);
            otpwd.calc();
            AC.send(otpwd.toString());
        }
        
        // ...or normal login
        else {
            AC.send(pw);
        }
        
        AR.get();
      }
      else if (AR.REPLY_status == AUTHEN_RESTART) {
        synchronized (tacacsSequence) {
          tacacsSequence = new Integer(1);
        }
        AS.send(user,pw);
        AR.get();
      }
      else if (tacacsSequence.intValue() > 5) { //only try 5 times
        exitLoop = true;
      }
      else {
        exitLoop = true;
      }
    }
    this.closeConnection();
    
    // Authentication ok?
    if (AR.REPLY_status == AUTHEN_PASS) {
      return true;
    }
    else {
      return false;
    }
  }
  private class AuthSTART {
    // flags
    private byte ACTION_LOGIN = (byte)0x01;
    private byte AUTHTYPE_ASCII = (byte)0x01;
    private byte PRIVLVL_MAX = (byte)0x0f;
    private byte SERVICE_LOGIN = (byte)0x01;

    private byte action = ACTION_LOGIN;
    private byte authtype = AUTHTYPE_ASCII;
    private byte privlvl = PRIVLVL_MAX;
    private byte service = SERVICE_LOGIN;

    private AuthSTART() {
    }
    private void send(String user, String pw) throws IOException, NoSuchAlgorithmException {
      byte[] Username = user.getBytes();
      byte[] Data = "".getBytes();  
      byte[] Port = "tty8".getBytes();
      byte[] RemoteAdd = Tacacs.getMyIp().getBytes();
      byte User_Len = (byte)Username.length;
      byte Port_Len = (byte)Port.length;
      byte Data_Len = (byte)Data.length;
      byte RemoteAdd_Len = (byte)RemoteAdd.length;

      // Build message
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      stream.write(action);
      stream.write(privlvl);
      stream.write(authtype);
      stream.write(service);
      stream.write(User_Len);
      stream.write(Port_Len);
      stream.write(RemoteAdd_Len);
      stream.write(Data_Len);
      stream.write(Username);
      stream.write(Port);
      stream.write(RemoteAdd);
      stream.write(Data);
      
      // Encrypt with TACACS+ encryption key
      byte[] body = Header.crypt(version.byteValue(), tacacsSequence.byteValue(), 
            stream.toByteArray(),headerFlags, sessionID, secretkey);
      stream.reset();
      byte[] header = Header.buildHeader(body,version,Header.TYPE_AUTHENTIC,
            tacacsSequence,headerFlags,sessionID); //make header from packet
      stream.write(header);
      stream.write(body);
      
      // Send to TACACS+ server
      stream.writeTo(socket.getOutputStream());
    }
  }
  private class AuthCONT {
    private AuthCONT() {
    }
    private void send(String userMsg) throws IOException, NoSuchAlgorithmException {
      byte[] UserMsg = userMsg.getBytes();
      byte[] CONT_data = "".getBytes();
      byte CONT_Flags = ZEROBYTE;
      byte[] UserMsg_Len = Bytes.ShorttoBytes((short)UserMsg.length);
      byte[] CONT_data_Len = Bytes.ShorttoBytes((short)CONT_data.length);
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      stream.write(UserMsg_Len);
      stream.write(CONT_data_Len);
      stream.write(CONT_Flags);
      stream.write(UserMsg);
      
      // Encrypt with TACACS+ encryption key
      byte[] body = Header.crypt(version.byteValue(),tacacsSequence.byteValue(),
            stream.toByteArray(),headerFlags,sessionID, secretkey);
      byte[] header = Header.buildHeader(body,version,Header.TYPE_AUTHENTIC,
            tacacsSequence,headerFlags,sessionID);
      stream.reset();
      stream.write(header);
      stream.write(body);
      
      // Send to TACACS+ server
      stream.writeTo(socket.getOutputStream());
    }
  }
  private class AuthREPLY {
      
    // Flags
    private byte REPLY_status;
    private byte REPLY_flags;
    private byte[] servermsgLen = new byte[2];
    private byte[] dataLen = new byte[2];
    private int skeyChallengeSeq;
    private String skeyChallengeSeed;
    private boolean useSkeyLogin = false;

    private AuthREPLY() {
    }
    private void get() throws IOException,SocketException,NoSuchAlgorithmException {
      DataInputStream dis = null;
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      byte[] body = null;
      byte[] header = null;
      dis = new DataInputStream(socket.getInputStream());
      for (int i = 0 ; i < 12 ; i++) {
        stream.write(dis.readByte());
      }
      header = stream.toByteArray();
      stream.reset();
      int Body_Len = Header.extractBodyLen(header);
      for (int i = 0 ; i < Body_Len ; i++) {
        stream.write(dis.readByte());
      }
      byte[] tempBody = stream.toByteArray();
      byte headerVersionNumber = Header.extractVersionNumber(header);
      byte headerFlags = Header.extractFlags(header);
      byte headerSequenceNumber = Header.extractSeqNum(header);
      body = Header.crypt(headerVersionNumber,headerSequenceNumber,tempBody,headerFlags,
            sessionID,secretkey);
      REPLY_status = body[0];
      REPLY_flags = body[1];
      servermsgLen[0] = body[2];
      servermsgLen[1] = body[3];
      dataLen[0] = body[4];
      dataLen[1] = body[5];
      
      int servermsgLenInt = ((servermsgLen[0] & 0xff) << 8) | (servermsgLen[1] & 0xff);
      
      StringBuffer sb = new StringBuffer();
      for (int i = 6; i < (6 + servermsgLenInt); i++) {
          sb.append((char) (body[i] & 0xff));
      }
      
      String serverMessageString = sb.toString();
      
      // Server message contains skey challenge? => Save challenge 
      if ((serverMessageString != null) && ((serverMessageString.contains("otp-md4")) || (serverMessageString.contains("s/key")) || (serverMessageString.contains("skey")))) {
          
          // Take first line. 
          StringTokenizer st = new StringTokenizer(serverMessageString, "\n");
          serverMessageString = st.nextToken();
          
          // Split by spaces. The prefix is not strictly defined - the challenge is a comination of the last two tokens in that line.
          st = new StringTokenizer(serverMessageString, " ");
          int tokenCount = st.countTokens();
          
          if (st.countTokens() > 2) {
              for (int i = 0; i < (tokenCount - 2); i++) {
                  String next = st.nextToken();
              }
              skeyChallengeSeq = Integer.parseInt(st.nextToken());
              skeyChallengeSeed = st.nextToken();
              useSkeyLogin = true;
          }
      }
    }
    private byte getStatus() {
      return REPLY_status;
    }
    private byte getFlags() {
      return REPLY_flags;
    }
    
    public int getSkeyChallengeSeq() {
        return skeyChallengeSeq;
    }
    public String getSkeyChallengeSeed() {
        return skeyChallengeSeed;
    }
    
    public boolean isUseSkeyLogin() {
        return useSkeyLogin;
    }
    
  }
}
