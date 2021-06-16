
To reproduce IE/IR baseline, we want:
1. English Indexer
2. Query generator
3. English Searcher

I have created a folder, called CasterCLIR, which uses the automatic-IR codebase but replaces the Arabic entries (parameters) with those English correspondences. Also the output file (outputToIE.txt) that this codebase writes does not have sentence-segmented information (includes the index of line break characters).

To run this repo: create IntelliJ IDE environment and copy the script, pom.xml, config.properties, and other necessary files in this CastedCLIR repo.
* config.properties --> has the variable/ parameters values set by default for the scripts
* pom.xml --> has additional dependencies needed for IntelliJ IDE to load necessary Lucene and other libraries to run the scripts
* scratch/ --> this is where the outputs and intermediate files can be written
* required_files/ --> this folder has necessary files for scripts to run


======================================
==== Code repo information ===========
======================================

1. Indexing:
		path: /Users/ramraj/Summer-2021/BETTER-IR/IR-IE/CasterCLIR/src/main/java/EnglishIndexerBETTER/
		main script: BETTERIRIndexer.java
			--> use this script to index
		other script: VerifyIndexer.java and SearchDocID.java
			--> use this script to verify the search and search a give document ID's content

2. Query Formulator:
		path: /Users/ramraj/Summer-2021/BETTER-IR/IR-IE/CasterCLIR/src/main/java/EnglishQueryFormulator/
		main script: EngQueryGen.java
			--> it uses the automatic-IR run nature for query generation. That means only task-level and request-level example documents were given for querying (query-by-example). But you can alter this to enhance the query formulation. This will write two files output.json and output.json.entities.json in scratch/ folder for the following script to read.

3. English Searcher: [since I used the exact codebase of CLIR eval and casted here for English settings, minimal variable name refactoring was done.]
		path: /Users/ramraj/Summer-2021/BETTER-IR/IR-IE/CasterCLIR/src/main/java/ArabicSearcher/
		main script: MainArabicSearcher.java
			--> it reads the generated query files (query-text from output.json and query-entities from output.json.entities.json) from disk and retrieve the top-1000 documents.

		return: two files will be written to disk
			1. scratch/Arabic.output.txt --> it has the ranked top-1000 document IDs per query
			2. scratch/Arabic.outputToIE.json --> it has the ranked top-100 document IDs along with document text per query to facilitate the successive IE task



===================================
==== Appendix: Indexing ===========
===================================


Large volumne files are uploaded in CLSP, and can be grabbed with the path names given accordingly.
* Eval English & Arabic index dir: /home/rchan31/Year3BETTER/Phase1/Eval-Index-Dirs/
* JAR Files: /home/rchan31/Year3BETTER/Phase1/Eval-LargeVolume-Files/Jar-Files/


English indexing
----------------

If you want English Index dir, it is already built and available here in CLSP: /home/rchan31/Year3BETTER/Phase1/Eval-Index-Dirs/English_IndexDir

Otherwise, if you want to build the English index, you can follow these below steps.

	1. config and necessary files are in Github: /IE-IR/reproducible_codebase/english_indexing/required-files/IR.English.config.properties
	2. Index JAR file in CLSP : /home/rchan31/Year3BETTER/Phase1/Eval-LargeVolume-Files/Jar-Files/JHUIRSystem-EnglishDryRunIndexer-noEntities.jar
	3. How to run JAR file with config files to generate English indexing:
	java -jar JHUIRSystem-EnglishDryRunIndexer-noEntities.jar ${JAVA_CONFIG_FILE} ${ENGLISH_INDEX_DIR} ${CHUNKED_ENGLISH_DATA_DIR} ${LOG_INDEXER}

		ENGLISH_INDEX_DIR : folder name you want to create
		JAVA_CONFIG_FILE : /required-files/IR.English.config.properties
		CHUNKED_ENGLISH_DATA_DIR : folder that contains preproessed English data into chunks
		LOG_INDEXER : any log file name to write the Java logs
