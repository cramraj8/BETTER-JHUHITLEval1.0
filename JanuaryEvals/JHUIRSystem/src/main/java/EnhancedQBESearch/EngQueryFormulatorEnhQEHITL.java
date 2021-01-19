package EnhancedQBESearch;

import BETTERUtils.BETTERQueryParserEval;
import BETTERUtils.BETTERRequest;
import BETTERUtils.BETTERTask;
import CLIR.QETermGenerator;
import com.cedarsoftware.util.io.JsonWriter;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.apache.log4j.PropertyConfigurator;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class EngQueryFormulatorEnhQEHITL {
    public Properties prop;
    public Logger logger;
    public String queryFilename;
    public BETTERQueryParserEval betterQueryParserEval;
    public List<BETTERTask> queries;
//    public StanfordCoreNLP pipeline;
//    public Properties coreNLPprops;
//    public CoreDocument doc;
    public IndexReader indexReader;
    public File indexFile;
    public String outputWriteFile;
    public String constructedEngQueryText;
    public Query constructedQuery;
    public StandardQueryParser queryParser;
    public Analyzer analyzer;
    public String fieldToSearch;
    public String constructedQueryText;
    public String[] qToknsLIST;
    public StringBuffer truncatedQText;
    public QETermGenerator qeTermGenerator;
    public int numQETerms;
    public int numQEDocs;
    public IndexSearcher indexSearcher;

    private List<BETTERTask> constructQueries() { // Loading the TREC format of queries
        return betterQueryParserEval.queryFileParse();
    }

    public EngQueryFormulatorEnhQEHITL(Properties prop, String indexDirPath, String queryFilename, String outputWriteFile, String searcherLogFilename) throws IOException {
        this.prop = prop;
        this.logger = Logger.getLogger("BETTERDryRun_CLIRQueryFormulator_Log");
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

//        String log4jConfPath = prop.getProperty("log4jConfPath");
//        PropertyConfigurator.configure(log4jConfPath);
//        this.coreNLPprops = new Properties();
//        this.coreNLPprops.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
//        this.pipeline = new StanfordCoreNLP(this.coreNLPprops);

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

        this.qeTermGenerator = new QETermGenerator(this.prop, this.indexFile, this.logger);
        this.numQETerms = Integer.parseInt(this.prop.getProperty("numQETerms"));
        this.numQEDocs = Integer.parseInt(this.prop.getProperty("numQEDocs"));
        this.indexSearcher = new IndexSearcher(indexReader);
    }

    private String generateReqExtrTextHITL(BETTERRequest betterRequest) {
        StringBuilder reqHITLQuery = new StringBuilder("");

        reqHITLQuery.append(betterRequest.req_text).append(" "); // qString4
        for ( String reqExtr: betterRequest.req_extr) reqHITLQuery.append(reqExtr).append(" ");
        constructedQueryText = reqHITLQuery.toString().replaceAll("[^a-zA-Z0-9]", " ");

        try {
            constructedQuery = queryParser.parse(constructedQueryText, fieldToSearch);
            return constructedQueryText;
        } catch (Exception e) {
            logger.warning("--- queryGenerator - HITLMainSearcher : Formulated Query for queryID : " + betterRequest.req_num + " : exceeded max-lucene QueryParser length, so truncating !");

            qToknsLIST = constructedQueryText.split(" ");
            truncatedQText = new StringBuffer("");
            for (int i=0; i < 1023; i++) truncatedQText.append(qToknsLIST[i]).append(" ");
            return truncatedQText.toString();
        }
    }

    private String generateTaskTextHITL(BETTERTask betterTask) throws QueryNodeException {
        StringBuffer taskHITLQuery = new StringBuffer("");
        taskHITLQuery.append(betterTask.task_title).append(" "); // qString1
        taskHITLQuery.append(betterTask.task_stmt).append(" "); // qString2
        taskHITLQuery.append(betterTask.task_narr).append(" "); // qString3
        constructedQueryText = taskHITLQuery.toString().replaceAll("[^a-zA-Z0-9]", " ");
        try {
            constructedQuery = queryParser.parse(constructedQueryText, fieldToSearch);
            return constructedQueryText;
        } catch (Exception e) {
            logger.warning("--- queryGenerator - HITLMainSearcher : Formulated Query for queryID : " + betterTask.task_num + " : exceeded max-lucene QueryParser length, so truncating !");

            qToknsLIST = constructedQueryText.split(" ");
            truncatedQText = new StringBuffer("");
            for (int i=0; i < 1023; i++) truncatedQText.append(qToknsLIST[i]).append(" ");
            return truncatedQText.toString();
        }
    }

    public void mainEngQueryConstruction() throws IOException, QueryNodeException {
        JSONArray outputQueryList = new JSONArray();
        JSONObject taskInfo;
        JSONArray requestList;
        JSONObject queryInfo;

        ArrayList<TaskQETerms> taskQETermsList = new ArrayList<>();
        TaskQETerms taskQETerms;
        ReqQETerms reqQETerms;
        TopDocs top100Hits;
        ArrayList<String> filteredQETerms;
        StringBuilder filteredQETermsString;
        Query reqQ;
        Query taskQ;

        for (BETTERTask query : queries) {
            filteredQETermsString = new StringBuilder("");
            requestList = new JSONArray();

            taskQETerms = new TaskQETerms();

            for (BETTERRequest betterRequest : query.requests) {
                filteredQETermsString = new StringBuilder("");
                // ================= Query Generator ==================
                reqQETerms = new ReqQETerms();
                // generate req query
                // search docs
                // call QE generator
                // add QE terms to reqQETerms.reqQETermsSet
                // add reqQETerms to taskQETerms.reqQEMap

                String reqQueryText = generateReqExtrTextHITL(betterRequest);
                queryInfo = new JSONObject();
                queryInfo.put("req-num", betterRequest.req_num);
                queryInfo.put("req-text-query", reqQueryText);
                requestList.add(queryInfo);

                // 1.1 - generate req-query
                reqQ = queryParser.parse(reqQueryText, fieldToSearch);
                // 1.2 - search docs
                top100Hits = this.indexSearcher.search(reqQ, numQEDocs);
                // 1.3 - call QE terms generator
                filteredQETerms = qeTermGenerator.generateQETerms(top100Hits.scoreDocs, reqQ, numQETerms);
                for (String s: filteredQETerms) filteredQETermsString.append(s).append(" ");
                // 1.4 - add them to the list as string
                queryInfo.put("req-QE-terms-list", filteredQETermsString.toString());

                this.logger.info("---- Finished generating query - " + betterRequest.req_num);
            }

            // generate task query
            // search docs
            // call QE generator
            // add QE terms to taskQETerms.taskQETermsSet
            // 1.1 - generate req-query
            String taskQueryText = generateTaskTextHITL(query);
            taskQ = queryParser.parse(taskQueryText, fieldToSearch);
            top100Hits = this.indexSearcher.search(taskQ, numQEDocs);
            filteredQETerms = qeTermGenerator.generateQETerms(top100Hits.scoreDocs, taskQ, numQETerms);
            for (String s: filteredQETerms) filteredQETermsString.append(s).append(" ");

            taskInfo = new JSONObject();
            taskInfo.put("task-num", query.task_num);
            taskInfo.put("task-title", query.task_title);
            taskInfo.put("task-stmt", query.task_stmt);
            taskInfo.put("task-narr", query.task_narr);
            taskInfo.put("task-QE-terms-list", filteredQETermsString.toString());
            taskInfo.put("taskRequests", requestList);
            outputQueryList.add(taskInfo);

            taskQETermsList.add(taskQETerms);
        }

        try (FileWriter file = new FileWriter(this.outputWriteFile)) {
            file.write(JsonWriter.formatJson(outputQueryList.toJSONString()));
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException, QueryNodeException {
        args = new String[5];
        args[0] = "actual.config.properties";
        args[1] = "/Users/ramraj/better-ir/English-Turkey-run/Eval/environment-setup/English-Indexing/ENGLISH_INDEX_DIR";
        args[2] = "/Users/ramraj/better-ir/English-Turkey-run/Eval/environment-setup/QueryGeneration/turkey-run-hitl-tasks.json";
        args[3] = "/Users/ramraj/better-ir/English-Turkey-run/Eval/environment-setup/QueryGeneration/enhanced-autoIR-queryGen-output/HITLOutput.json";
        args[4] = "/Users/ramraj/better-ir/English-Turkey-run/Eval/environment-setup/QueryGeneration/enhanced-autoIR-queryGen-output/HITL.Log.log";

        Properties prop = new Properties();
        prop.load(new FileReader(args[0]));
        EngQueryFormulatorEnhQEHITL engQueryFormulatorEnhQEHITL = new EngQueryFormulatorEnhQEHITL(prop, args[1], args[2], args[3], args[4]);
        engQueryFormulatorEnhQEHITL.mainEngQueryConstruction();
    }
}
