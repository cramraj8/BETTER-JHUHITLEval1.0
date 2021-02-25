package Utils;

import org.apache.lucene.search.Query;
import java.util.ArrayList;
import java.util.List;


public class BETTERTask {
    public String task_num;
    public String task_title;
    public String task_stmt;
    public String task_link;
    public String task_narr;
    public String task_in_scope;
    public String task_not_in_scope;
    public ArrayList<String> task_docs;

    public List<BETTERRequest> requests;

    @Override
    public String toString() {
        return task_num + "\t" + task_title;
    }
}