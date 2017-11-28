/*
 * Copyright 2017 Alfresco, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.activiti.cloud.services.events.listeners;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProcessEngineEventsAggregator extends BaseCommandContextEventsAggregator<ProcessEngineEvent, MessageProducerCommandContextCloseListener>{

    private final MessageProducerCommandContextCloseListener closeListener;

    @Autowired
    public ProcessEngineEventsAggregator(MessageProducerCommandContextCloseListener closeListener) {
        this.closeListener = closeListener;
    }

    @Override
    protected Class<MessageProducerCommandContextCloseListener> getCloseListenerClass() {
        return MessageProducerCommandContextCloseListener.class;
    }

    @Override
    protected MessageProducerCommandContextCloseListener getCloseListener() {
        return closeListener;
    }

    @Override
    protected String getAttributeKey() {
        return MessageProducerCommandContextCloseListener.PROCESS_ENGINE_EVENTS;
    }
}
