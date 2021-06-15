package BETTERCLIRIndexer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.json.simple.JSONArray;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Logger;

public class BETTERIndexerWSentSeg {
    private IndexWriter writer;
    public String FIELD_ID;
    public String FIELD_TOKENIZED_CONTENT;
    public String FIELD_RAW_CONTENT;
    public Properties prop;
    public Logger logger;

    public BETTERIndexerWSentSeg(Properties prop, String indexDirectoryPath, Logger logger) throws IOException {
        this.prop = prop;
        this.FIELD_ID = this.prop.getProperty("FIELD_ID");
        this.FIELD_TOKENIZED_CONTENT = this.prop.getProperty("FIELD_TOKENIZED_CONTENT");
        this.FIELD_RAW_CONTENT = this.prop.getProperty("FIELD_RAW_CONTENT");
        this.logger = logger;

        Directory indexDirectory = FSDirectory.open(Paths.get(indexDirectoryPath));
        Analyzer arabicSimpleAnalyzer = new WhitespaceAnalyzer();
        IndexWriterConfig conf = new IndexWriterConfig(arabicSimpleAnalyzer);
        writer = new IndexWriter(indexDirectory, conf);
    }

    public void close() throws IOException {
        writer.close();
    }

    public int createIndex(String fileName, String tokenziedContent, String rawContent, JSONArray sentSegArabicContent) throws IOException {
        Document document = getDocument(fileName, tokenziedContent, rawContent, sentSegArabicContent);
        writer.addDocument(document);
        return writer.getDocStats().numDocs;
    }

    private Document getDocument(String uuidData, String tokenizedText, String rawText, JSONArray sentSegArabicContent) throws IOException {
        Document document = new Document();

        Field docIDField = new StringField( FIELD_ID, uuidData, Field.Store.YES);

        FieldType fieldIndexType = new FieldType();
        fieldIndexType.setStored(true);
        fieldIndexType.setTokenized(true);
        fieldIndexType.setStoreTermVectors(true);
        fieldIndexType.setIndexOptions(IndexOptions.DOCS_AND_FREQS); // TODO: try to change IndexOption-without positional posting list
        Field arabicTextField = new Field(FIELD_TOKENIZED_CONTENT, tokenizedText, fieldIndexType);

        document.add(docIDField);
        document.add(arabicTextField);
        document.add(new StoredField(FIELD_RAW_CONTENT, rawText));
        document.add(new StoredField(this.prop.getProperty("FIELD_SENT_SEG"), serialize(sentSegArabicContent)));

        return document;
    }

    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(200000);
        ObjectOutputStream os = new ObjectOutputStream(bos);
        os.writeObject(obj);
        os.close();
        return bos.toByteArray();
    }

}