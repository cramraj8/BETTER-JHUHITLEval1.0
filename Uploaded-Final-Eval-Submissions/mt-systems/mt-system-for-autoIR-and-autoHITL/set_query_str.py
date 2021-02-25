import json
import codecs
import sys
import math

#######
# python set_query_str.py json.en translation json.ar
#######

num_splits = 5

with open(sys.argv[1], encoding='utf-8') as f:
    data = json.load(f)


translations = []
append_line = ''
i = 0
with open(sys.argv[2], encoding='utf-8') as f2:
    for line in f2:
        line = line.rstrip('\n')
        if (i + 1) % num_splits == 0 and i != 0:
            translations.append(append_line)
            append_line = ''
        else:
            append_line += ' ' + line
        i += 1

translationCounter = 0
for taskRequests in data:
    tr = taskRequests["taskRequests"]
    for task in tr:
        task["reqQueryText"] = translations[translationCounter]
        translationCounter = translationCounter + 1

with open(sys.argv[3], 'w', encoding='utf8') as write_ar_json:
    json.dump(data, write_ar_json, ensure_ascii=False, indent=4)