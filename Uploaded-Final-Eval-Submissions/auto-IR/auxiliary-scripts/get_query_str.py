import json
import sys

TOKEN_LIMIT = 30

with open(sys.argv[1]) as f:
	data = json.load(f)
for taskRequests in data:
	tr = taskRequests["taskRequests"]
	for task in tr:
		tokens = task["reqQueryText"].strip().split()
		for i in range(0, len(tokens), TOKEN_LIMIT):
			print(' '.join(tokens[i : i + TOKEN_LIMIT]))