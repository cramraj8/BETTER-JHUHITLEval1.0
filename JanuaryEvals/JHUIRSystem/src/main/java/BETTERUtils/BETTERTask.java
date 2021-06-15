package BETTERUtils;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.search.Query;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;


public class BETTERTask {
    public String task_num;
    public String task_title;
    public String task_stmt;
    public String task_link;
    public String task_narr;
    public String task_in_scope;
    public String task_not_in_scope;

    public Query luceneQuery;
    public String fieldToSearch;

    public ArrayList<String> task_docs;

    // VARIABLES FOR REQUEST STREAM
    public List<BETTERRequest> requests;

    @Override
    public String toString() {
        return task_num + "\t" + task_title;
    }

    public Query getLuceneQuery() { return luceneQuery; }

//    public String queryFieldAnalyze(Analyzer analyzer, String queryFieldText) throws Exception {
//        fieldToSearch = Params.FIELD_BODY;
//        StringBuffer localBuff = new StringBuffer();
//        TokenStream stream = analyzer.tokenStream(fieldToSearch, new StringReader(queryFieldText));
//        CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
//        stream.reset();
//        while (stream.incrementToken()) {
//            String term = termAtt.toString();
//            term = term.toLowerCase();
//            localBuff.append(term).append(" ");
//        }
//        stream.end();
//        stream.close();
//        return localBuff.toString();
//    }
}