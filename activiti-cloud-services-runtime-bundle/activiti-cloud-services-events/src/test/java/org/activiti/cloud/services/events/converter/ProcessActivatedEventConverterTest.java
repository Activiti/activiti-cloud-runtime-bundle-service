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

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.api.model.ProcessInstance;
import org.activiti.cloud.services.api.model.converter.ProcessInstanceConverter;
import org.activiti.cloud.services.events.ProcessActivatedEvent;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.engine.delegate.event.ActivitiEntityEvent;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.activiti.cloud.services.events.converter.EventConverterContext.getPrefix;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class ProcessActivatedEventConverterTest {

    @InjectMocks
    private ProcessActivatedEventConverter processActivatedEventConverter;

    @Mock
    private ProcessInstanceConverter processInstanceConverter;

    @Mock
    private RuntimeBundleProperties runtimeBundleProperties;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void fromShouldConvertInternalProcessActivatedEventToExternalEvent() throws Exception {
        //given
        ActivitiEntityEvent activitiEvent = mock(ActivitiEntityEvent.class);
        given(activitiEvent.getType()).willReturn(ActivitiEventType.ENTITY_ACTIVATED);
        given(activitiEvent.getExecutionId()).willReturn("1");
        given(activitiEvent.getProcessInstanceId()).willReturn("1");
        given(activitiEvent.getProcessDefinitionId()).willReturn("myProcessDef");
        ExecutionEntityImpl executionEntity = mock(ExecutionEntityImpl.class);
        ExecutionEntityImpl internalProcessInstance = mock(ExecutionEntityImpl.class);

        given(activitiEvent.getEntity()).willReturn(executionEntity);
        given(executionEntity.getProcessInstance()).willReturn(internalProcessInstance);

        given(runtimeBundleProperties.getName()).willReturn("myApp");

        ProcessInstance externalProcessInstance = mock(ProcessInstance.class);
        given(processInstanceConverter.from(internalProcessInstance)).willReturn(externalProcessInstance);

        //when
        ProcessEngineEvent pee = processActivatedEventConverter.from(activitiEvent);

        //then
        assertThat(pee).isInstanceOf(ProcessActivatedEvent.class);
        assertThat(pee.getExecutionId()).isEqualTo("1");
        assertThat(pee.getProcessInstanceId()).isEqualTo("1");
        assertThat(pee.getProcessDefinitionId()).isEqualTo("myProcessDef");
        assertThat(pee.getApplicationName()).isEqualTo("myApp");
        assertThat(((ProcessActivatedEvent) pee).getProcessInstance()).isEqualTo(externalProcessInstance);
    }

    @Test
    public void handledTypeShouldReturnProcessActivated() throws Exception {
        //when
        String activitiEventType = processActivatedEventConverter.handledType();
        ActivitiEntityEvent activitiEvent = mock(ActivitiEntityEvent.class);
        given(activitiEvent.getType()).willReturn(ActivitiEventType.ENTITY_ACTIVATED);
        ExecutionEntityImpl executionEntity = mock(ExecutionEntityImpl.class);
        ExecutionEntityImpl internalProcessInstance = mock(ExecutionEntityImpl.class);
        given(activitiEvent.getEntity()).willReturn(executionEntity);
        given(executionEntity.getProcessInstance()).willReturn(internalProcessInstance);

        //then
        assertThat(activitiEventType).isEqualTo(getPrefix(activitiEvent) + ActivitiEventType.ENTITY_ACTIVATED);
    }
}