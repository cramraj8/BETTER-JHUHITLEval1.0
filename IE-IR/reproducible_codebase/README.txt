
To reproduce IE/IR baseline, we want:
1. English Indexer
2. Query generator
3. English Searcher

Description:
1. English indexer is readily available, and please refer below details.
2. TODO ...


===================
==== Indexing =====
===================


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


===================
======= MT ========
===================

TODO ...