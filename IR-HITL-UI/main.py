import json
import os
import re
from flask import Flask, request, session, Markup, jsonify
from flask import render_template
from flask_restful import Api
import copy

app = Flask(__name__)
api = Api(app)
counter = 0

ROOT_FOLDER = "/Users/ramraj/PythonBasedDemo/Finals_UIDemo/"
FILES_FOLDER = "file-transfers/"

INPUT_FILE = os.path.join(ROOT_FOLDER, FILES_FOLDER, "input_live_stream.json")
MANUAL_ANNOTATION_INPUT_FILE = os.path.join(ROOT_FOLDER, FILES_FOLDER, "input_manual_live_stream.json")
OUTPUT_RM3_FILE = os.path.join(ROOT_FOLDER, FILES_FOLDER, "output_RM3_live_stream.json")
OUTPUT_HITL_FILE = os.path.join(ROOT_FOLDER, FILES_FOLDER, "output_HITL_live_stream.json")
OUTPUT_QE_FILE = os.path.join(ROOT_FOLDER, FILES_FOLDER, "output_QE_live_stream.json")

CONFIG_FILE = os.path.join(ROOT_FOLDER, "core/config.properties")

JAR_FILE = "/Users/ramraj/BETTER-IR/JARDemoProject/BETTER_IR_DryRun_Model-ENH3_IEHITLSent-addedHString.jar"

DISPLAY_NUM_HITS = 10
NUM_FILTER_TOKENS = 0

# ======================================================================================================================
# ========================================   Hardcoding the document text to mix Q
# ======================================================================================================================

# ======================================================================================================================
# ==========================================   Start actual playground
# ======================================================================================================================


if not os.path.exists(os.path.join(ROOT_FOLDER, "file-transfers")):
    os.makedirs(os.path.join(ROOT_FOLDER, "file-transfers"))

# global variables to save all past results
n_query = 0
current_query_id = 0
all_query_results = []
all_highlighted_text_list = []
all_terms_QE_list = []

FREEZE_PARAMETERS = True


@app.route("/")
def index():
    return render_template("index.html", n_query=n_query)


# ============================================================
# ============   create the query json
# ============================================================
def generate_query_file(query_input_data):
    task_title = query_input_data[0]['task_title']
    task_stmt = query_input_data[1]['task_stmt']
    task_narr = query_input_data[2]['task_narr']
    req_text = query_input_data[3]['req_text']

    print("\n\n################################################################################\n")
    print("\t\t\tReading input query info : \n")
    print("\ttask_title \t: %s\n" % task_title )
    print("\ttask_stmt  \t: %s\n" % task_stmt )
    print("\ttask_narr \t: %s\n" % task_narr )
    print("\treq_text \t: %s\n" % req_text )
    print("\n################################################################################\n\n")

    no_fd = query_input_data[4]["no_fd"]
    no_ft = query_input_data[5]["no_ft"]
    firstRangeInput = query_input_data[6]["queryWeight"]
    secondRangeInput = query_input_data[7]["HITLWeight"]
    remove_stopwords = query_input_data[8]["remove_stopwords"]
    if no_fd == "":
        no_fd = 10
    if no_ft == "":
        no_ft = 25
    if firstRangeInput == "":
        firstRangeInput = 0.3
    if secondRangeInput == "":
        secondRangeInput = 0.8
    if remove_stopwords == "":
        remove_stopwords = "true"

    queryWeight = firstRangeInput
    HITLWeight = (1.0 - float(secondRangeInput))

    ####################################################################################################################
    # handle example buttons
    global n_query

    # if n_query == 0:
    #     no_fd, no_ft, queryWeight, HITLWeight = 30, 100, 0.9, 0.0

    ####################################################################################################################

    query_dict = [
        {
            "task-num": "",
            "task-title": task_title,
            "task-stmt": task_stmt,
            "task-link": "",
            "task-narr": task_narr,
            "task-in-scope": "",
            "task-not-in-scope": "",
            "task-docs": ["", ""],
            "requests": [{"req-num": "", "req-text": req_text, "req-docs": ["", ""], "req-extr": [""]}],
        }
    ]
    with open(INPUT_FILE, "w") as input_json:
        json.dump(query_dict, input_json, indent=4)

    return no_fd, no_ft, queryWeight, str(HITLWeight), remove_stopwords


# ===================================================
#       Layer-1 search function - AJAX call
# ===================================================
@app.route("/layer1_search", methods=["POST", "GET"])
def result():
    if request.method == "POST":
        no_fd, no_ft, queryWeight, HITLWeight, remove_stopwords = generate_query_file(request.get_json())

        print("\n\n################################################################################\n")
        print("\t\t\tReading input parameters info : \n")
        print("\tNum. feedDocs \t\t: %s\n" % no_fd)
        print("\tNum. feedTerms  \t: %s\n" % no_ft)
        print("\tQueryWeight \t\t: %s\n" % queryWeight)
        print("\tHITLWeight \t\t\t: %s\n" % HITLWeight)
        print("\tRemove StopWords \t: %s\n" % remove_stopwords)
        print("\n################################################################################\n\n")

        # ============================================================
        # ============   Run the JAR file
        # ============================================================
        reRanking = "RM3"
        os.system(
            'java -jar {} {} {} {} {} {} {} {} {} {} {} {} "" false'.format(JAR_FILE, CONFIG_FILE, INPUT_FILE,
                                                                            OUTPUT_QE_FILE, OUTPUT_RM3_FILE,
                                                                            DISPLAY_NUM_HITS, no_fd, no_ft,
                                                                            queryWeight, HITLWeight, remove_stopwords,
                                                                            reRanking)
        )

        # ============================================================
        # ============   Prepare and return the outputs
        # ============================================================
        retrieval_dict = json.load(open(OUTPUT_RM3_FILE, "r"))

        # ============================================================
        # ============   Filter results
        # ============================================================
        for i, ret_line in enumerate(retrieval_dict):
            filtered_sentences = []
            sentences = ret_line["docTextSentences"]
            for sent in sentences:
                if len(sent.split(" ")) >= NUM_FILTER_TOKENS:
                    filtered_sentences.append(sent)

            retrieval_dict[i]["docTextSentences"] = filtered_sentences
            retrieval_dict[i]["filteredDocText"] = ". ".join(filtered_sentences)

        # ============================================================
        # ============   Highlight IE Event mentions
        # ============================================================
        print("Start doing highlighting on IE events")
        for i, ret_line in enumerate(retrieval_dict):
            ieEventsPerSentences = copy.deepcopy(ret_line["docTextSentences"])
            retrieval_dict[i]["ie_events_per_sentences"] = []
            for sent_idx in range(len(ieEventsPerSentences)):
                extractions = ret_line["eventExtractions"]
                for extraction in extractions:
                    anchor_texts = extraction['anchor']
                    agents_texts = extraction['agents']
                    patients_texts = extraction['patients']

                    for anchor_text in anchor_texts:
                        ieEventsPerSentences[sent_idx] = ieEventsPerSentences[sent_idx].replace(
                            anchor_text,
                            '<span class="ie-anchor ENT_neutral_like" \
                            onclick="IE_TAB_EntitiesClicked()" rate_val="0">{}</span>'.format(anchor_text))

                    # for agents_text in agents_texts:
                    #     ieEventsPerSentences[sent_idx] = ieEventsPerSentences[sent_idx].replace(
                    #         agents_text,
                    #         '<span class="ie-agent ENT_neutral_like" \
                    #         onclick="IE_TAB_EntitiesClicked()" rate_val="0">{}</span>'.format(agents_text))

                retrieval_dict[i]["ie_events_per_sentences"].append(Markup(ieEventsPerSentences[sent_idx]))

        # ============================================================
        # ============   Highlight entity mentions
        # ============================================================
        highlighted_text_list = {}
        ent_id = 0
        """
        - PERSON
        - ORG
        - GPE
        - FAC
        - DATE
        - WORK_OF_ART
        - TIME
        - CARDINAL
        - ORDINAL
        - MONEY
        """
        FILTER_ENT_TYPES = ["PERSON", "ORG", "GPE", "FAC", "WORK_OF_ART", "DATE"]
        print("Start doing highlighting entities on sentences")
        for i, ret_line in enumerate(retrieval_dict):
            docFilteredSentences = copy.deepcopy(ret_line["docTextSentences"])
            retrieval_dict[i]["filteredTextWithEntitiesHighlighted"] = []
            for sent_idx in range(len(docFilteredSentences)):
                entities = ret_line["entities"]
                for entity_type, entity_mentions in entities.items():
                    if entity_type not in FILTER_ENT_TYPES:
                        continue
                    for entity_mention in entity_mentions:  # filter entities
                        docFilteredSentences[sent_idx] = docFilteredSentences[sent_idx].replace(
                            entity_mention,
                            '<span class="ent-hl ENT_neutral_like" id="ent_spn_{}" \
                                                  onclick="EntityTAB_EntitiesClicked()" \
                                                  rate_val="0">{}</span>'.format(
                                ent_id, entity_mention
                            ),
                        )
                        ent_id += 1
                retrieval_dict[i]["filteredTextWithEntitiesHighlighted"].append(Markup(docFilteredSentences[sent_idx]))

        # ============================================================
        # ============   Load query-expanded terms
        # ============================================================
        terms_QE_list_RM3 = json.load(open(OUTPUT_QE_FILE, "r"))[:20]

        message = "Search document results !"

        global n_query, current_query_id
        # global current_query_id

        if n_query > 0:
            prev_terms_QE_list = all_terms_QE_list[-1]
            prev_docids = [doc['docID'] for doc in all_query_results[-1]]
        else:
            prev_terms_QE_list = []
            prev_docids = []

        n_query += 1
        current_query_id = n_query
        all_query_results.append(retrieval_dict)
        all_highlighted_text_list.append(highlighted_text_list)
        all_terms_QE_list.append(terms_QE_list_RM3)

        return jsonify(
            n_query=n_query,
            current_query_id=current_query_id,
            query_results=all_query_results[-1],
            highlighted_text_list=all_highlighted_text_list[-1],
            message_board=message,
            terms_QE_list=all_terms_QE_list[-1],
            prev_terms_QE_list=prev_terms_QE_list,
            prev_docids=prev_docids
        )


# ===================================================
#       Layer-1 search function - AJAX call
# ===================================================
@app.route("/layer2_search", methods=["POST", "GET"])  # sent_HITL_ranker
def enter_data():
    if request.method == "POST":
        no_fd, no_ft, queryWeight, HITLWeight, remove_stopwords = generate_query_file(
            request.get_json()["input_query_array"])

        print("\n\n################################################################################\n")
        print("\t\t\tReading input parameters info : \n")
        print("\tNum. feedDocs \t\t: %s\n" % no_fd)
        print("\tNum. feedTerms  \t: %s\n" % no_ft)
        print("\tQueryWeight \t\t: %s\n" % queryWeight)
        print("\tHITLWeight \t\t\t: %s\n" % HITLWeight)
        print("\tRemove StopWords \t: %s\n" % remove_stopwords)
        print("\n################################################################################\n\n")
        global n_query
        if (n_query >= 1):
            queryWeight = 0.1
            HITLWeight = 0.8
            no_fd = 10
            no_ft = 100

        # ============================================================
        # ============   Run the JAR file
        # ============================================================
        reRanking = "RM3"

        pos_text_list = []
        neg_text_list = []
        write_dict = {"positive": [], "negative": []}
        for manual_label in request.get_json()["annotations_array"]:
            key = list(manual_label.keys())[0]
            if key == "positive":
                pos_text_list.append(manual_label[key])
                write_dict["positive"].append(re.sub("[^A-Za-z0-9]+", " ", manual_label[key]))
            else:
                neg_text_list.append(manual_label[key])
                write_dict["negative"].append(re.sub("[^A-Za-z0-9]+", " ", manual_label[key]))
        if len(write_dict["positive"]) == 0:
            write_dict["positive"].append("the")
        if len(write_dict["negative"]) == 0:
            write_dict["negative"].append("the")
        with open(MANUAL_ANNOTATION_INPUT_FILE, "w") as write_json:
            json.dump(write_dict, write_json, indent=4)

        os.system(
            "java -jar {} {} \
                            {} \
                            {} \
                            {} \
                            {} {} \
                            {} \
                            {} {} \
                            {} {} {} true".format(JAR_FILE, CONFIG_FILE, INPUT_FILE, OUTPUT_QE_FILE, OUTPUT_HITL_FILE,
                                                  DISPLAY_NUM_HITS,
                                                  no_fd,
                                                  no_ft,
                                                  queryWeight,
                                                  HITLWeight,
                                                  remove_stopwords,
                                                  reRanking,
                                                  MANUAL_ANNOTATION_INPUT_FILE,
                                                  )
        )

        # ============================================================
        # ============   Prepare and return the outputs
        # ============================================================
        retrieval_dict = json.load(open(OUTPUT_HITL_FILE, "r"))

        # ============================================================
        # ============   Filter results
        # ============================================================
        for i, ret_line in enumerate(retrieval_dict):
            filtered_sentences = []
            sentences = ret_line["docTextSentences"]
            for sent in sentences:
                if len(sent.split(" ")) >= NUM_FILTER_TOKENS:
                    filtered_sentences.append(sent)

            retrieval_dict[i]["docTextSentences"] = filtered_sentences
            retrieval_dict[i]["filteredDocText"] = ". ".join(filtered_sentences)

        # ============================================================
        # ============   Highlight IE Event mentions
        # ============================================================
        print("Start doing highlighting on IE events")
        for i, ret_line in enumerate(retrieval_dict):
            ieEventsPerSentences = copy.deepcopy(ret_line["docTextSentences"])
            retrieval_dict[i]["ie_events_per_sentences"] = []
            for sent_idx in range(len(ieEventsPerSentences)):
                extractions = ret_line["eventExtractions"]
                for extraction in extractions:
                    anchor_texts = extraction['anchor']
                    agents_texts = extraction['agents']
                    patients_texts = extraction['patients']
                    # anchor_hString

                    for anchor_text in anchor_texts:
                        ieEventsPerSentences[sent_idx] = ieEventsPerSentences[sent_idx].replace(
                            anchor_text,
                            '<span class="ie-anchor ENT_neutral_like" \
                            onclick="IE_TAB_EntitiesClicked()" rate_val="0">{}</span>'.format(anchor_text))

                    # for agents_text in agents_texts:
                    #     ieEventsPerSentences[sent_idx] = ieEventsPerSentences[sent_idx].replace(
                    #         agents_text,
                    #         '<span class="ie-agent ENT_neutral_like" \
                    #         onclick="IE_TAB_EntitiesClicked()" rate_val="0">{}</span>'.format(agents_text))

                retrieval_dict[i]["ie_events_per_sentences"].append(Markup(ieEventsPerSentences[sent_idx]))

        # ============================================================
        # ============   Highlight entity mentions
        # ============================================================
        highlighted_text_list = {}
        ent_id = 0
        FILTER_ENT_TYPES = ["PERSON", "ORG", "GPE", "FAC", "WORK_OF_ART", "DATE"]
        for i, ret_line in enumerate(retrieval_dict):
            docText = copy.deepcopy(ret_line["filteredDocText"])
            entities = ret_line["entities"]
            for entity_type, entity_mentions in entities.items():
                if entity_type not in FILTER_ENT_TYPES:
                    continue
                for entity_mention in entity_mentions:
                    docText = docText.replace(
                        entity_mention,
                        '<span class="ent-hl ENT_neutral_like" id="ent_spn_{}" \
                                              onclick="EntityTAB_EntitiesClicked()" \
                                              rate_val="0">{}</span>'.format(
                            ent_id, entity_mention
                        ),
                    )
                    ent_id += 1
            highlighted_text_list[ret_line["docID"]] = Markup(docText)

        message = "Refined search document results !"
        # ============================================================
        # ============   Load query-expanded terms
        # ============================================================
        terms_QE_list_HITL = json.load(open(OUTPUT_QE_FILE, "r"))[:20]

        # global n_query
        global current_query_id

        if n_query > 0:
            prev_terms_QE_list = all_terms_QE_list[-1]
            prev_docids = [doc['docID'] for doc in all_query_results[-1]]
        else:
            prev_terms_QE_list = []
            prev_docids = []

        n_query += 1
        current_query_id = n_query
        all_query_results.append(retrieval_dict)
        all_highlighted_text_list.append(highlighted_text_list)
        all_terms_QE_list.append(terms_QE_list_HITL)

        return jsonify(
            n_query=n_query,
            current_query_id=current_query_id,
            query_results=all_query_results[-1],
            highlighted_text_list=all_highlighted_text_list[-1],
            message_board=message,
            terms_QE_list=all_terms_QE_list[-1],
            prev_terms_QE_list=prev_terms_QE_list,
            prev_docids=prev_docids
        )


@app.route("/prev", methods=["POST", "GET"])
def prev_query():
    global n_query, current_query_id
    if current_query_id > 1:
        current_query_id -= 1  # NOTE: this starts with 1

    if current_query_id > 1:
        prev_terms_QE_list = all_terms_QE_list[current_query_id - 2]
        prev_docids = [doc['docID'] for doc in all_query_results[current_query_id - 2]]
    else:
        prev_terms_QE_list = []
        prev_docids = []

    return jsonify(
        n_query=n_query,
        current_query_id=current_query_id,
        query_results=all_query_results[current_query_id - 1],
        highlighted_text_list=all_highlighted_text_list[current_query_id - 1],
        terms_QE_list=all_terms_QE_list[current_query_id - 1],
        prev_terms_QE_list=prev_terms_QE_list,
        prev_docids=prev_docids
    )


@app.route("/next", methods=["POST", "GET"])
def next_query():
    global n_query, current_query_id
    if current_query_id < n_query:
        current_query_id += 1  # NOTE: this starts with 1

    if current_query_id > 1:
        prev_terms_QE_list = all_terms_QE_list[current_query_id - 2]
        prev_docids = [doc['docID'] for doc in all_query_results[current_query_id - 2]]
    else:
        prev_terms_QE_list = []
        prev_docids = []

    return jsonify(
        n_query=n_query,
        current_query_id=current_query_id,
        query_results=all_query_results[current_query_id - 1],
        highlighted_text_list=all_highlighted_text_list[current_query_id - 1],
        terms_QE_list=all_terms_QE_list[current_query_id - 1],
        prev_terms_QE_list=prev_terms_QE_list,
        prev_docids=prev_docids
    )


@app.route("/reset", methods=["POST"])
def reset():
    print('Resetting search history')
    global n_query, current_query_id, all_query_results, all_highlighted_text_list, all_terms_QE_list
    n_query = 0
    current_query_id = 0
    all_query_results = []
    all_highlighted_text_list = []
    all_terms_QE_list = []
    return jsonify({'message': 'Reset Success!'})


if __name__ == "__main__":
    app.run(debug=True)
