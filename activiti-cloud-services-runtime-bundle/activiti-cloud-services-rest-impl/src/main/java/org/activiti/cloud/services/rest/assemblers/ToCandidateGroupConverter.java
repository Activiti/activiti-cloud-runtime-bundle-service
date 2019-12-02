package org.activiti.cloud.services.rest.assemblers;

import java.util.ArrayList;
import java.util.List;

import org.activiti.cloud.api.process.model.impl.CandidateGroup;;

public class ToCandidateGroupConverter {

    public List<CandidateGroup> from (List<String> users){
        List<CandidateGroup> list = new ArrayList();
        users.forEach(u -> list.add(new CandidateGroup(u)));
        return list;
    }

}
