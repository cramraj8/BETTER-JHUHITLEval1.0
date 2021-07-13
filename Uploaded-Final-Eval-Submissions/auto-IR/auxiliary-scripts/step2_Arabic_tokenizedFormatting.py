import argparse
import glob
import os


def dump_tokenized_document_batches(source_dir, dest_dir):
    if not os.path.exists(dest_dir):
        os.makedirs(dest_dir)

    # tokenized arabic data loading
    batch_arab_files = glob.glob(os.path.join(source_dir, "*.txt"))
    for batch_arab_file in batch_arab_files:
        with open(batch_arab_file, 'r') as f:
            batch_arab_data = f.read().strip()
            # arab_docs = batch_arab_data.split("< DOC - SEP >")
            arab_docs = batch_arab_data.split("DOCUMENT")
            arab_docs = [e for e in arab_docs if e != ""]
            n_docs = len(arab_docs)
            for arab_doc_idx, arab_doc in enumerate(arab_docs):
                tmp_docLines = arab_doc.strip().split("\n")
                tmp_docLines = [e for e in tmp_docLines if e != ""]
                tmp_docID = tmp_docLines[0].replace(" ", "")
                if tmp_docID == "":
                    print("... ... ... DocID was not found in batched file !")
                    continue

                tmp_docText = "\n".join(tmp_docLines[1:])
                save_docFilename = os.path.join(dest_dir, "{}.txt".format(tmp_docID))
                with open(save_docFilename, 'w') as write_file:
                    write_file.write(tmp_docText)

                if arab_doc_idx % 1000 == 0:
                    print("Finished deserializing tokenized documents : {} / {}".format(arab_doc_idx + 1, n_docs))


if __name__ == "__main__":
    # Instantiate the parser
    parser = argparse.ArgumentParser(description='Deserializing batched files in python script arguments')

    # Required positional argument
    parser.add_argument('source_dir', type=str,
                        help='Source Arabic data directory')
    parser.add_argument('dest_dir', type=str,
                        help='Destination Arabic data directory')

    args = parser.parse_args()

    dump_tokenized_document_batches(args.source_dir, args.dest_dir)
