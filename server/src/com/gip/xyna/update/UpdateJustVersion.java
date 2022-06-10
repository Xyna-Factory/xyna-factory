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

package com.gip.xyna.update;

import com.gip.xyna.utils.exceptions.XynaException;


/**
 * einfach nur dafür da, die versionsnummer zu aktualisieren
 *
 */
public class UpdateJustVersion extends Update {
  
  private Version newVersion;
  private Version oldVersion;
  private boolean mustUpdateGeneratedClasses;
  private boolean mustRewriteWorkflows;
  private boolean mustRewriteDatatypes;
  private boolean mustRewriteExceptions;
  
  public UpdateJustVersion(Version oldVersion, Version newVersion) {
    this(oldVersion, newVersion, false);
  }


  public UpdateJustVersion(Version oldVersion, Version newVersion, boolean mustUpdateGeneratedClasses,
                           boolean mustRewriteWorkflows, boolean mustRewriteDatatypes, boolean mustRewriteExceptions) {
    this.newVersion = newVersion;
    this.oldVersion = oldVersion;
    this.mustUpdateGeneratedClasses = mustUpdateGeneratedClasses;
    this.mustRewriteDatatypes = mustRewriteDatatypes;
    this.mustRewriteWorkflows = mustRewriteWorkflows;
    this.mustRewriteExceptions = mustRewriteExceptions;
  }

  public UpdateJustVersion(Version oldVersion, Version newVersion, boolean mustUpdateGeneratedClasses) {
    this.newVersion = newVersion;
    this.oldVersion = oldVersion;
    this.mustUpdateGeneratedClasses = mustUpdateGeneratedClasses;
  }

  public void setMustUpdateGeneratedClasses(boolean mustUpdateGeneratedClasses) {
    this.mustUpdateGeneratedClasses = mustUpdateGeneratedClasses;
  }

  
  @Override
  protected Version getAllowedVersionForUpdate() {
    return oldVersion;
  }

  @Override
  protected Version getVersionAfterUpdate() throws XynaException {
    return newVersion;
  }

  @Override
  protected void update() throws XynaException {
    //tut nichts 
  }

  @Override
  public boolean mustUpdateGeneratedClasses() {
    return mustUpdateGeneratedClasses;
  }


  @Override
  public boolean mustRewriteWorkflows() {
    return mustRewriteWorkflows;
  }


  @Override
  public boolean mustRewriteDatatypes() {
    return mustRewriteDatatypes;
  }


  @Override
  public boolean mustRewriteExceptions() {
    return mustRewriteExceptions;
  }

}
