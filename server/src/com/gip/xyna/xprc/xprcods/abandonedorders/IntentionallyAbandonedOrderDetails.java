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

package com.gip.xyna.xprc.xprcods.abandonedorders;



import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import com.gip.xyna.xfmg.Constants;



public class IntentionallyAbandonedOrderDetails extends AbandonedOrderDetails {

  private static final long serialVersionUID = 1L;


  private Throwable cause;


  public IntentionallyAbandonedOrderDetails(long orderID, long rootOrderId, Throwable cause) {
    super(orderID, rootOrderId);
    this.cause = cause;
  }


  public Throwable getCause() {
    return cause;
  }


  public String getCauseDetails() {
    if (cause != null) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      cause.printStackTrace(new PrintStream(baos));
      try {
        return "Abandoning the order was cause by the following exception:\n"
            + baos.toString(Constants.DEFAULT_ENCODING);
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
    } else {
      return "unknown";
    }
  }

}
