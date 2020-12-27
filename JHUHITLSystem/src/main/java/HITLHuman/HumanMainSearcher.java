package HITLHuman;

import BETTERUtils.BETTERQueryParserEvalHITL;
import BETTERUtils.BETTERRequest;
import BETTERUtils.BETTERTask;
import com.cedarsoftware.util.io.JsonWriter;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiTerms;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
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

public class HumanMainSearcher {
    public Properties prop;
    public Logger logger;
    public String indexPath;
    public String queryPath;
    public String outputPath;
    public String modelChoice;
    public int modelChoiceInt;
    public String initialRankingSimMeasure;
    public StandardQueryParser queryParser;
    public String[] reqEntityFieldChoices;

    public Analyzer analyzer;
    public File indexFile;
    public IndexReader indexReader;
    public IndexSearcher indexSearcher;
    public String fieldToSearch;
    public File queryFile;
    // public BETTERQueryParser betterQueryParser;
    public BETTERQueryParserEvalHITL betterQueryParserEvalHITL;
    public List<BETTERTask> queries;
    public int numFeedbackTerms;
    public int numFeedbackDocs;
    public float rm3Mix;
    public float lMSmoothParam1;
    public int numHits;
    public FileWriter outputFileWriter;
    public FileWriter QEWriteFile;

    public ArrayList<String> entityMentions;
    public String outputToIEFilename;
    public int numIEDocs;
    public String outputWatermark;

    // defining some frequent useful variables
    public Query constructedQuery;
    public String constructedQueryText;
    public String[] qToknsLIST;
    public StringBuffer truncatedQText;

    public HumanMainSearcher(Properties prop,
                            String modelChoice, String indexDirPath,
                            String queryPath, String outputPath,
                            String outputQEPath, String outputToIEFilename, String searcherLogFilename,
                            String outputWatermark) throws Exception {
        this.prop = prop;
        this.outputWatermark = outputWatermark;
        this.modelChoice = modelChoice;
        this.indexPath = indexDirPath;
        this.queryPath = queryPath;
        this.outputPath = outputPath;
        this.logger = Logger.getLogger("BETTERDryRun_Searcher_Log");
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
        this.numIEDocs = Integer.parseInt(this.prop.getProperty("numIEDocs"));

        this.analyzer = new StandardAnalyzer();
        this.queryParser = new StandardQueryParser(this.analyzer);
        this.indexFile = new File(indexPath);
        Directory indexDir = FSDirectory.open(indexFile.toPath());
        if (!DirectoryReader.indexExists(indexDir)) {
            logger.severe("Exception - HITLMainSearcher.java - Index Folder is empty.");
            System.exit(1);
        }
        indexReader = DirectoryReader.open(indexDir);
        indexSearcher = new IndexSearcher(indexReader);

        queryFile = new File(this.queryPath);
        betterQueryParserEvalHITL = new BETTERQueryParserEvalHITL(this.prop, this.queryPath, this.logger);
        queries = constructQueries();

        outputFileWriter = new FileWriter(outputPath);
        logger.info("--- Result will be stored in: " + outputPath);
        QEWriteFile = new FileWriter(outputQEPath);

        fieldToSearch = prop.getProperty("fieldToSearch", "TEXT");
        numFeedbackTerms = Integer.parseInt(prop.getProperty("numFeedbackTerms"));
        numFeedbackDocs = Integer.parseInt(prop.getProperty("numFeedbackDocs"));
        numHits = Integer.parseInt(prop.getProperty("numHits","100"));
        rm3Mix = Float.parseFloat(prop.getProperty("rm3.mix"));
        lMSmoothParam1 = Float.parseFloat(prop.getProperty("LMSmoothParam1"));

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
        this.initialRankingSimMeasure = prop.getProperty("initialRankingSimMeasure");


    }

    private List<BETTERTask> constructQueries() { // Loading the TREC format of queries
        return betterQueryParserEvalHITL.queryFileParse();
    }

    private int findLuceneDocID(IndexReader index, String documentField, String docIDString ) throws IOException {
        BytesRef term = new BytesRef( docIDString );
        PostingsEnum posting = MultiTerms.getTermPostingsEnum( index, documentField, term, PostingsEnum.NONE );
        if ( posting != null ) {
            int docid = posting.nextDoc();
            if ( docid != PostingsEnum.NO_MORE_DOCS ) {
                return docid;
            }
        }
        return -1;
    }

    private ScoreDoc[] createRelTaskLabelScoreDocs(BETTERTask betterTask) throws IOException {
        ArrayList<ScoreDoc> scoreDocs = new ArrayList<>();
        Iterator<String> it = betterTask.task_docs.iterator();
        while(it.hasNext()){
            String docID = it.next();
            int LuceneID = findLuceneDocID(indexReader, prop.getProperty("FIELD_ID"), docID);
            if (LuceneID == -1) {
                logger.info("--- Task-Document with ID - " + docID + " - was not found in the Index directory !");
                it.remove();
            } else {
                scoreDocs.add( new ScoreDoc(LuceneID, 1.0f) );
            }
        }

        ScoreDoc[] scoreDocsArray = new ScoreDoc[scoreDocs.size()];
        if (scoreDocs.size() == 0) logger.info("--- Task-Example-Documents are None for query : " + betterTask.task_num);
        else {
            for (int i=0; i < scoreDocs.size(); i++) scoreDocsArray[i] = scoreDocs.get(i);
        }

        return scoreDocsArray;
    }

    private ScoreDoc[] createRelRequestLabelScoreDocs(BETTERRequest betterRequest) throws IOException {

        ArrayList<ScoreDoc> scoreDocs = new ArrayList<>();
        Iterator<String> it = betterRequest.req_docs.iterator();
        while(it.hasNext()){
            String docID = it.next();
            int LuceneID = findLuceneDocID(indexReader, prop.getProperty("FIELD_ID"), docID);
            if (LuceneID == -1) {
                logger.info("--- Req-Document with ID - " + docID + " - was not found in the Index directory !");
                it.remove();
            } else {
                scoreDocs.add( new ScoreDoc(LuceneID, 2.0f) );
            }
        }

        ScoreDoc[] scoreDocsArray = new ScoreDoc[scoreDocs.size()];
        if (scoreDocs.size() == 0) logger.info("--- Req-Example-Documents are None for query : " + betterRequest.req_num);
        else {
            for (int i = 0; i < scoreDocs.size(); i++) scoreDocsArray[i] = scoreDocs.get(i);
        }

        return scoreDocsArray;
    }

    public Document findDocumentContent(String docID) throws IOException {
        int internalLuceneID = findLuceneDocID(this.indexReader, prop.getProperty("FIELD_ID"), docID);
        if (internalLuceneID == -1) return null;
        else return this.indexReader.document(internalLuceneID);
    }


    private Query HITLQueryGenerator(BETTERTask betterTask, BETTERRequest betterRequest) throws QueryNodeException {
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

        return constructedQuery;
    }

    private HashMap<String, HashSet<String>> readFeedbackFile(String feedbackFile) {
        String line = "";
        String splitBy = ",";
        Set<String> queryDocsSet;

        String qID;
        String dID;

        HashMap<String, HashSet<String>> feedbackQDocMap = new HashMap<String, HashSet<String>>();
        HashSet<String> docSet;
        try {
            BufferedReader br = new BufferedReader(new FileReader(feedbackFile));
            while ((line = br.readLine()) != null) {
                String[] feedbackLine = line.split(splitBy);    // use comma as separator
                if (feedbackLine[0].equals("QueryID")) continue;
                // if (feedbackLine[1].equals("NO")) continue;
                if ( !(feedbackLine[1].equals("YES")) ) continue;

                qID = feedbackLine[0];
                dID = feedbackLine[2];
                if (feedbackQDocMap.containsKey(qID)) {
                    docSet = feedbackQDocMap.get(qID);
                } else {
                    docSet = new HashSet<>();
                }
                docSet.add(dID);
                feedbackQDocMap.put(qID, docSet);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return feedbackQDocMap;
    }

    private ScoreDoc[] createRelHumanLabelScoreDocs(BETTERRequest betterRequest, HashSet<String> feedbackDocSet) throws IOException {
        ArrayList<ScoreDoc> scoreDocs = new ArrayList<>();
        Iterator<String> it = feedbackDocSet.iterator();
        while(it.hasNext()){
            String docID = it.next();
            int LuceneID = findLuceneDocID(indexReader, prop.getProperty("FIELD_ID"), docID);
            if (LuceneID == -1) {
                logger.info("--- Req-Document with ID - " + docID + " - was not found in the Index directory !");
                it.remove();
            } else {
                scoreDocs.add( new ScoreDoc(LuceneID, 3.0f) );
            }
        }
        ScoreDoc[] scoreDocsArray = new ScoreDoc[scoreDocs.size()];
        if (scoreDocs.size() == 0) logger.info("--- Req-Example-Documents are None for query : " + betterRequest.req_num);
        else {
            for (int i = 0; i < scoreDocs.size(); i++) scoreDocsArray[i] = scoreDocs.get(i);
        }

        return scoreDocsArray;
    }

    public void retrieveAndRank(String createInterfaceString, String HITLTopKDocsString, String hitlFilename,
                                String loadInterfaceString, String hitlLabelledFilename) throws Exception {
        boolean createInterface = Boolean.parseBoolean(createInterfaceString);
        int HITLTopKDocs = Integer.parseInt(HITLTopKDocsString);
        boolean loadInterface = Boolean.parseBoolean(loadInterfaceString);

        // System.out.println(HITLTopKDocs);


        FileWriter csvWriter = null;
        if (createInterface) {
            csvWriter = new FileWriter(hitlFilename);
            csvWriter.append("QueryID");
            csvWriter.append(",");
            csvWriter.append("Feedback");
            csvWriter.append(",");
            csvWriter.append("DocID");
            csvWriter.append(",");
            csvWriter.append("DocText");
            csvWriter.append("\n");
        }

        HashMap<String, HashSet<String>> feedbackHumanLabels = null;
        if (loadInterface) {
            feedbackHumanLabels = readFeedbackFile(hitlLabelledFilename);
        }

        JSONArray outputToIEList = new JSONArray();
        JSONObject taskInfo;
        JSONArray requestList;
        JSONArray documentsList;
        JSONObject queryDocumentPair;
        JSONObject documentInfo;

        ScoreDoc[] hits;
        TopDocs topDocs;
        for (BETTERTask query : queries) {

            ScoreDoc[] relevanceTaskScoreDocs = null;
            if (modelChoiceInt == 3) {
                relevanceTaskScoreDocs = createRelTaskLabelScoreDocs(query);
            }

            if (initialRankingSimMeasure.equals("LMJelinekMercer")) indexSearcher.setSimilarity( new LMJelinekMercerSimilarity(lMSmoothParam1) );
            else if (initialRankingSimMeasure.equals("LMDirichlet")) indexSearcher.setSimilarity( new LMDirichletSimilarity(lMSmoothParam1) );
            else indexSearcher.setSimilarity( new BM25Similarity(2.0f, 1.0f) ); // For RM3, base initial run is using BM25 // Default k1 = 1.2 b = 0.75

            // =========================================================================================================
            requestList = new JSONArray();
            for (BETTERRequest betterRequest : query.requests) {

                ScoreDoc[] relevanceRequestScoreDocs = null;
                if (modelChoiceInt == 3) {
                    relevanceRequestScoreDocs = createRelRequestLabelScoreDocs(betterRequest);
                }

                ScoreDoc[] relevanceHumanScoreDocs = null;
                if (loadInterface) {
                    relevanceHumanScoreDocs = createRelHumanLabelScoreDocs(betterRequest, feedbackHumanLabels.get(betterRequest.req_num));
                }

                // ================= Query Generator ==================
                // Query refactoredQuery = queryGenerator(betterRequest, query.task_docs);
                Query refactoredQuery = HITLQueryGenerator(query, betterRequest);
                logger.info("--- retrieveAndRank - HITLMainSearcher : " + betterRequest.req_num + ": Query is : " + refactoredQuery.toString(prop.getProperty("FIELD_BODY")) + "\n");

                if (modelChoiceInt == 3) { // RM3
                    HumanRM3Searcher humanRM3Searcher = new HumanRM3Searcher(indexReader, logger, this);
                    topDocs = humanRM3Searcher.search(refactoredQuery, relevanceTaskScoreDocs, relevanceRequestScoreDocs, relevanceHumanScoreDocs, numHits, betterRequest);

                } else { // BM25
                    topDocs = indexSearcher.search(refactoredQuery, numHits);
                }

                hits = topDocs.scoreDocs;
                // ================================================================================
                queryDocumentPair = new JSONObject();
                queryDocumentPair.put("reqQueryID", betterRequest.req_num);
                queryDocumentPair.put("reqQueryText", refactoredQuery.toString(prop.getProperty("FIELD_BODY")));
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
                        documentInfo.put("docText", d.get(prop.getProperty("FIELD_BODY")));
                        documentInfo.put("docRank", (i + 1));
                        documentInfo.put("docScore", rerankedScoreString);
                        documentsList.add(documentInfo);
                    }
                    if (createInterface && (i < HITLTopKDocs)) {
                        csvWriter.append(String.format(Locale.US, "%s", betterRequest.req_num)).append(",");
                        csvWriter.append(" ").append(",");
                        csvWriter.append(d.get(prop.getProperty("FIELD_ID"))).append(",");
                        // csvWriter.append(String.format(Locale.US, "%s", d.get(prop.getProperty("FIELD_BODY"))));
                        csvWriter.append( d.get(prop.getProperty("FIELD_BODY")).replaceAll("\\s+"," ") );
                        // csvWriter.append( d.get(prop.getProperty("FIELD_BODY")).replace(" ", "\\ ") );

                        // .replaceAll(",", "\\,")
                        csvWriter.append("\n");
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
        if (createInterface) {
            csvWriter.flush();
            csvWriter.close();
        }


        outputFileWriter.close(); // without closing sometimes the file contents might not be stored
        QEWriteFile.close();
    }


    public static void main(String[] args) throws Exception {

//        args = new String[14];
//        args[0] = "config.properties";
//        args[1] = "RM3";
//        args[2] = "/Users/ramraj/better-ir/English-Turkey-run/Eval/English-IR-scratch/JHI_IR_English_IndexDir";
//        args[3] = "./ir-tasks.json";
//        args[4] = "./testing-files/HITLOUTPUT.txt";
//        args[5] = "./testing-files/HITLQEOUTPUT.txt";
//        args[6] = "./testing-files/HITLoutputToIE.json";
//        args[7] = "./testing-files/HITLlog.log";
//        args[8] = "JHU.EmoryHITLDryRun";
//        args[9] = "true";
//        args[10] = "10";
//        args[11] = "humanAnnotations-verify.csv";
//        args[12] = "false";
//        args[13] = "humanLabelledAnnotations.csv";


        Properties prop = new Properties();
        prop.load(new FileReader(args[0]));

        HumanMainSearcher humanMainSearcher = new HumanMainSearcher(prop, args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8]);
        humanMainSearcher.retrieveAndRank(args[9], args[10], args[11],
                args[12], args[13] );
    }
}



