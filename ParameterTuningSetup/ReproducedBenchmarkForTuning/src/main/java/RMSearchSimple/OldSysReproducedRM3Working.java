//package RMSearchSimple;
//
//import BETTERUtils.BETTERRequest;
//import org.apache.lucene.analysis.CharArraySet;
//import org.apache.lucene.analysis.TokenStream;
//import org.apache.lucene.analysis.en.EnglishAnalyzer;
//import org.apache.lucene.analysis.standard.StandardAnalyzer;
//import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
//import org.apache.lucene.index.IndexReader;
//import org.apache.lucene.index.Terms;
//import org.apache.lucene.index.TermsEnum;
//import org.apache.lucene.queryparser.classic.ParseException;
//import org.apache.lucene.queryparser.classic.QueryParser;
//import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
//import org.apache.lucene.search.*;
//import org.apache.lucene.search.similarities.BM25Similarity;
//import org.apache.lucene.search.similarities.LMDirichletSimilarity;
//import org.apache.lucene.util.BytesRef;
//import org.tartarus.snowball.ext.EnglishStemmer;
//
//import java.io.*;
//import java.util.*;
//
//public class OldSysReproducedRM3Working {
//}
//
//
//package RMSearchSimple;
//
//        import BETTERUtils.BETTERRequest;
//        import org.apache.lucene.analysis.CharArraySet;
//        import org.apache.lucene.analysis.TokenStream;
//        import org.apache.lucene.analysis.en.EnglishAnalyzer;
//        import org.apache.lucene.analysis.standard.StandardAnalyzer;
//        import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
//        import org.apache.lucene.index.IndexReader;
//        import org.apache.lucene.index.Terms;
//        import org.apache.lucene.index.TermsEnum;
//        import org.apache.lucene.queryparser.classic.ParseException;
//        import org.apache.lucene.queryparser.classic.QueryParser;
//        import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
//        import org.apache.lucene.search.*;
//        import org.apache.lucene.search.similarities.BM25Similarity;
//        import org.apache.lucene.search.similarities.LMDirichletSimilarity;
//        import org.apache.lucene.util.BytesRef;
//        import org.tartarus.snowball.ext.EnglishStemmer;
//
//        import java.io.*;
//        import java.util.*;
//
//public class RM3Searcherv1 extends IndexSearcher {
//    double lambda;
//    int expandAmount;
//    int numDocs;
//    long docSum;
//    double mu;
//    String stopFilePath;
//    List stopWordsList;
//    public MainSearcher mainSearcher;
//    public EnglishAnalyzer englishAnalyzer;
//    public EnglishStemmer englishStemmer;
//    public RM3Searcherv1(IndexReader r, MainSearcher mainSearcher) {
//        super(r);
//        this.setSimilarity(new LMDirichletSimilarity());
//        this.mainSearcher = mainSearcher;
//        this.expandAmount = mainSearcher.numFeedbackTerms;
//        this.numDocs = mainSearcher.numFeedbackDocs;
//        this.lambda = mainSearcher.rm3Mix;
//        this.stopFilePath = mainSearcher.prop.getProperty("stopWords.path");
//        this.stopWordsList = getStopWordsList();
//
//        CharArraySet englishStopWordsSet = CharArraySet.unmodifiableSet(new CharArraySet(this.stopWordsList, false));
//        this.englishAnalyzer = new EnglishAnalyzer(englishStopWordsSet);
//        this.englishStemmer = new EnglishStemmer();
//    }
//    public RM3Searcherv1(IndexReader r, int wordExpand) {
//        super(r);
//        this.setSimilarity(new BM25Similarity(2.0f, 1.0f));
//        this.expandAmount =wordExpand;
//    }
//
//    public List<String> getStopWordsList() {
//        List<String> stopWords = new ArrayList<>();
//
//        String line;
//        try {
//            // System.out.println("Provided Stop Words Path: " + this.stopFilePath);
//            FileReader fr = new FileReader(this.stopFilePath);
//            BufferedReader br = new BufferedReader(fr);
//            while ( (line = br.readLine()) != null )
//                stopWords.add( line.trim() );
//            br.close(); fr.close();
//        }
//        catch (FileNotFoundException e) {
//            e.printStackTrace();
//            System.exit(1);
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//            System.exit(1);
//        }
//
//        return stopWords;
//    }
//
//    private ScoreDoc[] mergeTaskReqScoreDocs(ScoreDoc[] labelRelevanceTaskScoreDocs, ScoreDoc[] labelRelevanceRequestScoreDocs) {
//        boolean foundRelMatch;
//        ArrayList<ScoreDoc> mergedNewScoreDocs = new ArrayList<ScoreDoc>();
//        if ((labelRelevanceTaskScoreDocs == null) || (labelRelevanceTaskScoreDocs.length == 0)) {
//            return mergedNewScoreDocs.toArray(new ScoreDoc[mergedNewScoreDocs.size()]);
//        }
//        for (ScoreDoc labelRelevanceTaskScoreDoc : labelRelevanceTaskScoreDocs) {
//            foundRelMatch = false;
//            for (ScoreDoc labelRelevanceRequestScoreDoc : labelRelevanceRequestScoreDocs) {
//
//                if (labelRelevanceTaskScoreDoc.doc == labelRelevanceRequestScoreDoc.doc) {
//                    mergedNewScoreDocs.add( new ScoreDoc(labelRelevanceRequestScoreDoc.doc,
//                            labelRelevanceRequestScoreDoc.score) ); // this is 2.0; i am not summing up 1.0 + 2.0 TODO:
//
//                    foundRelMatch = true;
//                }
//
//                if (foundRelMatch) break;
//            }
//            if (!foundRelMatch) mergedNewScoreDocs.add( new ScoreDoc(labelRelevanceTaskScoreDoc.doc, labelRelevanceTaskScoreDoc.score) );
//
//        }
//        return mergedNewScoreDocs.toArray(new ScoreDoc[mergedNewScoreDocs.size()]);
//    }
//
//    // @Override
//    public TopDocs search(Query query, ScoreDoc[] labelRelevanceTaskScoreDocs, ScoreDoc[] labelRelevanceRequestScoreDocs, int n, BETTERRequest betterRequest, FileWriter QEWriteFile) throws IOException, QueryNodeException {
//
//        /*
//        Steps followed:
//        1. Get top-1000 retrieved documents
//        2. Get relevance labelled documents and set relevance score = 1.0
//        3. Normalize the scores of top-1000 documents between [0, 1]
//        4. Combine both documents list, but check if they have mutual appearance, if so merge
//        5. do QE
//        6. re-rank the merged document list and pick top-1000
//
//        feedbackDocs = 50
//        labelledRel = 14
//        feedbackTerms = 100
//         */
//
//        // Step-1 : Get top-1000 retrieved documents
//        TopDocs top1000Hits = super.search(query, n) ;
//
//        // Step-2.1 : Get relevance labelled documents for task and keep score = 1.0 ===> already have labelRelevanceScoreDocs
//
//        // Step-2.2 : Get relevance labelled documents for req and keep score = 2.0 ===> already have labelRelevanceScoreDocs
//
//        // Step-2.3 : Merge 2.1 & 2.2
//        ScoreDoc[] mergedTaskReqScoreDocs = mergeTaskReqScoreDocs(labelRelevanceTaskScoreDocs, labelRelevanceRequestScoreDocs);
//
//        // Step-3 : Normalize the scores of top-1000 documents between [0, 1]
//        ScoreDoc[] normedTop1000ScoreDocs = normalizeInitialTopSearchScores(top1000Hits.scoreDocs);
//
//        // Step-4 : Combine both documents, but check if they have mutual appearance, if so merge
//        ArrayList<Integer> mergedRelevanceDocLuceneID = new ArrayList<Integer>();
//        ArrayList<ScoreDoc> combinedScoreDocs = new ArrayList<ScoreDoc>();
//
//        boolean foundMatch;
//        // Merge top-1000 documents with merged-Task-Req documents
//        for (ScoreDoc normedTop1000ScoreDoc : normedTop1000ScoreDocs) {
//            foundMatch = false;
//            for (ScoreDoc mergedTaskReqScoreDoc : mergedTaskReqScoreDocs) {
//
//                if (normedTop1000ScoreDoc.doc == mergedTaskReqScoreDoc.doc) {
//                    combinedScoreDocs.add( new ScoreDoc(mergedTaskReqScoreDoc.doc,
//                            normedTop1000ScoreDoc.score + mergedTaskReqScoreDoc.score) );
//                    mergedRelevanceDocLuceneID.add(mergedTaskReqScoreDoc.doc);
//                    foundMatch = true;
//                }
//
//                if (foundMatch) break;
//            }
//            if (!foundMatch) combinedScoreDocs.add( new ScoreDoc(normedTop1000ScoreDoc.doc, normedTop1000ScoreDoc.score) );
//
//        }
//
//        // Add non-merged Task~Req documents
//        for (ScoreDoc mergedTaskReqScoreDoc : mergedTaskReqScoreDocs) {
//            if (mergedRelevanceDocLuceneID.contains(mergedTaskReqScoreDoc.doc)) {
//                continue;
//            } else {
//                combinedScoreDocs.add( new ScoreDoc(mergedTaskReqScoreDoc.doc, mergedTaskReqScoreDoc.score) );
//            }
//        }
//
//        ScoreDoc[] combinedScoreDocsArray = combinedScoreDocs.toArray(new ScoreDoc[combinedScoreDocs.size()]);
//
//        // Step-5 : do RM3 mix & QE
//        ScoreDoc[] newQEDocs = reRank(combinedScoreDocsArray, query, betterRequest, QEWriteFile);
//
//        // Step-6 : Re-rank(sort) the both top-1000 + relevance documents
//        List<ScoreDoc> newQEDocsArrayList = Arrays.asList(newQEDocs);
//        Collections.sort(newQEDocsArrayList, new Comparator<ScoreDoc>(){
//            @Override
//            public int compare(ScoreDoc scoreDoc1, ScoreDoc scoreDoc2) {
//                return scoreDoc1.score < scoreDoc2.score ? 1: scoreDoc1.score==scoreDoc2.score ? 0:-1;
//            }
//        });
//
//        // Step-7 : Pick top-1000 documents
//        List<ScoreDoc> TopNewQEDocsArrayList = newQEDocsArrayList.subList(0, this.mainSearcher.numHits);
//        ScoreDoc[] reRankedNewScoreDocs = TopNewQEDocsArrayList.toArray(new ScoreDoc[TopNewQEDocsArrayList.size()]);
//
//        // Put the re-ranked documents in TopDocs
//        TopDocs newHits = new TopDocs(new TotalHits(this.mainSearcher.numHits, TotalHits.Relation.GREATER_THAN_OR_EQUAL_TO), reRankedNewScoreDocs);
//
//        return newHits;
//    }
//
//    private ScoreDoc[] normalizeInitialTopSearchScores(ScoreDoc[] top1000ScoreDocs) {
//        double normDenominator = 0;
//        for (ScoreDoc scoreDoc : top1000ScoreDocs) normDenominator += scoreDoc.doc;
//        for (ScoreDoc scoreDoc : top1000ScoreDocs) scoreDoc.score = (float) (scoreDoc.score / normDenominator);
//        // for (ScoreDoc scoreDoc : top1000ScoreDocs) System.out.println(scoreDoc.score);
//        return top1000ScoreDocs;
//    }
//
//    private String QETermsAnalyze(HashMap<String, Double> wordsMap) throws IOException {
//        Set<String> QRWords = wordsMap.keySet();
//        String QRWordsFlatString = "";
//        for (String e : QRWords) {
//            QRWordsFlatString += e + " ";
//        }
//        // System.out.println(QRWordsFlatString);
//
//        StringBuffer localBuff = new StringBuffer();
//        // TokenStream stream = this.mainSearcher.analyzer.tokenStream(this.mainSearcher.fieldToSearch, new StringReader(QRWordsFlatString));
//        TokenStream stream = englishAnalyzer.tokenStream(this.mainSearcher.fieldToSearch, new StringReader(QRWordsFlatString));
//        CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
//        stream.reset();
//        while (stream.incrementToken()) {
//            String term = termAtt.toString();
//            term = term.toLowerCase();
//            localBuff.append(term).append(" ");
//        }
//        stream.end();
//        stream.close();
//        // System.out.println(localBuff);
//        return localBuff.toString();
//    }
//
//    public ScoreDoc[] reRank(ScoreDoc[] initialResults, Query q, BETTERRequest betterRequest, FileWriter QEWriteFile) throws IOException {
//
//        /** ============================= RM3 start-steps ============================= **/
//        ArrayList<String> qTerms = getRawQueryString(q);
//        HashMap<String, Double> queryCounts = new HashMap<String, Double>();
//
//        for(String query: qTerms){ // get query level counts
//            if(queryCounts.containsKey(query)){
//                queryCounts.put(query, queryCounts.get(query) + 1);
//            }
//            else{
//                queryCounts.put(query, 1.0);
//            }
//        }
//        int hashSize = queryCounts.size();
//
//        for(String query: qTerms){ // get query level probabilities
//            Double count = queryCounts.get(query) / hashSize;
//            queryCounts.put(query, count);
//        }
//        /** ============================= RM3 end-steps ============================= **/
//
//
//        HashMap<String,Long> globalWordCount = globalWordCount(initialResults); //generate an exaustive list of inidividual word counts
//        this.setRelevantDocSum(globalWordCount);
//
//        HashMap<String,Double> scoredWords =  scoreWords(initialResults, q, globalWordCount); //generate word probabilities.
//        /** ============================= RM3 start-steps ============================= **/
//        for(String query: qTerms){
//            if(scoredWords.containsKey(query)){
//                Double value = ( 1 - lambda) * queryCounts.get(query) + lambda * scoredWords.get(query);
//                scoredWords.put(query, value);
//            }
//        }
//        /** ============================= RM3 end-steps ============================= **/
//
//        /** ====================================================================================================== **/
//        /** ============v2 - use StandardAnalyzer - BEFORE PROVIDING TOTAL TERMS FOR QE-SORT-PICKING, LETS REMOVE STOP WORDS ================= **/
//        /** ====================================================================================================== **/
//        // // Method-1
//        // String dump = QETermsAnalyze(scoredWords);
//        /** ====================================================================================================== **/
//        /** ============BEFORE PROVIDING TOTAL TERMS FOR QE-SORT-PICKING, LETS REMOVE STOP WORDS ================= **/
//        /** ====================================================================================================== **/
//        Iterator it = scoredWords.entrySet().iterator();
//        while(it.hasNext()){
//            Map.Entry mapEntry = (Map.Entry)it.next();
//            if (stopWordsList.contains(mapEntry.getKey())) {
//                it.remove();
//            }
//        }
//        /** ====================================================================================================== **/
//        /** ====================================================================================================== **/
//        /** ====================================================================================================== **/
//
//        ArrayList<String> wordCandidate =  getWordCandidates(scoredWords); //based on  word probabilities sort a list of words
//        // ArrayList<String> wordCandidate =  getWordCandidates(scoredWords); //based on  word probabilities sort a list of words
//        // System.out.println("Expanded Query Terms : ");
//        // System.out.println(wordCandidate);
//
//        // Writing QE-Terms in write file.
//        // FileWriter QEWriteFile = new FileWriter("./query_expansion_terms_extractions/QEWriteFile.txt");
//        StringBuffer QEBuffer = new StringBuffer();
//
//        QEBuffer.append(betterRequest.req_num);
//        int cntTopTerms = 0;
//        for (String w : wordCandidate) {
//            QEBuffer.append(" ").append(w);
//            cntTopTerms ++;
//            if (cntTopTerms >= this.expandAmount) break;
//        }
//        QEBuffer.append("\n");
//        System.out.println(cntTopTerms);
//
//        QEWriteFile.write(QEBuffer.toString());
//        // QEWriteFile.close();
//
////        for (String w : wordCandidate) System.out.print(w + " ");
////        System.out.println(wordCandidate.size());
//
//        /** ====================================================================================================== **/
//        /** ============================================== DO STEMMING =========================================== **/
//        /** ====================================================================================================== **/
////        EnglishStemmer english = new EnglishStemmer();
////        String currWord = null;
////        for (int i=0; i<wordCandidate.size(); i++) {
////            currWord = wordCandidate.get(i);
////            english.setCurrent(currWord);
////            english.stem();
////            wordCandidate.set(i, english.getCurrent());
////            // System.out.println(currWord + " : " + wordCandidate.get(i));
////        }
//        /** ====================================================================================================== **/
//        /** ====================================================================================================== **/
//        /** ====================================================================================================== **/
//
//        Query expQuery = expandQuery(wordCandidate,q); // uses the top n word candidates to  do search
//
//        ArrayList<String> queryTerms = getRawQueryString(expQuery); // This is just a utility method that let's me get the list of words in a query.
//
//        float[] rankingScores = new float[initialResults.length];
//        // System.out.println(queryTerms.toString());
//        for(int i=0; i < rankingScores.length; i ++){
//            rankingScores[i] = generateScore(initialResults[i].doc, this.getIndexReader(), queryTerms, globalWordCount);
//            initialResults[i].score  *= rankingScores[i]; // weight each score by the original model score.
//        }
//
//        return  initialResults;
//
//    }
//
//    private float generateScore(int docID, IndexReader indexReader, ArrayList<String> queryTerms, HashMap<String, Long> counts) {
//        // Calculate the new ranking score for document (docID) given the expanded query
//        Terms t;
//        long totalFreq;
//        try {
//            t = indexReader.getTermVector(docID, "TEXT");
//            totalFreq = t.getSumTotalTermFreq();
//        }catch (IOException e ){
//            System.out.println("Something happened");
//            System.out.println(e.getMessage());
//            return 0;
//        }
//        ArrayList<TermWrapper> docTerms = getTerms(t, indexReader);
//
//        float cumProduct= 0;
//        int i;
//        double globalWordProb;
//        double wordFreq;
//
//        this.mu =  this.mu* 1.1;
//        double lambda =   this.mu /( totalFreq + this.mu);
//
//        for(String e : queryTerms){
//            i = docTerms.indexOf(new TermWrapper(e,0) ); // the query word is in the document
//            if( i >= 0 ){
//                wordFreq= (double) docTerms.get(i).freq;
//            }else{
//                wordFreq = 0.0; // word does not occur in document
//            }
//
//            if( counts.get(e) != null){
//                globalWordProb = (double) counts.get(e) / this.docSum;
//            }else{
//                globalWordProb  =  0.0; // word does not occur in relevant corpus
//            }
//
//            cumProduct +=  Math.log( lambda * ( wordFreq/totalFreq ) + (1-lambda) * (globalWordProb) + 1 ); //add plus one to avoid taking the log of 0. which will return nan.
//        }
//        return  (float) Math.exp(cumProduct);
//    }
//
//    private ArrayList<String> getRawQueryString(Query q) {
//        //query string format is .  field:word  field:word  this func basically pulls out the word
//        ArrayList<String>  output = new ArrayList<String>();
//        String queryString = q.toString();
//        String[] words = queryString.split(" ");
//        for(String e : words){
//            String[] contentSplit = e.split(":");
//            output.add(contentSplit[1]);
//        }
//        return output;
//    }
//
//    private Query expandQuery(ArrayList<String> wordCandidate, Query q) {
//        ArrayList<String> queryWords = getRawQueryString(q);
//        String queryString ="";
//        for(String e: queryWords){ // add original query terms
//            queryString += " " + e ;
//        }
//        for(int i =0; i <this.expandAmount; i++){
//            queryString += " " + wordCandidate.get(i);  // add new query terms
//        }
//        QueryParser myQParser = new QueryParser("TEXT", new StandardAnalyzer());
//        try{
//            Query myQuery = myQParser.parse(QueryParser.escape(queryString));
//            return myQuery;
//        }catch(ParseException e){
//            System.out.println("Couldn't pase returning original query");
//            return q;
//        }
//    }
//
//    private ArrayList<String> getWordCandidates(HashMap<String, Double> counts) {
//        // Given a map of word to probabilities we create a list of all our words and sort them by their corresponding
//        // probability. Note output is in decreasing order
//        ArrayList<String> elements = new ArrayList<String>();
//        elements.addAll(counts.keySet());
//        elements.sort(Comparator.comparingDouble(s-> counts.get(s ) ));
//        Collections.reverse(elements);
//        return elements ;
//    }
//
//    private HashMap<String, Double> scoreWords(ScoreDoc[] hits, Query q, HashMap<String, Long> globalWordCount) {
//        // Calculate P(t|d)*p(q|d)  for every document
//        // Create a hash map   we may words  to P(t|d)*p(q|d)
//        HashMap<String,Double> newScores = new HashMap<String,Double>();
//        IndexReader reader = this.getIndexReader();
//        ScoreDoc hit;
//        // compute the denominator //calculate probs on a per given document updatind the numerator
//        for ( int i =0;  i < this.numDocs; i ++){ //only iterate through the relevant document set not the corpus
//            hit = hits[i];
//            try {
//                Terms t = reader.getTermVector(hit.doc, "TEXT");
//                long totalFreq = t.getSumTotalTermFreq(); // Number of words in a particular documents
//                //double lambda = ( this.mu)/( totalFreq + this.mu);
//                ArrayList<TermWrapper> docTerms = getTerms(t, reader);
//                double q_given_D = 1;  // double check this // pre-compute p(q|D). previous search models score is this value
//                q_given_D = hit.score;
//
//                for(TermWrapper e: docTerms){ //calculate p(t|d)*p(q|d)
//                    double termProb = ( (double) e.freq / totalFreq );
//                    termProb *= q_given_D;
//                    Double prevValue = newScores.get(e.text); //get previous value of a word
//                    if (prevValue != null){
//                        newScores.put(e.text, prevValue + termProb );
//                    }else{
//                        // prevValue = 0.0;
//                        newScores.put(e.text, termProb); // HHHHHHHHH
//                    }
//                }
//            }catch (IOException e ){
//                System.out.println("Something happened");
//                System.out.println(e.getMessage());
//                System.exit(-1);
//            }
//        }
//        //in this section we normalize our values
//        double denominator = 0.0;
//        for(String e : newScores.keySet()){
//            denominator += newScores.get(e);
//        }
//        for(String e: newScores.keySet()){
//            newScores.put(e,newScores.get(e )/denominator);
//        }
//        return newScores;
//    }
//
//    public static ArrayList<TermWrapper> getTerms(Terms terms, IndexReader reader){
//        TermsEnum a;
//        TermWrapper t ;
//        BytesRef term ;
//        long termFreq;
//        ArrayList<TermWrapper> output = new ArrayList<TermWrapper>();
//        try{
//            a =terms.iterator();
//            while( ( term=a.next())!= null ){
//                termFreq =  a.totalTermFreq();
//                t = new TermWrapper(term.utf8ToString(), termFreq );
//                output.add(t);
//            }
//        }catch (IOException e){
//            System.out.println(e.getMessage());
//            System.out.println("MEssed up with iterator in getTerms");
//        }
//        return output;
//    }
//
//    private HashMap<String, Long> globalWordCount(ScoreDoc[] hits) throws IOException {
//        IndexReader reader = this.getIndexReader();
//        HashMap<String,Long> counts = new HashMap<String,Long>();
//        double mu = 0.0;
//        for(int i=0; i < this.numDocs; i++){
//            Terms a  = reader.getTermVector(hits[i].doc, "TEXT");
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
//        this.mu  = 1 * (mu/this.numDocs);
//        return counts;
//    }
//
//    private void setRelevantDocSum(HashMap<String, Long> globalWordCount) {
//        // Just sum the number of occurence of words. This gives us the total sum in our collection.
//        long accumulator = 0;
//        for(long e: globalWordCount.values()){
//            accumulator +=e;
//        }
//        this.docSum = accumulator;
//    }
//}
//
//
