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
package com.gip.xyna;



import java.util.ArrayList;

import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xmcp.RMIAdapter;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskCreationParameter;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskStatisticsParameter;
import com.gip.xyna.xprc.xfqctrl.ordercreation.LoadControlledOrderCreationTaskCreationParameter;
import com.gip.xyna.xprc.xfqctrl.ordercreation.RateControlledOrderCreationTaskCreationParameter;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;


/**
 * klasse kann zusammen mit lasttest.sh im gleichen verzeichnis genutzt werden um lasttests einfach einzustellen.
 * kann zb an projekte/QS-team zum testen gegeben werden. 
 */
public class LastTestClient {

  public static void main(String[] args) {
    try {
      if (args.length == 7) {
        if (args[0].equals("start")) {
          String hostname = args[1];
          int port = Integer.valueOf(args[2]);
          String orderType = args[3];
          String type = args[4];
          int max = Integer.valueOf(args[5]);
          double parameter = Double.valueOf(args[6]);
          startTask(hostname, port, orderType, type, max, parameter);
        } else {
          printHelp();
        }
      } else if (args.length == 4) {
        if (args[0].equals("stop")) {
          String hostname = args[1];
          int port = Integer.valueOf(args[2]);
          long id = Long.valueOf(args[3]);
          endTask(hostname, port, id);
        } else {
          printHelp();
        }
      } else {
        printHelp();
      }
    } catch (NumberFormatException e) {
      e.printStackTrace();
      printHelp();
    } catch (Throwable t) {
      t.printStackTrace();
    }

  }


  private static void printHelp() {
    System.out.println("usage: start <hostname> <rmi-port> <ordertype> ('load'|'rate'') <maxorders> <load|rate>\n"
        + "          starts tests with <maxorders> orders of type <ordertype>, with the provided load or rate.\n"
        + "          load = number of orders executed simultanously.\n"
        + "          rate = number of orders started per second (Hz).\n "
        + "       stop <hostname> <rmi-port> <task id>\n" + "          stops an existing task.\n"
        + "example: start 10.11.12.13 1099 xact.TestWf rate 500 2.5");
  }


  private static void addOrder(ArrayList<XynaOrderCreationParameter> list, String ot, XynaObject input) {
    XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(new DestinationKey(ot), input);
    xocp.setPriority(5);
    list.add(xocp);
  }


  public static void startTask(String hostname, int port, String ordertype, String type, int max, double parameter)
      throws Exception {
    RMIAdapter rmi =
        new RMIAdapter(RMIAdapter.getSingleURLChooser("//" + hostname + ":" + port + "/XynaRMIChannel"), "XYNAADMIN",
                       "XYNAADMIN");
    ArrayList<XynaOrderCreationParameter> list = new ArrayList<XynaOrderCreationParameter>();
    addOrder(list, ordertype, new Container());

    FrequencyControlledTaskCreationParameter creationParas;
    if (type.equalsIgnoreCase("load")) {
      creationParas =
          new LoadControlledOrderCreationTaskCreationParameter(list.get(0).getOrderType() + "-"
              + System.currentTimeMillis(), max, Math.round(parameter), list); //3000 , 1000 gleichzeitig
    } else if (type.equals("rate")) {
      creationParas =
          new RateControlledOrderCreationTaskCreationParameter(list.get(0).getOrderType() + "-"
              + System.currentTimeMillis(), max, parameter, list);
    } else {
      throw new Exception("unsupported type: " + type + ". supported are 'load', 'rate'.");
    }

    creationParas
        .setFrequencyControlledTaskStatisticsParameters(new FrequencyControlledTaskStatisticsParameter(300, 1l)); //maximal datenpunkte

    long id =
        rmi.getRmiInterface().startFrequencyControlledTask(rmi.getUserName(), rmi.getPasswordHashed(), creationParas);
    System.out.println("started frequency controlled task with id = " + id);
  }


  public static void endTask(String hostname, int port, long taskid) throws Exception {
    RMIAdapter rmi =
        new RMIAdapter(RMIAdapter.getSingleURLChooser("//" + hostname + ":" + port + "/XynaRMIChannel"), "XYNAADMIN",
                       "XYNAADMIN");
    rmi.getRmiInterface().cancelFrequencyControlledTask(rmi.getUserName(), rmi.getPasswordHashed(), taskid);
    System.out.println("cancelled task");
  }

}
