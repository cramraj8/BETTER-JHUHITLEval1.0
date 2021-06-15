package DecryptedQuery;

import BETTERUtils.BETTERRequest;
import BETTERUtils.BETTERTask;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

public class BETTERQueryParserTurkeyDecryption {
    public StringBuffer             sbuff;
    public Logger logger;
    public String                   queryFilePath;
    public BETTERTask query;
    public List<BETTERTask> queries;
    public BETTERRequest requestStream;
    public Properties prop;

    public BETTERQueryParserTurkeyDecryption(Properties prop, String queryFilePath, Logger logger ) {
        this.prop = prop;
        this.sbuff = new StringBuffer();
        this.queryFilePath = queryFilePath;
        this.queries = new LinkedList<>();
        this.logger = logger;
    }
    private void parseDocumentObject(JSONObject document) {
        query = new BETTERTask();

        String lastSeenRequestQueryText = null;

        // Get task info
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
        // =============================================================================================

        query.requests = new ArrayList<>();
        for (JSONObject requestsInfo:  requestsInfoList) {
            requestStream = new BETTERRequest();
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

        String queryFilePath = "/Users/ramraj/better-ir/English-Turkey-run/Eval/environment-setup/Decrypted-Query/query-formulation-output/generated-query.en.json";


        BETTERQueryParserTurkeyDecryption betterQueryParserTurkeyDecryption = new BETTERQueryParserTurkeyDecryption(prop, queryFilePath, logger);
        betterQueryParserTurkeyDecryption.queryFileParse();

        // ======================= display the loaded queries ===========
        for (BETTERTask query : betterQueryParserTurkeyDecryption.queries) { // queries : is the List<TRECQuery>
            for (BETTERRequest BETTERMTedRequest : query.requests) {
                System.out.println(BETTERMTedRequest.req_num + " : " + BETTERMTedRequest.req_text);
            }
        }
    }
}
