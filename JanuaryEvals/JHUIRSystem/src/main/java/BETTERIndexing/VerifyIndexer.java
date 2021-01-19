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
import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Properties;


public class VerifyIndexer {

    public static Object unSerialize(byte[] bytes) throws ClassNotFoundException, IOException {
        ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(bytes));
        Object result = is.readObject();
        is.close();
        return result;
    }


    public static void main(String[] args) throws IOException, ParseException, ClassNotFoundException {

        Properties prop = new Properties();
        prop.load(new FileReader("config.properties")); // todo: pass it as argument

        String queryStr = prop.getProperty("testQuery", "world");

        Directory dir = FSDirectory.open(Paths.get(prop.getProperty("IndexDir")));
        StandardAnalyzer analyzer = new StandardAnalyzer();
        Query q = new QueryParser(prop.getProperty("FIELD_BODY"), analyzer).parse(queryStr);
        IndexReader reader = DirectoryReader.open(dir);
        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs docs = searcher.search(q, Integer.parseInt(prop.getProperty("testHits")));
        searcher.setSimilarity(new BM25Similarity());
        ScoreDoc[] hits = docs.scoreDocs;

        System.out.println("Indexed Num of Docs : " + reader.numDocs());
        System.out.println("Num of Hits : " + hits.length);

        for (int i = 0; i < hits.length; ++i) {
            int docId = hits[i].doc;
            double score = hits[i].score;
            Document d = searcher.doc(docId);
            String documentUUID = d.get(prop.getProperty("FIELD_ID"));

            ArrayList<Object> documentENTITIES = null;
            try {
                documentENTITIES = (ArrayList<Object>) unSerialize(d.getBinaryValue(prop.getProperty("FIELD_ENTITIES")).bytes);
                /** Printing ArrayList elements **/
                if (! documentENTITIES.get(0).equals("")) {
                    System.out.print(documentUUID + " : ");
                    System.out.println(documentENTITIES); //            System.out.println("\t" + d.get("TEXT"));
                } else {
                    System.out.println(documentUUID + " : " + "None Entities");
                }
            } catch (Exception e) {
                System.out.println(documentUUID + " : ");
                System.out.println(d.get(prop.getProperty("FIELD_BODY")));

            }



        }
    }
}
