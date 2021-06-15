package BETTERIndexing;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;

public class SearchDocID {
    public static void main(String[] args) throws IOException, ParseException, ClassNotFoundException {

        Properties prop = new Properties();
        prop.load(new FileReader("config.properties"));


        String queryStr = prop.getProperty("test_query_string", "the");

        Directory dir = FSDirectory.open(Paths.get(prop.getProperty("IndexDir")));

        // StandardAnalyzer object initialization for search APIs.
        StandardAnalyzer analyzer = new StandardAnalyzer();
        // Setting data type for query to match or search within indexed documents.
        Query q = new QueryParser(prop.getProperty("FIELD_BODY"), analyzer).parse(queryStr);

        IndexReader reader = DirectoryReader.open(dir);

        // IndexSearcher object initialization to retrieve documents from the IndexDir.
        IndexSearcher searcher = new IndexSearcher(reader);
        // Main Lucene search API with parameters: query and top-k.
        TopDocs docs = searcher.search(q, Integer.parseInt(prop.getProperty("MAX_SEARCH")));
        searcher.setSimilarity(new BM25Similarity());

        ScoreDoc[] hits = docs.scoreDocs;

        System.out.println("Indexed Num of Docs : " + reader.numDocs());
        System.out.println("Num of Hits : " + hits.length);

        for (int i = 0; i < hits.length; ++i) {
            int docId = hits[i].doc;
            double score = hits[i].score;

            Document d = searcher.doc(docId);
            String documentUUID = d.get(prop.getProperty("FIELD_ID"));

            System.out.println(documentUUID);
            // System.out.println("\t" + d.get("TEXT"));




        }
    }
}
