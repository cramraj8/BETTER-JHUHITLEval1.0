package BETTERCLIRIndexer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;

public class SimpleSearcher {

    public static void main(String[] args) throws ParseException, IOException {
        boolean isArabicQuery = true;
        // String queryStr = "Dujardin السعودية تعلن انتهاء عاصفة الحزم بالقضاء على تهديدات الحوثيين للمملكة ودول الجوار وبدء عمإعادة الأمل"; // Equestrian Dujardin إلى حول عرب
//        String queryStr = "Charlotte Dujardin's world record-breaking routine at Olympia ودول الجوار وبدء";
        // String queryStr = "doc-232543.txt"; // doc-232543.txt doc-1417.txt
         String queryStr = "توافق تجار";
        /**
         * نعم --> YES
         * عرب--> Arab
         * و --> and
         * واحد --> one
         */
        Analyzer arabicSimpleAnalyzer = new WhitespaceAnalyzer();

        Query query = new QueryParser("FIELD_TOKENIZED_CONTENT", arabicSimpleAnalyzer).parse(queryStr);
        int hitsPerPage = 5;
        Directory index = FSDirectory.open(Paths.get("JHUBETTER_CLIRDryRun_Arabic_IndexDir"));
        // Directory dir = FSDirectory.open(Paths.get("IndexDir"));
        // IndexReader reader = DirectoryReader.open(dir);
        IndexReader reader = DirectoryReader.open(index);
//        System.out.println("Indexed Documents : " + reader.getDocCount("ID") + "\n");
        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs docs = searcher.search(query, hitsPerPage, Sort.INDEXORDER);

        ScoreDoc[] hits = docs.scoreDocs;
        System.out.println("Found " + hits.length + " hits: \n");
        String docText;
        for (ScoreDoc hit : hits) {
            int docId = hit.doc;
            Document d = searcher.doc(docId);
            System.out.println(d.get("UUID") + " :");
//            docText = d.get("FIELD_TOKENIZED_CONTENT");
            docText = d.get("FIELD_RAW_CONTENT");
            String[] docTextLines = docText.split("\n");
            for (int i=0; i<docTextLines.length; i++) System.out.println("\t" + docTextLines[i]);
            // System.out.println(d.get("ID") + " : \n" + d.get("TEXT") + "\n");
            // System.out.printf("\t(%s): %s\n", d.get("number"), d.get("title"));
        }
    }
}
