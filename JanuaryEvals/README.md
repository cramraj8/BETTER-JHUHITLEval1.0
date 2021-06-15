

1. Use IntelliJ IDEA to load and run the Java codebases

2. To run Arabic codebase (JHUIRCrossLingualSystem)
	- open the project (folder) JHUIRCrossLingualSystem in IntelliJ
	- open the script : JHUIRCrossLingualSystem/src/main/java/BETTERCLIRSearch/BETTERCLIRSearcherWSentSegRM3.java
	- set the pathnames in the main function
	- run the script in the IDE

3. script BETTERCLIRSearcherWSentSegRM3.java is the main function that loads query file, load indexing directory. But script RM3Searcherv1.java is the actual RM3 code.

4. This code works for both English and Arabic systems. Sometimes when running the script might throw errors. In such cases, look for the data-fields in config.properties file and use the same data-field in whichever line throws error.