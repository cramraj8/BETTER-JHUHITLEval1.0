package ArabicSearch;

import Utils.BETTERQueryParserArb;
import Utils.BETTERRequest;
import Utils.BETTERTask;
import com.cedarsoftware.util.io.JsonWriter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.*;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.io.FileReader;
import java.util.Properties;

public class MainSearcher {
    public Properties prop;
    public Logger logger;
    public String indexPath;
    public String queryPath;
    public String outputPath;
    public String modelChoice;
    public int modelChoiceInt;
    public String initialRankingSimMeasure;
    public StandardQueryParser queryParser;
    public Analyzer analyzer;
    public File indexFile;
    public IndexReader indexReader;
    public IndexSearcher indexSearcher;
    public String fieldToSearch;
    public File queryFile;
    public BETTERQueryParserArb betterQueryParserArb;
    public List<BETTERTask> queries;
    public FileWriter outputFileWriter;

    public String outputToIEFilename;
    public String outputWatermark;

    public float BM25_k1_val;
    public float BM25_b_val;
    public float lMSmoothParam1;
    public int numHits;
    public int numIEDocs;

    public int numFeedbackTerms;
    public int numFeedbackDocs;
    public float rm3Mix;
    public float weight_arabReqQuery;
    public float weight_arabQE;
    public float weight_TaskTitle;
    public float weight_TaskNarr;
    public float weight_TaskStmt;
    public HashMap<String, HashMap<String, String>> taskEntityMap;
    private List<BETTERTask> constructQueries() { // Loading the TREC format of queries
        return betterQueryParserArb.queryFileParse();
    }
    public static Object unSerialize(byte[] bytes) throws ClassNotFoundException, IOException {
        ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(bytes));
        Object result = is.readObject();
        is.close();
        return result;
    }
    private void parseEntityDocumentObject(JSONObject document) {

        String reqIDString;
        String reqEntitiesString;
        HashMap<String, String> reqEntityMap = new HashMap<>();

        String task_num = (String) document.getOrDefault("taskID", "");
        if (task_num.equals("")) {
            System.out.println("Provided query file has a task with no task-num field !");
            this.logger.warning("BETTERQueryParserEval - parseDocumentObject : Provided query file has a task with no task-num field !");
        }

        // =============================== LOADING THE REQUESTS STREAMS ===============================
        List<JSONObject> requestsInfoList = (JSONArray) document.get("taskRequests");
        if ( (requestsInfoList == null) || (requestsInfoList.size() == 0) ) {
            System.out.println("Provided query file has a task with no requests : so cannot do search on this element !");
            this.logger.warning("BETTERQueryParserEval - parseDocumentObject : Provided query file has a task with no requests : so cannot do search on this element !");
            return;
        }
        // =============================================================================================

        for (JSONObject requestsInfo:  requestsInfoList) {

            reqIDString = (String) requestsInfo.getOrDefault("reqQueryID", "");
            reqEntitiesString = (String) requestsInfo.getOrDefault("reqEntities", "");
            if (reqIDString.equals("")) {
                System.out.println("Provided query file has a request with no req-num (reqQueryID) field !");
                this.logger.warning("BETTERQueryParserEval - parseDocumentObject : Provided query file has a request with no req-num (reqQueryID) field !");
            }
            if (reqEntitiesString.equals("")) {
                System.out.println("Provided query file has a req with no query (reqQueryText) field !");
                this.logger.warning("BETTERQueryParserEval - parseDocumentObject : Provided query file has a req with no query (reqQueryText) field !");
            }

            reqEntityMap.put(reqIDString, reqEntitiesString);
            this.taskEntityMap.put(task_num, reqEntityMap);
        }
    }
    public void readNamedEntityFile(String filename) {
        JSONParser jsonParser = new JSONParser();
        try {
            FileReader reader = new FileReader(filename);
            Object obj = jsonParser.parse(reader);
            JSONArray documentList = (JSONArray) obj;
            documentList.forEach( document -> parseEntityDocumentObject( (JSONObject) document ) );
        } catch (IOException | ParseException e) {
            logger.severe("Exception - BETTERQueryParser.java : entity input file not found : " + filename);
            // System.exit(1);
        }
    }
    public MainSearcher(Properties prop,
                        String modelChoice, String indexDirPath,
                        String queryPath, String outputPath,
                        String outputToIEFilename, String searcherLogFilename,
                        String outputWatermark, String EngNamedEntityFilename) throws IOException {
        this.prop = prop;
        this.outputWatermark = outputWatermark;
        this.modelChoice = modelChoice;
        this.indexPath = indexDirPath;
        this.queryPath = queryPath;
        this.outputPath = outputPath;
        this.logger = Logger.getLogger("BETTERDryRun_CLIRSearcher_Log");
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
        this.outputToIEFilename = outputToIEFilename;
        this.numIEDocs = Integer.parseInt(this.prop.getProperty("numIEDocs", "100"));

        this.analyzer = new WhitespaceAnalyzer();
        this.queryParser = new StandardQueryParser(this.analyzer);
        this.indexFile = new File(indexPath);
        Directory indexDir = FSDirectory.open(indexFile.toPath());
        if (!DirectoryReader.indexExists(indexDir)) {
            logger.severe("Exception - MainSearcher.java - Index Folder is empty.");
            System.exit(1);
        }
        indexReader = DirectoryReader.open(indexDir);
        indexSearcher = new IndexSearcher(indexReader);

        queryFile = new File(this.queryPath);
        betterQueryParserArb = new BETTERQueryParserArb(this.prop, this.queryPath, this.logger);
        queries = constructQueries();

        outputFileWriter = new FileWriter(outputPath);
        logger.info("--- Result will be stored in: " + outputPath);

        fieldToSearch = prop.getProperty("fieldToSearch", "FIELD_TOKENIZED_CONTENT");
        numHits = Integer.parseInt(prop.getProperty("numHits","1000"));
        lMSmoothParam1 = Float.parseFloat(prop.getProperty("LMSmoothParam1"));

        //### Similarity functions:
        if (this.modelChoice.equals("BM25")) { // Main-1
            modelChoiceInt = 0;
        }else if (this.modelChoice.equals("LMJelinekMercerSimilarity")) {
            modelChoiceInt = 1;
        }else if (this.modelChoice.equals("LMDirichletSimilarity")) {
            modelChoiceInt = 2;
        }else if (this.modelChoice.equals("RM3")) { // Main-4
            modelChoiceInt = 3;
        }
        this.initialRankingSimMeasure = prop.getProperty("initialRankingSimMeasure");
        this.BM25_k1_val = Float.parseFloat(prop.getProperty("BM25K1"));
        this.BM25_b_val = Float.parseFloat(prop.getProperty("BM25b"));

        this.numFeedbackTerms = Integer.parseInt(prop.getProperty("numFeedbackTerms"));
        this.numFeedbackDocs = Integer.parseInt(prop.getProperty("numFeedbackDocs"));
        this.rm3Mix = Float.parseFloat(prop.getProperty("rm3Mix"));

        this.weight_arabReqQuery = Float.parseFloat(prop.getProperty("weight.arabReqQuery"));
        this.weight_arabQE = Float.parseFloat(prop.getProperty("weight.arabQE"));
        this.weight_TaskTitle = Float.parseFloat(prop.getProperty("weight.TaskTitle"));
        this.weight_TaskNarr = Float.parseFloat(prop.getProperty("weight.TaskNarr"));
        this.weight_TaskStmt = Float.parseFloat(prop.getProperty("weight.TaskStmt"));

        this.taskEntityMap = new HashMap();
        readNamedEntityFile(EngNamedEntityFilename);
    }

    public void retrieveAndRank() throws Exception {

        JSONArray outputToIEList = new JSONArray();
        JSONObject taskInfo;
        JSONArray requestList;
        JSONArray documentsList;
        JSONObject queryDocumentPair;
        JSONObject documentInfo;

        ScoreDoc[] hits;
        TopDocs topDocs;
        Set<String> prevReqQETermsSet;

        String initialQuery;
        String namedEntitiesString;
        String finalQueryString;
        Query refactoredQuery;
        String[] qToknsLIST;
        StringBuffer truncatedQText;

        int ReqCount;
        for (BETTERTask betterTask : queries) {

            if (initialRankingSimMeasure.equals("LMJelinekMercer")) indexSearcher.setSimilarity( new LMJelinekMercerSimilarity(lMSmoothParam1) );
            else if (initialRankingSimMeasure.equals("LMDirichlet")) indexSearcher.setSimilarity( new LMDirichletSimilarity(lMSmoothParam1) );
            else indexSearcher.setSimilarity( new BM25Similarity(BM25_k1_val, BM25_b_val) ); // For RM3, base initial run is using BM25 // Default k1 = 1.2 b = 0.75

            // =========================================================================================================
            requestList = new JSONArray();
            prevReqQETermsSet = new HashSet<>();
            ReqCount = 1;
            for (BETTERRequest betterRequest : betterTask.requests) {

                namedEntitiesString = taskEntityMap.get(betterTask.task_num).get(betterRequest.req_num).replaceAll("[^a-zA-Z0-9]", " ");

                if (ReqCount > 2) {
                    logger.info("... This is 3+ request ... Therefore inferring the initial query");
                    initialQuery = betterRequest.expanded_text + " " + namedEntitiesString + " " + betterRequest.req_text + " " +
                            betterTask.task_title + " " + betterTask.task_stmt + " " + betterTask.task_narr;
                } else {
                    if (betterRequest.expanded_text.equals("")) {
                        initialQuery = betterRequest.expanded_text + " " + namedEntitiesString + " " + betterRequest.req_text + " " +
                                betterTask.task_title + " " + betterTask.task_stmt + " " + betterTask.task_narr;
                    } else {
                        initialQuery = betterRequest.expanded_text;
                    }
                }





                // ================= Query Generator ==================
                finalQueryString = QueryParser.escape(initialQuery);
                try {
                    refactoredQuery = queryParser.parse(finalQueryString, fieldToSearch);
                } catch (Exception e) {
                    logger.warning("--- queryGenerator - MainSearcher : Formulated Query for queryID : " + betterRequest.req_num + " : exceeded max-lucene QueryParser length, so truncating !");
                }

                qToknsLIST = finalQueryString.split(" ");
                truncatedQText = new StringBuffer("");
                for (int i=0; i < Math.min(900, qToknsLIST.length); i++) truncatedQText.append(qToknsLIST[i]).append(" ");

                refactoredQuery = queryParser.parse(truncatedQText.toString(), fieldToSearch);

                logger.info("--- retrieveAndRank - MainSearcher : " + betterRequest.req_num + ": Query is : " + refactoredQuery.toString(prop.getProperty("FIELD_TOKENIZED_CONTENT")) + "\n");

                /**
                 * =====================================================================================================
                 */
                if (modelChoiceInt == 3) { // RM3
                    RM3Searcher rm3Searcher = new RM3Searcher(indexReader, logger, this);
                    topDocs = rm3Searcher.search(refactoredQuery, numHits, betterTask, betterRequest, prevReqQETermsSet);
                } else { // BM25
                    topDocs = indexSearcher.search(refactoredQuery, numHits);
                }

                hits = topDocs.scoreDocs;
                // ================================================================================
                queryDocumentPair = new JSONObject();
                queryDocumentPair.put("reqQueryID", betterRequest.req_num);
                queryDocumentPair.put("reqQueryText", refactoredQuery.toString(prop.getProperty("FIELD_TOKENIZED_CONTENT")));
                documentsList = new JSONArray();
                StringBuffer resBuffer = new StringBuffer();
                for (int i = 0; i < hits.length; ++i) {
                    int docId = hits[i].doc;
                    Document d = indexSearcher.doc(docId);

                    double rerankedScore = hits[i].score;
                    String rerankedScoreString = String.format ("%.16f", rerankedScore);
                    resBuffer.append(betterRequest.req_num).append("\tq0\t").
                            append(d.get(prop.getProperty("FIELD_ID"))).append("\t").
                            append((i + 1)).append("\t").
                            append(rerankedScoreString).append("\t").
                            append(this.outputWatermark).append("\n");

                    if ( i < numIEDocs) {
                        documentInfo = new JSONObject();
                        documentInfo.put("docID", d.get(prop.getProperty("FIELD_ID")));
                        documentInfo.put("docText", d.get(prop.getProperty("FIELD_RAW_CONTENT")).toString());
                        documentInfo.put("docRank", (i + 1));
                        documentInfo.put("docScore", rerankedScoreString);
                        documentInfo.put("docSentenceSegments", unSerialize(d.getBinaryValue(prop.getProperty("FIELD_SENT_SEG")).bytes) );
                        documentsList.add(documentInfo);

//                        System.out.println(d.get(prop.getProperty("FIELD_RAW_CONTENT")));
                    }
                }
                queryDocumentPair.put("relevanceDocuments", documentsList);
                requestList.add(queryDocumentPair);
                outputFileWriter.write(resBuffer.toString());
                // ================================================================================
                ReqCount++;
            }
            taskInfo = new JSONObject();
            taskInfo.put("taskID", betterTask.task_num);
            taskInfo.put("taskRequests", requestList);
            outputToIEList.add(taskInfo);
            // =========================================================================================================
        }
        try (FileWriter file = new FileWriter(this.outputToIEFilename)) {
            file.write(JsonWriter.formatJson(outputToIEList.toJSONString()));
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        outputFileWriter.close(); // without closing sometimes the file contents might not be stored
    }

    public static void main(String[] args) throws Exception {
//        args = new String[9];
//        args[0] = "Arb.Search.config.properties";
//        args[1] = "RM3";
//        args[2] = "/Users/ramraj/better-ir/English-Turkey-run/Submissions/BuildingDocker/Eval-fullHITL/scratch-old/JHI_IR_Arabic_IndexDir";
//        // args[3] = "generated-query.ar.json";
//        args[3] = "/Users/ramraj/better-ir/English-Turkey-run/Submissions/BuildingDocker/Eval-fullHITL/scratch-old/output_queries.json.ar";
//        // args[4] = "generated-query.en.json.entities.json";
//        args[4] = "/Users/ramraj/better-ir/English-Turkey-run/Submissions/BuildingDocker/Eval-fullHITL/scratch-old/input_queries.json.en.entities.json";
//        args[5] = "/Users/ramraj/better-ir/English-Turkey-run/Submissions/BuildingDocker/Eval-fullHITL/scratch-old/final-output/search-output.txt";
//        args[6] = "/Users/ramraj/better-ir/English-Turkey-run/Submissions/BuildingDocker/Eval-fullHITL/scratch-old/final-output/search-outputToIE.json";
//        args[7] = "/Users/ramraj/better-ir/English-Turkey-run/Submissions/BuildingDocker/Eval-fullHITL/scratch-old/final-output/search-log.log";
//        args[8] = "JHU.EmoryCLIRDryRun";

        args = new String[9];
        args[0] = "/Users/ramraj/better-ir/English-Turkey-run/Submissions/BuildingDocker/Eval-fullHITL/JHUCLIRSystem/config-files/Arb.Search.config.properties";
        args[1] = "RM3";
        // args[2] = "/Users/ramraj/better-ir/English-Turkey-run/Submissions/BuildingDocker/Eval-fullHITL/scratch-old/JHI_IR_Arabic_IndexDir";
        args[2] = "/Users/ramraj/better-ir/English-Turkey-run/Submissions/BuildingDocker/Eval-fullHITL/scratch/JHI_IR_Arabic_IndexDir";
        args[3] = "/Users/ramraj/better-ir/English-Turkey-run/Submissions/BuildingDocker/Eval-fullHITL/scratch/output_queries.json.ar";
        args[4] = "/Users/ramraj/better-ir/English-Turkey-run/Submissions/BuildingDocker/Eval-fullHITL/scratch/input_queries.json.en.entities.json";
        args[5] = "/Users/ramraj/better-ir/English-Turkey-run/Eval/environment-setup/full-HITL/FullHITLArabicRetrieval/submission-verification/search-output.txt";
        args[6] = "/Users/ramraj/better-ir/English-Turkey-run/Eval/environment-setup/full-HITL/FullHITLArabicRetrieval/submission-verification/search-outputToIE.json";
        args[7] = "/Users/ramraj/better-ir/English-Turkey-run/Eval/environment-setup/full-HITL/FullHITLArabicRetrieval/submission-verification/search-log.log";
        args[8] = "JHU.EmoryCLIRDryRun";

        Properties prop = new Properties();
        prop.load(new FileReader(args[0]));

        MainSearcher mainSearcher = new MainSearcher(prop, args[1], args[2], args[3], args[5], args[6], args[7], args[8], args[4]);
        mainSearcher.retrieveAndRank();
    }
}
