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
package xact.telnet.impl;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;


import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;

public class TelnetConnectionServiceOperationImpl implements
        ExtendedDeploymentTask {

    private static Map<Long, TransientConnectionData> openConnections = new HashMap<Long, TransientConnectionData>();

    private static AtomicLong idGenerator = new AtomicLong(0);

    public void onDeployment() throws XynaException {
    }

    public void onUndeployment() throws XynaException {
        for (TransientConnectionData transientData : openConnections.values()) {
            transientData.disconnect();
        }
    }

    public static long registerOpenConnection(long id,
            TransientConnectionData transientConnectionData) {
        if (id == -1) {
            id = idGenerator.incrementAndGet();
        }
        openConnections.put(id, transientConnectionData);
        return id;
    }

    public static TransientConnectionData getTransientData(long id) {
        return openConnections.get(id);
    }

    public static void removeTransientData(long id) {
        openConnections.remove(id);
    }


    public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
        // TODO Auto-generated method stub
        return BehaviorAfterOnUnDeploymentTimeout.IGNORE;
    }


    public Long getOnUnDeploymentTimeout() {
        return 3000L;
    }

}
