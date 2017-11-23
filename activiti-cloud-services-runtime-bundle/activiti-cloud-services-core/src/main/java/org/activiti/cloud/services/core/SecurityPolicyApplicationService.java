package org.activiti.cloud.services.core;

import org.activiti.cloud.services.SecurityPolicy;
import org.activiti.cloud.services.SecurityPolicyService;
import org.activiti.engine.UserGroupLookupProxy;
import org.activiti.engine.UserRoleLookupProxy;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class SecurityPolicyApplicationService {

    @Autowired(required = false)
    private UserGroupLookupProxy userGroupLookupProxy;

    @Autowired(required = false)
    private UserRoleLookupProxy userRoleLookupProxy;

    private AuthenticationWrapper authenticationWrapper = new AuthenticationWrapper();

    @Autowired
    private SecurityPolicyService securityPolicyService;


    public ProcessDefinitionQuery processDefQuery(ProcessDefinitionQuery query, SecurityPolicy securityPolicy){

        if (!securityPolicyService.policiesDefined()){
            return query;
        }

        Set<String> keys = definitionKeysAllowedForPolicy(securityPolicy);

        if(keys != null){ //restrict query to only these keys
            query = query.processDefinitionKeys(keys);
        }
        return query;
    }

    private Set<String> definitionKeysAllowedForPolicy(SecurityPolicy securityPolicy) {
        List<String> groups = null;
        if(userGroupLookupProxy!=null){
            groups = userGroupLookupProxy.getGroupsForCandidateUser(authenticationWrapper.getAuthenticatedUserId());
        }

        return securityPolicyService.getProcessDefinitionKeys(authenticationWrapper.getAuthenticatedUserId(),
                groups, securityPolicy);
    }

    public ProcessInstanceQuery processInstQuery(ProcessInstanceQuery query, SecurityPolicy securityPolicy){
        if (!securityPolicyService.policiesDefined()){
            return query;
        }

        Set<String> keys = definitionKeysAllowedForPolicy(securityPolicy);

        if(keys != null){
            query = query.processDefinitionKeys(keys);
        }
        return query;
    }

    public boolean canWrite(String processDefId){
        return hasPermission(processDefId, SecurityPolicy.WRITE);
    }

    public boolean canRead(String processDefId){
        return hasPermission(processDefId, SecurityPolicy.READ);
    }

    private boolean hasPermission(String processDefId, SecurityPolicy securityPolicy){
        if(userRoleLookupProxy != null && userRoleLookupProxy.isAdmin(authenticationWrapper.getAuthenticatedUserId())){
            return true;
        }

        if (!securityPolicyService.policiesDefined() || userGroupLookupProxy ==null){
            return true;
        }

        Set<String> keys = definitionKeysAllowedForPolicy(securityPolicy);

        if (keys != null && keys.contains(processDefId)){
            return true;
        }
        return false;
    }

}
