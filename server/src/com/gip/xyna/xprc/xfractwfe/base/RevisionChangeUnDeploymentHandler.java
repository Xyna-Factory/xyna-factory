/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package com.gip.xyna.xprc.xfractwfe.base;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.trigger.FilterInstanceStorable;
import com.gip.xyna.xact.trigger.FilterStorable;
import com.gip.xyna.xact.trigger.TriggerInstanceStorable;
import com.gip.xyna.xact.trigger.TriggerStorable;
import com.gip.xyna.xprc.exceptions.XPRC_DeploymentHandlerException;
import com.gip.xyna.xprc.exceptions.XPRC_UnDeploymentHandlerException;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.DeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling.UndeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.planning.Capacity;


public class RevisionChangeUnDeploymentHandler implements DeploymentHandler, UndeploymentHandler {

  private static Logger logger = CentralFactoryLogging.getLogger(RevisionChangeUnDeploymentHandler.class);

  private final Set<Long> objects = new HashSet<Long>();
  private Consumer<Set<Long>> consumer;

  public RevisionChangeUnDeploymentHandler(Consumer<Set<Long>> consumer) {
    this.consumer = consumer;
  }
  //DeploymentHandler

  @Override
  public void begin() throws XPRC_DeploymentHandlerException {
    objects.clear();
  }

  @Override
  public void exec(GenerationBase object, DeploymentMode mode) throws XPRC_DeploymentHandlerException {
    objects.add(object.getRevision());
  }

  @Override
  public void finish(boolean success) throws XPRC_DeploymentHandlerException {
    if (!success) {
      objects.clear();
      return;
    }

    this.consumer.accept(objects);
    objects.clear();
  }

  // UndeploymentHandler

  @Override
  public void exec(GenerationBase object) throws XPRC_UnDeploymentHandlerException {
    objects.add(object.getRevision());
  }

  @Override
  public void finish() throws XPRC_UnDeploymentHandlerException {
    // invalidate changed revisions
    Set<Long> revisionsToInvalidate = new HashSet<Long>();
    for (Long object : objects) {
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getParentRevisionsRecursivly(object, revisionsToInvalidate);
    }
   
    this.consumer.accept(revisionsToInvalidate);
    objects.clear();
  }

  @Override
  public boolean executeForReservedServerObjects() { return false; }
  @Override
  public void exec(FilterInstanceStorable object) { }
  @Override
  public void exec(TriggerInstanceStorable object) { }
  @Override
  public void exec(FilterStorable object) { }
  @Override
  public void exec(TriggerStorable object) { }
  @Override
  public void exec(Capacity object) { }
  @Override
  public void exec(DestinationKey object) { }
}