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
package com.gip.xyna.utils.misc.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;

import org.apache.commons.net.ftp.FTPClient;

public class FileTransferFTP implements FileTransfer {
  
  public static final int ASCII = FTPClient.ASCII_FILE_TYPE;
  public static final int BINARY = FTPClient.BINARY_FILE_TYPE;
  private IOLogger logger;
  
  FTPClient ftp = new FTPClient();
  
  /**
   * Anmeldung am FTP-Server
   * @param server
   * @param username
   * @param password
   * @param type Übertragungsweise ASCII oder BINARY
   * @throws SocketException
   * @throws IOException
   */
  public void connect(String server, String username, String password, int type ) throws SocketException, IOException {
    ftp.connect( server );
    ftp.login( username, password );
    if(ftp.getReplyCode()==530){
        throw new IOException("Login failed: "+ftp.getReplyString());
    }
    logFTPReply();
    ftp.setFileType( type );
    logFTPReply();
    ftp.enterLocalPassiveMode();//??    
    logFTPReply();
    
  }

  /**
   * Wechsel in das Verzeichnis directory auf dem FTP-Server
   * @param directory
   * @throws IOException
   */
  public void changeDir(String directory) throws IOException {
    ftp.changeWorkingDirectory( directory );
    logFTPReply();
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.misc.io.FileTransfer#upload(java.io.File)
   */
  public int upload(File file) throws FileNotFoundException, IOException {
    return upload( file.getName(), new FileInputStream( file ) );
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.utils.misc.io.FileTransfer#upload(java.lang.String, java.io.InputStream)
   */
  public int upload(String remoteFilename, InputStream content) throws IOException {
    ftp.storeFile( remoteFilename, content );
    logFTPReply();
    if( ftp.getReplyCode() != 226 ) {
      throw new IOException("Error: " + ftp.getReplyString() );
    }
    return 0;
  }

  /**
   * Löschen einer Datei auf dem FTP-Server
   * @param name
   * @return
   * @throws IOException
   */
  public boolean delete( String name ) throws IOException {
    boolean del =  ftp.deleteFile( name );
    if(ftp.getReplyCode()!=250){
        throw new IOException("Delete failed: "+ftp.getReplyString());       
    }    
    logFTPReply();
    return del;
  }
  
  /* (non-Javadoc)
   * @see com.gip.xyna.utils.misc.io.FileTransfer#close()
   */
  public void close() {
    if( ! ftp.isConnected() ) {
      return;
    }
    try {
      ftp.logout();
      logFTPReply();
    } catch(IOException e) { e.printStackTrace(); }
    try {
      ftp.disconnect();
    } catch(IOException e) { e.printStackTrace(); }
  }

  /**
   * Vereinfachung des Loggings
   */
  private void logFTPReply() {
    if( logger != null ) {
      logger.logFTPReply(ftp.getReplyString() );
    }
  }

  /**
   * Einrichtung des Loggers
   * @param ioLogger
   */
  public void setIOLogger(IOLogger ioLogger ) {
    logger = ioLogger;
  }
  
}
