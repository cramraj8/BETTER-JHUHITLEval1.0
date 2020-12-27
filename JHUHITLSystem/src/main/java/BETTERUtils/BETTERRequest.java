package BETTERUtils;

import org.apache.lucene.search.Query;
import java.util.List;

public class BETTERRequest {

    public String req_num;
    public String req_text;
    public List<String> req_docs;
    public List<String> req_extr;

    public Query requestLuceneQuery;

    @Override
    public String toString() {
        return req_num + "\t" + req_text;
    }
}