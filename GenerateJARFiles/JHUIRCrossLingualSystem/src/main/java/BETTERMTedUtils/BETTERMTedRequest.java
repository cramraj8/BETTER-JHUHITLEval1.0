package BETTERMTedUtils;

public class BETTERMTedRequest {

    public String req_num;
    public String req_text;
    // public List<String> req_docs;
    // public List<String> req_extr;

    @Override
    public String toString() {
        return req_num + "\t" + req_text;
    }
}