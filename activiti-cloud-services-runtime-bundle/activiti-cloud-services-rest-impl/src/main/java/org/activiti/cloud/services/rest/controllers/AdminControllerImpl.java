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

import java.util.Map;

import org.activiti.cloud.services.api.model.Task;
import org.activiti.cloud.services.rest.api.resources.TaskResource;
import org.activiti.cloud.services.rest.assemblers.TaskResourceAssembler;
import org.activiti.cloud.services.core.ProcessEngineWrapper;
import org.activiti.cloud.services.api.commands.ClaimTaskCmd;
import org.activiti.cloud.services.api.commands.CompleteTaskCmd;
import org.activiti.cloud.services.api.commands.ReleaseTaskCmd;
import org.activiti.cloud.services.core.AuthenticationWrapper;
import org.activiti.cloud.services.rest.api.AdminController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminControllerImpl implements AdminController {

    private ProcessEngineWrapper processEngine;

    private final TaskResourceAssembler taskResourceAssembler;

    private AuthenticationWrapper authenticationWrapper;

    @Autowired
    public AdminControllerImpl(ProcessEngineWrapper processEngine,
                               TaskResourceAssembler taskResourceAssembler,
                               AuthenticationWrapper authenticationWrapper) {
        this.authenticationWrapper = authenticationWrapper;
        this.processEngine = processEngine;
        this.taskResourceAssembler = taskResourceAssembler;
    }

    @Override
    public PagedResources<TaskResource> getAllTasks(Pageable pageable,
                                                 PagedResourcesAssembler<Task> pagedResourcesAssembler) {
        Page<Task> page = processEngine.getAllTasks(pageable);
        return pagedResourcesAssembler.toResource(page,
                                                  taskResourceAssembler);
    }

    public AuthenticationWrapper getAuthenticationWrapper() {
        return authenticationWrapper;
    }
    
    public void setAuthenticationWrapper(AuthenticationWrapper authenticationWrapper) {
        this.authenticationWrapper = authenticationWrapper;
    }
}
