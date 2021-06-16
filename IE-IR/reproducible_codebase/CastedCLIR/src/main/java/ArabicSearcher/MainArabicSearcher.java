package ArabicSearcher;

import ArabicUtils.ArbRequestBETTER;
import ArabicUtils.ArbTaskBETTER;
import ArabicUtils.BETTERQueryParserTurkey;
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

public class MainArabicSearcher {
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
    public BETTERQueryParserTurkey betterQueryParserTurkey;
    public List<ArbTaskBETTER> queries;
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

    public HashMap<String, HashMap<String, String>> taskEntityMap;

    public MainArabicSearcher(Properties prop,
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
        this.numIEDocs = Integer.parseInt(this.prop.getProperty("Arabic.numIEDocs", "100"));

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
        betterQueryParserTurkey = new BETTERQueryParserTurkey(this.prop, this.queryPath, this.logger);
        queries = constructQueries();

        outputFileWriter = new FileWriter(outputPath);
        logger.info("--- Result will be stored in: " + outputPath);

        fieldToSearch = prop.getProperty("ARB_FIELD_TO_SEARCH", "FIELD_TOKENIZED_CONTENT");
        numHits = Integer.parseInt(prop.getProperty("Arabic.numHits","1000"));
        lMSmoothParam1 = Float.parseFloat(prop.getProperty("Arabic.LMSmoothParam1"));

        //### Similarity functions:
        //#0 - BM25Similarity
        //#1 - LMJelinekMercerSimilarity
        //#2 - LMDirichletSimilarity
        //#3 - RM3
        if (this.modelChoice.equals("BM25")) { // Main-1
            modelChoiceInt = 0;
        }else if (this.modelChoice.equals("LMJelinekMercerSimilarity")) {
            modelChoiceInt = 1;
        }else if (this.modelChoice.equals("LMDirichletSimilarity")) {
            modelChoiceInt = 2;
        }else if (this.modelChoice.equals("RM3")) { // Main-4
            modelChoiceInt = 3;
        }
        this.initialRankingSimMeasure = prop.getProperty("Arabic.initialRankingSimMeasure");
        this.BM25_k1_val = Float.parseFloat(prop.getProperty("Arabic.BM25K1"));
        this.BM25_b_val = Float.parseFloat(prop.getProperty("Arabic.BM25b"));

        this.taskEntityMap = new HashMap();
        readNamedEntityFile(EngNamedEntityFilename);

        this.numFeedbackTerms = Integer.parseInt(this.prop.getProperty("Arabic.numFeedbackTerms"));
        this.numFeedbackDocs = Integer.parseInt(this.prop.getProperty("Arabic.numFeedbackDocs"));
        this.rm3Mix = Float.parseFloat(this.prop.getProperty("Arabic.rm3Mix"));
    }

    private List<ArbTaskBETTER> constructQueries() { // Loading the TREC format of queries
        return betterQueryParserTurkey.queryFileParse();
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
            System.exit(1);
        }
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
        String finalQueryString;

        Query refactoredQuery;
        String[] qToknsLIST;
        StringBuffer truncatedQText;
        for (ArbTaskBETTER query : queries) {

            if (initialRankingSimMeasure.equals("LMJelinekMercer")) indexSearcher.setSimilarity( new LMJelinekMercerSimilarity(lMSmoothParam1) );
            else if (initialRankingSimMeasure.equals("LMDirichlet")) indexSearcher.setSimilarity( new LMDirichletSimilarity(lMSmoothParam1) );
            else indexSearcher.setSimilarity( new BM25Similarity(BM25_k1_val, BM25_b_val) ); // For RM3, base initial run is using BM25 // Default k1 = 1.2 b = 0.75

            // =========================================================================================================
            requestList = new JSONArray();
            for (ArbRequestBETTER arbRequestBETTER : query.requests) {


                // ================= Query Generator ==================
                // Query refactoredQuery = queryParser.parse(BETTERMTedRequest.req_text, fieldToSearch);
                finalQueryString = QueryParser.escape(arbRequestBETTER.req_text) + " " + taskEntityMap.get(query.task_num).get(arbRequestBETTER.req_num).replaceAll("[^a-zA-Z0-9]", " ");
                try {
                    refactoredQuery = queryParser.parse(finalQueryString, fieldToSearch);
                } catch (Exception e) {
                    logger.warning("--- queryGenerator - MainSearcher : Formulated Query for queryID : " + arbRequestBETTER.req_num + " : exceeded max-lucene QueryParser length, so truncating !");
//                     try {
//                     } catch (Exception ee) {
//                         refactoredQuery = queryParser.parse(QueryParser.escape(bettermTedRequest.req_text), fieldToSearch);
//                     }
                }

                qToknsLIST = finalQueryString.split(" ");
                truncatedQText = new StringBuffer("");
                for (int i=0; i < Math.min(900, qToknsLIST.length); i++) truncatedQText.append(qToknsLIST[i]).append(" ");

                refactoredQuery = queryParser.parse(truncatedQText.toString(), fieldToSearch);


                logger.info("--- retrieveAndRank - MainSearcher : " + arbRequestBETTER.req_num + ": Query is : " + refactoredQuery.toString(prop.getProperty("ARB_FIELD_TOKENIZED_CONTENT")) + "\n");

                if (modelChoiceInt == 3) { // RM3
                    RM3Searcherv1 rm3Searcherv1 = new RM3Searcherv1(indexReader, logger, this);
                    topDocs = rm3Searcherv1.search(refactoredQuery, numHits, arbRequestBETTER);
                } else { // BM25
                    topDocs = indexSearcher.search(refactoredQuery, numHits);
                }

                hits = topDocs.scoreDocs;
                // ================================================================================
                queryDocumentPair = new JSONObject();
                queryDocumentPair.put("reqQueryID", arbRequestBETTER.req_num);
                queryDocumentPair.put("reqQueryText", refactoredQuery.toString(prop.getProperty("ARB_FIELD_TOKENIZED_CONTENT")));
                documentsList = new JSONArray();
                StringBuffer resBuffer = new StringBuffer();
                for (int i = 0; i < hits.length; ++i) {
                    int docId = hits[i].doc;
                    Document d = indexSearcher.doc(docId);

                    double rerankedScore = hits[i].score;
                    String rerankedScoreString = String.format ("%.16f", rerankedScore);
                    resBuffer.append(arbRequestBETTER.req_num).append("\tq0\t").
                            append(d.get(prop.getProperty("ARB_FIELD_ID"))).append("\t").
                            append((i + 1)).append("\t").
                            append(rerankedScoreString).append("\t").
                            append(this.outputWatermark).append("\n");

                    if ( i < numIEDocs) {
                        documentInfo = new JSONObject();
                        documentInfo.put("docID", d.get(prop.getProperty("ARB_FIELD_ID")));
                        documentInfo.put("docText", d.get(prop.getProperty("ARB_FIELD_RAW_CONTENT")).toString());
                        documentInfo.put("docRank", (i + 1));
                        documentInfo.put("docScore", rerankedScoreString);
                        // ArrayList<Object> paraEntityPageList = (ArrayList<Object>) unSerialize(d.getBinaryValue(prop.getProperty("FIELD_ENTITY_Page")).bytes);
//                        documentInfo.put("docSentenceSegments", unSerialize(d.getBinaryValue(prop.getProperty("ARB_FIELD_SENT_SEG")).bytes) );
//                        documentInfo.put("docSentenceSegments", unSerialize(d.getBinaryValue(prop.getProperty("ARB_FIELD_SENT_SEG")).bytes) );
                        documentsList.add(documentInfo);
                    }
                }
                queryDocumentPair.put("relevanceDocuments", documentsList);
                requestList.add(queryDocumentPair);
                outputFileWriter.write(resBuffer.toString());
                // ================================================================================
            }
            taskInfo = new JSONObject();
            taskInfo.put("taskID", query.task_num);
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

    public static Object unSerialize(byte[] bytes) throws ClassNotFoundException, IOException {
        ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(bytes));
        Object result = is.readObject();
        is.close();
        return result;
    }

    public static void main(String[] args) throws Exception {
        args = new String[9];
        args[0] = "config.properties";
        args[1] = "RM3";
//        args[2] = "/Users/ramraj/Projects/better-ir/Eval-1.0-BETTER-Jan_Mar/Building-refactored-Docker-systems/IndexDirs/Arabic_IndexDir";
        args[2] = "/Users/ramraj/Projects/better-ir/Eval-1.0-BETTER-Jan_Mar/Building-refactored-Docker-systems/IndexDirs/English_IndexDir";
        args[3] = "scratch/output.json";
        args[4] = "scratch/output.json.entities.json";
        args[5] = "scratch/Arabic.output.txt";
        args[6] = "scratch/Arabic.outputToIE.json";
        args[7] = "scratch/Arabic.logs.log";
        args[8] = "JHU.Emory.CLIR.Eval";

        Properties prop = new Properties();
        prop.load(new FileReader(args[0]));

        MainArabicSearcher mainArabicSearcher = new MainArabicSearcher(prop, args[1], args[2], args[3], args[5], args[6], args[7], args[8], args[4]);
        mainArabicSearcher.retrieveAndRank();
    }

}
