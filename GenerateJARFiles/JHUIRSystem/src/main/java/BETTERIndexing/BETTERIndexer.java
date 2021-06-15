package BETTERIndexing;

//import edu.stanford.nlp.pipeline.CoreDocument;
//import edu.stanford.nlp.pipeline.CoreEntityMention;
//import edu.stanford.nlp.pipeline.StanfordCoreNLP;
//import org.apache.log4j.PropertyConfigurator;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Logger;


public class BETTERIndexer {
    private IndexWriter writer;
    public String FIELD_ID;
    public String FIELD_BODY;
    public Properties prop;

//    public StanfordCoreNLP pipeline;
//    public Properties coreNLPprops;
//    public CoreDocument doc;
    public Logger logger;

    public Directory indexDirectory;
    public StandardAnalyzer analyzer;
    public IndexWriterConfig conf;

    public ArrayList<String> entityMentions;

    public int indexedDocCount;
    public int exceptionalEntitiesInDocCount;

    public BETTERIndexer(Properties prop, String indexDirectoryPath, Logger logger) throws IOException {
        this.prop = prop;
        this.FIELD_ID = this.prop.getProperty("FIELD_ID");
        this.FIELD_BODY = this.prop.getProperty("FIELD_BODY");

        this.logger = logger;

        indexDirectory = FSDirectory.open(Paths.get(indexDirectoryPath));
        analyzer = new StandardAnalyzer();
        conf = new IndexWriterConfig(analyzer);
        writer = new IndexWriter(indexDirectory, conf);

//        String log4jConfPath = prop.getProperty("log4jConfPath");
//        PropertyConfigurator.configure(log4jConfPath);
//
////        this.coreNLPprops = PropertiesUtils.asProperties(
////                "annotators", "tokenize, ssplit, ner", // "tokenize,ssplit,parse,pos,ner,lemma,parse,natlog,depparse",
////                "tokenize.options", "splitHyphenated=true, normalizeParentheses=false",
////                "tokenize.whitespace", "false",
////                "ssplit.isOneSentence", "false",
////                "tokenize.language", "en"); // "parse.model", "edu/stanford/nlp/models/srparser/englishSR.ser.gz",
////        this.coreNLPprops.setProperty("ssplit.boundaryTokenRegex", "\\.|[!?]+");
////        this.pipeline = new StanfordCoreNLP(this.coreNLPprops);
//        this.coreNLPprops = new Properties();
//        this.coreNLPprops.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
//        pipeline = new StanfordCoreNLP(this.coreNLPprops);

        indexedDocCount = 0;
        exceptionalEntitiesInDocCount = 0;
    }

    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(200000);
        ObjectOutputStream os = new ObjectOutputStream(bos);
        os.writeObject(obj);
        os.close();
        return bos.toByteArray();
    }

    public void close() throws CorruptIndexException, IOException {
        writer.close();
    }

    private Document getDocument(String uuidData, String textData) throws IOException {
        Document document = new Document();

        /** Field for reference : DOCID **/
        Field docIDField = new StringField( FIELD_ID,
                uuidData,
                Field.Store.YES);

        /** Field for indexing : Original English Sentences **/
        FieldType fieldIndexType  = new FieldType();
        fieldIndexType.setStored(true);
        fieldIndexType.setTokenized(true);
        fieldIndexType.setStoreTermVectors(true);
        fieldIndexType.setIndexOptions(IndexOptions.DOCS_AND_FREQS); // TODO: try to change IndexOption-without positional posting list // DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS // DOCS_AND_FREQS // DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS
        Field engTextField = new Field(FIELD_BODY, textData, fieldIndexType);

        document.add(docIDField);
        document.add(engTextField);

//        // ================================== extract entities
//        entityMentions = new ArrayList<>();
//        try {
//            doc = new CoreDocument(textData);
//            pipeline.annotate(doc);
//
//            for (CoreEntityMention em : doc.entityMentions()) {
//                if (em.entityType().equals("DATE") || em.entityType().equals("TIME") || em.entityType().equals("MONEY")) {
//                    continue;
//                }
//                entityMentions.add(em.text().replaceAll("\n", " "));
//                // System.out.println("\tdetected entity: \t" + em.text() + "\t" + em.entityType());
//            }
//        } catch (Exception e) {
//            // e.printStackTrace();
//            exceptionalEntitiesInDocCount++;
//            logger.warning("Exception - BETTERIndexer : StanfordCore annotations (entities) threw exception for docID - " + uuidData);
//            entityMentions.add("the");
//        }
//
//
//        document.add(new StoredField(this.prop.getProperty("FIELD_ENTITIES"), serialize(entityMentions)));

        indexedDocCount++;
        // System.out.println("... ... indexed documents count : " + indexedDocCount);

        return document;
    }

    private void indexFile(String uuidData, String textData) throws IOException {
        Document document = getDocument(uuidData, textData);
        writer.addDocument(document);
    }


    public int createIndex(ArrayList<Object> dataTuplesContent) throws IOException {

        if (dataTuplesContent.size() == 1) {
            logger.severe("parse ArrayList<Object> to BETTERIndexer-createIndex doesn't have ID or TEXT parsed !");
            System.exit(1);
        } else if (dataTuplesContent.size() != 2) {
            logger.severe("parse ArrayList<Object> to BETTERIndexer-createIndex doesn't have ID & TEXT parsed !");
            System.exit(1);
        }

        ArrayList<String> stringUUIDArray = (ArrayList<String>) dataTuplesContent.get(0);
        ArrayList<String> stringTextArray = (ArrayList<String>) dataTuplesContent.get(1);

        if (stringUUIDArray.size() != stringTextArray.size()) {
            logger.severe("parse TEXT & ID to BETTERIndexer-createIndex don't have same number of samples !");
            System.exit(1);
        }

        for (int i=0; i<stringUUIDArray.size(); i++){
            indexFile( stringUUIDArray.get(i), stringTextArray.get(i) );
        }

        return writer.getDocStats().numDocs;
    }
}
