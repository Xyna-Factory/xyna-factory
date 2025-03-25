/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
package xdev.yang.impl.operation;



import java.util.LinkedList;
import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.RunnableForFilterAccess;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBoolean;

import xact.http.URLPath;
import xdev.yang.impl.GuiHttpInteraction;



public class AsyncDeployment {

  public static final XynaPropertyBoolean PROP_ASYNC_DEPLOY = new XynaPropertyBoolean("xdev.yang.AsyncDeploy", true)
      .setDefaultDocumentation(DocumentationLanguage.EN, "Deploy OperationGroup asynchroniously when assigning values.")
      .setDefaultDocumentation(DocumentationLanguage.DE, "Asynchrones deploy der OperationGroup beim bearbeiten von Yang Zuweisungen");

  private static AsyncDeployment instance;

  private List<Thread> executionThreads;
  private long deploymentCount;


  public static AsyncDeployment getInstance() {
    if (instance == null) {
      synchronized (AsyncDeployment.class) {
        if (instance == null) {
          instance = new AsyncDeployment();
        }
      }
    }
    return instance;
  }


  private AsyncDeployment() {
    executionThreads = new LinkedList<>();
  }


  public void requestAsyncDeploy(DeployData data) {
    Thread executionThread = new Thread(() -> deployAsync(data), "Yang-AsyncDeployment-" + deploymentCount++);
    executionThread.setDaemon(true);
    synchronized (executionThreads) {
      executionThreads.add(executionThread);
    }
    executionThread.start();
  }


  private void deployAsync(DeployData toDeploy) {
    try {
      executeDeploy(toDeploy.getRunnable(), toDeploy.getUrl());
    } finally {
      synchronized (executionThreads) {
        executionThreads.remove(Thread.currentThread());
      }
    }
  }


  private void executeDeploy(RunnableForFilterAccess runnable, URLPath url) {
    try {
      runnable.execute(url, GuiHttpInteraction.METHOD_POST, "{\"revision\":3}");
    } catch (XynaException e) {
      throw new RuntimeException("Could not deploy datatype", e);
    }
  }


  public static class DeployData {

    private RunnableForFilterAccess runnable;
    private URLPath url;


    public DeployData(RunnableForFilterAccess runnable, URLPath url) {
      this.runnable = runnable;
      this.url = url;
    }


    public RunnableForFilterAccess getRunnable() {
      return runnable;
    }


    public URLPath getUrl() {
      return url;
    }

  }
}
