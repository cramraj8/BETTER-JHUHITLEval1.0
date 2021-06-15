//package Searcher;
//
//
//import BETTERUtils.BETTERQueryParserEval;
//import BETTERUtils.BETTERRequest;
//import BETTERUtils.BETTERTask;
//import BETTERUtils.BETTERQueryParser;
//import edu.stanford.nlp.pipeline.CoreDocument;
//import edu.stanford.nlp.pipeline.CoreEntityMention;
//import edu.stanford.nlp.pipeline.StanfordCoreNLP;
//import org.apache.log4j.PropertyConfigurator;
//import org.apache.lucene.analysis.Analyzer;
//import org.apache.lucene.analysis.standard.StandardAnalyzer;
//import org.apache.lucene.document.Document;
//import org.apache.lucene.index.DirectoryReader;
//import org.apache.lucene.index.IndexReader;
//import org.apache.lucene.index.MultiTerms;
//import org.apache.lucene.index.PostingsEnum;
//import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
//import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
//import org.apache.lucene.search.IndexSearcher;
//import org.apache.lucene.search.Query;
//import org.apache.lucene.search.ScoreDoc;
//import org.apache.lucene.search.TopDocs;
//import org.apache.lucene.search.similarities.BM25Similarity;
//import org.apache.lucene.search.similarities.LMDirichletSimilarity;
//import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
//import org.apache.lucene.store.Directory;
//import org.apache.lucene.store.FSDirectory;
//import org.apache.lucene.util.BytesRef;
//import org.json.simple.JSONArray;
//import org.json.simple.JSONObject;
//import com.cedarsoftware.util.io.JsonWriter;
//import java.io.*;
//import java.util.*;
//import java.util.logging.FileHandler;
//import java.util.logging.Logger;
//import java.util.logging.SimpleFormatter;
//
//
//public class MainSearcher {
//    public Properties prop;
//    public  Logger logger;
//    public String indexPath;
//    public String queryPath;
//    public String outputPath;
//    public String modelChoice;
//    public int modelChoiceInt;
//    public String initialRankingSimMeasure;
//    public StandardQueryParser queryParser;
//    public String[] reqEntityFieldChoices;
//
//    public Analyzer analyzer;
//    public File indexFile;
//    public IndexReader indexReader;
//    public IndexSearcher indexSearcher;
//    public String fieldToSearch;
//    public File queryFile;
//    // public BETTERQueryParser betterQueryParser;
//    public BETTERQueryParserEval betterQueryParserEval;
//    public List<BETTERTask> queries;
//    public int numFeedbackTerms;
//    public int numFeedbackDocs;
//    public float rm3Mix;
//    public float lMSmoothParam1;
//    public int numHits;
//    public FileWriter outputFileWriter;
//    public FileWriter QEWriteFile;
//
//    public StanfordCoreNLP pipeline;
//    public Properties coreNLPprops;
//    public CoreDocument doc;
//    public ArrayList<String> entityMentions;
//    public String outputToIEFilename;
//    public int numIEDocs;
//    public String outputWatermark;
//
//    // defining some frequent useful variables
//    public Query constructedQuery;
//    public String constructedQueryText;
//    public String[] qToknsLIST;
//    public StringBuffer truncatedQText;
//
//    public MainSearcher(Properties prop,
//                        String modelChoice, String indexDirPath,
//                        String queryPath, String outputPath,
//                        String outputQEPath, String outputToIEFilename, String searcherLogFilename,
//                        String outputWatermark) throws Exception {
//        this.prop = prop;
//        this.outputWatermark = outputWatermark;
//        this.modelChoice = modelChoice;
//        this.indexPath = indexDirPath;
//        this.queryPath = queryPath;
//        this.outputPath = outputPath;
//        this.logger = Logger.getLogger("BETTERDryRun_Searcher_Log");
//        FileHandler fh;
//        try {
//            fh = new FileHandler(searcherLogFilename);
//            logger.addHandler(fh);
//            SimpleFormatter formatter = new SimpleFormatter();
//            fh.setFormatter(formatter);
//            logger.info("... starting to log");
//        } catch (SecurityException | IOException e) {
//            e.printStackTrace();
//        }
//        this.outputToIEFilename = outputToIEFilename;
//        this.numIEDocs = Integer.parseInt(this.prop.getProperty("numIEDocs"));
//
//        this.analyzer = new StandardAnalyzer();
//        this.queryParser = new StandardQueryParser(this.analyzer);
//        this.indexFile = new File(indexPath);
//        Directory indexDir = FSDirectory.open(indexFile.toPath());
//        if (!DirectoryReader.indexExists(indexDir)) {
//            logger.severe("Exception - MainSearcher.java - Index Folder is empty.");
//            System.exit(1);
//        }
//        indexReader = DirectoryReader.open(indexDir);
//        indexSearcher = new IndexSearcher(indexReader);
//
//        queryFile = new File(this.queryPath);
//        betterQueryParserEval = new BETTERQueryParserEval(this.prop, this.queryPath, this.logger);
//        queries = constructQueries();
//
//        outputFileWriter = new FileWriter(outputPath);
//        logger.info("--- Result will be stored in: " + outputPath);
//        QEWriteFile = new FileWriter(outputQEPath);
//
//        fieldToSearch = prop.getProperty("fieldToSearch", "TEXT");
//        numFeedbackTerms = Integer.parseInt(prop.getProperty("numFeedbackTerms"));
//        numFeedbackDocs = Integer.parseInt(prop.getProperty("numFeedbackDocs"));
//        numHits = Integer.parseInt(prop.getProperty("numHits","100"));
//        rm3Mix = Float.parseFloat(prop.getProperty("rm3.mix"));
//        lMSmoothParam1 = Float.parseFloat(prop.getProperty("LMSmoothParam1"));
//
//        //### Similarity functions:
//        //#0 - BM25Similarity
//        //#1 - LMJelinekMercerSimilarity
//        //#2 - LMDirichletSimilarity
//        //#3 - RM3
//        if (this.modelChoice.equals("BM25")) { // Main-1
//            modelChoiceInt = 0;
//        }else if (this.modelChoice.equals("LMJelinekMercerSimilarity")) {
//            modelChoiceInt = 1;
//        }else if (this.modelChoice.equals("LMDirichletSimilarity")) {
//            modelChoiceInt = 2;
//        }else if (this.modelChoice.equals("RM3")) { // Main-4
//            modelChoiceInt = 3;
//        }
//        this.initialRankingSimMeasure = prop.getProperty("initialRankingSimMeasure");
//
//        String log4jConfPath = prop.getProperty("log4jConfPath");
//        PropertyConfigurator.configure(log4jConfPath);
//
////        this.coreNLPprops = PropertiesUtils.asProperties(
////                "annotators", "tokenize, ssplit, ner", // "tokenize,ssplit,parse,pos,ner,lemma,parse,natlog,depparse",
////                "tokenize.options", "splitHyphenated=true, normalizeParentheses=false",
////                "tokenize.whitespace", "false",
////                "ssplit.isOneSentence", "false",
////                "tokenize.language", "en"); // "parse.model", "edu/stanford/nlp/models/srparser/englishSR.ser.gz",
////        this.coreNLPprops.setProperty("ssplit.boundaryTokenRegex", "\\.|[!?]+");
////        this.pipeline = new StanfordCoreNLP(this.coreNLPprops);
//        this.coreNLPprops = new Properties();
//        this.coreNLPprops.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
//        pipeline = new StanfordCoreNLP(this.coreNLPprops);
//    }
//
//    private List<BETTERTask> constructQueries() { // Loading the TREC format of queries
//        return betterQueryParserEval.queryFileParse();
//    }
//
//    private int findLuceneDocID(IndexReader index, String documentField, String docIDString ) throws IOException {
//        BytesRef term = new BytesRef( docIDString );
//        PostingsEnum posting = MultiTerms.getTermPostingsEnum( index, documentField, term, PostingsEnum.NONE );
//        if ( posting != null ) {
//            int docid = posting.nextDoc();
//            if ( docid != PostingsEnum.NO_MORE_DOCS ) {
//                return docid;
//            }
//        }
//        return -1;
//    }
//
//    private ScoreDoc[] createRelTaskLabelScoreDocs(BETTERTask betterTask) throws IOException {
//        ArrayList<ScoreDoc> scoreDocs = new ArrayList<>();
//        Iterator<String> it = betterTask.task_docs.iterator();
//        while(it.hasNext()){
//            String docID = it.next();
//            int LuceneID = findLuceneDocID(indexReader, prop.getProperty("FIELD_ID"), docID);
//            if (LuceneID == -1) {
//                logger.info("--- Task-Document with ID - " + docID + " - was not found in the Index directory !");
//                it.remove();
//            } else {
//                scoreDocs.add( new ScoreDoc(LuceneID, 1.0f) );
//            }
//        }
//
//        ScoreDoc[] scoreDocsArray = new ScoreDoc[scoreDocs.size()];
//        if (scoreDocs.size() == 0) logger.info("--- Task-Example-Documents are None for query : " + betterTask.task_num);
//        else {
//            for (int i=0; i < scoreDocs.size(); i++) scoreDocsArray[i] = scoreDocs.get(i);
//        }
//
//        return scoreDocsArray;
//    }
//
//    private ScoreDoc[] createRelRequestLabelScoreDocs(BETTERRequest betterRequest) throws IOException {
//
//        ArrayList<ScoreDoc> scoreDocs = new ArrayList<>();
//        Iterator<String> it = betterRequest.req_docs.iterator();
//        while(it.hasNext()){
//            String docID = it.next();
//            int LuceneID = findLuceneDocID(indexReader, prop.getProperty("FIELD_ID"), docID);
//            if (LuceneID == -1) {
//                logger.info("--- Req-Document with ID - " + docID + " - was not found in the Index directory !");
//                it.remove();
//            } else {
//                scoreDocs.add( new ScoreDoc(LuceneID, 2.0f) );
//            }
//        }
//
//        ScoreDoc[] scoreDocsArray = new ScoreDoc[scoreDocs.size()];
//        if (scoreDocs.size() == 0) logger.info("--- Req-Example-Documents are None for query : " + betterRequest.req_num);
//        else {
//            for (int i = 0; i < scoreDocs.size(); i++) scoreDocsArray[i] = scoreDocs.get(i);
//        }
//
//        return scoreDocsArray;
//    }
//
//    private Object unSerialize(byte[] bytes) throws ClassNotFoundException, IOException {
//        ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(bytes));
//        Object result = is.readObject();
//        is.close();
//        return result;
//    }
//
//    public Document findDocumentContent(String docID) throws IOException {
//        int internalLuceneID = findLuceneDocID(this.indexReader, prop.getProperty("FIELD_ID"), docID);
//        if (internalLuceneID == -1) return null;
//        else return this.indexReader.document(internalLuceneID);
//    }
//
//    public ArrayList<String> extractEntitiesFromDocumentText(String docID, String textData) {
//        // ================================== extract entities
//        entityMentions = new ArrayList<>();
//        try {
//            doc = new CoreDocument(textData);
//            pipeline.annotate(doc);
//
//            for (CoreEntityMention em : doc.entityMentions()) {
//                if (em.entityType().equals("DATE") || em.entityType().equals("TIME") || em.entityType().equals("MONEY")) {
//                    continue;
//                }
//                entityMentions.add(em.text().replaceAll("\n", " "));
//                // System.out.println("\tdetected entity: \t" + em.text() + "\t" + em.entityType());
//            }
//        } catch (Exception e) {
//            logger.warning("Exception - BETTERIndexer : StanfordCore annotations (entities) threw exception for docID - " + docID);
//            entityMentions.add("the");
//        }
//
//        return entityMentions;
//    }
//
//    private Query queryGenerator(BETTERRequest betterRequest, ArrayList<String> taskDocs) throws IOException, QueryNodeException {
//
//        StringBuilder potentialQueryText = new StringBuilder("");
//
//        // stage-1 : use req-doc titles as query
//        String reqDocText;
//        Document documentContent;
//        for (String docID: betterRequest.req_docs) {
//            documentContent = findDocumentContent(docID);
//            try {
//                reqDocText = documentContent.get(this.prop.getProperty("FIELD_BODY"));
//                potentialQueryText.append((reqDocText.split("\n")[0] + " ").replaceAll("[^a-zA-Z0-9]", " "));
//            } catch (Exception e) {
//                logger.warning("--- queryGenerator - MainSearcher : req-document not found in the index with docID - " + docID);
//                continue;
//            }
//
//        }
//        potentialQueryText.append(" ");
//
//        // stage-2 : use req-entity choices
//        ArrayList<String> entityList = null;
//        for (String docID: betterRequest.req_docs) {
//            documentContent = findDocumentContent(docID);
//
//            entityList = extractEntitiesFromDocumentText(docID, documentContent.get(prop.getProperty("FIELD_BODY")));
//
//            if (entityList.size() == 0) {
//                logger.info("--- MainSearcher : None entities found for req - " + betterRequest.req_num + " - with docID - " + docID);
//            } else {
//                for (String entity: entityList) potentialQueryText.append( entity ).append(" ");
//            }
//        }
//        potentialQueryText.append(" ");
//
//        // stage-3: use req-extractions as query
//        for (String reqExt: betterRequest.req_extr) {
//            potentialQueryText.append(reqExt).append(" ");
//        }
//        potentialQueryText.append(" ");
//
//
//        // stage-4 : use task-doc titles as query
//        String taskDocText;
//        for (String docID: taskDocs) {
//            documentContent = findDocumentContent(docID);
//            try {
//                taskDocText = documentContent.get(this.prop.getProperty("FIELD_BODY"));
//                potentialQueryText.append((taskDocText.split("\n")[0] + " ").replaceAll("[^a-zA-Z0-9]", " "));
//            } catch (Exception e) {
//                logger.warning("--- queryGenerator - MainSearcher : task-document not found in the index with docID - " + docID);
//                continue;
//            }
//        }
//        // todo: limit Q to 1024 tokens
//
//        constructedQueryText = potentialQueryText.toString().replaceAll("[^a-zA-Z0-9]", " ");
//        try {
//            constructedQuery = queryParser.parse(constructedQueryText, fieldToSearch);
//        } catch (Exception e) {
//            logger.warning("--- queryGenerator - MainSearcher : Formulated Query for queryID : " + betterRequest.req_num + " : exceeded max-lucene QueryParser length, so truncating !");
//
//            qToknsLIST = constructedQueryText.split(" ");
//            truncatedQText = new StringBuffer("");
//            for (int i=0; i < 1023; i++) truncatedQText.append(qToknsLIST[i]).append(" ");
//
//            constructedQuery = queryParser.parse(truncatedQText.toString(), fieldToSearch);
//        }
//
//        return constructedQuery;
//    }
//
//    public void retrieveAndRank() throws Exception {
//
//        JSONArray outputToIEList = new JSONArray();
//        JSONObject taskInfo;
//        JSONArray requestList;
//        JSONArray documentsList;
//        JSONObject queryDocumentPair;
//        JSONObject documentInfo;
//
//        ScoreDoc[] hits;
//        TopDocs topDocs;
//        for (BETTERTask query : queries) {
//
//            ScoreDoc[] relevanceTaskScoreDocs = null;
//            if (modelChoiceInt == 3) {
//                relevanceTaskScoreDocs = createRelTaskLabelScoreDocs(query);
//            }
//
//            if (initialRankingSimMeasure.equals("LMJelinekMercer")) indexSearcher.setSimilarity( new LMJelinekMercerSimilarity(lMSmoothParam1) );
//            else if (initialRankingSimMeasure.equals("LMDirichlet")) indexSearcher.setSimilarity( new LMDirichletSimilarity(lMSmoothParam1) );
//            else indexSearcher.setSimilarity( new BM25Similarity(2.0f, 1.0f) ); // For RM3, base initial run is using BM25 // Default k1 = 1.2 b = 0.75
//
//            // =========================================================================================================
//            requestList = new JSONArray();
//            for (BETTERRequest betterRequest : query.requests) {
//
//                ScoreDoc[] relevanceRequestScoreDocs = null;
//                if (modelChoiceInt == 3) {
//                    relevanceRequestScoreDocs = createRelRequestLabelScoreDocs(betterRequest);
//                }
//
//                // ================= Query Generator ==================
//                Query refactoredQuery = queryGenerator(betterRequest, query.task_docs);
//                logger.info("--- retrieveAndRank - MainSearcher : " + betterRequest.req_num + ": Query is : " + refactoredQuery.toString(prop.getProperty("FIELD_BODY")) + "\n");
//
//                if (modelChoiceInt == 3) { // RM3
//                    RM3Searcherv1 rm3Searcherv1 = new RM3Searcherv1(indexReader, logger, this);
//                    topDocs = rm3Searcherv1.search(refactoredQuery, relevanceTaskScoreDocs, relevanceRequestScoreDocs, numHits, betterRequest);
//
//                } else { // BM25
//                    topDocs = indexSearcher.search(refactoredQuery, numHits);
//                }
//
//                hits = topDocs.scoreDocs;
//                // ================================================================================
//                queryDocumentPair = new JSONObject();
//                queryDocumentPair.put("reqQueryID", betterRequest.req_num);
//                queryDocumentPair.put("reqQueryText", refactoredQuery.toString(prop.getProperty("FIELD_BODY")));
//                documentsList = new JSONArray();
//                StringBuffer resBuffer = new StringBuffer();
//                for (int i = 0; i < hits.length; ++i) {
//                    int docId = hits[i].doc;
//                    Document d = indexSearcher.doc(docId);
//
//                    double rerankedScore = hits[i].score;
//                    String rerankedScoreString = String.format ("%.16f", rerankedScore);
//                    resBuffer.append(betterRequest.req_num).append("\tq0\t").
//                            append(d.get(prop.getProperty("FIELD_ID"))).append("\t").
//                            append((i + 1)).append("\t").
//                            append(rerankedScoreString).append("\t").
//                            append(this.outputWatermark).append("\n");
//
//                    if ( i < numIEDocs) {
//                        documentInfo = new JSONObject();
//                        documentInfo.put("docID", d.get(prop.getProperty("FIELD_ID")));
//                        documentInfo.put("docText", d.get(prop.getProperty("FIELD_BODY")));
//                        documentInfo.put("docRank", (i + 1));
//                        documentInfo.put("docScore", rerankedScoreString);
//                        documentsList.add(documentInfo);
//                    }
//                }
//                queryDocumentPair.put("relevanceDocuments", documentsList);
//                requestList.add(queryDocumentPair);
//                outputFileWriter.write(resBuffer.toString());
//                // ================================================================================
//            }
//            taskInfo = new JSONObject();
//            taskInfo.put("taskID", query.task_num);
//            taskInfo.put("taskRequests", requestList);
//            outputToIEList.add(taskInfo);
//            // =========================================================================================================
//        }
//        try (FileWriter file = new FileWriter(this.outputToIEFilename)) {
//            file.write(JsonWriter.formatJson(outputToIEList.toJSONString()));
//            file.flush();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        outputFileWriter.close(); // without closing sometimes the file contents might not be stored
//        QEWriteFile.close();
//    }
//
//
//    public static void main(String[] args) throws Exception {
////        args = new String[4];
////        args[0] = "RM3";
////        args[1] = "./required-files/dry-run-topics.auto.json";
////        args[2] = "./testing-files/thanksgiving-output.txt";
////        // args[2] = "./testing-files/RM3.auto.reqDoc-titles-AND-req-extr-AND-reqDoc-entities.txt";
//
//        // args[0] --> RM3
//        // args[1] --> Index Directory
//        // args[2] --> Query-task file
//        // args[3] --> Output file
//        // args[4] --> QE write file
//        // args[5] --> log file
//
////        args = new String[9];
////        args[0] = "config.properties";
////        args[1] = "RM3";
////        args[2] = "/Users/ramraj/better-ir/English-Turkey-run/building-docker-English/shared-space/JHU_BETTER_IRDryRunIndexDir";
////        args[3] = "../hitl-example-one-task.json";
////        args[4] = "./testing-files/OUTPUT.txt";
////        args[5] = "./testing-files/QEOUTPUT.txt";
////        args[6] = "./testing-files/outputToIE.json";
////        args[7] = "./testing-files/log.log";
////        args[8] = "JHU.EmoryIRDryRun";
//
//        Properties prop = new Properties();
//        prop.load(new FileReader(args[0]));
//
//        MainSearcher mainSearcher = new MainSearcher(prop, args[1], args[2], args[3], args[4], args[5], args[6], args[7], args[8]);
//        mainSearcher.retrieveAndRank();
//    }
//}
//
