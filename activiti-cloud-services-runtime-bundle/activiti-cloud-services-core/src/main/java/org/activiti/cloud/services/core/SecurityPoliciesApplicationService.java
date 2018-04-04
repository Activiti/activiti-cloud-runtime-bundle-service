package org.activiti.cloud.services.core;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.activiti.cloud.services.security.SecurityPoliciesService;
import org.activiti.cloud.services.security.SecurityPolicy;
import org.activiti.engine.UserGroupLookupProxy;
import org.activiti.engine.UserRoleLookupProxy;
import org.activiti.engine.query.Query;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.ProcessInstanceQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SecurityPoliciesApplicationService {


    @Autowired(required = false)
    private UserGroupLookupProxy userGroupLookupProxy;

    @Autowired(required = false)
    private UserRoleLookupProxy userRoleLookupProxy;

    @Autowired
    private AuthenticationWrapper authenticationWrapper;

    @Autowired
    private SecurityPoliciesService securityPoliciesService;

    @Autowired
    private SecurityPoliciesProcessDefinitionRestrictionApplier processDefinitionRestrictionApplier;

    @Autowired
    private SecurityPoliciesProcessInstanceRestrictionApplier processInstanceRestrictionApplier;

    public ProcessDefinitionQuery restrictProcessDefQuery(ProcessDefinitionQuery query, SecurityPolicy securityPolicy){

        return restrictQuery(query, processDefinitionRestrictionApplier, securityPolicy);
    }

    private boolean noSecurityPoliciesOrNoUser() {
        return !securityPoliciesService.policiesDefined() || authenticationWrapper.getAuthenticatedUserId()== null;
    }

    private Set<String> definitionKeysAllowedForRBPolicy(SecurityPolicy securityPolicy) {
        //this is an RB restriction and for RB we don't care about appName, just aggregate all the keys
        Map<String,Set<String>> restrictions = definitionKeysAllowedForPolicy(securityPolicy);
        Set<String> keys = new HashSet<>();

        for(String appName:restrictions.keySet()) {
            keys.addAll(restrictions.get(appName));
        }
        return keys;
    }

    private Map<String, Set<String>> definitionKeysAllowedForPolicy(SecurityPolicy securityPolicy) {
        List<String> groups = null;

        if(userGroupLookupProxy!=null && authenticationWrapper.getAuthenticatedUserId()!=null){
            groups = userGroupLookupProxy.getGroupsForCandidateUser(authenticationWrapper.getAuthenticatedUserId());
        }

        return securityPoliciesService.getProcessDefinitionKeys(authenticationWrapper.getAuthenticatedUserId(),
                groups, securityPolicy);
    }

    public ProcessInstanceQuery restrictProcessInstQuery(ProcessInstanceQuery query, SecurityPolicy securityPolicy){
        return restrictQuery(query, processInstanceRestrictionApplier, securityPolicy);
    }

    private  <T extends Query<?,?>> T restrictQuery(T query, SecurityPoliciesRestrictionApplier<T> restrictionApplier, SecurityPolicy securityPolicy){
        if (noSecurityPoliciesOrNoUser()){
            return query;
        }

        Set<String> keys = definitionKeysAllowedForRBPolicy(securityPolicy);

        if(keys != null && !keys.isEmpty()){

            if(keys.contains(securityPoliciesService.getWildcard())){
                return query;
            }

            return restrictionApplier.restrictToKeys(query, keys);
        }

        if((keys != null || !keys.isEmpty()) && securityPoliciesService.policiesDefined()){
            restrictionApplier.denyAll(query);
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

        if (!securityPoliciesService.policiesDefined() || userGroupLookupProxy == null || authenticationWrapper.getAuthenticatedUserId() == null){
            return true;
        }

        if(userRoleLookupProxy != null && userRoleLookupProxy.isAdmin(authenticationWrapper.getAuthenticatedUserId())){
            return true;
        }

        Set<String> keys = definitionKeysAllowedForRBPolicy(securityPolicy);

        return (keys != null && (keys.contains(processDefId) || keys.contains(securityPoliciesService.getWildcard()) ));
    }

}
