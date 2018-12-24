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

package org.activiti.cloud.starter.tests.runtime;

import static org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate.PROCESS_INSTANCES_RELATIVE_URL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.model.payloads.SignalPayload;
import org.activiti.api.task.model.Task;
import org.activiti.cloud.api.model.shared.CloudVariableInstance;
import org.activiti.cloud.api.process.model.CloudProcessDefinition;
import org.activiti.cloud.api.process.model.CloudProcessInstance;
import org.activiti.cloud.api.task.model.CloudTask;
import org.activiti.cloud.starter.tests.definition.ProcessDefinitionIT;
import org.activiti.cloud.starter.tests.helper.ProcessInstanceRestTemplate;
import org.activiti.cloud.starter.tests.helper.TestEventListener;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.event.ActivitiEvent;
import org.activiti.engine.delegate.event.ActivitiEventListener;
import org.activiti.engine.delegate.event.ActivitiEventType;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandContextCloseListener;
import org.activiti.services.subscription.SignalSender;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resources;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource("classpath:application-test.properties")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SignalIT {

    @Value("${spring.application.name")
    private String serviceName;

    @Autowired
    private TestEventListener testEventListener;

    @Autowired
    private SignalSender signalSender;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ProcessEngineConfiguration processEngineConfiguration;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProcessInstanceRestTemplate processInstanceRestTemplate;

    private static final String SIGNAL_PROCESS = "ProcessWithBoundarySignal";

    private Map<String, String> processDefinitionIds = new HashMap<>();

    @Before
    public void setUp() {
        ResponseEntity<PagedResources<CloudProcessDefinition>> processDefinitions = getProcessDefinitions();
        assertThat(processDefinitions.getBody().getContent()).isNotNull();
        for (ProcessDefinition pd : processDefinitions.getBody().getContent()) {
            processDefinitionIds.put(pd.getName(),
                                     pd.getId());
        }
    }

    @Test
    public void shouldBroadcastSignals() {
        //when
        runtimeService.startProcessInstanceByKey("broadcastSignalCatchEventProcess");
        ReflectionTestUtils.setField(signalSender, "serviceName", "dummyService");
        ((ProcessEngineConfigurationImpl) processEngineConfiguration).getCommandExecutor().execute(new Command<Void>() {
            public Void execute(CommandContext commandContext) {
                runtimeService.startProcessInstanceByKey("broadcastSignalEventProcess");
                commandContext.addCloseListener(new CommandContextCloseListener() {
                    @Override
                    public void closing(CommandContext context) {
                        runtimeService.startProcessInstanceByKey("broadcastSignalCatchEventProcess");
                    }
                    @Override
                    public void closed(CommandContext context) {
                        // Do nothing
                    }
                    @Override
                    public void closeFailure(CommandContext context) {
                        // Do nothing
                    }
                    @Override
                    public void afterSessionsFlush(CommandContext context) {
                        // Do nothing
                    }
                });
                return null;
            }
        });

        long count = runtimeService.createProcessInstanceQuery().processDefinitionKey("broadcastSignalCatchEventProcess").count();
        assertThat(count).isEqualTo(1);

        await("Broadcast Signals").untilAsserted(() -> {
            List<org.activiti.engine.runtime.ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().processDefinitionKey("broadcastSignalCatchEventProcess").list();
            assertThat(processInstances).isEmpty();
        });

        //then
        List<org.activiti.engine.runtime.ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().processDefinitionKey("broadcastSignalCatchEventProcess").list();
        assertThat(processInstances).isEmpty();

        ReflectionTestUtils.setField(signalSender, "serviceName", serviceName);
    }

    @Test
    public void shouldBroadcastSignalsWithSameRuntimeBundle() throws InterruptedException {
        // given
        CountDownLatch signalLatch = new CountDownLatch(1);
        ActivitiEventListener signalEventListener = new ActivitiEventListener() {
            
            @Override
            public void onEvent(ActivitiEvent event) {
                signalLatch.countDown();
            }
            
            @Override
            public boolean isFailOnException() {
                return false;
            }
        };
                
        runtimeService.addEventListener(signalEventListener, ActivitiEventType.ACTIVITY_SIGNALED);
        
        //when
        runtimeService.startProcessInstanceByKey("broadcastSignalCatchEventProcess");
        ((ProcessEngineConfigurationImpl) processEngineConfiguration).getCommandExecutor().execute(new Command<Void>() {
            public Void execute(CommandContext commandContext) {
                runtimeService.startProcessInstanceByKey("broadcastSignalEventProcess");
                commandContext.addCloseListener(new CommandContextCloseListener() {
                    @Override
                    public void closing(CommandContext context) {
                        runtimeService.startProcessInstanceByKey("broadcastSignalCatchEventProcess");
                    }
                    @Override
                    public void closed(CommandContext context) {
                        // Do nothing
                    }
                    @Override
                    public void closeFailure(CommandContext context) {
                        // Do nothing
                    }
                    @Override
                    public void afterSessionsFlush(CommandContext context) {
                        // Do nothing
                    }
                });
                return null;
            }
        });

        
        // then signal has been received
        assertThat(signalLatch.await(2, TimeUnit.SECONDS)).isTrue();

        // then
        assertThat(runtimeService.createProcessInstanceQuery()
                                 .processDefinitionKey("broadcastSignalCatchEventProcess")
                                 .count())
                                 .isEqualTo(1);
    }

    @Test
    public void shouldNotBroadcastSignalsWithProcessInstanceScope() throws InterruptedException {
        //when
        ReflectionTestUtils.setField(signalSender, "serviceName", "dummyService");
        ((ProcessEngineConfigurationImpl) processEngineConfiguration).getCommandExecutor().execute(new Command<Void>() {
            public Void execute(CommandContext commandContext) {
                runtimeService.startProcessInstanceByKey("signalThrowEventWithProcessInstanceScopeProcess");
                commandContext.addCloseListener(new CommandContextCloseListener() {
                    @Override
                    public void closing(CommandContext context) {
                        runtimeService.startProcessInstanceByKey("broadcastSignalCatchEventProcess");
                    }
                    @Override
                    public void closed(CommandContext context) {
                        // Do nothing
                    }
                    @Override
                    public void closeFailure(CommandContext context) {
                        // Do nothing
                    }
                    @Override
                    public void afterSessionsFlush(CommandContext context) {
                        // Do nothing
                    }
                });
                return null;
            }
        });

        long count = runtimeService.createProcessInstanceQuery().processDefinitionKey("signalThrowEventWithProcessInstanceScopeProcess").count();
        assertThat(count).isEqualTo(0);

        count = runtimeService.createProcessInstanceQuery().processDefinitionKey("broadcastSignalCatchEventProcess").count();
        assertThat(count).isEqualTo(1);

        Thread.sleep(1000);

        //then
        count = runtimeService.createProcessInstanceQuery().processDefinitionKey("signalThrowEventWithProcessInstanceScopeProcess").count();
        assertThat(count).isEqualTo(0);

        count = runtimeService.createProcessInstanceQuery().processDefinitionKey("broadcastSignalCatchEventProcess").count();
        assertThat(count).isEqualTo(1);

        // cleanUp
        ReflectionTestUtils.setField(signalSender, "serviceName", serviceName);
    }

    @Test
    public void shouldBroadcastSignalsWithProcessInstanceRest() {
        //when
        ReflectionTestUtils.setField(signalSender, "serviceName", "dummyService");
        testEventListener.setActive(true);
        SignalPayload signalProcessInstancesCmd = ProcessPayloadBuilder.signal().withName("Test").build();
        ResponseEntity<Void> responseEntity = restTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + "/signal",
                                                                    HttpMethod.POST,
                                                                    new HttpEntity<>(signalProcessInstancesCmd),
                                                                    new ParameterizedTypeReference<Void>() {
                                                                    });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        long count = runtimeService.createProcessInstanceQuery().processDefinitionKey("broadcastSignalCatchEventProcess").count();
        assertThat(count).isEqualTo(1);

        await("Broadcast Signals").untilAsserted(() -> {
            List<org.activiti.engine.runtime.ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().processDefinitionKey("broadcastSignalCatchEventProcess").list();
            assertThat(processInstances).isEmpty();
        });

        //then
        List<org.activiti.engine.runtime.ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().processDefinitionKey("broadcastSignalCatchEventProcess").list();
        assertThat(processInstances).isEmpty();

        // cleanUp
        testEventListener.setActive(false);
        ReflectionTestUtils.setField(signalSender, "serviceName", serviceName);
    }

    @Test
    public void shouldBroadcastSignalsWithProcessInstanceRestAndSameRuntimeBundle() throws InterruptedException {
        // given
        CountDownLatch signalLatch = new CountDownLatch(1);
        ActivitiEventListener signalEventListener = new ActivitiEventListener() {
            
            @Override
            public void onEvent(ActivitiEvent event) {
                signalLatch.countDown();
            }
            
            @Override
            public boolean isFailOnException() {
                return false;
            }
        };
        
        runtimeService.addEventListener(signalEventListener, ActivitiEventType.ACTIVITY_SIGNALED);
        
        //when
        runtimeService.startProcessInstanceByKey("broadcastSignalCatchEventProcess");
        SignalPayload signalProcessInstancesCmd = ProcessPayloadBuilder.signal().withName("Test").build();
        ResponseEntity<Void> responseEntity = restTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + "/signal",
                                                                    HttpMethod.POST,
                                                                    new HttpEntity<>(signalProcessInstancesCmd),
                                                                    new ParameterizedTypeReference<Void>() {
                                                                    });
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        assertThat(signalLatch.await(2, TimeUnit.SECONDS)).isTrue();

        // then
        await("Broadcast Signals").untilAsserted(() -> {
            assertThat(runtimeService.createProcessInstanceQuery()
                                     .processDefinitionKey("broadcastSignalCatchEventProcess")
                                     .count())
                                     .isEqualTo(0);
        });
        
    }

    @Test
    public void shouldBroadcastSignalsWithVariables() {
        //when
        ReflectionTestUtils.setField(signalSender, "serviceName", "dummyService");
        org.activiti.engine.runtime.ProcessInstance procInst1 = runtimeService.startProcessInstanceByKey("broadcastSignalCatchEventProcess2");
        org.activiti.engine.runtime.ProcessInstance procInst2 = ((ProcessEngineConfigurationImpl)processEngineConfiguration).getCommandExecutor().execute(new Command<org.activiti.engine.runtime.ProcessInstance>() {
            public org.activiti.engine.runtime.ProcessInstance execute(CommandContext commandContext) {
                runtimeService.startProcessInstanceByKey("broadcastSignalEventProcess", Collections.singletonMap("myVar", "myContent"));
                return runtimeService.startProcessInstanceByKey("broadcastSignalCatchEventProcess2");
            }
        });
        assertThat(procInst1).isNotNull();
        assertThat(procInst2).isNotNull();

        await("Broadcast Signals").untilAsserted(() -> {
            org.activiti.engine.task.Task task = taskService.createTaskQuery().processInstanceId(procInst1.getId()).singleResult();
            assertThat(task.getTaskDefinitionKey()).isEqualTo("usertask1");
            task = taskService.createTaskQuery().processInstanceId(procInst2.getId()).singleResult();
            assertThat(task.getTaskDefinitionKey()).isEqualTo("usertask1");
        });

        //then
        assertThat(runtimeService.getVariables(procInst1.getId()).get("myVar")).isEqualTo("myContent");
        assertThat(runtimeService.getVariables(procInst2.getId()).get("myVar")).isEqualTo("myContent");

        ReflectionTestUtils.setField(signalSender, "serviceName", serviceName);
    }

    @Test
    public void shouldBroadcastDifferentSignals() {
        //when
        org.activiti.engine.runtime.ProcessInstance procInst1 = runtimeService.startProcessInstanceByKey("broadcastSignalCatchEventProcess");
        org.activiti.engine.runtime.ProcessInstance procInst2 = runtimeService.startProcessInstanceByKey("broadcastSignalEventProcess");
        assertThat(procInst1).isNotNull();
        assertThat(procInst2).isNotNull();

        await("Broadcast Signals").untilAsserted(() -> {
            List<org.activiti.engine.runtime.ProcessInstance> processInstances = runtimeService.createProcessInstanceQuery().processInstanceId(procInst1.getId()).list();
            assertThat(processInstances).isEmpty();
        });

        //then
        List<org.activiti.engine.runtime.ProcessInstance> processInstances1 = runtimeService.createProcessInstanceQuery().processInstanceId(procInst1.getId()).list();
        assertThat(processInstances1).isEmpty();

        org.activiti.engine.runtime.ProcessInstance procInst3 = runtimeService.startProcessInstanceByKey(SIGNAL_PROCESS);
        org.activiti.engine.runtime.ProcessInstance procInst4 = runtimeService.startProcessInstanceByKey("signalThrowEventProcess");
        assertThat(procInst3).isNotNull();
        assertThat(procInst4).isNotNull();

        await("Broadcast Signals").untilAsserted(() -> {
            String taskName = taskService.createTaskQuery().processInstanceId(procInst3.getId()).singleResult().getName();
            assertThat(taskName).isEqualTo("Boundary target");
        });

        //then
        String taskName = taskService.createTaskQuery().processInstanceId(procInst3.getId()).singleResult().getName();
        assertThat(taskName).isEqualTo("Boundary target");
    }

    @Test
    public void processShouldTakeExceptionPathWhenSignalIsSent() {
        //given
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIGNAL_PROCESS));
        SignalPayload signalProcessInstancesCmd = ProcessPayloadBuilder.signal().withName("go").build();

        //when
        ResponseEntity<Void> responseEntity = restTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + "/signal",
                                                                    HttpMethod.POST,
                                                                    new HttpEntity<>(signalProcessInstancesCmd),
                                                                    new ParameterizedTypeReference<Void>() {
                                                                    });

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        ResponseEntity<PagedResources<CloudTask>> taskEntity = processInstanceRestTemplate.getTasks(startProcessEntity);
        assertThat(taskEntity.getBody().getContent()).extracting(Task::getName).containsExactly("Boundary target");
    }

    @Test
    public void processShouldHaveVariablesSetWhenSignalCarriesVariables() {
        //given
        ResponseEntity<CloudProcessInstance> startProcessEntity = processInstanceRestTemplate.startProcess(processDefinitionIds.get(SIGNAL_PROCESS));
        SignalPayload signalProcessInstancesCmd = ProcessPayloadBuilder.signal().withName("go").withVariables(
                Collections.singletonMap("myVar",
                                         "myContent")).build();

        //when
        ResponseEntity<Void> responseEntity = restTemplate.exchange(PROCESS_INSTANCES_RELATIVE_URL + "/signal",
                                                                    HttpMethod.POST,
                                                                    new HttpEntity<>(signalProcessInstancesCmd),
                                                                    new ParameterizedTypeReference<Void>() {
                                                                    });

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<PagedResources<CloudTask>> taskEntity = processInstanceRestTemplate.getTasks(startProcessEntity);
        assertThat(taskEntity.getBody().getContent()).extracting(Task::getName).containsExactly("Boundary target");

        await().untilAsserted(() -> {
            ResponseEntity<Resources<CloudVariableInstance>> variablesEntity = processInstanceRestTemplate.getVariables(startProcessEntity);
            Collection<CloudVariableInstance> variableCollection = variablesEntity.getBody().getContent();
            VariableInstance variable = variableCollection.iterator().next();
            assertThat(variable.getName()).isEqualToIgnoringCase("myVar");
            assertThat(variable.<Object>getValue()).isEqualTo("myContent");
        });
    }

    private ResponseEntity<PagedResources<CloudProcessDefinition>> getProcessDefinitions() {
        ParameterizedTypeReference<PagedResources<CloudProcessDefinition>> responseType = new ParameterizedTypeReference<PagedResources<CloudProcessDefinition>>() {
        };
        return restTemplate.exchange(ProcessDefinitionIT.PROCESS_DEFINITIONS_URL,
                                     HttpMethod.GET,
                                     null,
                                     responseType);
    }
}