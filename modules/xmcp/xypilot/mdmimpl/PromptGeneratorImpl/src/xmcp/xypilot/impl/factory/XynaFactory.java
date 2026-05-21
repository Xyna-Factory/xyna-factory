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
package xmcp.xypilot.impl.factory;


/**
 * A singleton providing access to the XynaFactoryFacade.
 * It defaults to the DefaultXynaFactory, but can be set to a different implementation using setInstance().
 * @see XynaFactoryFacade
 */
public class XynaFactory {

    private static XynaFactoryFacade instance = null;

    public static XynaFactoryFacade getInstance() {
        if (instance == null) {
            instance = new DefaultXynaFactory();
        }
        return instance;
    }

    public static void setInstance(XynaFactoryFacade instance) {
        XynaFactory.instance = instance;
    }

}
