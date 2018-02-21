package org.activiti.cloud.services.events.converter;

import org.activiti.cloud.services.api.events.ProcessEngineEvent;
import org.activiti.cloud.services.api.model.converter.TaskCandidateUserConverter;
import org.activiti.cloud.services.events.TaskCandidateUserAddedEvent;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.delegate.event.impl.ActivitiEntityEventImpl;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.IdentityLinkType;
import org.activiti.engine.task.Task;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.activiti.cloud.services.events.converter.EventConverterContext.getPrefix;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

public class TaskCandidateUserAddedEventConverterTest {

    @InjectMocks
    private TaskCandidateUserAddedEventConverter taskCandidateUserAddedEventConverter;

    @Mock
    private TaskCandidateUserConverter taskCandidateUserConverter;

    @Mock
    private RuntimeBundleProperties runtimeBundleProperties;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void fromShouldConvertInternalEventToExternalEvent() throws Exception {
        //given
        ActivitiEntityEventImpl activitiEvent = mock(ActivitiEntityEventImpl.class);
        given(activitiEvent.getType()).willReturn(ActivitiEventType.ENTITY_CREATED);
        given(activitiEvent.getExecutionId()).willReturn("1");
        given(activitiEvent.getProcessInstanceId()).willReturn("1");
        given(activitiEvent.getProcessDefinitionId()).willReturn("myProcessDef");

        IdentityLink internalIdentityLink = mock(IdentityLink.class);
        given(activitiEvent.getEntity()).willReturn(internalIdentityLink);

        org.activiti.cloud.services.api.model.TaskCandidateUser externalTaskCandidateUser = mock(org.activiti.cloud.services.api.model.TaskCandidateUser.class);
        given(taskCandidateUserConverter.from(internalIdentityLink)).willReturn(externalTaskCandidateUser);

        given(runtimeBundleProperties.getName()).willReturn("myApp");

        //when
        ProcessEngineEvent pee = taskCandidateUserAddedEventConverter.from(activitiEvent);

        //then
        assertThat(pee).isInstanceOf(TaskCandidateUserAddedEvent.class);
        assertThat(pee.getExecutionId()).isEqualTo("1");
        assertThat(pee.getProcessInstanceId()).isEqualTo("1");
        assertThat(pee.getProcessDefinitionId()).isEqualTo("myProcessDef");
        assertThat(pee.getApplicationName()).isEqualTo("myApp");
        assertThat(((TaskCandidateUserAddedEvent) pee).getTaskCandidateUser()).isEqualTo(externalTaskCandidateUser);
    }

    @Test
    public void handledTypeShouldReturnTaskCandidateUser() throws Exception {
        //when
        String activitiEventType = taskCandidateUserAddedEventConverter.handledType();
        TaskCandidateUserAddedEvent activitiEvent = mock(TaskCandidateUserAddedEvent.class);
        given(activitiEvent.getType()).willReturn(ActivitiEventType.ENTITY_CREATED);
        IdentityLink internalIdentityLink = mock(IdentityLink.class);
        given(activitiEvent.getEntity()).willReturn(internalIdentityLink);
        given(internalIdentityLink.getUserId()).willReturn("bob");
        given(internalIdentityLink.getType()).willReturn(IdentityLinkType.CANDIDATE);
        //then
        assertThat(activitiEventType).isEqualTo(getPrefix(activitiEvent) + ActivitiEventType.ENTITY_CREATED);
    }

}
