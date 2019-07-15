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

import java.util.Date;

import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.engine.impl.asyncexecutor.DefaultJobManager;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.engine.runtime.Job;

public class MessageBasedJobManager extends DefaultJobManager {

    private final RuntimeBundleProperties runtimeBundleProperties;
    private final JobMessageProducer jobMessageProducer;

    public MessageBasedJobManager(ProcessEngineConfigurationImpl processEngineConfiguration,
                                  RuntimeBundleProperties runtimeBundleProperties,
                                  JobMessageProducer jobMessageProducer) {
        super(processEngineConfiguration);
        
        this.runtimeBundleProperties = runtimeBundleProperties;
        this.jobMessageProducer = jobMessageProducer;
    }

    @Override
    protected void triggerExecutorIfNeeded(final JobEntity jobEntity) {
        sendMessage(jobEntity);
    }

    @Override
    public void unacquire(final Job job) {

        if (job instanceof JobEntity) {
            JobEntity jobEntity = (JobEntity) job;

            // When unacquiring, we up the lock time again., so that it isn't cleared by the reset expired thread.
            jobEntity.setLockExpirationTime(new Date(processEngineConfiguration.getClock()
                                                                               .getCurrentTime()
                                                                               .getTime() + processEngineConfiguration.getAsyncExecutor()
                                                                                                                      .getAsyncJobLockTimeInMillis()));
        }

        sendMessage(job);
    }
    
    /**
     * Scoped destination name by runtime bundle service name   
     * 
     */
    public String getDestination() {
        return runtimeBundleProperties.getServiceName() + "." + this.getInputChannelName();
    }
   
    protected String getInputChannelName() {
        return this.getClass().getSimpleName();
    }

    public void sendMessage(final Job job) {
        jobMessageProducer.sendMessage(getDestination(), job);
    }
    
}