package BETTERMTedUtils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class BETTERMTedQueryParser {
    public StringBuffer             sbuff;
    public Logger                   logger;
    public String                   queryFilePath;
    public BETTERMTedTask query;
    public List<BETTERMTedTask>         queries;
    public BETTERMTedRequest requestStream;
    public Properties               prop;

    public BETTERMTedQueryParser(Properties prop, String queryFilePath, Logger logger ) {
        this.prop = prop;
        this.sbuff = new StringBuffer();
        this.queryFilePath = queryFilePath;
        this.queries = new LinkedList<>();
        this.logger = logger;
    }
    private void parseDocumentObject(JSONObject document) {
        query = new BETTERMTedTask();

        String lastSeenRequestQueryText = null;

        String task_num = (String) document.getOrDefault("taskID", "");
        if (task_num.equals("")) {
            System.out.println("Provided query file has a task with no task-num field !");
            this.logger.warning("BETTERQueryParserEval - parseDocumentObject : Provided query file has a task with no task-num field !");
        }
        query.task_num = task_num;


        // =============================== LOADING THE REQUESTS STREAMS ===============================
        List<JSONObject> requestsInfoList = (JSONArray) document.get("taskRequests");
        if ( (requestsInfoList == null) || (requestsInfoList.size() == 0) ) {
            System.out.println("Provided query file has a task with no requests : so cannot do search on this element !");
            this.logger.warning("BETTERQueryParserEval - parseDocumentObject : Provided query file has a task with no requests : so cannot do search on this element !");
            return;
        }

        query.requests = new ArrayList<> ();
        for (JSONObject requestsInfo:  requestsInfoList) {
            requestStream = new BETTERMTedRequest();
            requestStream.req_num = (String) requestsInfo.getOrDefault("reqQueryID", "");
            requestStream.req_text = (String) requestsInfo.getOrDefault("reqQueryText", "");

            if (requestStream.req_num.equals("")) {
                System.out.println("Provided query file has a request with no req-num (reqQueryID) field !");
                this.logger.warning("BETTERQueryParserEval - parseDocumentObject : Provided query file has a request with no req-num (reqQueryID) field !");
            }
            if (requestStream.req_text.equals("")) {
                System.out.println("Provided query file has a req with no query (reqQueryText) field !");
                this.logger.warning("BETTERQueryParserEval - parseDocumentObject : Provided query file has a req with no query (reqQueryText) field !");
                if (lastSeenRequestQueryText != null) {
                    requestStream.req_text = lastSeenRequestQueryText;
                }
                // continue;
            }

            query.requests.add(requestStream);
            lastSeenRequestQueryText = requestStream.req_text;
        }

        queries.add(query);

    }
    public List<BETTERMTedTask> queryFileParse() {

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

        // String queryFilePath = "EnglishQuery-for-MTSystem.json";
        // String queryFilePath = "kenton_queries.json";
        String queryFilePath = "queries.json";


        BETTERMTedQueryParser simpleBETTERMTedQueryParser = new BETTERMTedQueryParser(prop, queryFilePath, logger);
        simpleBETTERMTedQueryParser.queryFileParse();

        // ======================= display the loaded queries ===========
        for (BETTERMTedTask query : simpleBETTERMTedQueryParser.queries) { // queries : is the List<TRECQuery>
            for (BETTERMTedRequest BETTERMTedRequest : query.requests) {
                System.out.println(BETTERMTedRequest.req_num + " : " + BETTERMTedRequest.req_text);
            }
        }
    }
}
