package Utils;

import java.util.ArrayList;
import java.util.List;

public class BETTERTuningTask {
    public String task_num;
    public String task_title;
    public String task_stmt;
    public String task_narr;
    public String task_qe_terms;
    public ArrayList<String> task_docs;

    public List<BETTERTuningRequest> requests;

    @Override
    public String toString() {
        return task_num; // + "\t" + task_title;
    }
}