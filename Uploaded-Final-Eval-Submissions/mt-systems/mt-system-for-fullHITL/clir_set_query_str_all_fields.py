import json
import codecs
import sys

#######
# python set_query_str.py json.en translation json.ar
#######

with open(sys.argv[1], encoding='utf-8') as f:
    data = json.load(f)

translations = []
with open(sys.argv[2], encoding='utf-8') as f2:
    for line in f2:
        line = line.rstrip('\n')
        translations.append(line)

translationCounter = 0
for task in data:

    task["task-title"] = translations[translationCounter]
    translationCounter += 1
    task["task-stmt"] = translations[translationCounter]
    translationCounter += 1    
    task["task-narr"] = translations[translationCounter]
    translationCounter += 1

    tr = task["requests"]
    for request in tr:
        request["req-text"] = translations[translationCounter]
        translationCounter += 1
        request["expanded_text"] = translations[translationCounter]
        translationCounter += 1

    

with open(sys.argv[3], 'w', encoding='utf8') as write_ar_json:
    json.dump(data, write_ar_json, ensure_ascii=False, indent=4)