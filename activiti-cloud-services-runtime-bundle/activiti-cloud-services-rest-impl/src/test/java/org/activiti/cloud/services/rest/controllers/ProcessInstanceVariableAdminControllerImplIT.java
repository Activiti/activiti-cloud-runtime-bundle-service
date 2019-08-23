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

package org.activiti.cloud.services.rest.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.runtime.ProcessAdminRuntime;
import org.activiti.api.runtime.conf.impl.CommonModelAutoConfiguration;
import org.activiti.api.runtime.conf.impl.ProcessModelAutoConfiguration;
import org.activiti.api.runtime.model.impl.ProcessInstanceImpl;
import org.activiti.cloud.services.events.ProcessEngineChannels;
import org.activiti.cloud.services.events.configuration.CloudEventsAutoConfiguration;
import org.activiti.cloud.services.events.configuration.RuntimeBundleProperties;
import org.activiti.cloud.services.events.listeners.CloudProcessDeployedProducer;
import org.activiti.cloud.services.rest.conf.ServicesRestAutoConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.ActivitiException;
import org.activiti.spring.process.conf.ProcessExtensionsAutoConfiguration;
import org.activiti.spring.process.model.ProcessExtensionModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@RunWith(SpringRunner.class)
@WebMvcTest(ProcessInstanceVariableAdminControllerImpl.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc(secure = false)
@AutoConfigureRestDocs(outputDir = "target/snippets")
@Import({CommonModelAutoConfiguration.class,
        ProcessModelAutoConfiguration.class,
        RuntimeBundleProperties.class,
        CloudEventsAutoConfiguration.class,
        ProcessExtensionsAutoConfiguration.class,
        ServicesRestAutoConfiguration.class})
@ComponentScan(basePackages = {"org.activiti.cloud.services.rest.assemblers", "org.activiti.cloud.alfresco"})
public class ProcessInstanceVariableAdminControllerImplIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessAdminRuntime processAdminRuntime;

    @MockBean
    private Map<String, ProcessExtensionModel> processExtensionModelMap;
    
    @MockBean
    private ProcessVariablesHelper processVariablesHelper;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private ResourcesAssembler resourcesAssembler;

    @MockBean
    private RepositoryService repositoryService;
    
    @MockBean
    private ProcessEngineChannels processEngineChannels;

    @MockBean
    private CloudProcessDeployedProducer processDeployedProducer;

    @Before
    public void setUp() {
        ProcessInstanceImpl processInstance;
        processInstance = new ProcessInstanceImpl();
        processInstance.setId("1");
        processInstance.setProcessDefinitionKey("1");

        given(processAdminRuntime.processInstance(any()))
              .willReturn(processInstance);
    }

    @Test
    public void shouldReturn200WithEmptyErrorListWhenSetVariablesWithCorrectNamesAndTypes() throws Exception {
        //GIVEN
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "Alice");
        variables.put("age", 24);
        variables.put("subscribe", false);
        String expectedResponseBody = "";

        //WHEN
        ResultActions resultActions = mockMvc.perform(put("/admin/v1/process-instances/1/variables",
                1).contentType(MediaType.APPLICATION_JSON)
                .contentType(MediaTypes.HAL_JSON_VALUE)
                .content(
                        mapper.writeValueAsString(ProcessPayloadBuilder.setVariables().withProcessInstanceId("1").
                                withVariables(variables).build())))

                //THEN
                .andExpect(status().isOk());
        MvcResult result = resultActions.andReturn();
        String actualResponseBody = result.getResponse().getContentAsString();

        assertThat(expectedResponseBody).isEqualTo(actualResponseBody);
        verify(processAdminRuntime).setVariables(any());
    }

    @Test
    public void shouldReturn400WithErrorListWhenSetVariablesWithWrongNames() throws Exception {
        //GIVEN
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "Alice");
        variables.put("age", 24);
        variables.put("subs", false);

        String expectedResponseBody = "Variable with name subs does not exists.";
       
        List<ActivitiException> activitiExceptions = new ArrayList<>();
        activitiExceptions.add(new ActivitiException(expectedResponseBody));
        
        given(processVariablesHelper.checkPayloadVariables(any(),any()))
        .willReturn(activitiExceptions);
     
        //WHEN
        ResultActions resultActions = mockMvc.perform(put("/admin/v1/process-instances/1/variables",
                1).contentType(MediaType.APPLICATION_JSON)
                .contentType(MediaTypes.HAL_JSON_VALUE)
                .content(
                        mapper.writeValueAsString(ProcessPayloadBuilder.setVariables().withProcessInstanceId("1").
                                withVariables(variables).build())))

                //THEN
                .andExpect(status().isBadRequest());
        MvcResult result = resultActions.andReturn();
        String actualResponseBody = result.getResponse().getContentAsString();

        assertThat(actualResponseBody).contains(expectedResponseBody);
    }
    
    @Test
    public void deleteVariables() throws Exception {
        this.mockMvc.perform(delete("/admin/v1/process-instances/{processInstanceId}/variables",
                "1")
                .accept(MediaTypes.HAL_JSON_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(ProcessPayloadBuilder.removeVariables().withVariableNames(Arrays.asList("varName1",
                        "varName2"))
                        .build())))
                .andDo(print())
                .andExpect(status().isOk());
        verify(processAdminRuntime).removeVariables(any());
    }
}