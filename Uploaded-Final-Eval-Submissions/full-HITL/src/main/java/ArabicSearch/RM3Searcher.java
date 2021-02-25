package ArabicSearch;

import Utils.BETTERRequest;
import Utils.BETTERTask;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.util.BytesRef;
import org.tartarus.snowball.ext.EnglishStemmer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class RM3Searcher extends IndexSearcher {
    double lambda;
    int expandAmount;
    int numDocs;
    long docSum;
    double mu;
    String stopFilePath;
    List stopWordsList;
    public MainSearcher mainSearcher;
    public EnglishAnalyzer englishAnalyzer;
    public EnglishStemmer englishStemmer;
    public Logger logger;

    public float weight_arabReqQuery;
    public float weight_arabQE;
    public float weight_TaskTitle;
    public float weight_TaskNarr;
    public float weight_TaskStmt;
    public RM3Searcher(IndexReader r, Logger logger, MainSearcher mainSearcher) {
        super(r);
        this.setSimilarity(new LMDirichletSimilarity());
        this.mainSearcher = mainSearcher;
        this.expandAmount = mainSearcher.numFeedbackTerms;
        this.numDocs = mainSearcher.numFeedbackDocs;
        this.lambda = mainSearcher.rm3Mix;
        this.stopFilePath = mainSearcher.prop.getProperty("stopWords.path");
        this.stopWordsList = getStopWordsList();
        this.logger = logger;

        CharArraySet englishStopWordsSet = CharArraySet.unmodifiableSet(new CharArraySet(this.stopWordsList, false));
        this.englishAnalyzer = new EnglishAnalyzer(englishStopWordsSet);
        this.englishStemmer = new EnglishStemmer();

        weight_arabReqQuery = this.mainSearcher.weight_arabReqQuery;
        weight_arabQE = this.mainSearcher.weight_arabQE;
        weight_TaskTitle = this.mainSearcher.weight_TaskTitle;
        weight_TaskNarr = this.mainSearcher.weight_TaskNarr;
        weight_TaskStmt = this.mainSearcher.weight_TaskStmt;
    }

    public List<String> getStopWordsList() {
        List<String> stopWords = new ArrayList<>();

        String line;
        try {
            FileReader fr = new FileReader(this.stopFilePath);
            BufferedReader br = new BufferedReader(fr);
            while ( (line = br.readLine()) != null )
                stopWords.add( line.trim() );
            br.close(); fr.close();
        } catch (IOException e) {
            logger.severe("Exception - RM3Searcher.java - getStopWordsList : Stop-words file not found : " + this.stopFilePath);
            System.exit(1);
        }

        return stopWords;
    }

    // @Override
    public TopDocs search(Query query, int n, BETTERTask betterTask, BETTERRequest betterRequest, Set<String> prevReqQETermsSet) throws IOException {
        int numHitsMin = this.mainSearcher.numHits;

        // Step-1 : Get top-1000 retrieved documents
        TopDocs top1000Hits = super.search(query, n) ;
        numHitsMin = top1000Hits.scoreDocs.length;

        // Step-5 : do RM3 mix & QE
        ScoreDoc[] newQEDocs = reRank(top1000Hits.scoreDocs, query, betterTask, betterRequest, numHitsMin);

        // Step-6 : Re-rank(sort) the both top-1000 + relevance documents
        List<ScoreDoc> newQEDocsArrayList = Arrays.asList(newQEDocs);
        Collections.sort(newQEDocsArrayList, new Comparator<ScoreDoc>(){
            @Override
            public int compare(ScoreDoc scoreDoc1, ScoreDoc scoreDoc2) {
                return scoreDoc1.score < scoreDoc2.score ? 1: scoreDoc1.score==scoreDoc2.score ? 0:-1;
            }
        });

        // Step-7 : Pick top-1000 documents
        List<ScoreDoc> TopNewQEDocsArrayList = newQEDocsArrayList.subList(0, numHitsMin);
        ScoreDoc[] reRankedNewScoreDocs = TopNewQEDocsArrayList.toArray(new ScoreDoc[TopNewQEDocsArrayList.size()]);

        // Put the re-ranked documents in TopDocs
        TopDocs newHits = new TopDocs(new TotalHits(numHitsMin, TotalHits.Relation.GREATER_THAN_OR_EQUAL_TO), reRankedNewScoreDocs);

        return newHits;
    }

    public HashMap<String, Double> calculateFieldTermScores(ArrayList<String> queryTermString) {
        HashMap<String, Double> queryCounts = new HashMap<String, Double>();
        for(String query: queryTermString){ // get query level counts
            if(queryCounts.containsKey(query)){
                queryCounts.put(query, queryCounts.get(query) + 1);
            }
            else{
                queryCounts.put(query, 1.0);
            }
        }
        int hashSize = queryCounts.size();
        for(String query: queryTermString){ // get query level probabilities
            Double count = queryCounts.get(query) / hashSize;
            queryCounts.put(query, count);
        }
        return queryCounts;
    }

    public ScoreDoc[] reRank(ScoreDoc[] initialResults, Query q, BETTERTask bettermTedTask, BETTERRequest betterRequest, int numHitsMin) throws IOException {

        /** ============================= Getting score for query terms ============================= **/
        ArrayList<String> qTerms = getRawQueryString(q);
        HashMap<String, Double> arabReqQuery_QueryCounts = calculateFieldTermScores(qTerms);

        /** ============================= Getting score for other field terms ============================= **/
        String[] tmpStringArray;
        tmpStringArray = bettermTedTask.task_title.split(" ");
        List<String> tmpStringArrayList1 = Arrays.<String>asList(tmpStringArray);
        ArrayList<String> taskTitleTermsList = new ArrayList<String>(tmpStringArrayList1);
        HashMap<String, Double> taskTitle_QueryCounts = calculateFieldTermScores(taskTitleTermsList);

        tmpStringArray = bettermTedTask.task_stmt.split(" ");
        List<String> tmpStringArrayList2 = Arrays.<String>asList(tmpStringArray);
        ArrayList<String> taskStmtTermsList = new ArrayList<String>(tmpStringArrayList2);
        HashMap<String, Double> taskStmt_QueryCounts = calculateFieldTermScores(taskStmtTermsList);

        tmpStringArray = bettermTedTask.task_narr.split(" ");
        List<String> tmpStringArrayList3 = Arrays.<String>asList(tmpStringArray);
        ArrayList<String> taskNarrTermsList = new ArrayList<String>(tmpStringArrayList3);
        HashMap<String, Double> taskNarr_QueryCounts = calculateFieldTermScores(taskNarrTermsList);

        Set<String> stitchedQueryTermsSet = new HashSet<>();
        stitchedQueryTermsSet.addAll(qTerms);
        stitchedQueryTermsSet.addAll(taskTitleTermsList);
        stitchedQueryTermsSet.addAll(taskStmtTermsList);
        stitchedQueryTermsSet.addAll(taskNarrTermsList);

        /** ============================= Arabic-RM3 QE term scoring ============================= **/
        HashMap<String,Long> globalWordCount = globalWordCount(initialResults, betterRequest.expanded_text);
//        HashMap<String,Long> globalWordCount = globalWordCount(initialResults, numHitsMin); //generate an exaustive list of inidividual word counts
        this.setRelevantDocSum(globalWordCount);
        HashMap<String,Double> scoredWords =  scoreWords(initialResults, q, globalWordCount, numHitsMin); //generate word probabilities.

        /** ============================= English's fields + QE terms scoring ============================= **/
        //

        /** ============================= RM3 mixing ============================= **/
        /**
         * value = Arb(a1 . reqQ + a2. arabicQE) + Eng( [a3.task-title + a4.task-stmt + a5.task-narr] + [a6.QEofCurrReq + a7.QEofPrevReq] )
         *
         * reqQ + [a3.task-title + a4.task-stmt + a5.task-narr] ++ arabicQE + [a6.QEofCurrReq + a7.QEofPrevReq]
         */
        Double taskTitleVal = 0.0;
        Double taskStmtVal = 0.0;
        Double taskNarrVal = 0.0;
        Double arabReqTextQueryVal = 0.0;
        Double stringCombVal = 0.0;
        for(String query: stitchedQueryTermsSet){
            if (taskTitleTermsList.contains(query)) taskTitleVal = taskTitle_QueryCounts.get(query);
            if (taskStmtTermsList.contains(query)) taskStmtVal = taskStmt_QueryCounts.get(query);
            if (taskNarrTermsList.contains(query)) taskNarrVal = taskNarr_QueryCounts.get(query);
            if (qTerms.contains(query)) arabReqTextQueryVal = arabReqQuery_QueryCounts.get(query);

            stringCombVal = (weight_TaskTitle * taskTitleVal) + (weight_TaskStmt * taskStmtVal) + (weight_TaskNarr * taskNarrVal) + (weight_arabReqQuery * arabReqTextQueryVal);

            if(scoredWords.containsKey(query)){
                Double value = ( 1 - lambda) * stringCombVal + lambda * scoredWords.get(query);
                scoredWords.put(query, value);
            }
        }

        /** ============================= RM3 end-steps ============================= **/

        /** ====================================================================================================== **/
        /** ============v2 - use StandardAnalyzer - BEFORE PROVIDING TOTAL TERMS FOR QE-SORT-PICKING, LETS REMOVE STOP WORDS ================= **/
        /** ====================================================================================================== **/
        // // Method-1
        // String dump = QETermsAnalyze(scoredWords);
        /** ====================================================================================================== **/
        /** ============BEFORE PROVIDING TOTAL TERMS FOR QE-SORT-PICKING, LETS REMOVE STOP WORDS ================= **/
        /** ====================================================================================================== **/
        Iterator it = scoredWords.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry mapEntry = (Map.Entry)it.next();
            if (stopWordsList.contains(mapEntry.getKey())) {
                it.remove();
            }
        }
        /** ====================================================================================================== **/
        /** ====================================================================================================== **/
        /** ====================================================================================================== **/

        ArrayList<String> wordCandidate =  getWordCandidates(scoredWords); //based on  word probabilities sort a list of words

        // Writing QE-Terms in write file.
        StringBuffer QEBuffer = new StringBuffer();

        QEBuffer.append(betterRequest.req_num);
        int cntTopTerms = 0;
        for (String w : wordCandidate) {
            QEBuffer.append(" ").append(w);
            cntTopTerms ++;
            if (cntTopTerms >= this.expandAmount) break;
        }
        QEBuffer.append("\n");
//        this.mainSearcher.QEWriteFile.write(QEBuffer.toString());

        /** ====================================================================================================== **/
        /** ====================================================================================================== **/
        /** ====================================================================================================== **/

        Query expQuery = expandQuery(wordCandidate,q); // uses the top n word candidates to  do search

        ArrayList<String> queryTerms = getRawQueryString(expQuery); // This is just a utility method that let's me get the list of words in a query.

        float[] rankingScores = new float[initialResults.length];
        for(int i=0; i < rankingScores.length; i ++){
            rankingScores[i] = generateScore(initialResults[i].doc, this.getIndexReader(), queryTerms, globalWordCount);
            initialResults[i].score  *= rankingScores[i]; // weight each score by the original model score.
        }
        return  initialResults;
    }

    private float generateScore(int docID, IndexReader indexReader, ArrayList<String> queryTerms, HashMap<String, Long> counts) {
        // Calculate the new ranking score for document (docID) given the expanded query
        Terms t;
        long totalFreq;
        try {
            t = indexReader.getTermVector(docID, this.mainSearcher.prop.getProperty("FIELD_TOKENIZED_CONTENT"));
//            t = indexReader.getTermVector(docID, this.mainSearcher.prop.getProperty("FIELD_BODY"));
            totalFreq = t.getSumTotalTermFreq();
        }catch (IOException e ){
            logger.severe("Exception : generateScore - RM3Searcherv1.java : Something happened");
            return 0;
        }
        ArrayList<TermWrapper> docTerms = getTerms(t, indexReader);

        float cumProduct= 0;
        int i;
        double globalWordProb;
        double wordFreq;

        this.mu =  this.mu* 1.1;
        double lambda =   this.mu /( totalFreq + this.mu);

        for(String e : queryTerms){
            i = docTerms.indexOf(new TermWrapper(e,0) ); // the query word is in the document
            if( i >= 0 ){
                wordFreq= (double) docTerms.get(i).freq;
            }else{
                wordFreq = 0.0; // word does not occur in document
            }

            if( counts.get(e) != null){
                globalWordProb = (double) counts.get(e) / this.docSum;
            }else{
                globalWordProb  =  0.0; // word does not occur in relevant corpus
            }

            cumProduct +=  Math.log( lambda * ( wordFreq/totalFreq ) + (1-lambda) * (globalWordProb) + 1 ); //add plus one to avoid taking the log of 0. which will return nan.
        }
        return  (float) Math.exp(cumProduct);
    }

    private ArrayList<String> getRawQueryString(Query q) {
        //query string format is .  field:word  field:word  this func basically pulls out the word
        ArrayList<String>  output = new ArrayList<String>();
        String queryString = q.toString();
        String[] words = queryString.split(" ");
        for(String e : words){
            String[] contentSplit = e.split(":");
            try {
                output.add(contentSplit[1]);
            } catch (Exception exception) {
                continue;
            }
        }
        return output;
    }

    private Query expandQuery(ArrayList<String> wordCandidate, Query q) {
        ArrayList<String> queryWords = getRawQueryString(q);
        String queryString ="";
        for(String e: queryWords){ // add original query terms
            queryString += " " + e ;
        }
        for(int i =0; i <this.expandAmount; i++){
            queryString += " " + wordCandidate.get(i);  // add new query terms
        }
        QueryParser myQParser = new QueryParser("TEXT", new StandardAnalyzer());
        try{
            Query myQuery = myQParser.parse(QueryParser.escape(queryString));
            return myQuery;
        }catch(ParseException e){
            logger.warning("Exception : expandQuery - RM3Searcher.java : Couldn't parse returning original query");
            return q;
        }
    }

    private ArrayList<String> getWordCandidates(HashMap<String, Double> counts) {
        // Given a map of word to probabilities we create a list of all our words and sort them by their corresponding
        // probability. Note output is in decreasing order
        ArrayList<String> elements = new ArrayList<String>();
        elements.addAll(counts.keySet());
        elements.sort(Comparator.comparingDouble(s-> counts.get(s ) ));
        Collections.reverse(elements);
        return elements ;
    }

    private HashMap<String, Double> scoreWords(ScoreDoc[] hits, Query q, HashMap<String, Long> globalWordCount, int numHitsMin) {
        // Calculate P(t|d)*p(q|d)  for every document
        // Create a hash map   we may words  to P(t|d)*p(q|d)
        HashMap<String,Double> newScores = new HashMap<String,Double>();
        IndexReader reader = this.getIndexReader();
        ScoreDoc hit;
        // compute the denominator //calculate probs on a per given document updatind the numerator
        for ( int i =0;  i < numHitsMin; i ++){ //only iterate through the relevant document set not the corpus
            hit = hits[i];
            try {
                Terms t = reader.getTermVector(hit.doc, this.mainSearcher.prop.getProperty("FIELD_TOKENIZED_CONTENT"));
//                Terms t = reader.getTermVector(hit.doc, this.mainSearcher.prop.getProperty("FIELD_BODY"));
                long totalFreq = t.getSumTotalTermFreq(); // Number of words in a particular documents
                //double lambda = ( this.mu)/( totalFreq + this.mu);
                ArrayList<TermWrapper> docTerms = getTerms(t, reader);
                double q_given_D = 1;  // double check this // pre-compute p(q|D). previous search models score is this value
                q_given_D = hit.score;

                for(TermWrapper e: docTerms){ //calculate p(t|d)*p(q|d)
                    double termProb = ( (double) e.freq / totalFreq );
                    termProb *= q_given_D;
                    Double prevValue = newScores.get(e.text); //get previous value of a word
                    if (prevValue != null){
                        newScores.put(e.text, prevValue + termProb );
                    }else{
                        // prevValue = 0.0;
                        newScores.put(e.text, termProb); // HHHHHHHHH
                    }
                }
            }catch (IOException e ){
                logger.severe("Exception : scoreWords - RM3Searcherv1.java : Something happened");
                System.exit(1);
            }
        }
        //in this section we normalize our values
        double denominator = 0.0;
        for(String e : newScores.keySet()){
            denominator += newScores.get(e);
        }
        for(String e: newScores.keySet()){
            newScores.put(e,newScores.get(e )/denominator);
        }
        return newScores;
    }

    public ArrayList<TermWrapper> getTerms(Terms terms, IndexReader reader){
        TermsEnum a;
        TermWrapper t ;
        BytesRef term ;
        long termFreq;
        ArrayList<TermWrapper> output = new ArrayList<TermWrapper>();
        try{
            a =terms.iterator();
            while( ( term=a.next())!= null ){
                termFreq =  a.totalTermFreq();
                t = new TermWrapper(term.utf8ToString(), termFreq );
                output.add(t);
            }
        }catch (IOException e){
            logger.severe("Exception : getTerms - RM3Searcher.java : messed up with iterator in getTerms");
            System.exit(1);
        }
        return output;
    }

    private HashMap<String, Long> globalWordCount(ScoreDoc[] hits, String expanded_text) throws IOException {
        IndexReader reader = this.getIndexReader();
        HashMap<String,Long> counts = new HashMap<String,Long>();
        double mu = 0.0;
//        String[] expanded_text_list = expanded_text.split(" ");
//        for(int i=0; i < this.numDocs; i++){
//            Terms a  = reader.getTermVector(hits[i].doc, this.mainSearcher.prop.getProperty("FIELD_TOKENIZED_CONTENT"));
////            Terms a  = reader.getTermVector(hits[i].doc, this.mainSearcher.prop.getProperty("FIELD_BODY"));
//            mu += a.getSumTotalTermFreq();
//            TermsEnum iter = a.iterator();
//            BytesRef term = iter.next();
//            while(term != null){
//                if (term.equals()) {
//
//                }
//                String s = term.utf8ToString();
//                if(counts.get(s) == null){
//                    counts.put(s, iter.totalTermFreq());
//                }else{
//                    counts.put(s, iter.totalTermFreq() + counts.get(s));
//                }
//                term = iter.next();
//            }
//        }

        String[] termStrings;
        termStrings = expanded_text.split(" ");

        HashMap<String,Long> termCounts = new HashMap<String,Long>();
        for(String term: termStrings){ // get query level counts
            if(termCounts.containsKey(term)){
                termCounts.put(term, termCounts.get(term) + 1);
            }
            else{
                termCounts.put(term, Long.valueOf(1));
            }
        }
        int hashSize = termCounts.size();
        mu += termCounts.size();
        counts.putAll(termCounts);
        this.mu  = 1 * (mu/this.numDocs);
        return counts;
    }

//    private HashMap<String, Long> globalWordCount(ScoreDoc[] hits, int numHitsMin) throws IOException {
//        IndexReader reader = this.getIndexReader();
//        HashMap<String,Long> counts = new HashMap<String,Long>();
//        double mu = 0.0;
//        for(int i=0; i < numHitsMin; i++){
//            Terms a  = reader.getTermVector(hits[i].doc, this.mainSearcher.prop.getProperty("FIELD_TOKENIZED_CONTENT"));
////            Terms a  = reader.getTermVector(hits[i].doc, this.mainSearcher.prop.getProperty("FIELD_BODY"));
//            mu += a.getSumTotalTermFreq();
//            TermsEnum iter = a.iterator();
//            BytesRef term = iter.next();
//            while(term != null){
//                String s = term.utf8ToString();
//                if(counts.get(s) == null){
//                    counts.put(s, iter.totalTermFreq());
//                }else{
//                    counts.put(s, iter.totalTermFreq() + counts.get(s));
//                }
//                term = iter.next();
//            }
//        }
//        this.mu  = 1 * (mu/numHitsMin);
//        return counts;
//    }

    private HashMap<String, Long> localPositiveFeedbackWordCount(ScoreDoc[] hits, int numHitsMin) throws IOException {
        IndexReader reader = this.getIndexReader();
        HashMap<String,Long> counts = new HashMap<String,Long>();
        double mu = 0.0;
        for(int i=0; i < numHitsMin; i++){
            Terms a  = reader.getTermVector(hits[i].doc, this.mainSearcher.prop.getProperty("FIELD_TOKENIZED_CONTENT"));
//            Terms a  = reader.getTermVector(hits[i].doc, this.mainSearcher.prop.getProperty("FIELD_BODY"));
            mu += a.getSumTotalTermFreq();
            TermsEnum iter = a.iterator();
            BytesRef term = iter.next();
            while(term != null){
                String s = term.utf8ToString();
                if(counts.get(s) == null){
                    counts.put(s, iter.totalTermFreq());
                }else{
                    counts.put(s, iter.totalTermFreq() + counts.get(s));
                }
                term = iter.next();
            }
        }
        this.mu  = 1 * (mu/numHitsMin);
        return counts;
    }

    private void setRelevantDocSum(HashMap<String, Long> globalWordCount) {
        // Just sum the number of occurence of words. This gives us the total sum in our collection.
        long accumulator = 0;
        for(long e: globalWordCount.values()){
            accumulator +=e;
        }
        this.docSum = accumulator;
    }
}
