package org.activiti.cloud.services.core.pageable;

import org.activiti.cloud.services.SecurityPolicy;
import org.activiti.cloud.services.api.model.converter.ProcessDefinitionConverter;
import org.activiti.cloud.services.core.SecurityPolicyApplicationService;
import org.activiti.cloud.services.core.pageable.sort.ProcessDefinitionSortApplier;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Pageable;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class PageableRepositoryServiceTest {

    @InjectMocks
    private PageableRepositoryService pageableRepositoryService;

    @Mock
    private RepositoryService repositoryService;
    @Mock
    private PageRetriever pageRetriever;
    @Mock
    private ProcessDefinitionConverter processDefinitionConverter;
    @Mock
    private ProcessDefinitionSortApplier sortApplier;
    @Mock
    private SecurityPolicyApplicationService securityService;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    @Test
    public void shouldApplySecurity(){
        Pageable pageable = mock(Pageable.class);
        ProcessDefinitionQuery query = mock(ProcessDefinitionQuery.class);
        when(repositoryService.createProcessDefinitionQuery()).thenReturn(query);
        when(securityService.processDefQuery(query, SecurityPolicy.READ)).thenReturn(query);

        pageableRepositoryService.getProcessDefinitions(pageable);

        verify(securityService).processDefQuery(query,SecurityPolicy.READ);
    }
}
