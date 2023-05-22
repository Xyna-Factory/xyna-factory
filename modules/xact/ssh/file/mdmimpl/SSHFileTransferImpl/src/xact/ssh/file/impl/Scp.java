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
package xact.ssh.file.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.streams.StreamUtils;
import com.gip.xyna.utils.timing.SleepCounter;
import com.gip.xyna.xfmg.xfctrl.filemgmt.FileManagement.TransientFile;
import com.gip.xyna.xprc.xsched.xynaobjects.RelativeDate;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import xact.ssh.file.FileTransferInfo;
import xact.ssh.file.SSHServerParameter;
import xfmg.xfctrl.filemgmt.FileSize;

public class Scp {
  
  
  private static final Logger logger = CentralFactoryLogging.getLogger(Scp.class);
  private static final SleepCounter sleepTemplate = new SleepCounter(10, 250, 25);
 
  private Session session;
  private Integer limitBandwidth;
  private boolean preserve;
  private boolean throwTimeoutException;
  private int readTimeout;
  
  public Scp(Session session, SSHServerParameter server) {
    this.session = session;
    this.limitBandwidth = server.getBandwidth();
    this.preserve = false;
    this.readTimeout = server.getSCPTimeouts() == null ? 0 : server.getSCPTimeouts().getSocketTimeout(); 
    this.throwTimeoutException = readTimeout > 0;
  }
  
  public FileTransferInfo copyInputStreamTo(InputStream is, long length, String toFileName) throws JSchException, IOException {
    long start = System.currentTimeMillis();
    copyInternalTo( is, length, toFileName);
    long end = System.currentTimeMillis();
    
    File f = new File(toFileName);
    return fileTransferInfo(f.getName(), length, end -start);
  }
  
  private FileTransferInfo copyOutputStreamFrom(String filename, OutputStream os) throws JSchException, IOException {
    long start = System.currentTimeMillis();
    long length = copyInternalFrom(filename, os);
    long end = System.currentTimeMillis();
    
    File f = new File(filename);
    return fileTransferInfo(f.getName(), length, end -start);
  }
  
  
  public FileTransferInfo copyDocumentTo(String document, String charsetName, String toFileName) throws JSchException, IOException {
    ByteArrayInputStream bais = null;
    try {
      byte[] bytes = document.getBytes(charsetName);
      bais = new ByteArrayInputStream( bytes );
      return copyInputStreamTo( bais, bytes.length, toFileName);
    } finally {
      finallyClose(bais);
    }
  }
 

  public FileTransferInfo copyTo(String fromFilename, String toFileName) throws JSchException, IOException {
    File from = new File(fromFilename);
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(from);
      return copyInputStreamTo( fis, from.length(), toFileName);
    } finally {
      finallyClose(fis);
    }
  }
  
  public FileTransferInfo copyTo(TransientFile transientFile, String toFileName) throws JSchException, IOException {
    InputStream is = transientFile.openInputStream();
    try {
      return copyInputStreamTo( is, transientFile.getSize(), toFileName);
    } finally {
      finallyClose(is);
    }
  }
  
  public FileTransferInfo copyFrom(String filename, OutputStream os) throws JSchException, IOException {
    return copyOutputStreamFrom(filename, os);
  }
  
  public FileTransferInfo copyFrom(String filename, String charsetName, StringBuilder text) throws JSchException, IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    FileTransferInfo info = copyOutputStreamFrom(filename, baos);
    text.append( new String(baos.toByteArray(), charsetName) );
    return info;
  }

  private FileTransferInfo fileTransferInfo(String toFileName, long length, long duration) {
    double averageBandwidth = 0.;
    if( length != 0 ) {
      long ab = 8000*length/duration; //  bit/s
      averageBandwidth = ab/1000.;    // kbit/s mit 3 Nachkommastellen
    }
    return new FileTransferInfo( toFileName, new FileSize(length), new RelativeDate(duration+"ms"), averageBandwidth );
  }
  
  private void copyInternalTo( InputStream source, long length, String filename) throws JSchException, IOException {
    //Dokumentation zu scp unter http://blogs.sun.com/janp/entry/how_the_scp_protocol_works
    Channel channel=null;
    OutputStream out=null;
    InputStream in=null;
    
    try {
      if( logger.isDebugEnabled() ) {
        logger.debug( "Copying "+length+" bytes to "+filename);
      }
      
      String command = buildCommand( filename, true);
      
      channel=session.openChannel("exec");
      ((ChannelExec)channel).setCommand(command);

      // get I/O streams for remote scp
      out=channel.getOutputStream();
      in=channel.getInputStream();

      channel.connect();
      checkAck(in,false);

      // send "C0644 filesize filename", where filename should not include '/' TODO timestamp?
      StringBuilder sb = new StringBuilder();
      sb.append("C0644 "); //0644 sind File-Zugriffsberechtigungen -rw-r--r--
      sb.append(length).append(" ");
      if( filename.lastIndexOf('/') != -1 ) {
        //nur Dateinamen �bergeben, nicht den Pfad
        sb.append( filename.substring(filename.lastIndexOf('/')+1) );
      } else {
        sb.append( filename );
      }
      sb.append( '\n' );
      out.write(sb.toString().getBytes());
      out.flush();
      checkAck(in,false);

      // send data
      StreamUtils.copy(source, out);
      
      // send '\0'
      out.write(0); out.flush();
      checkAck(in,false);

      out.close();
      
    } finally {
      if( in != null ) {
        in.close();
      }
      if( out != null ) {
        out.close();
      }
      if( channel != null ) {
        channel.disconnect();
      }
    }
  }
  

  
  
  private long copyInternalFrom(String filename, OutputStream os) throws JSchException, IOException {
    //nach http://www.jcraft.com/jsch/examples/ScpFrom.java
    Channel channel=null;
    OutputStream out=null;
    InputStream in=null;
    try {
      if( logger.isDebugEnabled() ) {
        logger.debug( "Copying "+filename+" to stream");
      }

      String command = buildCommand( filename, false);
      channel=session.openChannel("exec");
      ((ChannelExec)channel).setCommand(command);

      // get I/O streams for remote scp
      out=channel.getOutputStream();
      in=channel.getInputStream();

      channel.connect();

      byte[] buf=new byte[1024];

      // send '\0'
      buf[0]=0; out.write(buf, 0, 1); out.flush();

      long filesizeRet=0L;
      
      while(true){
        int c=checkAck(in, true);
        if(c!='C'){
          break;
        }

       prepareReadWithTimeout(in, readTimeout);
       // read '0644 '
       in.read(buf, 0, 5);
       
       long filesize=0L;
       while(true){
         prepareReadWithTimeout(in, readTimeout);
         if(in.read(buf, 0, 1)<0){
           // error
           break; 
         }
         if(buf[0]==' ')break;
         filesize=filesize*10L+(long)(buf[0]-'0');
       }
       filesizeRet = filesize;
       
       String file=null;
       for(int i=0;;i++){
         prepareReadWithTimeout(in, readTimeout);
         in.read(buf, i, 1);
         if(buf[i]==(byte)0x0a){
           file=new String(buf, 0, i);
           break;
         }
       }

       if( logger.isDebugEnabled() ) {
         logger.debug("filesize="+filesize+", file="+file);
       }

       // send '\0'
       buf[0]=0; out.write(buf, 0, 1); out.flush();

       // read a content of lfile
       int foo;
       while(true){
         if(buf.length<filesize) foo=buf.length;
         else foo=(int)filesize;
         prepareReadWithTimeout(in, readTimeout);
         foo=in.read(buf, 0, foo);
         if(foo<0){
           // error 
           break;
         }
         os.write(buf, 0, foo);
         filesize-=foo;
         if(filesize==0L) break;
       }
       
       if(checkAck(in, true)!=0){
         throw new RuntimeException("Unexpected char in InputStream");
       }

       // send '\0'
       buf[0]=0; out.write(buf, 0, 1); out.flush();
       
     }
      
     return filesizeRet;
      
    } finally {
      if( in != null ) {
        in.close();
      }
      if( out != null ) {
        out.close();
      }
      if( channel != null ) {
        channel.disconnect();
      }
    }
  }
  
  /* Before every read: check if there is data to read (provided within timeoutInMillis)*/
  protected void prepareReadWithTimeout(InputStream input, int timeoutInMillis) throws IOException{
    if(timeoutInMillis == 0)
      return;

    long timeout = System.currentTimeMillis() + timeoutInMillis;
    try {
      SleepCounter sleep = sleepTemplate.clone();
      while(true){
        if(input.available() > 0) {
          break;
        } else if (timeoutInMillis > 0 && System.currentTimeMillis() > timeout) {
          if (throwTimeoutException) {
            throw new TimeoutException("Read Timeout");
          } else {
            break;
          }
        }
        sleep.sleep();
      }
    }
    catch(TimeoutException e) {
        throw new IOException("Timeout");
    }
    catch(Exception e) {}
  }

  private String buildCommand(String filename, boolean toOrFrom) {
    StringBuilder sb = new StringBuilder();
    sb.append("scp");
    
    if( preserve ) {
      //Preserves modification times, access times, and modes from the original file.
      sb.append(" -p ");
    }
    
    if( limitBandwidth != null && limitBandwidth >= 0 ) {
      sb.append(" -l ").append(limitBandwidth);
    }
    
    if( toOrFrom ) {
      sb.append(" -t");
    } else {
      sb.append(" -f");
    }
    sb.append(" ").append(filename);
    return sb.toString();
  }

  private int checkAck(InputStream in, boolean from ) throws IOException {
    prepareReadWithTimeout(in, readTimeout);
    int b=in.read();
    if(b==0) return b; //success
    
    switch( b ) {
      case 1: //error
        throw new RuntimeException( "ERROR: "+ readLine(in) );
      case 2: //fatal error
        throw new RuntimeException( "FATAL: "+readLine(in) );
      default:
        if( from ) {
          return b;
        } else {
          logger.warn( "unexpected return code: " + b );
          throw new IllegalStateException( "unexpected return code: " + b );
        }
    }
  }

  private String readLine(InputStream in) throws IOException {
    StringBuilder sb = new StringBuilder();
    int c;
    do {
      prepareReadWithTimeout(in, readTimeout);
      c=in.read();
      sb.append((char)c);
    }
    while(c!='\n');
    return sb.toString();
  }
  
  private void finallyClose(InputStream is) {
    if( is != null ) {
      try {
        is.close();
      } catch (IOException e) {
        //ignorieren
      }
    }
  }

}
