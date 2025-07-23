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

package com.gip.xyna.xfmg.xfctrl.queuemgmnt;

import com.gip.xyna.xnwh.persistence.Storable;

public class QueueFacade implements IQueue {

    private final Queue queue;

    public QueueFacade(Queue q) {
        this.queue = q;
    }

    public Queue getQueue() {
        return (Queue)Storable.clone(queue);
    }

    public static QueueFacade fromQueue(Queue q) {
        return new QueueFacade(q);
    }

    public String getExternalName() {
        return queue.getExternalName();
    }

    public void setExternalName(String name) {
        queue.setExternalName(name);
    }

    public Integer getVersion() {
        return queue.getVersion();
    }

    public void setVersion(Integer i) {
        queue.setVersion(i);
    }

    @Override
    public String getUniqueName() {
        return queue.getUniqueName();
    }

    @Override
    public void setUniqueName(String uniqueName) {
       queue.setUniqueName(uniqueName);
    }

    @Override
    public QueueConnectData getConnectData() {
        return queue.getConnectDataForCurrentVersion();
    }

    @Override
    public void setConnectData(QueueConnectData connectData) {
        queue.setConnectData(connectData);
    }

    @Override
    public QueueType getQueueType() {
        return queue.getQueueTypeForCurrentVersion();
    }

    @Override
    public void setQueueType(QueueType queueType) {
       queue.setQueueType(queueType);
    }

    @Override
    public String toString() {
        return queue.toString();
    }

}