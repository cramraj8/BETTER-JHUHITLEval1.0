
1. auto-IR
	main files:
		English query formulator : /code-for-English-query-generation/src/main/java/EnhancedQBESearch/EngQueryFormulatorEnhQEIR.java
		Arabic search : /code-for-Arabic-search/src/main/java/BETTERCLIRSearch/BETTERCLIRSearcherWSentSegRM3.java


2. auto-HITL
	main files:
		English query formulator : /code-for-English-query-generation/src/main/java/EnhancedQBESearch/autoHITLEvalQueryFormulator.java
		Arabic search : code-for-Arabic-search/src/main/java/BETTERCLIRSearch/BETTERCLIRSearcherWSentSegRM3.java

3. full-HITL
	main files:
		English query formulator : /src/main/java/QueryFormulatorEng/GenerateEngQueryFile.java
		Arabic search : /src/main/java/ArabicSearch/MainSearcher.java

4. MT system
	* auto-hitl & auto-ir shares same code for MT system : /mt-systems/mt-system-for-autoIR-and-autoHITL
	* full-hitl has different code for MT system to handle multi fields : /mt-systems/mt-system-for-fullHITL