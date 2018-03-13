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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.bpmn.model.Process;
import org.activiti.cloud.services.api.model.ProcessDefinition;
import org.activiti.cloud.services.api.model.converter.ProcessDefinitionConverter;
import org.activiti.cloud.services.core.SecurityPoliciesApplicationService;
import org.activiti.cloud.services.core.pageable.PageableRepositoryService;
import org.activiti.cloud.services.rest.api.ProcessDefinitionMetaController;
import org.activiti.cloud.services.security.SecurityPolicy;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntityImpl;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.image.ProcessDiagramGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.halLinks;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(value = ProcessDefinitionControllerImpl.class)
@EnableSpringDataWebSupport
@AutoConfigureMockMvc
@AutoConfigureRestDocs(outputDir = "target/snippets")
@ComponentScan(basePackages = {"org.activiti.cloud.services.rest.assemblers", "org.activiti.cloud.alfresco"})
public class ProcessDefinitionControllerImplIT {

    private static final String DOCUMENTATION_IDENTIFIER = "process-definition";

    private static final String DOCUMENTATION_IDENTIFIER_ALFRESCO = "process-definition-alfresco";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SecurityPoliciesApplicationService securityPoliciesApplicationService;

    @MockBean
    private RepositoryService repositoryService;

    @MockBean
    private ProcessDiagramGenerator processDiagramGenerator;

    @MockBean
    private ProcessDefinitionConverter processDefinitionConverter;

    @MockBean
    private PageableRepositoryService pageableRepositoryService;

    @MockBean
    private ProcessDefinitionMetaController processDefinitionMetaController;

    @Test
    public void getProcessDefinitions() throws Exception {

        List<ProcessDefinition> processDefinitionList = Collections.singletonList(new ProcessDefinition("procId",
                                                                                                        "my process",
                                                                                                        "this is my process",
                                                                                                        1));
        Page<ProcessDefinition> processDefinitionPage = new PageImpl<>(processDefinitionList,
                                                                       PageRequest.of(0,
                                                                                      10),
                                                                       processDefinitionList.size());
        when(pageableRepositoryService.getProcessDefinitions(any())).thenReturn(processDefinitionPage);

        this.mockMvc.perform(get("/v1/process-definitions").accept(MediaTypes.HAL_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/list",
                                responseFields(subsectionWithPath("page").description("Pagination details."),
                                               subsectionWithPath("_links").description("The hypermedia links."),
                                               subsectionWithPath("_embedded").description("The process definitions.")),
                                links(halLinks(),
                                      linkWithRel("self").description("The current page."))
                ));
    }

    @Test
    public void getProcessDefinitionsShouldUseAlfrescoGuidelineWhenMediaTypeIsApplicationJson() throws Exception {
        //given
        String processDefId = UUID.randomUUID().toString();
        ProcessDefinition processDefinition = new ProcessDefinition(processDefId,
                                                                    "my process",
                                                                    "This is my process",
                                                                    1);
        List<ProcessDefinition> processDefinitionList = Collections.singletonList(processDefinition);
        PageRequest pageable = PageRequest.of(1,
                                              10);
        Page<ProcessDefinition> processDefinitionPage = new PageImpl<>(processDefinitionList,
                                                                       pageable,
                                                                       11);
        given(pageableRepositoryService.getProcessDefinitions(any())).willReturn(processDefinitionPage);

        //when
        MvcResult result = this.mockMvc.perform(get("/v1/process-definitions?skipCount=10&maxItems=10").accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER_ALFRESCO + "/list",
                                requestParameters(
                                        parameterWithName("skipCount")
                                                .description("How many entities exist in the entire addressed collection before those included in this list."),
                                        parameterWithName("maxItems")
                                                .description("The max number of entities that can be included in the result.")
                                ),
                                responseFields(
                                        subsectionWithPath("list").ignored(),
                                        subsectionWithPath("list.entries").description("List of results."),
                                        subsectionWithPath("list.entries[].entry").description("Wrapper for each entry in the list of results."),
                                        subsectionWithPath("list.pagination").description("Pagination metadata."),
                                        subsectionWithPath("list.pagination.skipCount")
                                                .description("How many entities exist in the entire addressed collection before those included in this list."),
                                        subsectionWithPath("list.pagination.maxItems")
                                                .description("The maxItems parameter used to generate this list."),
                                        subsectionWithPath("list.pagination.count")
                                                .description("The number of entities included in this list. This number must correspond to the number of objects in the \"entries\" array."),
                                        subsectionWithPath("list.pagination.hasMoreItems")
                                                .description("A boolean value that indicates whether there are further entities in the addressed collection beyond those returned " +
                                                                     "in this response. If true then a request with a larger value for either the skipCount or the maxItems " +
                                                                     "parameter is expected to return further results."),
                                        subsectionWithPath("list.pagination.totalItems")
                                                .description("An integer value that indicates the total number of entities in the addressed collection.")
                                )))
                .andReturn();

        //then
        String responseContent = result.getResponse().getContentAsString();
        assertThatJson(responseContent)
                .node("list.pagination.skipCount").isEqualTo(10)
                .node("list.pagination.maxItems").isEqualTo(10)
                .node("list.pagination.count").isEqualTo(1)
                .node("list.pagination.hasMoreItems").isEqualTo(false)
                .node("list.pagination.totalItems").isEqualTo(11);
        assertThatJson(responseContent)
                .node("list.entries[0].entry.id").isEqualTo(processDefId)
                .node("list.entries[0].entry.name").isEqualTo("my process")
                .node("list.entries[0].entry.description").isEqualTo("This is my process")
                .node("list.entries[0].entry.version").isEqualTo(1);
    }

    @Test
    public void getProcessDefinition() throws Exception {
        ProcessDefinitionQuery processDefinitionQuery = mock(ProcessDefinitionQuery.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
        when(securityPoliciesApplicationService.restrictProcessDefQuery(processDefinitionQuery,
                                                                        SecurityPolicy.READ)).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.processDefinitionId("1")).thenReturn(processDefinitionQuery);
        ProcessDefinitionEntityImpl processDefinitionEntity = new ProcessDefinitionEntityImpl();
        when(processDefinitionQuery.singleResult()).thenReturn(processDefinitionEntity);
        given(processDefinitionConverter.from(processDefinitionEntity)).willReturn(new ProcessDefinition("procId",
                                                                                                         "my process",
                                                                                                         "this is my process",
                                                                                                         1));

        this.mockMvc.perform(get("/v1/process-definitions/{id}",
                                 1).accept(MediaTypes.HAL_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/get",
                                pathParameters(parameterWithName("id").description("The process definition id"))));
    }

    @Test
    public void getProcessDefinitionShouldUseAlfrescoGuidelineWhenMediaTypeIsApplicationJson() throws Exception {
        String procDefId = UUID.randomUUID().toString();
        ProcessDefinitionQuery processDefinitionQuery = mock(ProcessDefinitionQuery.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
        when(securityPoliciesApplicationService.restrictProcessDefQuery(processDefinitionQuery,
                                                                        SecurityPolicy.READ)).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.processDefinitionId(procDefId)).thenReturn(processDefinitionQuery);
        ProcessDefinitionEntityImpl processDefinitionEntity = new ProcessDefinitionEntityImpl();
        when(processDefinitionQuery.singleResult()).thenReturn(processDefinitionEntity);
        given(processDefinitionConverter.from(processDefinitionEntity)).willReturn(new ProcessDefinition(procDefId,
                                                                                                         "my process",
                                                                                                         "This is my process",
                                                                                                         1));

        MvcResult result = this.mockMvc.perform(get("/v1/process-definitions/{id}",
                                                    procDefId).accept(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER_ALFRESCO + "/get",
                                pathParameters(parameterWithName("id").description("The process definition id.")),
                                responseFields(
                                        subsectionWithPath("entry").ignored(),
                                        subsectionWithPath("entry.id").description("The process definition id."),
                                        subsectionWithPath("entry.name").description("The process definition name."),
                                        subsectionWithPath("entry.description").description("The process definition description."),
                                        subsectionWithPath("entry.version").description("The process definition version.")
                                )
                       )
                ).andReturn();

        assertThatJson(result.getResponse().getContentAsString())
                .node("entry.id").isEqualTo(procDefId)
                .node("entry.name").isEqualTo("my process")
                .node("entry.description").isEqualTo("This is my process")
                .node("entry.version").isEqualTo(1)
        ;
    }

    @Test
    public void getProcessModel() throws Exception {
        InputStream xml = new ByteArrayInputStream("activiti".getBytes());
        when(repositoryService.getProcessModel("1")).thenReturn(xml);
        ProcessDefinitionQuery processDefinitionQuery = mock(ProcessDefinitionQuery.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
        when(securityPoliciesApplicationService.restrictProcessDefQuery(processDefinitionQuery,
                                                                        SecurityPolicy.READ)).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.processDefinitionId("1")).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.singleResult()).thenReturn(new ProcessDefinitionEntityImpl());

        this.mockMvc.perform(
                get("/v1/process-definitions/{id}/model",
                    1).accept(MediaType.APPLICATION_XML))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/model/get",
                                pathParameters(parameterWithName("id").description("The process model id"))));
    }

    @Test
    public void getBpmnModel() throws Exception {
        BpmnModel bpmnModel = new BpmnModel();
        Process process = new Process();
        process.setId("1");
        process.setName("Main Process");
        bpmnModel.getProcesses().add(process);
        when(repositoryService.getBpmnModel("1")).thenReturn(bpmnModel);
        ProcessDefinitionQuery processDefinitionQuery = mock(ProcessDefinitionQuery.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
        when(securityPoliciesApplicationService.restrictProcessDefQuery(processDefinitionQuery,
                                                                        SecurityPolicy.READ)).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.processDefinitionId("1")).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.singleResult()).thenReturn(new ProcessDefinitionEntityImpl());

        this.mockMvc.perform(
                get("/v1/process-definitions/{id}/model",
                    1).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/bpmn-model/get",
                                pathParameters(parameterWithName("id").description("The BPMN model id"))));
    }

    @Test
    public void getProcessDiagram() throws Exception {
        BpmnModel bpmnModel = new BpmnModel();
        when(repositoryService.getBpmnModel("1")).thenReturn(bpmnModel);
        ProcessDefinitionQuery processDefinitionQuery = mock(ProcessDefinitionQuery.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(processDefinitionQuery);
        when(securityPoliciesApplicationService.restrictProcessDefQuery(processDefinitionQuery,
                                                                        SecurityPolicy.READ)).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.processDefinitionId("1")).thenReturn(processDefinitionQuery);
        when(processDefinitionQuery.singleResult()).thenReturn(new ProcessDefinitionEntityImpl());
        InputStream img = new ByteArrayInputStream("img".getBytes());
        when(processDiagramGenerator.generateDiagram(bpmnModel,
                                                     processDiagramGenerator.getDefaultActivityFontName(),
                                                     processDiagramGenerator.getDefaultLabelFontName(),
                                                     processDiagramGenerator.getDefaultAnnotationFontName()))
                .thenReturn(img);

        this.mockMvc.perform(
                get("/v1/process-definitions/{id}/model",
                    1).accept("image/svg+xml"))
                .andExpect(status().isOk())
                .andDo(document(DOCUMENTATION_IDENTIFIER + "/diagram",
                                pathParameters(parameterWithName("id").description("The BPMN model id"))));
    }
}
