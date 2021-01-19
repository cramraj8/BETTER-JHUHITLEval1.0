package BETTERCLHITLSearcherQEEnh;

import BETTERMTedUtils.BETTERMTedRequest;
import BETTERMTedUtils.BETTERMTedTask;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
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
import java.io.StringReader;
import java.util.*;
import java.util.logging.Logger;

public class RM3Searcherv1 extends IndexSearcher {
    double lambda;
    int expandAmount;
    int numDocs;
    long docSum;
    double mu;
    String stopFilePath;
    List stopWordsList;
    public MainSearcherEnh mainSearcherEnh;
    public EnglishAnalyzer englishAnalyzer;
    public EnglishStemmer englishStemmer;
    public Logger logger;

    public float weight_arabReqQuery;
    public float weight_arabQE;
    public float weight_TaskTitle;
    public float weight_TaskNarr;
    public float weight_TaskStmt;
    public RM3Searcherv1(IndexReader r, Logger logger, MainSearcherEnh mainSearcherEnh) {
        super(r);
        this.setSimilarity(new LMDirichletSimilarity());
        this.mainSearcherEnh = mainSearcherEnh;
        this.expandAmount = mainSearcherEnh.numFeedbackTerms;
        this.numDocs = mainSearcherEnh.numFeedbackDocs;
        this.lambda = mainSearcherEnh.rm3Mix;
        this.stopFilePath = mainSearcherEnh.prop.getProperty("stopWords.path");
        this.stopWordsList = getStopWordsList();
        this.logger = logger;

        CharArraySet englishStopWordsSet = CharArraySet.unmodifiableSet(new CharArraySet(this.stopWordsList, false));
        this.englishAnalyzer = new EnglishAnalyzer(englishStopWordsSet);
        this.englishStemmer = new EnglishStemmer();

        weight_arabReqQuery = this.mainSearcherEnh.weight_arabReqQuery;
        weight_arabQE = this.mainSearcherEnh.weight_arabQE;
        weight_TaskTitle = this.mainSearcherEnh.weight_TaskTitle;
        weight_TaskNarr = this.mainSearcherEnh.weight_TaskNarr;
        weight_TaskStmt = this.mainSearcherEnh.weight_TaskStmt;
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
            logger.severe("Exception - RM3Searcherv1.java - getStopWordsList : Stop-words file not found : " + this.stopFilePath);
            System.exit(1);
        }

        return stopWords;
    }

    // @Override
    public TopDocs search(Query query, int n, BETTERMTedTask betterTask, BETTERMTedRequest betterRequest, Set<String> prevReqQETermsSet) throws IOException {

        // Step-1 : Get top-1000 retrieved documents
        TopDocs top1000Hits = super.search(query, n) ;

        // Step-5 : do RM3 mix & QE
        ScoreDoc[] newQEDocs = reRank(top1000Hits.scoreDocs, query, betterTask, betterRequest);

        // Step-6 : Re-rank(sort) the both top-1000 + relevance documents
        List<ScoreDoc> newQEDocsArrayList = Arrays.asList(newQEDocs);
        Collections.sort(newQEDocsArrayList, new Comparator<ScoreDoc>(){
            @Override
            public int compare(ScoreDoc scoreDoc1, ScoreDoc scoreDoc2) {
                return scoreDoc1.score < scoreDoc2.score ? 1: scoreDoc1.score==scoreDoc2.score ? 0:-1;
            }
        });

        // Step-7 : Pick top-1000 documents
        List<ScoreDoc> TopNewQEDocsArrayList = newQEDocsArrayList.subList(0, this.mainSearcherEnh.numHits);
        ScoreDoc[] reRankedNewScoreDocs = TopNewQEDocsArrayList.toArray(new ScoreDoc[TopNewQEDocsArrayList.size()]);

        // Put the re-ranked documents in TopDocs
        TopDocs newHits = new TopDocs(new TotalHits(this.mainSearcherEnh.numHits, TotalHits.Relation.GREATER_THAN_OR_EQUAL_TO), reRankedNewScoreDocs);

        return newHits;
    }

    private String QETermsAnalyze(HashMap<String, Double> wordsMap) throws IOException {
        Set<String> QRWords = wordsMap.keySet();
        String QRWordsFlatString = "";
        for (String e : QRWords) {
            QRWordsFlatString += e + " ";
        }

        StringBuffer localBuff = new StringBuffer();
        TokenStream stream = englishAnalyzer.tokenStream(this.mainSearcherEnh.fieldToSearch, new StringReader(QRWordsFlatString));
        CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
        stream.reset();
        while (stream.incrementToken()) {
            String term = termAtt.toString();
            term = term.toLowerCase();
            localBuff.append(term).append(" ");
        }
        stream.end();
        stream.close();
        return localBuff.toString();
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

    public ScoreDoc[] reRank(ScoreDoc[] initialResults, Query q, BETTERMTedTask bettermTedTask, BETTERMTedRequest betterRequest) throws IOException {

        /** ============================= Getting score for query terms ============================= **/
        ArrayList<String> qTerms = getRawQueryString(q);
        HashMap<String, Double> arabReqQuery_QueryCounts = calculateFieldTermScores(qTerms);

        String[] tmpStringArray;
        /** ============================= Getting score for other field terms ============================= **/
        tmpStringArray = bettermTedTask.task_title.split(" ");
        ArrayList<String> taskTitleTermsList = (ArrayList<String>) Arrays.asList(tmpStringArray);
        HashMap<String, Double> taskTitle_QueryCounts = calculateFieldTermScores(taskTitleTermsList);

        tmpStringArray = bettermTedTask.task_stmt.split(" ");
        ArrayList<String> taskStmtTermsList = (ArrayList<String>) Arrays.asList(tmpStringArray);
        HashMap<String, Double> taskStmt_QueryCounts = calculateFieldTermScores(taskStmtTermsList);

        tmpStringArray = bettermTedTask.task_narr.split(" ");
        ArrayList<String> taskNarrTermsList = (ArrayList<String>) Arrays.asList(tmpStringArray);
        HashMap<String, Double> taskNarr_QueryCounts = calculateFieldTermScores(taskNarrTermsList);

        Set<String> stitchedQueryTermsSet = new HashSet<>();
        stitchedQueryTermsSet.addAll(qTerms);
        stitchedQueryTermsSet.addAll(taskTitleTermsList);
        stitchedQueryTermsSet.addAll(taskStmtTermsList);
        stitchedQueryTermsSet.addAll(taskNarrTermsList);

        /** ============================= Arabic-RM3 QE term scoring ============================= **/
        HashMap<String,Long> globalWordCount = globalWordCount(initialResults); //generate an exaustive list of inidividual word counts
        this.setRelevantDocSum(globalWordCount);
        HashMap<String,Double> scoredWords =  scoreWords(initialResults, q, globalWordCount); //generate word probabilities.

        /** ============================= English's fields + QE terms scoring ============================= **/
        //

        /** ============================= RM3 mixing ============================= **/
        /**
         * value = Arb(a1 . reqQ + a2. arabicQE) + Eng( [a3.task-title + a4.task-stmt + a5.task-narr] + [a6.QEofCurrReq + a7.QEofPrevReq] )
         *
         * reqQ + [a3.task-title + a4.task-stmt + a5.task-narr] ++ arabicQE + [a6.QEofCurrReq + a7.QEofPrevReq]
         */
        // "i am a student" : [the: 0.1, school: 0.3, bag: 0.5, student: 0.8]
//        for(String query: qTerms){
//            if(scoredWords.containsKey(query)){
//                Double value = ( 1 - lambda) * queryCounts.get(query) + lambda * scoredWords.get(query);
//                scoredWords.put(query, value);
//            }
//        }
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
        /** ============================================== DO STEMMING =========================================== **/
        /** ====================================================================================================== **/
//        EnglishStemmer english = new EnglishStemmer();
//        String currWord = null;
//        for (int i=0; i<wordCandidate.size(); i++) {
//            currWord = wordCandidate.get(i);
//            english.setCurrent(currWord);
//            english.stem();
//            wordCandidate.set(i, english.getCurrent());
//            // System.out.println(currWord + " : " + wordCandidate.get(i));
//        }
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
            t = indexReader.getTermVector(docID, this.mainSearcherEnh.prop.getProperty("FIELD_TOKENIZED_CONTENT"));
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
            logger.warning("Exception : expandQuery - RM3Searcherv1.java : Couldn't parse returning original query");
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

    private HashMap<String, Double> scoreWords(ScoreDoc[] hits, Query q, HashMap<String, Long> globalWordCount) {
        // Calculate P(t|d)*p(q|d)  for every document
        // Create a hash map   we may words  to P(t|d)*p(q|d)
        HashMap<String,Double> newScores = new HashMap<String,Double>();
        IndexReader reader = this.getIndexReader();
        ScoreDoc hit;
        // compute the denominator //calculate probs on a per given document updatind the numerator
        for ( int i =0;  i < this.numDocs; i ++){ //only iterate through the relevant document set not the corpus
            hit = hits[i];
            try {
                Terms t = reader.getTermVector(hit.doc, this.mainSearcherEnh.prop.getProperty("FIELD_TOKENIZED_CONTENT"));
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
            logger.severe("Exception : getTerms - RM3Searcherv1.java : messed up with iterator in getTerms");
            System.exit(1);
        }
        return output;
    }

    private HashMap<String, Long> globalWordCount(ScoreDoc[] hits) throws IOException {
        IndexReader reader = this.getIndexReader();
        HashMap<String,Long> counts = new HashMap<String,Long>();
        double mu = 0.0;
        for(int i=0; i < this.numDocs; i++){
            Terms a  = reader.getTermVector(hits[i].doc, this.mainSearcherEnh.prop.getProperty("FIELD_TOKENIZED_CONTENT"));
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
        this.mu  = 1 * (mu/this.numDocs);
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


