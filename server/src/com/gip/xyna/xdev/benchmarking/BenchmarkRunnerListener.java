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
package com.gip.xyna.xdev.benchmarking;

import java.util.concurrent.atomic.AtomicLong;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.ResponseListener;
import com.gip.xyna.xprc.xpce.OrderContext;

public class BenchmarkRunnerListener extends ResponseListener {
  
  private static final long serialVersionUID = 1L;
  private final AtomicLong failed;
  private final AtomicLong succeeded;
  
  BenchmarkRunnerListener(AtomicLong failed, AtomicLong succeeded) {
    this.failed = failed;
    this.succeeded = succeeded;
  }
    

  @Override
  public void onError(XynaException[] e, OrderContext ctx) {
    failed.incrementAndGet();
  }

  @Override
  public void onResponse(GeneralXynaObject response, OrderContext ctx) {
    succeeded.incrementAndGet();
  }
 
}
