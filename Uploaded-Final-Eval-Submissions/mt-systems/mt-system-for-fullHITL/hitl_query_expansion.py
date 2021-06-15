import json
import argparse
import os 
import glob
import csv
import string 
import logging

stop_words =['i', 'me', 'my', 'myself', 'we', 'our', 'ours', 'ourselves', 'you', "you're", "you've", "you'll",
 "you'd", 'your', 'yours', 'yourself', 'yourselves', 'he', 'him', 'his', 'himself', 'she', "she's", 'her', 'hers', 
 'herself', 'it', "it's", 'its', 'itself', 'they', 'them', 'their', 'theirs', 'themselves', 'what', 'which', 
 'who', 'whom', 'this', 'that', "that'll", 'these', 'those', 'am', 'is', 'are', 'was', 'were', 'be', 'been', 'being', 
 'have', 'has', 'had', 'having', 'do', 'does', 'did', 'doing', 'a', 'an', 'the', 'and', 'but', 'if', 'or', 'because', 'as', 
 'until', 'while', 'of', 'at', 'by', 'for', 'with', 'about', 'against', 'between', 'into', 'through', 'during', 'before',
  'after', 'above', 'below', 'to', 'from', 'up', 'down', 'in', 'out', 'on', 'off', 'over', 'under', 'again', 'further', 
  'then', 'once', 'here', 'there', 'when', 'where', 'why', 'how', 'all', 'any', 'both', 'each', 'few', 'more', 'most', 
  'other', 'some', 'such', 'no', 'nor', 'not', 'only', 'own', 'same', 'so', 'than', 'too', 'very', 's', 't', 'can', 'will', 
  'just', 'don', "don't", 'should', "should've", 'now', 'd', 'll', 'm', 'o', 're', 've', 'y', 'ain', 'aren', "aren't", 'couldn',
   "couldn't", 'didn', "didn't", 'doesn', "doesn't", 'hadn', "hadn't", 'hasn', "hasn't", 'haven', "haven't", 'isn', "isn't", 'ma',
    'mightn', "mightn't", 'mustn', "mustn't", 'needn', "needn't", 'shan', "shan't", 'shouldn', "shouldn't", 'wasn', "wasn't", 
    'weren', "weren't", 'won', "won't", 'wouldn', "wouldn't"]



def preprocess(args):
    logging.info('start!!')
    
    exclude = set(string.punctuation)
    new_dataset = list()
    with open(args.input_file, 'r') as fin:
        dataset = json.load(fin)
    for data in dataset:
        task_sents = list()
        new_request = list()
        task_entity_dict = dict()
        task_sent_expand_words = list()
        #### find task level annotation
        for request in data['requests']:
            if 'req-num' not in request:
                continue
            req_num = request['req-num']
            path = args.label_dir + req_num + '/'
            if os.path.isdir(path):
                files = glob.glob(path + '*')

                for file in files:
                    if file == path + 'entity.txt':
                        with open(file, 'r') as f:
                            lines = f.readlines()
                            for line in lines:
                                entity = line.replace('\n', '')
                                if entity not in task_entity_dict:
                                    task_entity_dict[entity] = 1
                                else:
                                    task_entity_dict[entity] += 1
                    elif file == path + 'terms.tsv':
                        continue

                    else:   
                        with open(file) as ifile:
                            reader = csv.reader(ifile, delimiter='\t')
                            for row in reader:
                                request_label = row[2]
                                task_label = row[1]
                                if task_label == 'checked':
                                    task_sents.append(row[4])

        
        
        word_dict = dict()
        if len(task_sents) > 0:
            for sent in task_sents:
                sent = ''.join(ch for ch in sent if ch not in exclude)
                word_list = [wd for wd in sent.split(' ') if wd !='']
                for word in word_list:
                    word = word.lower()
                    if word not in word_dict:
                        word_dict[word] = 1.0 / len(word_list)
                    else:
                        word_dict[word] += 1.0 / len(word_list)
                        

        sorted_dict = dict(sorted(word_dict.items(), key=lambda item: item[1], reverse=True))
        count = 0 
        for word in sorted_dict:
            if word in stop_words:
                continue
            count += 1
            task_sent_expand_words.append(word)
            if count > 10:
                break

        ### request level data

        for request in data['requests']:
            if 'req-num' not in request:
                logging.warning('HITL: REQUEST NUMBER not found!!')
                request['expanded_text'] = ''
                new_request.append(request)
                continue

            req_num = request['req-num']
            
            #query = request['req-text']
            path = args.label_dir + req_num + '/'
            all_entities = list()
            request_sents = list()
            word_dict = dict()
            sent_expand_words = list()
            query_words = list()

                
            
            if os.path.isdir(path):
                files = glob.glob(path + '*')
                
                for file in files:
                    if file == path + 'entity.txt':
                        with open(file, 'r') as f:
                            lines = f.readlines()
                            for line in lines:
                                entity = line.replace('\n', '')
                                all_entities.append(entity)
                    elif file == path + 'terms.tsv':
                        with open(file) as ifile:
                            reader = csv.reader(ifile, delimiter='\t')
                            for row in reader:
                                request_label = row[1]
                                if request_label == 'checked':
                                    query_words.append(row[0])

                    else:   
                        with open(file) as ifile:
                            reader = csv.reader(ifile, delimiter='\t')
                            for row in reader:
                                request_label = row[2]
                                task_label = row[1]
                                if request_label == 'checked':
                                    request_sents.append(row[4])
                                    
                
                if len(request_sents) > 0:
                    for sent in request_sents:
                        sent = ''.join(ch for ch in sent if ch not in exclude)
                        word_list = [wd for wd in sent.split(' ') if wd !='']
                        for word in word_list:
                            word = word.lower()
                            if word not in word_dict:
                                word_dict[word] = 1.0 / len(word_list)
                            else:
                                word_dict[word] += 1.0 / len(word_list)
                             

                sorted_dict = dict(sorted(word_dict.items(), key=lambda item: item[1], reverse=True))
                count = 0 
                for word in sorted_dict:
                    if word in stop_words:
                        continue
                    count += 1
                    sent_expand_words.append(word)
                    if count > 10:
                        break


                
                if len(query_words) > 0:
                    query = ''
                for word in query_words:
                    query = query + word + ' '

                    
                for word in all_entities:
                    query = query + ' ' + word.replace('’',"\'")
                for word in sent_expand_words:
                    query = query + ' ' + word.replace('’',"\'")








            else:
                query = ''
                for word in task_entity_dict:
                    if task_entity_dict[word] > 1:
                        query = query + ' ' + word.replace('’',"\'")
                for word in task_sent_expand_words:
                    query = query + ' ' + word.replace('’',"\'")

            ### todo:add terms
            
            request['expanded_text'] = query
            new_request.append(request)
        data['requests'] = new_request      
        new_dataset.append(data)  
        #print(task_entity_dict)
        #print(task_sent_expand_words)
        #input()
        #print(data)
    with open(args.output_file, 'w', encoding='utf-8') as f:
        json.dump(new_dataset, f, indent=2)



def main():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--input_file",
        default="/fs/clip-quiz/chen/car/better-ui-real/better-ui/data/ir-hitl-performer-tasks.json",
        type=str,
        help="The input task file",
    )
    parser.add_argument(
        "--label_dir",
        default="/fs/clip-quiz/chen/car/better-ui-real/better-ui/data/",
        type=str,
        help="The input task file",
    )
    parser.add_argument(
        "--output_file",
        default="/fs/clip-quiz/chen/car/better-ui-real/better-ui/data/ir-tasks-after-hitl.json",
        type=str,
        help="The input task file",
    )

    args = parser.parse_args()
    preprocess(args)


if __name__ == '__main__':
    main()
