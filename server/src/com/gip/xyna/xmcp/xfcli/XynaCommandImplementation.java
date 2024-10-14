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



import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryPortal;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds;



public abstract class XynaCommandImplementation<T extends AXynaCommand> {

  protected final XynaFactoryPortal factory = XynaFactory.getPortalInstance();
  
  public static final XynaPropertyBuilds<List<DocumentationLanguage>> LANGUAGE_PREFERENCE = 
      new XynaPropertyBuilds<List<DocumentationLanguage>>("xyna.xmcp.xfcli.language_preference", 
      new DocumentationLanguageListBuilder(), 
      Arrays.asList(DocumentationLanguage.EN,DocumentationLanguage.DE) );

  private static class DocumentationLanguageListBuilder implements XynaPropertyBuilds.Builder<List<DocumentationLanguage>> {

    public List<DocumentationLanguage> fromString(String string)
        throws ParsingException {
      String[] parts = string.split("\\s*,\\s*");
      List<DocumentationLanguage> list = new ArrayList<DocumentationLanguage>();
      for( String p : parts ) {
        try {
          list.add( DocumentationLanguage.valueOf(p) );
        } catch ( IllegalArgumentException e ) {
          throw new ParsingException( "Illegal DocumentationLanguage "+p, e);
        }
      }
      return list;
    }

    public String toString(List<DocumentationLanguage> value) {
      StringBuilder sb = new StringBuilder();
      String sep = "";
      for( DocumentationLanguage dl : value ) {
        sb.append(sep).append(dl);
        sep = ",";
      }
      return sb.toString();
    }
   
  }
 

  public abstract void execute(OutputStream statusOutputStream, T command) throws XynaException;


  public final void writeLineToCommandLine(OutputStream outputstream, Object... s) {
    CommandLineWriter.createCommandLineWriter(outputstream).writeLineToCommandLine(s);
  }

  public final void writeStackTraceToCommandLine(OutputStream outputStream, StackTraceElement[] s) {
    CommandLineWriter clw = CommandLineWriter.createCommandLineWriter(outputStream);
    for (StackTraceElement ste : s) {
      clw.writeLineToCommandLine(ste);
    }
  }

  public final void writeToCommandLine(OutputStream outputstream, Object... s) {
    CommandLineWriter.createCommandLineWriter(outputstream).writeToCommandLine(s);
    /*
    try {
      if (s == null) {
        return;
      }
      Writer w = new OutputStreamWriter(outputstream, Constants.DEFAULT_ENCODING);
      for (Object part : s) {
        if (part == null) {
          w.write("null");
        } else {
          w.write(part.toString());
        }
      }
      w.flush();
    } catch (IOException e) {
      throw new RuntimeException("Unexpected exception while writing to stream.", e);
    }
    */
  }

  
  public final void writeEndToCommandLine(OutputStream outputstream, ReturnCode returnCode) {
    CommandLineWriter.createCommandLineWriter(outputstream).writeEndToCommandLine(returnCode);
  }

  
  public final void writeReaderToCommandLine(OutputStream outputstream, Reader reader) {
    CommandLineWriter clw = CommandLineWriter.createCommandLineWriter(outputstream);
    try {
      clw.writeReaderToCommandLine(reader);
    } catch (IOException e) {
      clw.writeLineToCommandLine("Error: " + e.getClass().getSimpleName() + " " + e.getMessage());
    }
  }
}
