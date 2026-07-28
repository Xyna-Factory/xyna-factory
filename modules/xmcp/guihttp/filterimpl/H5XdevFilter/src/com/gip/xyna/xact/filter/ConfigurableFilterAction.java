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
package com.gip.xyna.xact.filter;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.filter.FilterAction.FilterActionInstance;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;

/**
 * Marker interface for FilterActions that are aware of the filter configuration.
 * H5XdevFilter calls actWithConfig() instead of act() for actions implementing this interface.
 * All other actions remain untouched.
 */
public interface ConfigurableFilterAction extends FilterAction {

  FilterActionInstance actWithConfig(URLPath url, HTTPTriggerConnection tc, H5XdevFilterParameter config)
      throws XynaException;

}

