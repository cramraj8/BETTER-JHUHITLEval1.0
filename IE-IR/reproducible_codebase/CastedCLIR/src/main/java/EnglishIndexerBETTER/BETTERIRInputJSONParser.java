package EnglishIndexerBETTER;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Logger;


public class BETTERIRInputJSONParser {
    public Properties prop;
    public Logger logger;

    public ArrayList<String> IDList;
    public ArrayList<String> TEXTList;

    public BETTERIRInputJSONParser(Properties prop, Logger logger) {
        this.prop = prop;
        this.logger = logger;
    }

    private void parseDocumentObject(JSONObject document) {
        JSONObject derivedMetaData = (JSONObject) document.get("derived-metadata");

        //Get UUID
        String id = (String) derivedMetaData.get("id");
        IDList.add(id);

        //Get TEXT
        String text = (String) derivedMetaData.get("text");
        TEXTList.add(text);
    }

    public ArrayList<Object> parseJSONData(String jsonFilename) {

        ArrayList<Object> returnTupleList = new ArrayList<>();
        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader(jsonFilename))
        {
            Object obj = jsonParser.parse(reader);

            JSONArray documentList = (JSONArray) obj;

            IDList = new ArrayList<String>();
            TEXTList = new ArrayList<String>();

            documentList.forEach( document -> parseDocumentObject( (JSONObject) document ));

            returnTupleList.add(IDList);
            returnTupleList.add(TEXTList);
        } catch (IOException | ParseException e) {
            logger.severe("Exception - BETTERInputJSONParser.java : data source file (" + jsonFilename + ") not found.");
        }
        return returnTupleList;
    }

    public static void main(String[] args) throws IOException {

        Properties prop = new Properties();
        prop.load(new FileReader("config.properties"));

        String DATA_FILE = "../data/English-chunks/temporary_file.json";
        DATA_FILE = "/Users/ramraj/better-ir/English-Turkey-run/python-processings/test-data/data_1.json";
        BETTERIRInputJSONParser ir_input = new BETTERIRInputJSONParser(prop, null);
        ir_input.parseJSONData(DATA_FILE);

        System.out.println(ir_input.parseJSONData(DATA_FILE).get(0));
    }
}
