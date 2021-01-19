package BETTERCLIRIndexer;

import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


public class BETTERCLIRIndexer {
    public BETTERIndexer indexer;
    public Properties prop;
    public Logger logger;

    public BETTERCLIRIndexer(Properties prop, Logger logger, String indexDir,
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

    public void parseAndIndex(String tokenizedSourceCorpusDir, String rawSourceCorpusDir) throws IOException {

        File[] arabicFiles = new File(tokenizedSourceCorpusDir).listFiles();
        int fileCount = 0;
        String rawArabicContent = null;
        String tokenizedArabicContent = null;
        String rawArabicFilename = null;
        if(arabicFiles != null) {
            for (File arabicFile : arabicFiles) {
                if (!arabicFile.isDirectory()) {
                    rawArabicFilename = rawSourceCorpusDir + arabicFile.getName();

                    try {
                        tokenizedArabicContent = new String ( Files.readAllBytes( Paths.get(arabicFile.getPath()) ) );
                    } catch (IOException e) {
                        // e.printStackTrace();
                        System.out.println("... exception in reading tokenized Arabic doc : " + arabicFile.getName() + " file bytes content !");
                        logger.warning("... exception in reading tokenized Arabic doc : " + arabicFile.getName() + " file bytes content !");
                        continue;
                    }

                    try {
                        rawArabicContent = new String ( Files.readAllBytes( Paths.get(rawArabicFilename) ) );
                    } catch (IOException e) {
                        // e.printStackTrace();
                        System.out.println("... exception in reading raw Arabic doc : " + arabicFile.getName() + " file bytes content !");
                        logger.warning("... exception in reading raw Arabic doc : " + arabicFile.getName() + " file bytes content !");
                        continue;
                    }

                    int numIndexed;
                    long startTime = System.currentTimeMillis();

                    numIndexed = indexer.createIndex(arabicFile.getName(), tokenizedArabicContent, rawArabicContent); // Main function to call actual indexer
                    // System.out.println("Arabic : Indexed json files : " + (fileCount + 1) + " ; Indexed Documents : " + numIndexed);
                    logger.info("Arabic : Indexed json files : " + (fileCount + 1) + " ; Indexed Documents : " + numIndexed);

                    long endTime = System.currentTimeMillis();

                    fileCount ++;
                } else {
                    logger.info("warning message : Expected a file, but found a folder - " + arabicFile.getName() + " - in the path - " + arabicFile);
                }
            }
        } else {
            logger.severe("Exception - BETTERCLIRIndexer.java : Source Arabic Corpus Folder is empty.");
            System.exit(1);
        }
        indexer.close();
    }

    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
        // args[0] --> config.properties
        // args[1] --> Arabic IndexDir
        // args[2] --> ArabicSourceTokenizedDataDir
        // args[3] --> ArabicSourceRawDataDir
        // args[4] --> ArabicIndexingLog
        Properties prop = new Properties();
        Logger logger = Logger.getLogger("BETTERCLIRDryRun_Indexer_Log");
        prop.load(new FileReader(args[0]));
        BETTERCLIRIndexer betterclirIndexer = new BETTERCLIRIndexer(prop, logger, args[1], args[4]);
        betterclirIndexer.parseAndIndex(args[2], args[3]);




//        Properties prop = new Properties();
//        Logger logger = Logger.getLogger("BETTERCLIRDryRun_Indexer_Log");
//        prop.load(new FileReader("config.properties"));
//        BETTERCLIRIndexer betterclirIndexer = new BETTERCLIRIndexer(prop, logger, prop.getProperty("ArabicIndexDir"), "log.log");
//        betterclirIndexer.parseAndIndex(prop.getProperty("ArabicSourceTokenizedDataDir"), prop.getProperty("ArabicSourceRawDataDir"));


    }
}
