package BETTERUtils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;


public class BETTERQueryParser {
    public StringBuffer            sbuff;
    public Logger                   logger;
    public String                  queryFilePath;
    public BETTERTask query;
    public List<BETTERTask> queries;
    public BETTERRequest requestStream;
    public Properties prop;
    public HashMap<String, Integer> manualHITLAnnoations;
    public BETTERQueryParser( Properties prop, String queryFilePath, Logger logger ) {
        this.prop = prop;
        this.sbuff = new StringBuffer();
        this.queryFilePath = queryFilePath;
        this.queries = new LinkedList<>();
        this.logger = logger;
    }
    private void parseDocumentObject(JSONObject document) {
        query = new BETTERTask();

        String task_num = (String) document.getOrDefault("task-num", " ");
        query.task_num = task_num;
        // System.out.println(task_num);

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

        query.task_docs = (ArrayList<String>) document.getOrDefault("task-docs", " "); // This variables stores basically the labels(ground-truth) for the model


        // =============================== LOADING THE REQUESTS STREAMS ===============================
        List<BETTERRequest> betterRequests = new LinkedList<>();
        List<JSONObject> requestsList = (List<JSONObject>) document.get("requests");
        for (JSONObject request : requestsList) {
            requestStream = new BETTERRequest();
            requestStream.req_num = (String) request.getOrDefault("req-num", " ");
            requestStream.req_text = (String) request.getOrDefault("req-text", " ");
            requestStream.req_docs = (ArrayList<String>) request.getOrDefault("req-docs", " ");
            requestStream.req_extr = (ArrayList<String>) request.getOrDefault("req-extr", " ");

            betterRequests.add(requestStream);
        }
        query.requests = betterRequests;

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
        String queryFilePath = "../hitl-example-one-task.json";

        BETTERQueryParser simpleBETTERQueryParser = new BETTERQueryParser(prop, queryFilePath, logger);
        simpleBETTERQueryParser.queryFileParse();

        // ======================= display the loaded queries ===========
        int countRelDocs = 0;
        for (BETTERTask query : simpleBETTERQueryParser.queries) { // queries : is the List<TRECQuery>
            for (BETTERRequest betterRequest : query.requests) {
                System.out.println(betterRequest.req_num + " : with num rel docs : " + betterRequest.req_docs.size() + " - " + query.task_stmt);
                countRelDocs += query.task_docs.size();
            }
        }
        System.out.println(countRelDocs);
    }
}
