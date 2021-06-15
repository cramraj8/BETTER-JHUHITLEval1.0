package Utils;

import org.json.simple.JSONObject;

import java.util.ArrayList;

public class BETTERTuningRequest {

    public String req_num;
    public String req_text;
    public String req_qe_terms;
    public String expanded_text;
    public JSONObject req_docs;

    @Override
    public String toString() {
        return req_num + "\t" + req_text;
    }
}