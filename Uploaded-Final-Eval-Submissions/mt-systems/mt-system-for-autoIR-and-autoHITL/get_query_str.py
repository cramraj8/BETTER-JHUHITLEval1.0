from __future__ import division
import json
import sys
import math

with open(sys.argv[1]) as f:
	data = json.load(f)

num_splits = 5

for taskRequests in data:
	tr = taskRequests["taskRequests"]
	for task in tr:
		line = task["reqQueryText"]
		line = line.rstrip('\n').replace('\n', '')
		doc_splits = line.split()
		line_length = len(doc_splits)

		if (line_length < num_splits * 2 + 2):
			doc_splits.extend(['the'] * num_splits * 2)

		unit_length = int(math.ceil(line_length / num_splits))
		cnt = 0
		# tmp_text_len = 1000
		tmp_text_len = 500

		for i in range(num_splits - 1):

			# if unit_length > 1000:
			if unit_length > tmp_text_len:
				print(' '.join(doc_splits[cnt: cnt + tmp_text_len]) )
			else:
				print(' '.join(doc_splits[cnt: cnt + unit_length]) )
			cnt += unit_length


		print(' '.join(doc_splits[cnt:]) )

