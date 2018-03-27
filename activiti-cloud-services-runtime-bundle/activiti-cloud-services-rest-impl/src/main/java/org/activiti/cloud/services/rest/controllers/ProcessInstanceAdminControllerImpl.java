/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.activiti.cloud.services.rest.controllers;

import org.activiti.bpmn.model.BpmnModel;
import org.activiti.cloud.alfresco.data.domain.AlfrescoPagedResourcesAssembler;
import org.activiti.cloud.services.api.commands.ActivateProcessInstanceCmd;
import org.activiti.cloud.services.api.commands.SignalProcessInstancesCmd;
import org.activiti.cloud.services.api.commands.StartProcessInstanceCmd;
import org.activiti.cloud.services.api.commands.SuspendProcessInstanceCmd;
import org.activiti.cloud.services.api.model.ProcessInstance;
import org.activiti.cloud.services.core.ActivitiForbiddenException;
import org.activiti.cloud.services.core.ProcessEngineWrapper;
import org.activiti.cloud.services.core.SecurityPoliciesApplicationService;
import org.activiti.cloud.services.rest.api.ProcessInstanceAdminController;
import org.activiti.cloud.services.rest.api.ProcessInstanceController;
import org.activiti.cloud.services.rest.api.resources.ProcessInstanceResource;
import org.activiti.cloud.services.rest.assemblers.ProcessInstanceResourceAssembler;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.ActivitiObjectNotFoundException;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.impl.util.IoUtil;
import org.activiti.image.ProcessDiagramGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@RestController
public class ProcessInstanceAdminControllerImpl implements ProcessInstanceAdminController {

    private ProcessEngineWrapper processEngine;

    private final ProcessInstanceResourceAssembler resourceAssembler;

    private final AlfrescoPagedResourcesAssembler<ProcessInstance> pagedResourcesAssembler;

    @ExceptionHandler(ActivitiForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAppException(ActivitiForbiddenException ex) {
        return ex.getMessage();
    }


    @ExceptionHandler(ActivitiObjectNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleAppException(ActivitiObjectNotFoundException ex) {
        return ex.getMessage();
    }

    @Autowired
    public ProcessInstanceAdminControllerImpl(ProcessEngineWrapper processEngine,
                                              ProcessInstanceResourceAssembler resourceAssembler,
                                              AlfrescoPagedResourcesAssembler<ProcessInstance> pagedResourcesAssembler) {
        this.processEngine = processEngine;
        this.resourceAssembler = resourceAssembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @Override
    public PagedResources<ProcessInstanceResource> getAllProcessInstances(Pageable pageable) {
        return pagedResourcesAssembler.toResource(pageable, processEngine.getAllProcessInstances(pageable),
                                                  resourceAssembler);
    }

}
