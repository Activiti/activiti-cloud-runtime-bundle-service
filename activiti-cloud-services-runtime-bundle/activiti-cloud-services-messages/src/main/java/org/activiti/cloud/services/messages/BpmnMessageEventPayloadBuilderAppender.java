/*
 * Copyright 2018 Alfresco, Inc. and/or its affiliates.
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
package org.activiti.cloud.services.messages;

import org.activiti.api.process.model.payloads.MessageEventPayload;
import org.activiti.cloud.services.events.message.MessageBuilderAppender;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

public class BpmnMessageEventPayloadBuilderAppender implements MessageBuilderAppender {

    private final MessageEventPayload messageEventPayload;

    public BpmnMessageEventPayloadBuilderAppender(MessageEventPayload messageEventPayload) {
        Assert.notNull(messageEventPayload, "messageEventPayload must not be null");

        this.messageEventPayload = messageEventPayload;
    }

    @Override
    public <P> MessageBuilder<P> apply(MessageBuilder<P> request) {
        Assert.notNull(request, "request must not be null");
        
        return request.setHeader(BpmnMessageEventHeaders.BPMN_MESSAGE_BUSINESS_KEY, messageEventPayload.getBusinessKey())
                      .setHeader(BpmnMessageEventHeaders.BPMN_MESSAGE_CORRELATION_KEY, messageEventPayload.getCorrelationKey())
                      .setHeader(BpmnMessageEventHeaders.BPMN_MESSAGE_NAME, messageEventPayload.getName())
       ;
    }

}
