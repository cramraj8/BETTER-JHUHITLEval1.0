import json
import sys

#######
# python set_query_str.py json.en translation json.ar
#######

TOKEN_LIMIT = 30

with open(sys.argv[1], encoding='utf-8') as f:
    data = json.load(f)

with open(sys.argv[2], encoding='utf-8') as f:
    for taskRequests in data:
        tr = taskRequests["taskRequests"]
        for task in tr:
            tokens = task["reqQueryText"].strip().split()
            translations = []
            for _ in range(0, len(tokens), TOKEN_LIMIT):
                translations.append(next(f).strip())
            task["reqQueryText"] = ' '.join(translations)

with open(sys.argv[3], 'w', encoding='utf8') as f:
    json.dump(data, f, ensure_ascii=False, indent=4)