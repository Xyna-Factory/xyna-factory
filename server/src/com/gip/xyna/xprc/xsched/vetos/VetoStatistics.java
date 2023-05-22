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
package com.gip.xyna.xprc.xsched.vetos;

import java.util.Collection;

import com.gip.xyna.xfmg.statistics.XynaStatistics.StatisticsReportEntry;

public class VetoStatistics {

  public static StatisticsReportEntry[] createReport(Collection<VetoInformation> vetos) {
    
    StatisticsReportEntry[] report = new StatisticsReportEntry[vetos.size() * 2];
    
    int i=-1;
    for( final VetoInformation v : vetos ) {
      ++i;
      
      report[i * 2 + 0] =  new StatisticsReportEntry() {
        public String getValue() {
          String name = v.getName();
          return name;
        }

        public String getValuePath() {
          return "XPRC.XSched.CC.Vetos." + v.getName() + ".Name";
        }

        public String getDescription() {
          return "Name of the veto.";
        }
      };
      
      report[i * 2 + 1] =  new StatisticsReportEntry() {
        public Long getValue() {
          return v.getUsingOrderId();
        }

        public String getValuePath() {
          return "XPRC.XSched.CC.Vetos." + v.getName() + ".UsingOrderID";
        }

        public String getDescription() {
          return "Order ID of the order currently occupying this veto.";
        }
      };
      
//      report[i * 4 + 2] =  new StatisticsReportEntry() {
//        public String getValue() {
//          String ot = v.getUsingOrdertype();          
//          return ot;
//        }
//
//        public String getValuePath() {
//          return "XPRC.XSched.CC.Vetos." + v.getVetoName() + ".UsingOrderType";
//        }
//      };
//      
//      report[i * 4 + 3] =  new StatisticsReportEntry() {
//        public String getValue() {
//          String ot = v.;          
//          return ot;
//        }
//
//        public String getValuePath() {
//          return "XPRC.XSched.CC.Vetos." + v.getVetoName() + ".Administrative";
//        }
//      };
     
    }
    
    return report;
  }
  
  
}
