package CLIR;

import BETTERUtils.BETTERQueryParserEval;
import BETTERUtils.BETTERRequest;
import BETTERUtils.BETTERTask;
import com.cedarsoftware.util.io.JsonWriter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
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
    public IndexSearcher indexSearcher;
    public int numHits;
    public int numEngQETerms;
    public QueryFormulatorHITL(Properties prop, String indexDirPath, String queryFilename, String outputWriteFile, String searcherLogFilename, String numEngQETerms) throws IOException {
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

        this.indexSearcher = new IndexSearcher(indexReader);
        this.numHits = Integer.parseInt( this.prop.getProperty("numHits") );
        this.numEngQETerms = Integer.parseInt( numEngQETerms );
    }
    private List<BETTERTask> constructQueries() { // Loading the TREC format of queries
        return betterQueryParserEval.queryFileParse();
    }
    private String HITLQueryGenerator(BETTERTask betterTask, BETTERRequest betterRequest) throws QueryNodeException {
        StringBuffer potentialQueryString = new StringBuffer("");
        potentialQueryString.append(betterTask.task_title).append(" "); // qString1
        potentialQueryString.append(betterTask.task_stmt).append(" "); // qString2
        potentialQueryString.append(betterTask.task_narr).append(" "); // qString3

        potentialQueryString.append(betterRequest.req_text).append(" "); // qString4
        for ( String reqExtr: betterRequest.req_extr) potentialQueryString.append(reqExtr).append(" "); // qString5

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

        ArrayList<String> EngQETermsList;
        for (BETTERTask query : queries) {
            requestList = new JSONArray();
            for (BETTERRequest betterRequest : query.requests) {
                // ==== Step-1: Query Generator ==================
                String refactoredQuery = HITLQueryGenerator(query, betterRequest);
                Query q = queryParser.parse(QueryParser.escape(refactoredQuery), fieldToSearch);
                // === Step-2: Get search
                TopDocs top1000Hits = indexSearcher.search(q, numHits);
                // === Step-3: get QE terms
                QETermGenerator qeTermGenerator = new QETermGenerator(this.prop, this.indexFile, this.logger);
                EngQETermsList = qeTermGenerator.generateQETerms(top1000Hits.scoreDocs, q, this.numEngQETerms);

                queryInfo = new JSONObject();
                queryInfo.put("reqQueryID", betterRequest.req_num);
                queryInfo.put("reqQueryText", refactoredQuery);
                queryInfo.put("reqQueryQETerms", EngQETermsList);
                requestList.add(queryInfo);

                this.logger.info("Query : " + betterRequest.req_num + " generated for HITL settings !");
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
        args = new String[6];
        args[0] = "actual.config.properties";
        args[1] = "/Users/ramraj/better-ir/English-Turkey-run/Eval/environment-setup/English-Indexing/ENGLISH_INDEX_DIR";
        args[2] = "/Users/ramraj/better-ir/English-Turkey-run/Eval/environment-setup/turkey-run-hitl-tasks.json";
        args[3] = "/Users/ramraj/better-ir/English-Turkey-run/Eval/environment-setup/QEwithEnAr/output.txt";
        args[4] = "/Users/ramraj/better-ir/English-Turkey-run/Eval/environment-setup/QEwithEnAr/log.log";
        args[5] = "20";

        Properties prop = new Properties();
        prop.load(new FileReader(args[0]));
        QueryFormulatorHITL queryFormulatorHITL = new QueryFormulatorHITL(prop, args[1], args[2], args[3], args[4], args[5]);
        queryFormulatorHITL.constructQuery();
    }
}
