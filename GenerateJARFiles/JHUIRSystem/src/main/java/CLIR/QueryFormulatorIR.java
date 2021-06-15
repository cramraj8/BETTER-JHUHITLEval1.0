//package CLIR;
//
//import BETTERUtils.BETTERQueryParserEval;
//import BETTERUtils.BETTERRequest;
//import BETTERUtils.BETTERTask;
//import com.cedarsoftware.util.io.JsonWriter;
//import edu.stanford.nlp.pipeline.CoreDocument;
//import edu.stanford.nlp.pipeline.CoreEntityMention;
//import edu.stanford.nlp.pipeline.StanfordCoreNLP;
//import org.apache.log4j.PropertyConfigurator;
//import org.apache.lucene.document.Document;
//import org.apache.lucene.index.DirectoryReader;
//import org.apache.lucene.index.IndexReader;
//import org.apache.lucene.index.MultiTerms;
//import org.apache.lucene.index.PostingsEnum;
//import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
//import org.apache.lucene.store.Directory;
//import org.apache.lucene.store.FSDirectory;
//import org.apache.lucene.util.BytesRef;
//import org.json.simple.JSONArray;
//import org.json.simple.JSONObject;
//import java.io.File;
//import java.io.FileReader;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Properties;
//import java.util.logging.FileHandler;
//import java.util.logging.Logger;
//import java.util.logging.SimpleFormatter;
//
//public class QueryFormulatorIR {
//    public Properties prop;
//    public Logger logger;
//
//    public String queryFilename;
//    public BETTERQueryParserEval betterQueryParserEval;
//    public List<BETTERTask> queries;
//    public ArrayList<String> entityMentions;
//
//    public StanfordCoreNLP pipeline;
//    public Properties coreNLPprops;
//    public CoreDocument doc;
//
//    public IndexReader indexReader;
//    public File indexFile;
//
//    public String outputWriteFile;
//
//    private List<BETTERTask> constructQueries() { // Loading the TREC format of queries
//        return betterQueryParserEval.queryFileParse();
//    }
//
//    public QueryFormulatorIR(Properties prop, String indexDirPath, String queryFilename, String outputWriteFile, String searcherLogFilename) throws IOException {
//        this.prop = prop;
//        this.logger = Logger.getLogger("BETTERDryRun_CLIRQueryFormulator_Log");
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
//        this.queryFilename = queryFilename;
//        this.betterQueryParserEval = new BETTERQueryParserEval(this.prop, queryFilename, this.logger);
//        this.queries = constructQueries();
//
//        String log4jConfPath = prop.getProperty("log4jConfPath");
//        PropertyConfigurator.configure(log4jConfPath);
//        this.coreNLPprops = new Properties();
//        this.coreNLPprops.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
//        this.pipeline = new StanfordCoreNLP(this.coreNLPprops);
//
//        this.indexFile = new File(indexDirPath);
//        Directory indexDir = FSDirectory.open(indexFile.toPath());
//        if (!DirectoryReader.indexExists(indexDir)) {
//            logger.severe("Exception - MainSearcher.java - Index Folder is empty.");
//            System.exit(1);
//        }
//        this.indexReader = DirectoryReader.open(indexDir);
//        this.outputWriteFile = outputWriteFile;
//    }
//
//    public ArrayList<String> extractEntitiesFromDocumentText(String docID, String textData) {
//        // ================================== extract entities
//        // System.out.println("Get inside method");
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
//        // System.out.println(entityMentions);
//
//        return entityMentions;
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
//    public Document findDocumentContent(String docID) throws IOException {
//        int internalLuceneID = findLuceneDocID(this.indexReader, prop.getProperty("FIELD_ID"), docID);
//        if (internalLuceneID == -1) return null;
//        else return this.indexReader.document(internalLuceneID);
//    }
//
//    private String queryGenerator(BETTERRequest betterRequest, ArrayList<String> taskDocs) throws IOException {
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
//            // System.out.println(docID);
//            documentContent = findDocumentContent(docID);
//            if (documentContent == null) continue;
//            // System.out.println(documentContent);
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
//        return potentialQueryText.toString().replaceAll("[^a-zA-Z0-9]", " ");
//    }
//
//    public void constructQuery() throws IOException {
//        JSONArray outputQueryList = new JSONArray();
//        JSONObject taskInfo;
//        JSONArray requestList;
//        JSONObject queryInfo;
//
//        for (BETTERTask query : queries) {
//            requestList = new JSONArray();
//            for (BETTERRequest betterRequest : query.requests) {
//                // ================= Query Generator ==================
//                String refactoredQuery = queryGenerator(betterRequest, query.task_docs);
//                queryInfo = new JSONObject();
//                queryInfo.put("reqQueryID", betterRequest.req_num);
//                queryInfo.put("reqQueryText", refactoredQuery);
//                requestList.add(queryInfo);
//            }
//            taskInfo = new JSONObject();
//            taskInfo.put("taskID", query.task_num);
//            taskInfo.put("taskRequests", requestList);
//            outputQueryList.add(taskInfo);
//        }
//
//        try (FileWriter file = new FileWriter(this.outputWriteFile)) {
//            file.write(JsonWriter.formatJson(outputQueryList.toJSONString()));
//            file.flush();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static void main(String[] args) throws IOException, QueryNodeException {
////        Properties prop = new Properties();
////        prop.load(new FileReader("CLIR.config.properties"));
////        QueryFormulatorIR queryFormulatorIR = new QueryFormulatorIR(prop,
////                                                            prop.getProperty("IndexDir"),
////                                                            // prop.getProperty("testAutoQueryFile"),
////                "/Users/ramraj/better-ir/English-Turkey-run/building-docker-English-Arabic/shared-space/hitl-example-one-task.json",
////                                                            "./ramraj-EnglishQuery.json",
////                                                            prop.getProperty("CLIRQueryFormulatorLogFilename"));
////        queryFormulatorIR.constructQuery();
//
//
//        // args[0] - config.properties
//        // args[1] - IndexDir
//        // args[2] - Input English Query file
//        // args[3] - Output formulated Query file
//        // args[4] - Query Formulator logger
//        Properties prop = new Properties();
//        prop.load(new FileReader(args[0]));
//        QueryFormulatorIR queryFormulatorIR = new QueryFormulatorIR(prop, args[1], args[2], args[3], args[4]);
//        queryFormulatorIR.constructQuery();
//    }
//}
