package BETTERIndexing;

import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class BETTERIRIndexer {
    public BETTERIndexer indexer;
    public Properties prop;
    public Logger logger;

    public BETTERIRIndexer(Properties prop, Logger logger, String indexDir,
                           String indexerLogFilename) throws IOException {
        this.prop = prop;
        this.logger = logger;
        this.indexer = new BETTERIndexer(prop, indexDir, this.logger);

        FileHandler fh;
        try {
            fh = new FileHandler(indexerLogFilename);
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
            logger.info("... starting to log");
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }
    }

    public void parseAndIndex(String sourceJSONPath) throws IOException {

        ArrayList<Object> dataTuple;
        File[] files = new File(sourceJSONPath).listFiles();
        int fileCount = 0;
        if(files != null) {
            for (File file : files) {
                if (!file.isDirectory()) {

                    // Create an instance of InputParser extractor object.
                    BETTERIRInputJSONParser betterirInputJSONParser = new BETTERIRInputJSONParser(prop, logger);
                    dataTuple = betterirInputJSONParser.parseJSONData(file.getPath());

                    int numIndexed;
                    // long startTime = System.currentTimeMillis();
                    numIndexed = indexer.createIndex(dataTuple);
                    // System.out.println(numIndexed);
                    System.out.println("Indexed json files : " + (fileCount + 1) + " ; Indexed Documents : " + numIndexed);
                    logger.info("Indexed json files : " + (fileCount + 1) + " ; Indexed Documents : " + numIndexed);
                    long endTime = System.currentTimeMillis();

                    fileCount ++;
                } else {
                    logger.info("warning message : Expected a file, but found a folder - " + file.getName() + " - in the path - " + file);
                }
            }
        } else {
            logger.severe("Exception - BETTERIRIndexer.java : Source Corpus Folder is empty.");
            System.exit(1);
        }
        indexer.close();
    }

    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
//        args = new String[4];
//        args[0] = "/Users/ramraj/better-ir/English-Turkey-run/Eval/autoHITL/English-Indexing/CLIRconfig.properties";
//        args[1] = "/Users/ramraj/better-ir/English-Turkey-run/Eval/autoHITL/English-Indexing/ENGLISH_INDEX_DIR";
//        args[2] = "/Users/ramraj/better-ir/English-Turkey-run/Eval/autoHITL/English-Indexing/ENGLISH_CHUNKED_DATA_DIR/";
//        args[3] = "/Users/ramraj/better-ir/English-Turkey-run/Eval/autoHITL/English-Indexing/ENGLISH_LOG_INDEXER.log";


        // args[0] --> Index Directory
        // args[1] --> Source Path
        Properties prop = new Properties();

        prop.load(new FileReader(args[0]));
        Logger logger = Logger.getLogger("BETTERDryRun_Indexer_Log");
        BETTERIRIndexer betterirIndexer = new BETTERIRIndexer(prop, logger, args[1], args[3]);
        betterirIndexer.parseAndIndex(args[2]);

        // prop.load(new FileReader("config.properties"));
        // this.prop.getProperty("IndexerLogFilename")
        // BETTERIRIndexer betterirIndexer = new BETTERIRIndexer(prop, logger, prop.getProperty("IndexDir"));
        // betterirIndexer.parseAndIndex(prop.getProperty("EnglishSourceDataDir"));

        /**
         * prop.load(new FileReader("config.properties"));
         *         // args = new String[3];
         *         if (args[0] == null) args[0] = prop.getProperty("BETTER_INDEX_DIR");
         *         if (args[1] == null) args[1] = prop.getProperty("SRC_JSON_DIR");
         *
         *         System.out.println("Start running indexing .....");
         *         System.out.println("Index directory : " + args[0]);
         *
         *         parseAndIndex(prop, args[0], args[1]);
         */
    }
}
