#!/bin/bash

ENGLISH_INDEX_FLAG=true
ENGLISH_SRC_DIR="/corpus/turkey-run-english-corpus.jl"
ENGLISH_N_CHUNKS=1000
ENGLISH_N_DOCUMENT=-1
ENGLISH_INDEX_DIR="/scratch/JHI_IR_English_IndexDir"
QUERY_TASK_FILE="/app/ir-tasks.json"

ARABIC_INDEX_FLAG=true
ARABIC_SRC_DIR="/corpus/turkey-run-arabic-corpus.jl"
ARABIC_N_CHUNKS=1000
ARABIC_N_DOCUMENT=-1
ARABIC_INDEX_DIR="/scratch/JHI_IR_Arabic_IndexDir"

MT_PREPROCESSING_FLAG=true


# MT_DIR="./MT-system"
MT_DIR="/home/Project/JHUCLIRSystem/MT-system"
FAIRSEQ=./fairseq/fairseq_cli
# ====================================================================================================
# 						Handling command line arguments
# ====================================================================================================
echo -e "========================================================================================================"
echo -e "				Command Line Arguments Parsing"
echo -e "========================================================================================================"
while getopts "i:e:n:t:d:q:j:f:o:u:g:p:" arg; do
  case $arg in i) ENGLISH_INDEX_FLAG=$OPTARG;;
  	e) ENGLISH_SRC_DIR=$OPTARG;;
n) ENGLISH_N_CHUNKS=$OPTARG;;
t) ENGLISH_N_DOCUMENT=$OPTARG;;
d) ENGLISH_INDEX_DIR=$OPTARG;;
q) QUERY_TASK_FILE=$OPTARG;;

j) ARABIC_INDEX_FLAG=$OPTARG;;
f) ARABIC_SRC_DIR=$OPTARG;;
o) ARABIC_N_CHUNKS=$OPTARG;;
u) ARABIC_N_DOCUMENT=$OPTARG;;
g) ARABIC_INDEX_DIR=$OPTARG;;

p) MT_PREPROCESSING_FLAG=$OPTARG;;

  esac
done

echo -e "Indexing : User provided English-Indexing: flag-${ENGLISH_INDEX_FLAG} and Arabic-Indexing: flag-${ARABIC_INDEX_FLAG} to index before do searching ...\n"

echo -e "Arabic Query Preprocessing : User provided processing: flag-${MT_PREPROCESSING_FLAG} before do machine-translation ...\n"


# ====================================================================================================
# 						Parsing pathnames from pathnames-configuration file
# ====================================================================================================
ENGLISH_JAVA_CONFIG_FILE="./required-files/IR.English.config.properties"
ENGLISH_CHUNKED_DATA_DIR="/scratch/chunked_Eng_data_dir/"

# English Indexing parameters
ENGLISH_JAR_INDEXER="./JHUIRSystem-EnglishDryRunIndexer-noEntities.jar"
ENGLISH_LOG_INDEXER="/scratch/JHU.IR.English.IndexerLogFile.log"

# ====================================================================================================
EN_QUERY_OUTPUT="/scratch/input_queries.json.en"
AR_QUERY_INPUT="/scratch/output_queries.json.ar"
# QUERY_FORMULATOR_JAR_FILE="./JHUCLIRSystem-EnglishToArabicDryRun-QueryFormulator.jar"
# QUERY_FORMULATOR_JAR_FILE="./JHUCLIRSystem-En2ArEval-QueryFormulator.jar"
# QUERY_FORMULATOR_JAR_FILE="./JHUCLAutoHITLSystem-En2ArEval-QueryFormulator.jar"
QUERY_FORMULATOR_JAR_FILE="./Resubmission_English_QueryFormulator_AutoHITL_JHUEmory.jar"
QUERY_FORMULATOR_LOG_FILE="/scratch/JHU.IR.ENAR.QueryFormulatorLogFile.log"

# ====================================================================================================
# Now parse the pathnames from configuration file to bash script
ARABIC_SRC_CHUNKED_DATA_DIR="/scratch/data_chunked_arabic_dir/"
ARABIC_SRC_RAW_DATA_DIR="/scratch/data_raw_arabic_dir/"
ARABIC_SRC_INTERMEDIATE_DATA_DIR="/scratch/data_intermediate_arabic_dir/"
ARABIC_SRC_TOKENIZED_DATA_DIR="/scratch/data_tokenized_arabic_dir/"
ARABIC_SRC_SENTSEG_DATA_DIR="/scratch/data_sentseg_arabic_dir/"

FARASA_TOKENIZER_JAR_FILE="./FarasaSegmenterJar.jar"

ARABIC_JAVA_CONFIG_FILE="./required-files/CLIRconfig.properties"

# Indexing parameters
# ARABIC_INDEXER_JAR="./JHUCLIRSystem-ArabicDryRunIndexer.jar"
ARABIC_INDEXER_JAR="./JHUCLIRSystem-ArabicEvalIndexer.jar"
ARABIC_INDEXING_LOG_FILE="/scratch/JHU.IR.Arabic.IndexerLogFile.log"

# Searching parameters
# ARABIC_SEARCHER_JAR="./JHUCLIRSystem-ArabicDryRunSearcher.jar"
# ARABIC_SEARCHER_JAR="./JHUCLIRSystem-ArabicEvalSearcher.jar"
ARABIC_SEARCHER_JAR="./Resubmission_ArabicSearcher_JHUEmory.jar"
ARABIC_SEARCH_MODE="BM25"
ARABIC_OUTPUT_FILE="/scratch/JHU.IR.Arabic.output.txt"
ARABIC_OUTPUT_TO_IE_FILE="/scratch/outputToIE.json"
ARABIC_SEARCH_WATERMARK="JHU.Emory.IR.Arabic.Auto.DryRun"
ARABIC_SEARCHER_LOGGING_FILE="/scratch/JHU.IR.Arabic.SearcherLogFile.log"


echo -e "========================================================================================================"
echo -e "				Running CLIR System Components"
echo -e "========================================================================================================\n\n"

if [ "$ENGLISH_INDEX_FLAG" == true ]; then
	echo -e "********************************************************************************************************"
	echo -e "				Running Component - English Corpus Chunker"
	echo -e "********************************************************************************************************"
	#step-1
	python3 script_English_preprocessor.py ${ENGLISH_SRC_DIR} ${ENGLISH_CHUNKED_DATA_DIR} ${ENGLISH_N_CHUNKS} ${ENGLISH_N_DOCUMENT} "/scratch/English.IR.results/"


	echo -e "********************************************************************************************************"
	echo -e "				Running Component - English Corpus Indexer"
	echo -e "********************************************************************************************************"
	#step-2
	java -jar ${ENGLISH_JAR_INDEXER} ${ENGLISH_JAVA_CONFIG_FILE} ${ENGLISH_INDEX_DIR} ${ENGLISH_CHUNKED_DATA_DIR} ${ENGLISH_LOG_INDEXER}

else
	echo -e "********************************************************************************************************"
	echo -e "				Skipping English Indexing Phase"
	echo -e "********************************************************************************************************\n\n"
fi












# "********************************************************************************************************"
# "				Moving into Arabic
# "********************************************************************************************************"

if [ "$ARABIC_INDEX_FLAG" == true ]; then
	echo -e "********************************************************************************************************"
	echo -e "				Running Component - Arabic Corpus Chunker"
	echo -e "********************************************************************************************************"
	#step-1
	# python script_Arabic_corpus_processing.py ${ARABIC_SRC_DIR} ${ARABIC_SRC_CHUNKED_DATA_DIR} ${ARABIC_N_CHUNKS} ${ARABIC_N_DOCUMENT} ${ARABIC_SRC_RAW_DATA_DIR} ${ARABIC_SRC_INTERMEDIATE_DATA_DIR}
	python3 script_Arabic_corpus_processing_withSentSeg.py ${ARABIC_SRC_DIR} ${ARABIC_SRC_CHUNKED_DATA_DIR} ${ARABIC_N_CHUNKS} ${ARABIC_N_DOCUMENT} ${ARABIC_SRC_RAW_DATA_DIR} ${ARABIC_SRC_INTERMEDIATE_DATA_DIR} ${ARABIC_SRC_SENTSEG_DATA_DIR}



	echo -e "********************************************************************************************************"
	echo -e "				Running Component - Arabic Tokenizer using Farasa"
	echo -e "********************************************************************************************************"
	#step-2
	# shellcheck disable=SC1073
	for arabic_file in ${ARABIC_SRC_CHUNKED_DATA_DIR}data_*.txt
	do
	 save_filename="${arabic_file##*/}"
	 echo "... ... tokenizing Arabic documents batch file : ${ARABIC_SRC_CHUNKED_DATA_DIR}${save_filename}"
	 java -jar ${FARASA_TOKENIZER_JAR_FILE} -i ${arabic_file} -o ${ARABIC_SRC_INTERMEDIATE_DATA_DIR}/${save_filename}

	done



	echo -e "********************************************************************************************************"
	echo -e "				Running Component - Arabic Formatting for Indexer"
	echo -e "********************************************************************************************************"
	#step-2
	python3 step2_Arabic_tokenizedFormatting.py ${ARABIC_SRC_INTERMEDIATE_DATA_DIR} ${ARABIC_SRC_TOKENIZED_DATA_DIR}



	echo -e "********************************************************************************************************"
	echo -e "				Running Component - Arabic Corpus Indexer"
	echo -e "********************************************************************************************************"
	#step-2
	java -jar ${ARABIC_INDEXER_JAR} ${ARABIC_JAVA_CONFIG_FILE} ${ARABIC_INDEX_DIR} ${ARABIC_SRC_TOKENIZED_DATA_DIR} ${ARABIC_SRC_RAW_DATA_DIR} ${ARABIC_INDEXING_LOG_FILE} ${ARABIC_SRC_SENTSEG_DATA_DIR}

else
	echo -e "********************************************************************************************************"
	echo -e "				Skipping Arabic Indexing Phase"
	echo -e "********************************************************************************************************\n\n"
fi










# "********************************************************************************************************"
# "				Generate Query
# "********************************************************************************************************"

echo -e "********************************************************************************************************"
echo -e  "				Running Component - Query Generator"
echo -e "********************************************************************************************************"

java -jar ${QUERY_FORMULATOR_JAR_FILE} ${ENGLISH_JAVA_CONFIG_FILE} ${ENGLISH_INDEX_DIR} ${QUERY_TASK_FILE} ${EN_QUERY_OUTPUT} ${QUERY_FORMULATOR_LOG_FILE}










# "********************************************************************************************************"
# "				Moving into Enlish -> Arabic Translation
# "********************************************************************************************************"
echo -e "********************************************************************************************************"
echo -e "				Running Component - English --> Arabic Query Translation"
echo -e "********************************************************************************************************"
	

python3 ./get_query_str.py ${EN_QUERY_OUTPUT} > ${MT_DIR}/queries.raw.en # get text from json

python3 ./apply_bpe.py -c ${MT_DIR}/ar-en-both-32000.codes < ${MT_DIR}/queries.raw.en > ${MT_DIR}/queries.en # apply BPE
cp ${MT_DIR}/queries.en ${MT_DIR}/queries.ar # preprocessing expects a translated test set (hacky)

DST_PREPROCESSED_DIR=${MT_DIR}/en-ar-farasa-32k-test-data-bin

if [ "$MT_PREPROCESSING_FLAG" == true ]; then
	DST_PREPROCESSED_DIR=${MT_DIR}/en-ar-farasa-32k-test-data-bin-fresh

	python3 $FAIRSEQ/preprocess.py --source-lang en --target-lang ar  --trainpref ${MT_DIR}/train.32k --validpref ${MT_DIR}/dev.32k --testpref ${MT_DIR}/queries --destdir ${DST_PREPROCESSED_DIR}
else
	echo -e "********************************************************************************************************"
	echo -e "				Skipping Arabic Query Binarizing Step"
	echo -e "********************************************************************************************************\n\n"
fi

# Translate using pretrained model
python3 $FAIRSEQ/generate.py ${DST_PREPROCESSED_DIR} --path ${MT_DIR}/en-ar-farasa-nobpe/checkpoint_best.pt --batch-size 128 --beam 5 --remove-bpe > ${MT_DIR}/queries.translated

# Translations need to be sorted. Actually update queries.ar now
cat ${MT_DIR}/queries.translated | grep '^H-' | sed 's/^..//' | sort -n | awk 'BEGIN{FS="\t"}{print $3}' > ${MT_DIR}/queries.ar

python3 ./set_query_str.py ${EN_QUERY_OUTPUT} ${MT_DIR}/queries.ar ${AR_QUERY_INPUT}











# "********************************************************************************************************"
# "				Moving into Arabic
# "********************************************************************************************************"
echo -e "********************************************************************************************************"
echo -e  "				Running Component - Arabic Corpus Searcher"
echo -e "********************************************************************************************************"
#step-3
java -jar ${ARABIC_SEARCHER_JAR} ${ARABIC_JAVA_CONFIG_FILE} ${ARABIC_SEARCH_MODE} ${ARABIC_INDEX_DIR} ${AR_QUERY_INPUT} "${EN_QUERY_OUTPUT}.entities.json" ${ARABIC_OUTPUT_FILE} ${ARABIC_OUTPUT_TO_IE_FILE} ${ARABIC_SEARCHER_LOGGING_FILE} ${ARABIC_SEARCH_WATERMARK}




