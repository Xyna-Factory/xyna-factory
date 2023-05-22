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
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.TableFormatter;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.InfrastructureAlgorithmExecutionManagement;
import com.gip.xyna.xfmg.xfctrl.threadmgmt.ManagedAlgorithmInfo;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xmcp.xfcli.StringParameterFormatter;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listinfrastructurethreads;



public class ListinfrastructurethreadsImpl extends XynaCommandImplementation<Listinfrastructurethreads> {

  public void execute(OutputStream statusOutputStream, Listinfrastructurethreads payload) throws XynaException {
    InfrastructureAlgorithmExecutionManagement tm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getInfrastructureAlgorithmExecutionManagement();
    Collection<ManagedAlgorithmInfo> algInfos = tm.listManagedAlgorithms();
    ManagedAlgorithmTableFormatter matf = new ManagedAlgorithmTableFormatter(algInfos, payload.getVerbose());
    StringBuilder output = new StringBuilder();
    matf.writeTableHeader(output);
    matf.writeTableRows(output);
    if (payload.getError()) {
      for (ManagedAlgorithmInfo algInfo : algInfos) {
        if (algInfo.getTerminationException().isPresent()) {
          output.append(Constants.LINE_SEPARATOR);
          appendThrowableInformation(output, algInfo);
        }
      }
    }
    if (payload.getParameter()) {
      for (ManagedAlgorithmInfo algInfo : algInfos) {
        if (algInfo.getAdditionalParameters().size() > 0) {
          output.append(Constants.LINE_SEPARATOR);
          appendParameterInformation(output, algInfo);
        }
      }
    }
    writeToCommandLine(statusOutputStream, output.toString());
  }

  
  private void appendThrowableInformation(StringBuilder output, ManagedAlgorithmInfo algInfo) {
    output.append(algInfo.getName()).append(":").append(Constants.LINE_SEPARATOR);
    Throwable t = algInfo.getTerminationException().get();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    t.printStackTrace(new PrintStream(baos));
    try {
      output.append(baos.toString(Constants.DEFAULT_ENCODING));
    } catch (UnsupportedEncodingException e) {
      // highly unlikely
      output.append(baos.toString());
    }
    output.append(Constants.LINE_SEPARATOR);
  }

  
  private void appendParameterInformation(StringBuilder output, ManagedAlgorithmInfo algInfo) {
    output.append(algInfo.getName()).append(":").append(Constants.LINE_SEPARATOR);
    for (StringParameter<?> parameter : algInfo.getAdditionalParameters()) {
      output.append("   * ");
      StringParameterFormatter.appendStringParameter(output, parameter, DocumentationLanguage.EN, "       " );
      output.append(Constants.LINE_SEPARATOR);
    }
  }



  private static class ManagedAlgorithmTableFormatter extends TableFormatter {

    private List<List<String>> rows;
    private List<String> header;
    private List<ManagedAlgorithmColumn> columns;
   
    public ManagedAlgorithmTableFormatter(Collection<ManagedAlgorithmInfo> algInfos, boolean verbose) {
      columns = new ArrayList<>(Arrays.asList(ManagedAlgorithmColumn.Name, ManagedAlgorithmColumn.State, ManagedAlgorithmColumn.Error));
      if (verbose) {
        columns.add(ManagedAlgorithmColumn.StartParameter);
        columns.add(ManagedAlgorithmColumn.StartTime);
        columns.add(ManagedAlgorithmColumn.LastExecution);
        columns.add(ManagedAlgorithmColumn.StopTime);
      }
      generateRowsAndHeader(algInfos);
    }

    private void generateRowsAndHeader(Collection<ManagedAlgorithmInfo> algInfos) {
      header = new ArrayList<String>();
      for( ManagedAlgorithmColumn ac : columns ) {
        header.add( ac.toString() );
      }
      rows = new ArrayList<List<String>>();
      for( ManagedAlgorithmInfo ai : algInfos ) {
        rows.add( generateRow(ai) );
      }
    }

    private List<String> generateRow(ManagedAlgorithmInfo ai) {
      List<String> row = new ArrayList<String>();
      for( ManagedAlgorithmColumn ac : columns ) {
        row.add( ac.extract(ai) );
      }
      return row;
    }

    public List<String> getHeader() {
      return header;
    }
    
    @Override
    public List<List<String>> getRows() {
      return rows;
    }
    
    public enum ManagedAlgorithmColumn {
      Name {
        public String extract(ManagedAlgorithmInfo ai) {
          return ai.getName();
        }
      },
      State {
        public String extract(ManagedAlgorithmInfo ai) {
          return ai.getStatus().name();
        }
      },
      Error {
        public String extract(ManagedAlgorithmInfo ai) {
         return ai.getTerminationException().isPresent() ? ai.getTerminationException().get().getMessage() : "";
        }
      },
      StartParameter {
        public String extract(ManagedAlgorithmInfo ai) {
          return StringParameter.toString(ai.getAdditionalParameters(), ai.getParameter(), false);
        }
      },
      StartTime {
        public String extract(ManagedAlgorithmInfo ai) {
          return formatAsDate(ai.getStartTime());
        }
      },
      StopTime {
        public String extract(ManagedAlgorithmInfo ai) {
          return formatAsDate(ai.getStopTime());
        }
      },
      LastExecution {
        public String extract(ManagedAlgorithmInfo ai) {
          return ai.getLastExecution() < 0 ? "unavailable" : formatAsDate(ai.getLastExecution());
        }
      };

      public abstract String extract(ManagedAlgorithmInfo ai);
      
      private static String formatAsDate(long timestamp) {
        return timestamp > 0 ? Constants.defaultUTCSimpleDateFormatWithMS().format(new Date(timestamp)) : "-";
      }
    }

  }
  
}
