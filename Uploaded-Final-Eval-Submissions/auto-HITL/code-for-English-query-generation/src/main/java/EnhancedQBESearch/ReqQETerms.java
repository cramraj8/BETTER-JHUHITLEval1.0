package EnhancedQBESearch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class ReqQETerms {
    Set<String> reqQETermsSet;

    public ReqQETerms(){
        this.reqQETermsSet = new HashSet<String>();
    }
    public ReqQETerms(ArrayList<String> QETerms){
        assert this.reqQETermsSet != null;
        this.reqQETermsSet.addAll(QETerms);
    }
}
