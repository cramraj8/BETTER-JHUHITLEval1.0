package Utils;

import java.util.List;

public class BETTERRequest {

    public String req_num;
    public String req_text;
    public List<String> req_docs;
    public List<String> req_extr;

    public String expanded_text;

    @Override
    public String toString() {
        return req_num + "\t" + req_text;
    }
}
