package BETTERCLHITLSearcherQEEnh;

import BETTERMTedUtils.BETTERMTedQueryParser;
import BETTERMTedUtils.BETTERMTedTask;
import com.cedarsoftware.util.io.JsonWriter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiTerms;
import org.apache.lucene.index.PostingsEnum;
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
import org.apache.lucene.util.BytesRef;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.util.*;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class MainSearcherEnh {
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
    public BETTERMTedUtils.BETTERMTedQueryParser BETTERMTedQueryParser;
    public List<BETTERMTedTask> queries;
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

    public MainSearcherEnh(Properties prop,
                        String modelChoice, String indexDirPath,
                        String queryPath, String outputPath,
                        String outputToIEFilename, String searcherLogFilename,
                        String outputWatermark) throws IOException {
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
            logger.severe("Exception - MainSearcherEnh.java - Index Folder is empty.");
            System.exit(1);
        }
        indexReader = DirectoryReader.open(indexDir);
        indexSearcher = new IndexSearcher(indexReader);

        queryFile = new File(this.queryPath);
        BETTERMTedQueryParser = new BETTERMTedQueryParser(this.prop, this.queryPath, this.logger);
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
        weight_arabReqQuery = Float.parseFloat(prop.getProperty("weight.arabReqQuery"));
        weight_arabQE = Float.parseFloat(prop.getProperty("weight.arabQE"));
        weight_TaskTitle = Float.parseFloat(prop.getProperty("weight.TaskTitle"));
        weight_TaskNarr = Float.parseFloat(prop.getProperty("weight.TaskNarr"));
        weight_TaskStmt = Float.parseFloat(prop.getProperty("weight.TaskStmt"));
    }

    private List<BETTERMTedTask> constructQueries() { // Loading the TREC format of queries
        return BETTERMTedQueryParser.queryFileParse();
    }

    public static Object unSerialize(byte[] bytes) throws ClassNotFoundException, IOException {
        ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(bytes));
        Object result = is.readObject();
        is.close();
        return result;
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
        String[] splitQETerms;
        Set<String> prevReqQETermsSet;
        for (BETTERMTedTask query : queries) {

            if (initialRankingSimMeasure.equals("LMJelinekMercer")) indexSearcher.setSimilarity( new LMJelinekMercerSimilarity(lMSmoothParam1) );
            else if (initialRankingSimMeasure.equals("LMDirichlet")) indexSearcher.setSimilarity( new LMDirichletSimilarity(lMSmoothParam1) );
            else indexSearcher.setSimilarity( new BM25Similarity(BM25_k1_val, BM25_b_val) ); // For RM3, base initial run is using BM25 // Default k1 = 1.2 b = 0.75

            // =========================================================================================================
            requestList = new JSONArray();
            prevReqQETermsSet = new HashSet<>();
            for (BETTERMTedUtils.BETTERMTedRequest BETTERMTedRequest : query.requests) {
                splitQETerms = BETTERMTedRequest.req_qe_terms.split(" ");
                for (String s: splitQETerms) prevReqQETermsSet.add(s);


                // ================= Query Generator ==================
                // Query refactoredQuery = queryParser.parse(BETTERMTedRequest.req_text, fieldToSearch);
                Query refactoredQuery = queryParser.parse(QueryParser.escape(BETTERMTedRequest.req_text), fieldToSearch);
                logger.info("--- retrieveAndRank - MainSearcherEnh : " + BETTERMTedRequest.req_num + ": Query is : " + refactoredQuery.toString(prop.getProperty("FIELD_TOKENIZED_CONTENT")) + "\n");

                if (modelChoiceInt == 3) { // RM3
                    RM3Searcherv1 rm3Searcherv1 = new RM3Searcherv1(indexReader, logger, this);
                    topDocs = rm3Searcherv1.search(refactoredQuery, numHits, query, BETTERMTedRequest, prevReqQETermsSet);
                } else { // BM25
                    topDocs = indexSearcher.search(refactoredQuery, numHits);
                }

                hits = topDocs.scoreDocs;
                // ================================================================================
                queryDocumentPair = new JSONObject();
                queryDocumentPair.put("reqQueryID", BETTERMTedRequest.req_num);
                queryDocumentPair.put("reqQueryText", refactoredQuery.toString(prop.getProperty("FIELD_TOKENIZED_CONTENT")));
                documentsList = new JSONArray();
                StringBuffer resBuffer = new StringBuffer();
                for (int i = 0; i < hits.length; ++i) {
                    int docId = hits[i].doc;
                    Document d = indexSearcher.doc(docId);

                    double rerankedScore = hits[i].score;
                    String rerankedScoreString = String.format ("%.16f", rerankedScore);
                    resBuffer.append(BETTERMTedRequest.req_num).append("\tq0\t").
                            append(d.get(prop.getProperty("FIELD_ID"))).append("\t").
                            append((i + 1)).append("\t").
                            append(rerankedScoreString).append("\t").
                            append(this.outputWatermark).append("\n");

                    if ( i < numIEDocs) {
                        documentInfo = new JSONObject();
                        documentInfo.put("docID", d.get(prop.getProperty("FIELD_ID")));
                        documentInfo.put("docText", d.get(prop.getProperty("FIELD_RAW_CONTENT")));
                        documentInfo.put("docRank", (i + 1));
                        documentInfo.put("docScore", rerankedScoreString);
                        documentInfo.put("docSentenceSegments", unSerialize(d.getBinaryValue(prop.getProperty("FIELD_SENT_SEG")).bytes) );
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

    public static void main(String[] args) throws Exception {
        args = new String[8];
        args[0] = "config.properties";
        args[1] = "RM3";
        args[2] = "/Users/ramraj/better-ir/English-Turkey-run/Eval/environment-setup/Arabic-Indexing/ARABIC_INDEX_DIR_wSentSeg";
        args[3] = "/Users/ramraj/better-ir/English-Turkey-run/Eval/environment-setup/FinalImprovements/autoHITL/query.ar.HITL.json";
        args[4] = "/Users/ramraj/better-ir/English-Turkey-run/Eval/environment-setup/FinalImprovements/autoHITL/outputs/output.txt";
        args[5] = "/Users/ramraj/better-ir/English-Turkey-run/Eval/environment-setup/FinalImprovements/autoHITL/outputs/outputToIE.json";
        args[6] = "/Users/ramraj/better-ir/English-Turkey-run/Eval/environment-setup/FinalImprovements/autoHITL/outputs/log.log";
        args[7] = "JHU.EmoryCLIRDryRun";

        Properties prop = new Properties();
        prop.load(new FileReader(args[0]));

        MainSearcherEnh mainSearcherEnh = new MainSearcherEnh(prop, args[1], args[2], args[3], args[4], args[5], args[6], args[7]);
        mainSearcherEnh.retrieveAndRank();
    }

}
