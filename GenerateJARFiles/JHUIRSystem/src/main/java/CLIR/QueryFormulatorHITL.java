package CLIR;

import BETTERUtils.BETTERQueryParserEval;
import BETTERUtils.BETTERRequest;
import BETTERUtils.BETTERTask;
import com.cedarsoftware.util.io.JsonWriter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class QueryFormulatorHITL {
    public Properties prop;
    public Logger logger;

    public String queryFilename;
    public BETTERQueryParserEval betterQueryParserEval;
    public List<BETTERTask> queries;

    public IndexReader indexReader;
    public File indexFile;

    public String outputWriteFile;

    public String constructedQueryText;
    public Query constructedQuery;
    public String[] qToknsLIST;
    public StringBuffer truncatedQText;
    public StandardQueryParser queryParser;
    public String fieldToSearch;
    public Analyzer analyzer;

    private List<BETTERTask> constructQueries() { // Loading the TREC format of queries
        return betterQueryParserEval.queryFileParse();
    }

    public QueryFormulatorHITL(Properties prop, String indexDirPath, String queryFilename, String outputWriteFile, String searcherLogFilename) throws IOException {
        this.prop = prop;
        this.logger = Logger.getLogger("BETTERDryRun_CLHITLQueryFormulator_Log");
        FileHandler fh;
        try {
            fh = new FileHandler(searcherLogFilename);
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
            logger.info("... starting to log");
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }
        this.queryFilename = queryFilename;
        this.betterQueryParserEval = new BETTERQueryParserEval(this.prop, queryFilename, this.logger);
        this.queries = constructQueries();

        this.indexFile = new File(indexDirPath);
        Directory indexDir = FSDirectory.open(indexFile.toPath());
        if (!DirectoryReader.indexExists(indexDir)) {
            logger.severe("Exception - MainSearcher.java - Index Folder is empty.");
            System.exit(1);
        }
        this.indexReader = DirectoryReader.open(indexDir);
        this.outputWriteFile = outputWriteFile;
        this.analyzer = new StandardAnalyzer();
        this.queryParser = new StandardQueryParser(this.analyzer);
        fieldToSearch = prop.getProperty("fieldToSearch", "TEXT");
    }

    private String HITLQueryGenerator(BETTERTask betterTask, BETTERRequest betterRequest) throws QueryNodeException {
        StringBuffer potentialQueryString = new StringBuffer("");
        potentialQueryString.append(betterTask.task_title).append(" ");
        potentialQueryString.append(betterTask.task_stmt).append(" ");
        potentialQueryString.append(betterTask.task_narr).append(" ");

        potentialQueryString.append(betterRequest.req_text).append(" ");
        for ( String reqExtr: betterRequest.req_extr) potentialQueryString.append(reqExtr).append(" ");

        constructedQueryText = potentialQueryString.toString().replaceAll("[^a-zA-Z0-9]", " ");
        try {
            constructedQuery = queryParser.parse(constructedQueryText, fieldToSearch);
        } catch (Exception e) {
            logger.warning("--- queryGenerator - HITLMainSearcher : Formulated Query for queryID : " + betterRequest.req_num + " : exceeded max-lucene QueryParser length, so truncating !");

            qToknsLIST = constructedQueryText.split(" ");
            truncatedQText = new StringBuffer("");
            for (int i=0; i < 1023; i++) truncatedQText.append(qToknsLIST[i]).append(" ");

            constructedQuery = queryParser.parse(truncatedQText.toString(), fieldToSearch);
        }

        return constructedQuery.toString(fieldToSearch);
    }

    public void constructQuery() throws IOException, QueryNodeException {
        JSONArray outputQueryList = new JSONArray();
        JSONObject taskInfo;
        JSONArray requestList;
        JSONObject queryInfo;

        for (BETTERTask query : queries) {
            requestList = new JSONArray();
            for (BETTERRequest betterRequest : query.requests) {
                // ================= Query Generator ==================
                // String refactoredQuery = queryGenerator(betterRequest, query.task_docs);
                String refactoredQuery = HITLQueryGenerator(query, betterRequest);
                queryInfo = new JSONObject();
                queryInfo.put("reqQueryID", betterRequest.req_num);
                queryInfo.put("reqQueryText", refactoredQuery);
                requestList.add(queryInfo);
            }
            taskInfo = new JSONObject();
            taskInfo.put("taskID", query.task_num);
            taskInfo.put("taskRequests", requestList);
            outputQueryList.add(taskInfo);
        }

        System.out.println("HITL Query formulation is done !");
        logger.info("HITL Query formulation is done !");

        try (FileWriter file = new FileWriter(this.outputWriteFile)) {
            file.write(JsonWriter.formatJson(outputQueryList.toJSONString()));
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException, QueryNodeException {
//        Properties prop = new Properties();
//        prop.load(new FileReader("CLIR.config.properties"));
//        QueryFormulatorHITL queryFormulatorHITL = new QueryFormulatorHITL(prop,
//                                                            prop.getProperty("IndexDir"),
//                                                            // prop.getProperty("testAutoQueryFile"),
//                "/Users/ramraj/better-ir/English-Turkey-run/building-docker-English-Arabic/shared-space/hitl-example-one-task.json",
//                                                            "./ramraj-EnglishQuery.json",
//                                                            prop.getProperty("CLIRQueryFormulatorLogFilename"));
//        queryFormulatorHITL.constructQuery();


        // args[0] - config.properties
        // args[1] - IndexDir
        // args[2] - Input English Query file
        // args[3] - Output formulated Query file
        // args[4] - Query Formulator logger
        Properties prop = new Properties();
        prop.load(new FileReader(args[0]));
        QueryFormulatorHITL queryFormulatorHITL = new QueryFormulatorHITL(prop, args[1], args[2], args[3], args[4]);
        queryFormulatorHITL.constructQuery();
    }
}
