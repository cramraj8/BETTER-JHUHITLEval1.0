package BETTERUtils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class BETTERQueryParserEval {
    public StringBuffer            sbuff;
    public Logger logger;
    public String                  queryFilePath;
    public BETTERTask query;
    public List<BETTERTask> queries;
    public BETTERRequest requestStream;
    public Properties prop;
    public HashMap<String, Integer> manualHITLAnnoations;

    public JSONObject taskDocsInfoList;
    public Set<String> docInfoKeySet;
    public Set<String> reqDocInfoKeySet;
    public BETTERQueryParserEval( Properties prop, String queryFilePath, Logger logger ) {
        this.prop = prop;
        this.sbuff = new StringBuffer();
        this.queryFilePath = queryFilePath;
        this.queries = new LinkedList<>();
        this.logger = logger;
    }
    private void parseDocumentObject(JSONObject document) {
        query = new BETTERTask();

        String task_num = (String) document.getOrDefault("task-num", "");
        if (task_num.equals("")) {
            System.out.println("Provided query file has a task with no task-num field !");
            this.logger.warning("BETTERQueryParserEval - parseDocumentObject : Provided query file has a task with no task-num field !");
            return;
        }
        query.task_num = task_num;

        String task_title = (String) document.getOrDefault("task-title", " ");
        query.task_title = task_title;
        // System.out.println(task_title);

        String task_stmt = (String) document.getOrDefault("task-stmt", " ");
        query.task_stmt = task_stmt;
        // System.out.println(task_stmt);

        String task_link = (String) document.getOrDefault("task-link", " ");
        query.task_link = task_link;
        // System.out.println(task_link);

        String task_narr = (String) document.getOrDefault("task-narr", " ");
        query.task_narr = task_narr;
        // System.out.println(task_narr);

        String task_in_scope = (String) document.getOrDefault("task-in-scope", " ");
        query.task_in_scope = task_in_scope;
        // System.out.println(task_in_scope);

        String task_not_in_scope = (String) document.getOrDefault("task-not-in-scope", " ");
        query.task_not_in_scope = task_not_in_scope;
        // System.out.println(task_not_in_scope);

        taskDocsInfoList = (JSONObject) document.getOrDefault("task-docs", null);
        if (taskDocsInfoList == null) {
            System.out.println("Provided query file has a task with no task-docs field : so cannot apply Query By Example !");
            this.logger.warning("BETTERQueryParserEval - parseDocumentObject : Provided query file has a task with no task-docs field : so cannot apply Query By Example !");
            return;
        }
        docInfoKeySet =  taskDocsInfoList.keySet();
        if (docInfoKeySet.size() == 0) {
            System.out.println("Provided query file has a task with no task-docs examples : so cannot apply Query By Example !");
            this.logger.warning("BETTERQueryParserEval - parseDocumentObject : Provided query file has a task with no task-docs examples : so cannot apply Query By Example !");
            return;
        }
        query.task_docs = new ArrayList<>();
        for (String tasDocInfoKey: docInfoKeySet) query.task_docs.add(tasDocInfoKey);

        // =============================== LOADING THE REQUESTS STREAMS ===============================
        List<JSONObject> requestsInfoList = (JSONArray) document.get("requests");
        if (requestsInfoList.size() == 0) {
            System.out.println("Provided query file has a task with no requests : so cannot do search on this element !");
            this.logger.warning("BETTERQueryParserEval - parseDocumentObject : Provided query file has a task with no requests : so cannot do search on this element !");
            return;
        }

        query.requests = new ArrayList<> ();
        for (JSONObject requestsInfo:  requestsInfoList) {
            requestStream = new BETTERRequest();
            requestStream.req_num = (String) requestsInfo.getOrDefault("req-num", " ");
            requestStream.req_text = (String) requestsInfo.getOrDefault("req-text", " ");

            JSONObject reqDocsList = (JSONObject) requestsInfo.getOrDefault("req-docs", null);
            if (reqDocsList == null) {
                System.out.println("Provided query file has a task with no req-docs field : so cannot apply Query By Example !");
                this.logger.warning("BETTERQueryParserEval - parseDocumentObject : Provided query file has a task with no req-docs field : so cannot apply Query By Example !");
                return;
            }
            reqDocInfoKeySet =  reqDocsList.keySet();
            if (reqDocInfoKeySet.size() == 0) {
                System.out.println("Provided query file has a task with no req-docs examples : so cannot apply Query By Example !");
                this.logger.warning("BETTERQueryParserEval - parseDocumentObject : Provided query file has a task with no req-docs examples : so cannot apply Query By Example !");
                return;
            }
            requestStream.req_docs = new ArrayList<>();
            requestStream.req_extr = new ArrayList<>();
            JSONObject reqDocExtrInfo;
            String reqExtr;
            for (String reqDocInfoKey: reqDocInfoKeySet) {
                requestStream.req_docs.add(reqDocInfoKey);

                reqDocExtrInfo = (JSONObject) reqDocsList.get(reqDocInfoKey);
                reqExtr = (String) reqDocExtrInfo.getOrDefault("segment-text", " ");
                requestStream.req_extr.add( reqExtr.replaceAll("\\r\\n", " ").replaceAll("\\n", " ") );
            }

            query.requests.add(requestStream);
        }

        queries.add(query);

    }
    public List<BETTERTask> queryFileParse() {

        JSONParser jsonParser = new JSONParser();
        try {
            FileReader reader = new FileReader(this.queryFilePath);
            Object obj = jsonParser.parse(reader);
            JSONArray documentList = (JSONArray) obj;

            documentList.forEach( document -> parseDocumentObject( (JSONObject) document ) );

            return queries;

        } catch (IOException | ParseException e) {
            logger.severe("Exception - BETTERQueryParser.java : query input file not found : " + this.queryFilePath);
            System.exit(1);
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        Properties prop = new Properties();
        prop.load(new FileReader("config.properties"));
        Logger logger = Logger.getLogger("BETTERDryRun_Log");

        // String queryFilePath = "auto.json";
//        String queryFilePath = "../hitl-example-one-task.json";
        String queryFilePath = "/Users/ramraj/better-ir/English-Turkey-run/Eval/environment-setup/Decrypted-Query/decrypt-sources/decrypted/ir-hitl-performer-tasks.json";

        BETTERQueryParserEval simpleBETTERQueryParserEval = new BETTERQueryParserEval(prop, queryFilePath, logger);
        simpleBETTERQueryParserEval.queryFileParse();

        // ======================= display the loaded queries ===========
        int countRelDocs = 0;
        for (BETTERTask query : simpleBETTERQueryParserEval.queries) { // queries : is the List<TRECQuery>
            for (BETTERRequest betterRequest : query.requests) {
                // System.out.println(betterRequest.req_num + " : with num rel docs : " + betterRequest.req_docs.size() + " - " + query.task_stmt);
                System.out.println(betterRequest.req_num + " : with num rel docs : " + betterRequest.req_docs.size() + " - " + betterRequest.req_docs);
                countRelDocs += query.task_docs.size();
            }
        }
        System.out.println(countRelDocs);
    }
}
