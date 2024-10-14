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
package com.gip.xyna.xact.filter.session.repair;



import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;



/*package*/ class RemoveTypeCastData {

  private String location;
  private StepFunction step;
  private List<AVariable> variables;
  private Function<Integer, String> idGenerator;
  private Supplier<String[]> castSupplier;


  public String getLocation() {
    return location;
  }


  public void setLocation(String location) {
    this.location = location;
  }


  public StepFunction getStep() {
    return step;
  }


  public void setStep(StepFunction step) {
    this.step = step;
  }


  public List<AVariable> getVariables() {
    return variables;
  }


  public void setVariables(List<AVariable> variables) {
    this.variables = variables;
  }


  public Function<Integer, String> getIdGenerator() {
    return idGenerator;
  }


  public void setIdGenerator(Function<Integer, String> idGenerator) {
    this.idGenerator = idGenerator;
  }


  public Supplier<String[]> getCastSupplier() {
    return castSupplier;
  }


  public void setCastSupplier(Supplier<String[]> castSupplier) {
    this.castSupplier = castSupplier;
  }
}
