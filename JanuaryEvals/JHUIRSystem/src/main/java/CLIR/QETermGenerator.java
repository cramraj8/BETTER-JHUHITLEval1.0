package CLIR;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class QETermGenerator {
    public String indexPath;
    public File indexFile;
    public IndexReader indexReader;
    public Logger logger;
    public Properties prop;
    public int numDocs;
    public double mu;
    public long docSum;
    public double lambda;
    public String stopFilePath;
    public List stopWordsList;
    public QETermGenerator(Properties prop, File indexFile, Logger logger) throws IOException {
        this.indexFile = indexFile;
        Directory indexDir = FSDirectory.open(indexFile.toPath());
        if (!DirectoryReader.indexExists(indexDir)) {
            logger.severe("Exception - MainSearcher.java - Index Folder is empty.");
            System.exit(1);
        }
        indexReader = DirectoryReader.open(indexDir);
        this.logger = logger;
        this.prop = prop;
        this.numDocs = Integer.parseInt( this.prop.getProperty("numFeedbackDocs") );
        this.lambda = Double.parseDouble( this.prop.getProperty("rm3.mix") );
        this.stopFilePath = this.prop.getProperty("stopWords.path");
        this.stopWordsList = getStopWordsList();
    }

    public ArrayList<String> generateQETerms(ScoreDoc[] initialResults, Query q, int numEngQETerms) throws IOException {
        /** ============================= RM3 start-steps ============================= **/
        ArrayList<String> qTerms = getRawQueryString(q);
        HashMap<String, Double> queryCounts = new HashMap<String, Double>();

        for(String query: qTerms){ // get query level counts
            if(queryCounts.containsKey(query)){
                queryCounts.put(query, queryCounts.get(query) + 1);
            }
            else{
                queryCounts.put(query, 1.0);
            }
        }
        int hashSize = queryCounts.size();

        for(String query: qTerms){ // get query level probabilities
            Double count = queryCounts.get(query) / hashSize;
            queryCounts.put(query, count);
        }
        /** ============================= RM3 end-steps ============================= **/
        HashMap<String,Long> globalWordCount = globalWordCount(initialResults); //generate an exaustive list of inidividual word counts
        this.setRelevantDocSum(globalWordCount);

        HashMap<String,Double> scoredWords =  scoreWords(initialResults, q, globalWordCount); //generate word probabilities.
        /** ============================= RM3 start-steps ============================= **/
        for(String query: qTerms){
            if(scoredWords.containsKey(query)){
                Double value = ( 1 - lambda) * queryCounts.get(query) + lambda * scoredWords.get(query);
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

        ArrayList<String> filteredQETerms = new ArrayList<>();
        // StringBuffer QEBuffer = new StringBuffer();
        int cntTopTerms = 0;
        for (String w : wordCandidate) {
            filteredQETerms.add(w);
            cntTopTerms ++;
            if (cntTopTerms >= numEngQETerms) break;
        }
        // this.mainSearcher.QEWriteFile.write(QEBuffer.toString());
        return filteredQETerms;
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
        IndexReader reader = this.indexReader;
        HashMap<String,Long> counts = new HashMap<String,Long>();
        double mu = 0.0;
        for(int i=0; i < this.numDocs; i++){
            Terms a  = reader.getTermVector(hits[i].doc, this.prop.getProperty("FIELD_BODY"));
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
        // IndexReader reader = this.getIndexReader();
        ScoreDoc hit;
        // compute the denominator //calculate probs on a per given document updatind the numerator
        for ( int i =0;  i < this.numDocs; i ++){ //only iterate through the relevant document set not the corpus
            hit = hits[i];
            try {
                Terms t = this.indexReader.getTermVector(hit.doc, this.prop.getProperty("FIELD_BODY"));
                long totalFreq = t.getSumTotalTermFreq(); // Number of words in a particular documents
                //double lambda = ( this.mu)/( totalFreq + this.mu);
                ArrayList<TermWrapper> docTerms = getTerms(t, this.indexReader);
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
}
