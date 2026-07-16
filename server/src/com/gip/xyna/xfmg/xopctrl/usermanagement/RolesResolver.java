/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package com.gip.xyna.xfmg.xopctrl.usermanagement;



import java.util.Optional;

import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;



/**
 * Optional capability for DomainTypeSpecificData implementations that can resolve
 * available login roles through a dedicated workflow.
 */
public interface RolesResolver {

  /**
   * Optional ordertype used to resolve available roles for login.
   */
  Optional<String> getRolesResolverOrdertype();

  /**
   * Runtime context where the resolve workflow is located.
   */
  RuntimeContext getRolesResolverRuntimeContext();

  /**
   * OrderContext key under which credential data for the resolver workflow is passed.
   */
  String getRolesResolverOrderContextKey();
}

