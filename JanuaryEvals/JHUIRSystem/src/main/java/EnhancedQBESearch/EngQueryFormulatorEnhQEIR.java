package EnhancedQBESearch;

import BETTERUtils.BETTERQueryParserEval;
import BETTERUtils.BETTERRequest;
import BETTERUtils.BETTERTask;
import com.cedarsoftware.util.io.JsonWriter;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreEntityMention;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.apache.log4j.PropertyConfigurator;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiTerms;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
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

public class EngQueryFormulatorEnhQEIR {
    public Properties prop;
    public Logger logger;
    public String queryFilename;
    public BETTERQueryParserEval betterQueryParserEval;
    public List<BETTERTask> queries;
    public ArrayList<String> entityMentions;
    public StanfordCoreNLP pipeline;
    public Properties coreNLPprops;
    public CoreDocument doc;
    public IndexReader indexReader;
    public File indexFile;
    public String outputWriteFile;
    public String constructedEngQueryText;
    public Query constructedQuery;
    public StandardQueryParser queryParser;
    public Analyzer analyzer;
    public String fieldToSearch;

    private List<BETTERTask> constructQueries() { // Loading the TREC format of queries
        return betterQueryParserEval.queryFileParse();
    }

    public EngQueryFormulatorEnhQEIR(Properties prop, String indexDirPath, String queryFilename, String outputWriteFile, String searcherLogFilename) throws IOException {
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

        String log4jConfPath = prop.getProperty("log4jConfPath");
        PropertyConfigurator.configure(log4jConfPath);
        this.coreNLPprops = new Properties();
        this.coreNLPprops.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
        this.pipeline = new StanfordCoreNLP(this.coreNLPprops);

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

    public Document findDocumentContent(String docID) throws IOException {
        int internalLuceneID = findLuceneDocID(this.indexReader, prop.getProperty("FIELD_ID"), docID);
        if (internalLuceneID == -1) return null;
        else return this.indexReader.document(internalLuceneID);
    }

    public ArrayList<String> extractEntitiesFromDocumentText(String docID, String textData) {
        // ================================== extract entities
        // System.out.println("Get inside method");
        entityMentions = new ArrayList<>();
        try {
            doc = new CoreDocument(textData);
            pipeline.annotate(doc);

            for (CoreEntityMention em : doc.entityMentions()) {
                if (em.entityType().equals("DATE") || em.entityType().equals("TIME") || em.entityType().equals("MONEY")) {
                    continue;
                }
                entityMentions.add(em.text().replaceAll("\n", " "));
                // System.out.println("\tdetected entity: \t" + em.text() + "\t" + em.entityType());
            }
        } catch (Exception e) {
            logger.warning("Exception - BETTERIndexer : StanfordCore annotations (entities) threw exception for docID - " + docID);
            entityMentions.add("the");
        }

        // System.out.println(entityMentions);

        return entityMentions;
    }

    private ArrayList<String> queryGenerator(BETTERRequest betterRequest, ArrayList<String> taskDocs) throws IOException, QueryNodeException {
        Set<String> standaloneNamedEntities = new HashSet<>();

        String[] qToknsLIST;
        StringBuffer truncatedQText;

        /**
         * 1. req-extractions : available
         * 2. req-text + req-entities
         * 3. task-text + task-entities
         */

        StringBuilder fullQueryText = new StringBuilder("");
        StringBuilder temporaryQueryText = new StringBuilder("");

        // stage-1: use req-extractions text into query :  IMPORTANT
        for (String reqExt: betterRequest.req_extr) {
            fullQueryText.append(reqExt).append(" ");
            temporaryQueryText.append(reqExt).append(" ");
        }
        fullQueryText.append(" ");
        temporaryQueryText.append(" ");

        // stage-2 : use req-doc example document text & entities into query
        String reqDocText;
        Document documentContent;
        ArrayList<String> reqDocEntityList = null;
        for (String docID: betterRequest.req_docs) {
            documentContent = findDocumentContent(docID);
            if (documentContent == null) continue;

            // adding entities
            reqDocEntityList = extractEntitiesFromDocumentText(docID, documentContent.get(prop.getProperty("FIELD_BODY")));
            if (reqDocEntityList.size() == 0) {
                logger.info("--- MainSearcher : None entities found for req - " + betterRequest.req_num + " - with docID - " + docID);
            } else {
                for (String entity: reqDocEntityList) temporaryQueryText.append( entity ).append(" ");
                standaloneNamedEntities.addAll(reqDocEntityList);
            }

            // adding text
            try {
                reqDocText = documentContent.get(this.prop.getProperty("FIELD_BODY"));
                temporaryQueryText.append((reqDocText.split("\n")[0] + " ").replaceAll("[^a-zA-Z0-9]", " "));
            } catch (Exception e) {
                logger.warning("--- queryGenerator - MainSearcher : req-document not found in the index with docID - " + docID);
                continue;
            }
        }
        temporaryQueryText.append(" ");


        // stage-3 : use task-doc example-document text and entities into query
        String taskDocText;
        ArrayList<String> taskDocEntityList = null;
        for (String docID: taskDocs) {
            documentContent = findDocumentContent(docID);
            if (documentContent == null) continue;

            // adding entities
            taskDocEntityList = extractEntitiesFromDocumentText(docID, documentContent.get(prop.getProperty("FIELD_BODY")));
            if (taskDocEntityList.size() == 0) {
                logger.info("--- MainSearcher : None entities found for req - " + betterRequest.req_num + " - with docID - " + docID);
            } else {
                for (String entity: taskDocEntityList) temporaryQueryText.append( entity ).append(" ");
                standaloneNamedEntities.addAll(taskDocEntityList);
            }

            // adding text
            try {
                taskDocText = documentContent.get(this.prop.getProperty("FIELD_BODY"));
                temporaryQueryText.append((taskDocText.split("\n")[0] + " ").replaceAll("[^a-zA-Z0-9]", " "));
            } catch (Exception e) {
                logger.warning("--- queryGenerator - MainSearcher : task-document not found in the index with docID - " + docID);
                continue;
            }
        }
        // todo: limit Q to 1024 tokens

        // convert Set<String> into String
        StringBuilder namedEntityStringStream = new StringBuilder("");
        for (String s: standaloneNamedEntities) namedEntityStringStream.append(s).append(" ");

        constructedEngQueryText = temporaryQueryText.toString().replaceAll("[^a-zA-Z0-9]", " ");
        try {
            constructedQuery = queryParser.parse(constructedEngQueryText, fieldToSearch);

            ArrayList<String> returnString = new ArrayList<>();
            returnString.add(constructedEngQueryText);
            returnString.add(namedEntityStringStream.toString());
            return returnString;
        } catch (Exception e) {
            logger.warning("--- queryGenerator - MainSearcher : Formulated Query for queryID : " + betterRequest.req_num + " : exceeded max-lucene QueryParser length, so truncating !");

             // TODO:
             qToknsLIST = constructedEngQueryText.split(" ");
             truncatedQText = new StringBuffer("");
             for (int i=0; i < Math.min(900, qToknsLIST.length); i++) truncatedQText.append(qToknsLIST[i]).append(" ");
             // constructedQuery = queryParser.parse(truncatedQText.toString(), fieldToSearch);

//            // adding req-doc entities
//            if (reqDocEntityList.size() == 0) {
//                logger.info("--- MainSearcher : None entities found for req - " + betterRequest.req_num);
//            } else {
//                for (String entity: reqDocEntityList) fullQueryText.append( entity ).append(" ");
//            }
//
//            // adding task-doc entities
//            if (taskDocEntityList.size() == 0) {
//                logger.info("--- MainSearcher : None entities found for req - " + betterRequest.req_num);
//            } else {
//                for (String entity: taskDocEntityList) fullQueryText.append( entity ).append(" ");
//            }

            /** =========================================================== **/
            ArrayList<String> returnString = new ArrayList<>();
            // returnString.add(fullQueryText.toString());
            returnString.add(truncatedQText.toString());
            returnString.add(namedEntityStringStream.toString());
            return returnString;
        }
    }

    public void mainEngQueryConstruction() throws IOException, QueryNodeException {
        JSONArray outputQueryList = new JSONArray();
        JSONObject taskInfo;
        JSONArray requestList;
        JSONObject queryInfo;

        String namedEntities;
        JSONArray namedEntityTaskList = new JSONArray();
        JSONObject namedEntityTaskInfo;
        JSONArray namedEntityRequestList;
        JSONObject namedEntityRequestInfo;

        ArrayList<String> returnedStrings;
        String refactoredQuery;
        for (BETTERTask query : queries) {
            requestList = new JSONArray();
            namedEntityRequestList = new JSONArray();
            for (BETTERRequest betterRequest : query.requests) {
                // ================= Query Generator ==================
                returnedStrings = queryGenerator(betterRequest, query.task_docs);
                refactoredQuery = returnedStrings.get(0);
                namedEntities = returnedStrings.get(1);

                queryInfo = new JSONObject();
                queryInfo.put("reqQueryID", betterRequest.req_num);
                queryInfo.put("reqQueryText", refactoredQuery);
                requestList.add(queryInfo);

                namedEntityRequestInfo = new JSONObject();
                namedEntityRequestInfo.put("reqQueryID", betterRequest.req_num);
                namedEntityRequestInfo.put("reqEntities", namedEntities);
                namedEntityRequestList.add(namedEntityRequestInfo);

                this.logger.info("---- Finished generating req-query - " + betterRequest.req_num);

            }
            taskInfo = new JSONObject();
            taskInfo.put("taskID", query.task_num);
            taskInfo.put("taskRequests", requestList);
            outputQueryList.add(taskInfo);

            namedEntityTaskInfo = new JSONObject();
            namedEntityTaskInfo.put("taskID", query.task_num);
            namedEntityTaskInfo.put("taskRequests", namedEntityRequestList);
            namedEntityTaskList.add(namedEntityTaskInfo);
        }

        try (FileWriter file = new FileWriter(this.outputWriteFile)) {
            file.write(JsonWriter.formatJson(outputQueryList.toJSONString()));
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (FileWriter file = new FileWriter(this.outputWriteFile + ".entities.json")) {
            file.write(JsonWriter.formatJson(namedEntityTaskList.toJSONString()));
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException, QueryNodeException {
        args = new String[5];
        args[0] = "actual.config.properties";
        args[1] = "/Users/ramraj/better-ir/English-Turkey-run/Eval/environment-setup/English-Indexing/ENGLISH_INDEX_DIR";
        args[2] = "/Users/ramraj/better-ir/English-Turkey-run/Eval/environment-setup/QueryGeneration/turkey-run-auto-tasks.json";
        args[3] = "/Users/ramraj/better-ir/English-Turkey-run/Eval/environment-setup/QueryGeneration/enhanced-autoIR-queryGen-output/output.json";
        args[4] = "/Users/ramraj/better-ir/English-Turkey-run/Eval/environment-setup/QueryGeneration/enhanced-autoIR-queryGen-output/log.log";

        Properties prop = new Properties();
        prop.load(new FileReader(args[0]));
        EngQueryFormulatorEnhQEIR engQueryFormulatorEnhQEIR = new EngQueryFormulatorEnhQEIR(prop, args[1], args[2], args[3], args[4]);
        engQueryFormulatorEnhQEIR.mainEngQueryConstruction();
    }
}
