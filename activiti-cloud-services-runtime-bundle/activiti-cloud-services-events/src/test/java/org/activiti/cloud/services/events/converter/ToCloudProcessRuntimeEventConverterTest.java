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

package org.activiti.cloud.services.events.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.TimerPayload;
import org.activiti.api.runtime.event.impl.BPMNSignalReceivedEventImpl;
import org.activiti.api.runtime.event.impl.BPMNTimerFiredEventImpl;
import org.activiti.api.runtime.model.impl.BPMNSignalImpl;
import org.activiti.api.runtime.model.impl.BPMNTimerImpl;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.api.model.shared.impl.events.CloudRuntimeEventImpl;
import org.activiti.cloud.api.process.model.events.CloudBPMNSignalReceivedEvent;
import org.activiti.cloud.api.process.model.events.CloudBPMNTimerFiredEvent;
import org.activiti.cloud.api.process.model.events.CloudProcessStartedEvent;
import org.activiti.engine.impl.persistence.entity.JobEntity;
import org.activiti.runtime.api.event.impl.ProcessStartedEventImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class ToCloudProcessRuntimeEventConverterTest {

    @InjectMocks
    private ToCloudProcessRuntimeEventConverter converter;

    @Mock
    private RuntimeBundleInfoAppender runtimeBundleInfoAppender;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void fromShouldConvertInternalProcessStartedEventToExternalEvent() {
        //given
        ProcessInstanceImpl processInstance = new ProcessInstanceImpl();
        processInstance.setId("10");
        processInstance.setProcessDefinitionId("myProcessDef");

        ProcessStartedEventImpl event = new ProcessStartedEventImpl(processInstance);
        event.setNestedProcessDefinitionId("myParentProcessDef");
        event.setNestedProcessInstanceId("2");

        //when
        CloudProcessStartedEvent processStarted = converter.from(event);

        //then
        assertThat(processStarted).isInstanceOf(CloudProcessStartedEvent.class);

        assertThat(processStarted.getEntity().getId()).isEqualTo("10");
        assertThat(processStarted.getEntity().getProcessDefinitionId()).isEqualTo("myProcessDef");
        assertThat(processStarted.getNestedProcessDefinitionId()).isEqualTo("myParentProcessDef");
        assertThat(processStarted.getNestedProcessInstanceId()).isEqualTo("2");

        verify(runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(ArgumentMatchers.any(CloudRuntimeEventImpl.class));
    }

    @Test
    public void shouldConvertBPMNSignalReceivedEventToCloudBPMNSignalReceivedEvent() {
        //given
        BPMNSignalImpl signal = new BPMNSignalImpl();
        signal.setProcessDefinitionId("procDefId");
        signal.setProcessInstanceId("procInstId");
        BPMNSignalReceivedEventImpl signalReceivedEvent = new BPMNSignalReceivedEventImpl(signal);

        //when
        CloudBPMNSignalReceivedEvent cloudEvent = converter.from(signalReceivedEvent);
        assertThat(cloudEvent.getEntity()).isEqualTo(signal);
        assertThat(cloudEvent.getProcessDefinitionId()).isEqualTo("procDefId");
        assertThat(cloudEvent.getProcessInstanceId()).isEqualTo("procInstId");

        //then
        verify(runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(ArgumentMatchers.any(CloudRuntimeEventImpl.class));
    }
    
    @Test
    public void shouldConvertBPMNTimerFiredEventToCloudBPMNTimerFiredEvent() {
        //given
        BPMNTimerImpl timer = new BPMNTimerImpl("entityId");
        timer.setProcessInstanceId("procInstId");
        timer.setProcessDefinitionId("procDefId");
        TimerPayload timerPayload = ProcessPayloadBuilder.timer()
                                    .withJobType(JobEntity.JOB_TYPE_TIMER)
                                    .build();

        timer.setTimerPayload(timerPayload);
        BPMNTimerFiredEventImpl timerFiredEvent = new BPMNTimerFiredEventImpl(timer);

        //when
        CloudBPMNTimerFiredEvent cloudEvent = converter.from(timerFiredEvent);
        assertThat(cloudEvent.getEntity()).isEqualTo(timer);
        assertThat(cloudEvent.getProcessDefinitionId()).isEqualTo("procDefId");
        assertThat(cloudEvent.getProcessInstanceId()).isEqualTo("procInstId");

        //then
        verify(runtimeBundleInfoAppender).appendRuntimeBundleInfoTo(ArgumentMatchers.any(CloudRuntimeEventImpl.class));
    }
}