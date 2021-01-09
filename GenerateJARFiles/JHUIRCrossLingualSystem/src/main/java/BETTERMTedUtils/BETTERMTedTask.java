package BETTERMTedUtils;

import java.util.List;


public class BETTERMTedTask {
    public String task_num;
    // public String task_title;
    // public String task_stmt;
    // public String task_link;
    // public String task_narr;
    // public String task_in_scope;
    // public String task_not_in_scope;

    // public ArrayList<String> task_docs;

    // VARIABLES FOR REQUEST STREAM
    public List<BETTERMTedRequest> requests;

    @Override
    public String toString() {
        return task_num; // + "\t" + task_title;
    }
}