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

package org.activiti.cloud.services.job.executor;

import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.stream.binder.ConsumerProperties;
import org.springframework.cloud.stream.binding.BinderAwareChannelResolver;
import org.springframework.cloud.stream.binding.BindingService;
import org.springframework.cloud.stream.binding.SubscribableChannelBindingTargetFactory;
import org.springframework.cloud.stream.config.BindingServiceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "activiti.cloud.rb.job-executor.enabled", havingValue = "true", matchIfMissing = true)
public class MessageBasedJobManagerAutoConfiguration {
    
    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "activiti.cloud.rb.job-executor.message-job-consumer")
    public ConsumerProperties messageJobConsumerProperties() {
        return new ConsumerProperties();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public JobMessageInputChannelFactory jobMessageInputChannelFactory(SubscribableChannelBindingTargetFactory bindingTargetFactory,
                                                   BindingServiceProperties bindingServiceProperties,
                                                   ConfigurableListableBeanFactory beanFactory) {
        return new JobMessageInputChannelFactory(bindingTargetFactory, bindingServiceProperties, beanFactory);
        
    }    
    
    @Bean
    @ConditionalOnMissingBean
    public MessageBasedJobManagerFactory messageBasedJobManagerFactory(RuntimeBundleProperties runtimeBundleProperties,
                                                                       BinderAwareChannelResolver resolver) {
        return new DefaultMessageBasedJobManagerFactory(runtimeBundleProperties, resolver);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public MessageBasedJobManagerConfigurator messageBasedJobManagerConfigurator(BindingService bindingService,
                                                                        JobMessageInputChannelFactory jobMessageInputChannelFactory,
                                                                        MessageBasedJobManagerFactory messageBasedJobManagerFactory,
                                                                        ConsumerProperties messageJobConsumerProperties) {
        return new MessageBasedJobManagerConfigurator(bindingService,
                                                      jobMessageInputChannelFactory,
                                                      messageBasedJobManagerFactory,
                                                      messageJobConsumerProperties);
    }

}