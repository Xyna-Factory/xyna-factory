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
package com.gip.xyna.xprc.xfractwfe.generation.restriction;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;


@Retention(RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
public @interface ModelledRestriction {
  
  public static String PARAMETER_NAME_UTILIZATION_POLICY = "utilizationPolicy";
  
  
  @ModelledRestriction
  @Retention(RUNTIME)
  @Target({FIELD})
  public @interface MaxLength {
    
    public static String PARAMETER_NAME_LIMIT = "limit";

    int limit();
    String[] utilizationPolicy() default {};
    
  }

  @ModelledRestriction
  @Retention(RUNTIME)
  @Target({FIELD})
  public @interface Mandatory {

    String[] utilizationPolicy() default {};
    
  }
  
  @ModelledRestriction
  @Retention(RUNTIME)
  @Target({FIELD})
  public @interface DefaultType {
    
    public static String PARAMETER_NAME_DEFAULT_TYPE = "defaultType";
    
    String defaultType();
    String[] utilizationPolicy() default {};
    
  }
  

}
