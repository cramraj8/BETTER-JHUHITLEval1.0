package Utils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;


public class BETTERQueryParserArb {
    public StringBuffer            sbuff;
    public Logger logger;
    public String                  queryFilePath;
    public BETTERTask query;
    public List<BETTERTask> queries;
    public BETTERRequest requestStream;
    public Properties prop;

    public BETTERQueryParserArb(Properties prop, String queryFilePath, Logger logger ) {
        this.prop = prop;
        this.sbuff = new StringBuffer();
        this.queryFilePath = queryFilePath;
        this.queries = new LinkedList<>();
        this.logger = logger;
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

        String task_stmt = (String) document.getOrDefault("task-stmt", " ");
        query.task_stmt = task_stmt;

        String task_narr = (String) document.getOrDefault("task-narr", " ");
        query.task_narr = task_narr;

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
            requestStream.expanded_text = (String) requestsInfo.getOrDefault("expanded_text", " ");

            query.requests.add(requestStream);
        }
        queries.add(query);
    }

    public static void main(String[] args) throws Exception {
        Properties prop = new Properties();
        prop.load(new FileReader("Arb.Search.config.properties"));
        Logger logger = Logger.getLogger("BETTERDryRun_Log");

        String queryFilePath = "../get-set/generated-query.en.json";

        BETTERQueryParserArb betterQueryParserArb = new BETTERQueryParserArb(prop, queryFilePath, logger);
        betterQueryParserArb.queryFileParse();

        // ======================= display the loaded queries ===========
        int countRelDocs = 0;
        for (BETTERTask query : betterQueryParserArb.queries) { // queries : is the List<TRECQuery>
            for (BETTERRequest betterRequest : query.requests) {
                // System.out.println(betterRequest.req_num + " : with num rel docs : " + betterRequest.req_docs.size() + " - " + query.task_stmt);
                 System.out.println(betterRequest.req_num + " : with : " + query.task_title + " - " + betterRequest.req_text);
                System.out.println(" ... ... " + betterRequest.expanded_text);
            }
        }
    }
}
