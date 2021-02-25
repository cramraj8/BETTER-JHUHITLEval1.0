package EnhancedQBESearch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class TaskQETerms {
    Set<String> taskQETermsSet;
    HashMap<String, ReqQETerms> reqQEMap;

    public TaskQETerms(){
        this.taskQETermsSet = new HashSet<String>();
        this.reqQEMap = new HashMap<>();
    }
    public TaskQETerms(ArrayList<String> QETerms){
        assert this.taskQETermsSet != null;
        this.taskQETermsSet.addAll(QETerms);
    }
    public void addReqAndQETerms(String reqID, ReqQETerms reqQETerms) {
        reqQEMap.put(reqID, reqQETerms);
    }

}
