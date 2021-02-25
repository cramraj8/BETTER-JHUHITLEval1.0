import json
import sys

with open(sys.argv[1]) as f:
	data = json.load(f)

MAX_LEN = 500

# print(data)

for task in data:
	task_title = task["task-title"].replace('\n', '')
	task_title_list = task_title.split(' ')
	task_title = ' '.join(task_title_list[:MAX_LEN])

	task_stmt = task["task-stmt"].replace('\n', '')
	task_stmt_list = task_stmt.split(' ')
	task_stmt = ' '.join(task_stmt_list[:MAX_LEN])

	task_narr = task["task-narr"].replace('\n', '')
	task_narr_list = task_narr.split(' ')
	task_narr = ' '.join(task_narr_list[:MAX_LEN])


	print(task_title)
	print(task_stmt)
	print(task_narr)

	tr = task["requests"]
	for request in tr:
		req_text = request["req-text"].replace('\n', '')
		req_text_list = req_text.split(' ')
		req_text = ' '.join(req_text_list[:MAX_LEN])

		req_expanded_text = request["expanded_text"].replace('\n', '')
		req_expanded_text_list = req_expanded_text.split(' ')
		req_expanded_text = ' '.join(req_expanded_text_list[:MAX_LEN])

		print(req_text)
		print(req_expanded_text)