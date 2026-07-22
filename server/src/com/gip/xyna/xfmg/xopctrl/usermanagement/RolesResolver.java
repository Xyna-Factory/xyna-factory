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
 * available login roles.
 */
public interface RolesResolver {

  /**
   * Resolves available login roles for the given domain.
   * 
   * @param domainName Name of the domain for which roles are resolved
   * @param credential Credential/token used to determine available roles
   * @return List of available role names, empty if resolution fails or no roles available
   */
  java.util.List<String> resolveAvailableRoles(String domainName, String credential);
}

