import json
import os
import argparse


def function(source_file, dest_dir, chunk_size, max_documents, secondary_dir):
    chunk_size = int(chunk_size)
    max_documents = int(max_documents)
    if not os.path.exists(dest_dir):
        os.makedirs(dest_dir)

    if not os.path.exists(secondary_dir):
        os.makedirs(secondary_dir)


    data_samples = [json.loads(line) for line in open(source_file, 'r')]
    print(len(data_samples))


    break_flag_count = 0
    chunk_data = []
    for data_sample in data_samples:
        obj = data_sample["derived-metadata"]

        if break_flag_count % chunk_size == 0:
            chunk_data = []

        chunk_data.append(data_sample)

        print("Reading text sample %s with UUID : %s" % (break_flag_count, obj['id']))

        break_flag_count += 1

        if break_flag_count % chunk_size == 0:
            save_filename = os.path.join(dest_dir, "data_%d.json" % (break_flag_count / chunk_size))
            with open(save_filename, 'w', encoding='utf-8') as file:
                json.dump(chunk_data, file, ensure_ascii=False, indent=4)

        if (max_documents > 0) and (break_flag_count > max_documents): break

    save_filename = os.path.join(dest_dir, "data_%d_finalPiece.json" % (break_flag_count / chunk_size))
    with open(save_filename, 'w', encoding='utf-8') as file:
        json.dump(chunk_data, file, ensure_ascii=False, indent=4)


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
    parser.add_argument('secondary_dir', type=str,
                        help='Secondary data directory')

    args = parser.parse_args()

    function(args.source_file, args.dest_dir, args.chunk_size, args.max_documents, args.secondary_dir)


