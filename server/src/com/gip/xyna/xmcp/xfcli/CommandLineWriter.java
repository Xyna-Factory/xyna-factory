/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package com.gip.xyna.xmcp.xfcli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.Constants;


/**
 * CommandLineWriter hilft, einfacher den OutputStream zu befüllen und auch den ReturnCode am Ende besser 
 * übergeben zu können
 * TODO "extends OutputStream" ist ein dummer Trick, der fürs erste viel Refactoring-Aufwand erspart, 
 *       aber augebaut werden sollte
 */
public class CommandLineWriter extends OutputStream {
 
  private static Logger logger = CentralFactoryLogging.getLogger(CommandLineWriter.class);
  private OutputStream os;
  private boolean endWritten = false;
  private boolean linebreakWritten = false;
  
  public CommandLineWriter(OutputStream os) {
    this.os = os;
  }
  
  public static class CommandLineWriterIOException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CommandLineWriterIOException(IOException cause) {
      super(cause);
    }
    
  }
  

  public void writeToCommandLine(Object... s) {
    if (s == null) {
      return;
    }
    if (s.length == 1) {
      if (s[0] == null) {
        writeString("null");
      } else {
        writeString(s[0].toString());
      }
    } else {
      StringBuilder sb = new StringBuilder();
      for (Object part : s) {
        if (part == null) {
          sb.append("null");
        } else {
          if (part instanceof String) {
            sb.append((String) part);
          } else {
            sb.append(part);
          }
        }
      }
      writeString(sb.toString());
    }
  }


  public void writeString(String string) {
    try {
      os.write( string.getBytes(Constants.DEFAULT_ENCODING) );
    } catch (UnsupportedEncodingException e) {
      throw new CommandLineWriterIOException(e);
    } catch (IOException e) {
      throw new CommandLineWriterIOException(e);
    }
    linebreakWritten = string.endsWith("\n");
  }
  
  public void writeEndToCommandLine(ReturnCode returnCode) {
    if( ! endWritten  ) {
      if( !linebreakWritten ) {
        writeToCommandLine("\n");
      }
      writeToCommandLine(XynaFactoryCLIConnection.END_OF_STREAM + returnCode );
      endWritten = true;
      try {
        os.flush();
      } catch (IOException e) {
        throw new CommandLineWriterIOException(e);
      }
    }
  }
  
  public void setEndWritten(boolean endWritten) {
    this.endWritten = endWritten;
  }


  public void writeLineToCommandLine(Object... s) {
    if (s == null) {
      return;
    }
    if (s.length == 1) {
      writeToCommandLine(s[0], "\n");
    } else {
      StringBuilder sb = new StringBuilder();
      for (Object part : s) {
        if (part == null) {
          sb.append("null");
        } else {
          if (part instanceof String) {
            sb.append((String) part);
          } else {
            sb.append(part);
          }
        }
      }
      sb.append("\n");
      writeToCommandLine(sb.toString());
    }
  }

  public void write(byte[] bytes) {
    try {
      os.write(bytes);
    } catch (IOException e) {
      throw new CommandLineWriterIOException(e);
    }
    linebreakWritten = bytes[bytes.length-1] == 10;
  }


  public PrintStream getPrintStream() {
    return new PrintStream(os);
  }


  @Override
  public void write(int b) throws IOException {
    os.write(b);
    linebreakWritten = b == 10;
  }
 
  public void write(byte b[], int off, int len) throws IOException {
    os.write(b, off, len);
    linebreakWritten = b[off+len-1] == 10;
  }
  
  public void flush() throws IOException {
    os.flush();
  }

  public void close() {
    //nicht erlaubt!
    logger.warn("CommandLineWriter.close() called ", new Exception() );
  }


  public static CommandLineWriter createCommandLineWriter(OutputStream outputStream) {
    if( outputStream instanceof CommandLineWriter ) {
      return (CommandLineWriter)outputStream;
    } else {
      return new CommandLineWriter(outputStream);
    }
  }

  public void close(String message, ReturnCode returnCode) {
    try {
      writeToCommandLine(message);
      close(returnCode);
    } catch( CommandLineWriterIOException e ) {
      if (logger.isDebugEnabled()) {
        logger.debug("could not signal error and end communication", e);
      }
    }
  }

  public void close(ReturnCode returnCode) {
    try {
      writeEndToCommandLine(returnCode);
      os.close();
    } catch( CommandLineWriterIOException e ) {
      if (logger.isDebugEnabled()) {
        logger.debug("could not signal error and end communication", e);
      }
    } catch( IOException e ) {
      if (logger.isDebugEnabled()) {
        logger.debug("could not signal error and end communication", e);
      }
    }
  }

  
  public final void writeReaderToCommandLine(Reader reader) throws IOException {
    if (reader == null) {
      return;
    }
    String line = null;
    try (BufferedReader r = new BufferedReader(reader)) {
      while (null != (line = r.readLine())) {
        // workaround weil die lines vom buffered reader sonst nicht ordentlich getrennt werden. unklar wieso. siehe auch
        // XynaClusterPersistenceLayer
        if (line.length() > 0) {
          writeLineToCommandLine(line);
        }
      }
    }
  }
  
}
