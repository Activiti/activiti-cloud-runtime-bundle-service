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
package org.activiti.cloud.services.events.message;

import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

public class RuntimeBundleInfoMessageBuilderFilter<P> implements MessageBuilderFilter<P> {

    private final RuntimeBundleProperties properties;

    public RuntimeBundleInfoMessageBuilderFilter(RuntimeBundleProperties properties) {
        Assert.notNull(properties, "properties must not be null");

        this.properties = properties;
    }

    @Override
    public MessageBuilder<P> apply(MessageBuilder<P> request) {
        return request.setHeader("appName", properties.getAppName()).setHeader("appVersion", properties.getAppVersion())
                .setHeader("serviceName", properties.getServiceName())
                .setHeader("serviceFullName", properties.getServiceFullName())
                .setHeader("serviceType", properties.getServiceType())
                .setHeader("serviceVersion", properties.getServiceVersion());
    }

}