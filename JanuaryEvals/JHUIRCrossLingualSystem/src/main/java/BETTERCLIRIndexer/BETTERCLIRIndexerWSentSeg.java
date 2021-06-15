package BETTERCLIRIndexer;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;



public class BETTERCLIRIndexerWSentSeg {
    public BETTERIndexerWSentSeg indexer;
    public Properties prop;
    public Logger logger;

    public BETTERCLIRIndexerWSentSeg(Properties prop, Logger logger, String indexDir,
                             String indexerLogFilename) throws IOException {
        this.prop = prop;
        this.logger = logger;
        this.indexer = new BETTERIndexerWSentSeg(prop, indexDir, this.logger);

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

//    private void parseSentenceInfoObject(JSONObject sentenceInfo) {
//        String structural_element = (String) sentenceInfo.getOrDefault("structural-element", " ");
//        Long start = (Long) sentenceInfo.getOrDefault("start", 0);
//        Long end = (Long) sentenceInfo.getOrDefault("end", 0);
//    }

    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(200000);
        ObjectOutputStream os = new ObjectOutputStream(bos);
        os.writeObject(obj);
        os.close();
        return bos.toByteArray();
    }

    public JSONArray readSentSegJSONContent(String filename) {
        JSONParser jsonParser = new JSONParser();
        try {
            FileReader reader = new FileReader(filename);
            Object obj = jsonParser.parse(reader);
            JSONArray sentList = (JSONArray) obj;
            return sentList;
        } catch (IOException | ParseException e) {
            logger.severe("Exception - BETTERCLIRIndexerWSentSeg.java : sentence segment information file not found : " + filename);
            System.exit(1);
        }
        return null;
    }

    public void parseAndIndex(String tokenizedSourceCorpusDir, String rawSourceCorpusDir, String sentSegSourceCorpusDir) throws IOException {

        File[] arabicFiles = new File(tokenizedSourceCorpusDir).listFiles();
        int fileCount = 0;
        String rawArabicContent = null;
        String tokenizedArabicContent = null;
        JSONArray sentSegArabicContent = null;
        String rawArabicFilename = null;
        String sentSegArabicFilename = null;
        if(arabicFiles != null) {
            for (File arabicFile : arabicFiles) {
                if (!arabicFile.isDirectory()) {
                    rawArabicFilename = rawSourceCorpusDir + arabicFile.getName();
                    sentSegArabicFilename = sentSegSourceCorpusDir + arabicFile.getName().replace(".txt", "") + ".json";

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

                    sentSegArabicContent = readSentSegJSONContent(sentSegArabicFilename);
//                    try {
//                        sentSegArabicContent = new String ( Files.readAllBytes( Paths.get(rawArabicFilename) ) );
//                    } catch (IOException e) {
//                        // e.printStackTrace();
//                        System.out.println("... exception in reading Arabic Sentence Segmentation Info doc : " + arabicFile.getName() + " file bytes content !");
//                        logger.warning("... exception in reading Arabic Sentence Segmentation Info doc : " + arabicFile.getName() + " file bytes content !");
//                        continue;
//                    }

                    int numIndexed;
                    long startTime = System.currentTimeMillis();

                    numIndexed = indexer.createIndex(arabicFile.getName(), tokenizedArabicContent, rawArabicContent, sentSegArabicContent); // Main function to call actual indexer
                    // System.out.println("Arabic : Indexed json files : " + (fileCount + 1) + " ; Indexed Documents : " + numIndexed);
                    logger.info("Arabic : Indexed json files : " + (fileCount + 1) + " ; Indexed Documents : " + numIndexed);

                    long endTime = System.currentTimeMillis();

                    fileCount ++;

                    // break; // TODO
                } else {
                    logger.info("warning message : Expected a file, but found a folder - " + arabicFile.getName() + " - in the path - " + arabicFile);
                }
                // break; // TODO:
            }
        } else {
            logger.severe("Exception - BETTERCLIRIndexer.java : Source Arabic Corpus Folder is empty.");
            System.exit(1);
        }
        indexer.close();
    }

    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
//        args = new String[6];
//        args[0] = "config.properties";
//        args[1] = "/Users/ramraj/better-ir/English-Turkey-run/Eval/environment-setup/Arabic-Indexing/ARABIC_INDEX_DIR_wSentSeg";
//        args[2] = "/Users/ramraj/better-ir/English-Turkey-run/Eval/environment-setup/Arabic-Indexing/tokenized_data_dir/";
//        args[3] = "/Users/ramraj/better-ir/English-Turkey-run/Eval/environment-setup/Arabic-Indexing/raw_data_dir/";
//        args[4] = "/Users/ramraj/better-ir/English-Turkey-run/Eval/environment-setup/Arabic-Indexing/ARABIC_INDEXING_LOG_FILE.log";
//        args[5] = "/Users/ramraj/better-ir/English-Turkey-run/Eval/environment-setup/Arabic-Indexing/sentseg_data_dir/";

        Properties prop = new Properties();
        Logger logger = Logger.getLogger("BETTERCLIRDryRun_Indexer_Log");
        prop.load(new FileReader(args[0]));
        BETTERCLIRIndexerWSentSeg betterclirIndexerWSentSeg = new BETTERCLIRIndexerWSentSeg(prop, logger, args[1], args[4]);
        betterclirIndexerWSentSeg.parseAndIndex(args[2], args[3], args[5]);
    }
}
