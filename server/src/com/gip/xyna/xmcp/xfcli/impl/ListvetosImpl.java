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

package com.gip.xyna.xmcp.xfcli.impl;

import java.io.OutputStream;
import java.util.Collection;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listvetos;
import com.gip.xyna.xprc.xsched.VetoInformationStorable;



public class ListvetosImpl extends XynaCommandImplementation<Listvetos> {

  @Override
  public void execute(OutputStream statusOutputStream, Listvetos command) throws XynaException {

    Collection<VetoInformationStorable> vetos = XynaFactory.getInstance().getProcessing().listVetoInformation();

    String formatHeader = "   %15s  %15s  %30s  %7s  %-60s";
    String formatLine   = " - %15s  %15d  %30s  %7d  %-60s";

    String formattedHeader = String.format(formatHeader, "Name", "Order ID", "Ordertype", "Binding", "Documentation");
    writeLineToCommandLine(statusOutputStream, formattedHeader);
    for (VetoInformationStorable veto : vetos) {
      String s =
          String.format(formatLine, veto.getVetoName(), veto.getUsingOrderId(), veto.getUsingOrdertype(),
                        veto.getBinding(),
                        veto.getDocumentation() != null ? veto.getDocumentation() : "");
      writeLineToCommandLine(statusOutputStream, s);
    }

    /*
    VetoManagementInterface vma = XynaFactory.getInstance().getProcessing().getXynaScheduler().getVetoManagement().getVMAlgorithm();
    if( vma instanceof VM_SeparateThread ) {
      VM_SeparateThread vmst = (VM_SeparateThread)vma;
      writeLineToCommandLine(statusOutputStream, vmst.getVetoCache().showVetoCache() );
    }*/
    
  }

}
