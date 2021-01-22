package BETTERUtils;

import RMSearchSimple.Params;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class BETTERQueryParser {

    public StringBuffer            sbuff;
    public String                  fieldToSearch;

    public Analyzer analyzer;
    public StandardQueryParser queryParser;
    public String                  queryFilePath;

    public BETTERTask query;
    public List<BETTERTask> queries;

    public BETTERRequest requestStream;

    public Properties prop;

    public HashMap<String, Integer> manualHITLAnnoations;

    /**
     * Overloading Constructor-1: only parse query filename
     * @param queryFilePath
     */
    public BETTERQueryParser(String queryFilePath, Properties prop) {
        this.sbuff = new StringBuffer();
        this.fieldToSearch = Params.FIELD_BODY;

        this.analyzer = new StandardAnalyzer();
        this.queryParser = new StandardQueryParser();
        this.queryFilePath = queryFilePath;

        this.queries = new LinkedList<>(); // Default declaration of List is LinkedList

        this.prop = prop;
    }

    /**
     * Overloading Constructor-2: besides query filename parses Analyzer as well
     * @param queryFilePath
     * @param analyzer
     */
    public BETTERQueryParser(String queryFilePath, Analyzer analyzer,  Properties prop) {
        this.sbuff = new StringBuffer();
        this.fieldToSearch = Params.FIELD_BODY;

        this.analyzer = analyzer;
        this.queryParser = new StandardQueryParser(this.analyzer);
        this.queryFilePath = queryFilePath;

        this.queries = new LinkedList<>();

        this.prop = prop;
    }

    /**
     * Overloading Constructor-3: besides query filename & Analyzer, specifies the search-field as well.
     * @param queryFilePath
     * @param analyzer
     * @param fieldToSearch
     */
    public BETTERQueryParser(String queryFilePath, Analyzer analyzer, String fieldToSearch,  Properties prop) {
        this.sbuff = new StringBuffer();
        this.fieldToSearch = fieldToSearch;

        this.analyzer = analyzer;
        this.queryParser = new StandardQueryParser(analyzer);
        this.queryFilePath = queryFilePath;

        this.queries = new LinkedList<>();

        this.prop = prop;
    }

    private void parseDocumentObject(JSONObject document) {
        query = new BETTERTask();

        String task_num = (String) document.get("task-num");
        query.task_num = task_num;
        // System.out.println(task_num);

        String task_title = (String) document.get("task-title");
        query.task_title = task_title;
        // System.out.println(task_title);

        String task_stmt = (String) document.get("task-stmt");
        query.task_stmt = task_stmt;
        // System.out.println(task_stmt);

        String task_link = (String) document.get("task-link");
        query.task_link = task_link;
        // System.out.println(task_link);

        String task_narr = (String) document.get("task-narr");
        query.task_narr = task_narr;
        // System.out.println(task_narr);

        String task_in_scope = (String) document.get("task-in-scope");
        query.task_in_scope = task_in_scope;
        // System.out.println(task_in_scope);

        String task_not_in_scope = (String) document.get("task-not-in-scope");
        query.task_not_in_scope = task_not_in_scope;
        // System.out.println(task_not_in_scope);

        query.task_docs = (ArrayList<String>) document.get("task-docs"); // This variables stores basically the labels(ground-truth) for the model


        // =============================== LOADING THE REQUESTS STREAMS ===============================




        // ArrayList<String> requestNums = new ArrayList<>();
        // ArrayList<String> requestTexts = new ArrayList<>();
        // ArrayList<String> requestDoc;

        // List<ArrayList<String>> requestDocs = new ArrayList<ArrayList<String>>();
        List<BETTERRequest> betterRequests = new LinkedList<>();
        List<JSONObject> requestsList = (List<JSONObject>) document.get("requests");
        for (JSONObject request : requestsList) {
            requestStream = new BETTERRequest();
            requestStream.req_num = (String) request.get("req-num");
            requestStream.req_text = (String) request.get("req-text");
            requestStream.req_docs = (ArrayList<String>) request.get("req-docs");
            // query.requests = requestStream;
            betterRequests.add(requestStream);
            // requestNums.add((String) request.get("req-num"));
            // requestTexts.add((String) request.get("req-text"));
            // requestDoc = (ArrayList<String>) request.get("req-docs");
            // requestDocs.add(requestDoc);
        }
        // for (ArrayList<String> e : requestDocs) System.out.println(e);
        query.requests = betterRequests;
        // for (ArrayList<String> e : requestDocs) System.out.println(e);

        queries.add(query);
    }

    public List<BETTERTask> queryFileParse() {

        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader(this.queryFilePath)) {
            Object obj = jsonParser.parse(reader);

            JSONArray documentList = (JSONArray) obj;
            // System.out.println(documentList);

            documentList.forEach( document -> parseDocumentObject( (JSONObject) document ) );

            return queries;

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Query getAnalyzedRequestQuery(BETTERTask betterTask, BETTERRequest betterRequest) throws QueryNodeException {

        String queryPart = prop.getProperty("query.part");

        String qPrefixString = null;
        switch (queryPart) {
            case "title": qPrefixString = betterTask.task_title; break;
            case "stmt":  qPrefixString = betterTask.task_stmt; break;
            case "narr":  qPrefixString = betterTask.task_narr; break;
            case "all3":  qPrefixString = betterTask.task_title + " " + betterTask.task_stmt + " " + betterTask.task_narr; break;
            default: throw new IllegalArgumentException("Invalid Query Part : " + queryPart + " Provided !");
        }

        String qString = betterRequest.req_text + qPrefixString;

        // Pre-process the query string
        qString =   qString.replaceAll("/", " ").
                replaceAll("\\?", " ").
                replaceAll("\"", " ").
                replaceAll("\\&", " ");
        Query luceneRequestQuery = queryParser.parse(qString, fieldToSearch);
        betterRequest.requestLuceneQuery = luceneRequestQuery; // add the Query term back into the BETTER Query object itself
        return luceneRequestQuery; // but return only the Query-term
    }

    public Query getAnalyzedQuery(BETTERTask betterTask) throws QueryNodeException {

        // Choose the field we want from Queries
        String queryPart = prop.getProperty("query.part");
        String qString = null;
        switch (queryPart) {
            case "title": qString = betterTask.task_title; break;
            case "stmt":  qString = betterTask.task_stmt; break;
            case "narr":  qString = betterTask.task_narr; break;
            case "all3":  qString = betterTask.task_title + " " + betterTask.task_stmt + " " + betterTask.task_narr; break;
            default: throw new IllegalArgumentException("Invalid Query Part : " + queryPart + " Provided !");
        }

        // Pre-process the query string
        qString =   qString.replaceAll("/", " ").
                replaceAll("\\?", " ").
                replaceAll("\"", " ").
                replaceAll("\\&", " ");
        Query luceneQuery = queryParser.parse(qString, fieldToSearch);
        betterTask.luceneQuery = luceneQuery; // add the Query term back into the BETTER Query object itself
        return luceneQuery; // but return only the Query-term
    }

    public Query getAnalyzedQuery(String queryString) {
        Query luceneQuery = new TermQuery(new Term(fieldToSearch, queryString)); // queryString --> Term --> TermQuery --> Query
        return luceneQuery;
    }


    public static void main(String[] args) throws Exception {
        String sampleBETTERQueryRequestFile = "dry-run-topics.hitl.json";
        String manualHITLAnnotatinsFile = "";

        Properties prop = new Properties();
        prop.load(new FileReader("config.properties"));

        Analyzer tmpAnalyzer = new StandardAnalyzer();
        BETTERQueryParser simpleBETTERQueryParser = new BETTERQueryParser(sampleBETTERQueryRequestFile, tmpAnalyzer, prop);
        simpleBETTERQueryParser.queryFileParse();

        int countRelDocs = 0;

        for (BETTERTask query : simpleBETTERQueryParser.queries) { // queries : is the List<TRECQuery>
            // System.out.println( "ID : " + query.task_num );
            // System.out.println( "Title : " + query.task_title );
//            System.out.println( "HITL Documents : " + query.task_docs);
//
//            // But the LuceneQuery of the input 'title' query wouldn't be created by default, so let's create
//            Query luceneQuery;
//            luceneQuery = simpleBETTERQueryParser.getAnalyzedQuery(query);
//            System.out.println( "Lucene Query : " + luceneQuery ); //LuceneQuery is a Query object, so apply toString() to print the String
//            System.out.println( "Lucene Query : " + luceneQuery.toString( simpleBETTERQueryParser.fieldToSearch ) ); // this toString() is an override method
//
//            // Tokenize the 'title' and then print it
//            System.out.println( "Tokenized Query String : " + query.queryFieldAnalyze(tmpAnalyzer, query.task_title));
//
//            System.out.println( "Tasks Documents  :");
//            for (String task_doc : query.task_docs) System.out.println(task_doc);

            // System.out.println("Requests : " + query.requests);
            for (BETTERRequest betterRequest : query.requests) {
                System.out.println(betterRequest.req_num + " : with num rel docs : " + betterRequest.req_docs.size());
                countRelDocs += query.task_docs.size();
            }

            // countRelDocs += query.task_docs.size();


            // break;
        }

        System.out.println(countRelDocs);
    }

}
