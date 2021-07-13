import json
import os
import argparse


def function(source_file, dest_dir, chunk_size, max_documents, raw_data_dir, tokenized_data_dir, sentseg_data_dir):
    chunk_size = int(chunk_size)
    max_documents = int(max_documents)
    if not os.path.exists(dest_dir):
        os.makedirs(dest_dir)

    if not os.path.exists(raw_data_dir):
        os.makedirs(raw_data_dir)
    if not os.path.exists(tokenized_data_dir):
        os.makedirs(tokenized_data_dir)
    if not os.path.exists(sentseg_data_dir):
        os.makedirs(sentseg_data_dir)

    data_samples = [json.loads(line) for line in open(source_file, 'r')]
    print(len(data_samples))

    break_flag_count = 0
    for data_sample in data_samples:
        obj = data_sample["derived-metadata"]

        print("Reading text sample %s with UUID : %s" % (break_flag_count + 1, obj['id']))

        docID = obj['id']
        docText = obj['text'].encode('utf-8')
        segments = obj['segment-sections']        

        save_seg_filename = os.path.join(sentseg_data_dir, "{}.json".format(docID))
        with open(save_seg_filename, 'w') as seg_file:
            json.dump(segments, seg_file, indent=4)


        save_filename = os.path.join(dest_dir, "data_%d.txt" % (break_flag_count // chunk_size))
        with open(save_filename, 'a') as f:
            f.write("DOCUMENT\n{}\n".format(docID))
            f.write("{}".format(docText))
            f.write("\n")

        raw_save_filename = os.path.join(raw_data_dir, "{}.txt".format(docID))
        with open(raw_save_filename, 'w') as raw_f:
            raw_f.write("{}".format(docText))

        break_flag_count += 1

        if (max_documents > 0) and (break_flag_count >= max_documents):
            break
        


if __name__ == "__main__":
    # Instantiate the parser
    parser = argparse.ArgumentParser(description='Chunking larger file using python script arguments')

    # Required positional argument
    parser.add_argument('source_file', type=str,
                        help='Source data file')
    parser.add_argument('dest_dir', type=str,
                        help='Destination data directory')
    parser.add_argument('chunk_size', type=str,
                        help='Chunk size to process')
    parser.add_argument('max_documents', type=str,
                        help='Maximum documents to process')
    parser.add_argument('raw_data_dir', type=str,
                        help='Raw Arabic data directory')
    parser.add_argument('tokenized_data_dir', type=str,
                        help='Tokenized Aarabic data directory')
    parser.add_argument('sentseg_data_dir', type=str,
                        help='Sentence Segmentation data directory')

    args = parser.parse_args()

    function(args.source_file, args.dest_dir, args.chunk_size, args.max_documents, args.raw_data_dir, args.tokenized_data_dir, args.sentseg_data_dir)

