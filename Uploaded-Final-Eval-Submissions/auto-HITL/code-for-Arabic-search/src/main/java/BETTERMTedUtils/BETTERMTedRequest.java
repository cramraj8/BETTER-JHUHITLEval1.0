package BETTERMTedUtils;

public class BETTERMTedRequest {

    public String req_num;
    public String req_text;
    public String req_qe_terms;

    @Override
    public String toString() {
        return req_num + "\t" + req_text;
    }
}