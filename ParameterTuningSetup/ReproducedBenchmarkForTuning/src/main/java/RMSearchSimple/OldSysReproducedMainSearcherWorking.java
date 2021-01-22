//package RMSearchSimple;
//
//import BETTERUtils.BETTERQueryParser;
//import BETTERUtils.BETTERRequest;
//import BETTERUtils.BETTERTask;
//import org.apache.lucene.analysis.Analyzer;
//import org.apache.lucene.analysis.standard.StandardAnalyzer;
//import org.apache.lucene.document.Document;
//import org.apache.lucene.index.DirectoryReader;
//import org.apache.lucene.index.IndexReader;
//import org.apache.lucene.index.MultiTerms;
//import org.apache.lucene.index.PostingsEnum;
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
//
//import java.io.File;
//import java.io.FileReader;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Properties;
//import java.util.logging.Logger;
//
//public class OldSysReproducedMainSearcherWorking {
//}
//
//
//package RMSearchSimple;
//
//
//        import BETTERUtils.BETTERRequest;
//        import BETTERUtils.BETTERTask;
//        import BETTERUtils.BETTERQueryParser;
//        import org.apache.lucene.analysis.Analyzer;
//        import org.apache.lucene.analysis.standard.StandardAnalyzer;
//        import org.apache.lucene.document.Document;
//        import org.apache.lucene.index.DirectoryReader;
//        import org.apache.lucene.index.IndexReader;
//        import org.apache.lucene.index.MultiTerms;
//        import org.apache.lucene.index.PostingsEnum;
//        import org.apache.lucene.search.IndexSearcher;
//        import org.apache.lucene.search.Query;
//        import org.apache.lucene.search.ScoreDoc;
//        import org.apache.lucene.search.TopDocs;
//        import org.apache.lucene.search.similarities.BM25Similarity;
//        import org.apache.lucene.search.similarities.LMDirichletSimilarity;
//        import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
//        import org.apache.lucene.store.Directory;
//        import org.apache.lucene.store.FSDirectory;
//        import org.apache.lucene.util.BytesRef;
//
//        import java.io.File;
//        import java.io.FileReader;
//        import java.io.FileWriter;
//        import java.io.IOException;
//        import java.util.*;
//        import java.util.logging.Logger;
//
//public class MainSearcher {
//    public Properties prop;
//    public String indexPath;
//    public String queryPath;
//    public String outputPath;
//    public String modelChoice;
//    public int modelChoiceInt;
//    public String initialRankingSimMeasure;
//
//    public Analyzer analyzer;
//    public File indexFile;
//    public IndexReader indexReader;
//    public IndexSearcher indexSearcher;
//    public String fieldToSearch;
//    public File queryFile;
//    public BETTERQueryParser betterQueryParser;
//    public List<BETTERTask> queries;
//    public int numFeedbackTerms;
//    public int numFeedbackDocs;
//    public float rm3Mix;
//    public float lMSmoothParam1;
//    public int numHits;
//    public FileWriter outputFileWriter;
//    public boolean createLabelFile;
//
//    public MainSearcher(Properties prop, String modelChoice, String indexPath, String queryPath, String outputPath) throws Exception {
//        this.prop = prop;
//        this.modelChoice = modelChoice;
//        this.indexPath = indexPath;
//        this.queryPath = queryPath;
//        this.outputPath = outputPath;
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
//        this.createLabelFile = Boolean.parseBoolean(prop.getProperty("create.lbl.file"));
//
//        // Initializing instances
//        analyzer = new StandardAnalyzer();
//        indexFile = new File(indexPath);
//        Directory indexDir = FSDirectory.open(indexFile.toPath());
//        if (!DirectoryReader.indexExists(indexDir)) {
//            System.err.println("Index Directory doesn't exists in " + indexPath);
//            System.exit(1);
//        }
//        indexReader = DirectoryReader.open(indexDir);
//        indexSearcher = new IndexSearcher(indexReader);
//        fieldToSearch = prop.getProperty("fieldToSearch", Params.FIELD_BODY);
//        queryFile = new File(this.queryPath);
//        betterQueryParser = new BETTERQueryParser(queryPath, analyzer, prop);
//        queries = constructQueries();
//
//        numFeedbackTerms = Integer.parseInt(prop.getProperty("numFeedbackTerms"));
//        numFeedbackDocs = Integer.parseInt(prop.getProperty("numFeedbackDocs"));
//
//        outputFileWriter = new FileWriter(outputPath);
//        System.out.println("Result will be stored in: " + outputPath);
//
//        numHits = Integer.parseInt(prop.getProperty("numHits","100"));
//        rm3Mix = Float.parseFloat(prop.getProperty("rm3.mix"));
//        lMSmoothParam1 = Float.parseFloat(prop.getProperty("LMSmoothParam1"));
//    }
//
//    private List<BETTERTask> constructQueries() throws Exception { // Loading the TREC format of queries
//        return betterQueryParser.queryFileParse();
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
//
//        // int docSize = betterQuery.task_docs.size();
//        // int decrementalNumDocs = 0;
//
//        ArrayList<ScoreDoc> scoreDocs = new ArrayList<>();
//        Iterator<String> it = betterTask.task_docs.iterator();
//        while(it.hasNext()){
//            String docID = it.next();
//            int LuceneID = findLuceneDocID(indexReader, Params.FIELD_ID, docID);
//            if (LuceneID == -1) {
//                System.out.println("Document with ID - " + docID + " - was not found in the Index directory !");
//                // decrementalNumDocs ++;
//                it.remove();
//                continue;
//            } else {
//                scoreDocs.add( new ScoreDoc(LuceneID, 1.0f) );
//            }
//        }
//
//        ScoreDoc[] scoreDocsArray = new ScoreDoc[scoreDocs.size()];
//        for (int i=0; i < scoreDocs.size(); i++) scoreDocsArray[i] = scoreDocs.get(i);
//
//        return scoreDocsArray;
//    }
//
//    private ScoreDoc[] createRelRequestLabelScoreDocs(BETTERRequest betterRequest) throws IOException {
//
//        // int docSize = betterQuery.task_docs.size();
//        // int decrementalNumDocs = 0;
//
//        ArrayList<ScoreDoc> scoreDocs = new ArrayList<>();
//        Iterator<String> it = betterRequest.req_docs.iterator();
//        while(it.hasNext()){
//            String docID = it.next();
//            int LuceneID = findLuceneDocID(indexReader, Params.FIELD_ID, docID);
//            if (LuceneID == -1) {
//                System.out.println("Document with ID - " + docID + " - was not found in the Index directory !");
//                // decrementalNumDocs ++;
//                it.remove();
//                continue;
//            } else {
//                scoreDocs.add( new ScoreDoc(LuceneID, 2.0f) );
//            }
//        }
//
//        ScoreDoc[] scoreDocsArray = new ScoreDoc[scoreDocs.size()];
//        for (int i=0; i < scoreDocs.size(); i++) scoreDocsArray[i] = scoreDocs.get(i);
//
//        return scoreDocsArray;
//    }
//
//    public void retrieveAndRank() throws Exception {
//        FileWriter QEWriteFile = new FileWriter("./secondary_files/QEWriteFile.txt");
//
//        FileWriter qrelsFile = new FileWriter("qrels");
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
//            // =========================================================================================================
//            // =========================================================================================================
//
//            for (BETTERRequest betterRequest : query.requests) {
//
//                ScoreDoc[] relevanceRequestScoreDocs = null;
//                if (modelChoiceInt == 3) {
//                    relevanceRequestScoreDocs = createRelRequestLabelScoreDocs(betterRequest);
//                }
//
//                Query requestLuceneQuery = betterQueryParser.getAnalyzedRequestQuery(query, betterRequest);
//                System.out.println(betterRequest.req_num + ": Query is : " + requestLuceneQuery.toString(fieldToSearch));
//
//                if (modelChoiceInt == 3) { // RM3
////                    RM3Searcherv1 rm3Searcherv1 = new RM3Searcherv1(indexReader, this);
////                    relevanceTaskScoreDocs = null;
////                    relevanceRequestScoreDocs = null;
////                    topDocs = rm3Searcherv1.search(requestLuceneQuery, relevanceTaskScoreDocs, relevanceRequestScoreDocs, numHits, betterRequest, QEWriteFile);
//                    RM3Searcherv1 rm3Searcherv1 = new RM3Searcherv1(indexReader,  Logger.getLogger("BETTERDryRun_CLIRSearcher_SOME_Log"), this);
//                    relevanceTaskScoreDocs = null;
//                    relevanceRequestScoreDocs = null;
//                    topDocs = rm3Searcherv1.search(requestLuceneQuery, numHits, betterRequest, QEWriteFile);
//
//                } else { // BM25
//                    topDocs = indexSearcher.search(requestLuceneQuery, numHits);
//                }
//
//                hits = topDocs.scoreDocs;
//                // ================================================================================
//                // ================================================================================
//                // ================================================================================
//                StringBuffer resBuffer = new StringBuffer();
//                for (int i = 0; i < hits.length; ++i) {
//                    int docId = hits[i].doc;
//                    Document d = indexSearcher.doc(docId);
//
//                    double rerankedScore = hits[i].score;
//                    String rerankedScoreString = String.format ("%.16f", rerankedScore);
//                    // String rerankedScoreString = new BigDecimal(rerankedScore).toPlainString();
//                    resBuffer.append(betterRequest.req_num).append("\tq0\t").
//                            append(d.get("UUID")).append("\t").
//                            append((i + 1)).append("\t").
//                            append(rerankedScoreString).append("\t").
//                            append("JHU.EmoryIRDryRun.Manual"+ ".1").append("\n");
//                    // append("JHU.EmoryIRDryRun.Hitl." + this.modelChoice + ".2").append("\n");
//                }
//                outputFileWriter.write(resBuffer.toString());
//                // ================================================================================
//                // ================================================================================
//                // ================================================================================
//
//                if (createLabelFile) {
//                    StringBuffer qrelsOutput = new StringBuffer();
//
//                    String queryTaskDocID;
//                    boolean foundMatch;
//                    for (int i = 0; i < query.task_docs.size(); ++i) {
//                        queryTaskDocID = query.task_docs.get(i);
//                        if (queryTaskDocID.equals("6e90a4ff-ecOb-4a1a-8e70-1f4631ea168d")) continue;
//                        if (queryTaskDocID.equals("ef629023-56d0-44be-9136-aaf283647029")) continue;
//                        String queryRequestDocID;
//                        foundMatch = false;
//                        for (int j = 0; j < betterRequest.req_docs.size(); ++j) {
//                            queryRequestDocID = betterRequest.req_docs.get(j);
//                            if (queryRequestDocID.equals(queryTaskDocID)) {
//                                qrelsOutput.append(betterRequest.req_num).
//                                        append("\tq0\t").
//                                        append(queryRequestDocID).append("\t").
//                                        append(2).append("\t").append("\n");
//                                foundMatch = true;
//                            }
//
//                            if (foundMatch) break;
//
//                        }
//                        if (!foundMatch) qrelsOutput.append(betterRequest.req_num).
//                                append("\tq0\t").
//                                append(queryTaskDocID).append("\t").
//                                append(1).append("\t").append("\n");
//                    }
//                    qrelsFile.write(qrelsOutput.toString());
//                }
////                 break; // todo
//            }
//            // =========================================================================================================
//            // =========================================================================================================
//            // =========================================================================================================
////             break; // todo
//        }
//        outputFileWriter.close(); // without closing sometimes the file contents might not be stored
//        qrelsFile.close();
//        QEWriteFile.close();
//    }
//
//
//    public static void main(String[] args) throws Exception {
//        args = new String[4];
//        args[0] = "RM3";
//        // args[1] = "/Users/ramraj/Fall-2020/project-1_better/BETTER-IR/Dockerize/Dockerfile-Folder/BETTERIndexDir";
//        args[1] = "/Users/ramraj/better-ir/English-Turkey-run/BETTERIndexDir";
//        args[2] = "./required-files/dry-run-topics.hitl.json";
//        args[3] = "RM3.hitl.txt";
//
//        Properties prop = new Properties();
//
//        if(4 != args.length) {
//            System.out.println("format is : java -jar [model_choice] [index_dir] [query_file_path] [results_filename]");
//            System.out.println("Examples : ");
//            System.out.println("java -jar auto BETTERIndexDir dry-run-topics.auto.json results.BETTER.RM3.auto.all3.txt");
//            System.exit(1);
//        }
//        prop.load(new FileReader("config.properties"));
//
//        MainSearcher mainSearcher = new MainSearcher(prop, args[0], args[1], args[2], args[3]);
//        mainSearcher.retrieveAndRank();
//    }
//}
//
