#!/usr/bin/env python3.8

'''BETTER IR scorer
version 1.1
18 Aug 2020
Ian Soboroff

This is free and unencumbered software released into the public domain.

Anyone is free to copy, modify, publish, use, compile, sell, or
distribute this software, either in source code form or as a compiled
binary, for any purpose, commercial or non-commercial, and by any
means.

In jurisdictions that recognize copyright laws, the author or authors
of this software dedicate any and all copyright interest in the
software to the public domain. We make this dedication for the benefit
of the public at large and to the detriment of our heirs and
successors. We intend this dedication to be an overt act of
relinquishment in perpetuity of all present and future rights to this
software under copyright law.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

For more information, please refer to <https://unlicense.org>
'''

import logging
from collections import defaultdict


class Measure(object):
    '''
    Measures are the base class for evaluation measures computed using the
    EvalJig.
    self.formatstr: how to format the measure score as a string
    compute(): compute the measure
    reduce(): compute an summary (for example, an average or max) of scores
    pretty() and pretty_mean() are used by the EvalJig's output routines.
    '''
    def __init__(self):
        self.formatstr = '{:.4f}'

    def __str__(self):
        return 'measure name'

    def compute(self, resp, **kwargs):
        return 0

    def reduce(self, scores):
        '''Default reduce operator is arithmetic mean.'''
        return sum(scores.values()) / len(scores.values())

    def pretty(self, topic, value):
        return topic + ' ' + str(self) + ' ' + self.formatstr.format(value)

    def pretty_mean(self, value):
        '''Print the measure's mean value.'''
        return "all " + str(self) + " " + self.formatstr.format(value)


class EvalJig:
    def __init__(self, doc_qrels, extr_qrels, extractions):
        self.ops = []
        self.score = defaultdict(dict)
        self.means = dict()
        self.topics = set()
        self.evaldepth = 1000
        self.verbose = True
        self.qrels = doc_qrels
        self.erels = extr_qrels
        self.extractions = extractions

    def add_op(self, meas):
        self.ops.append(meas)

    def compute(self, topic, ranking):
        self.topics.add(topic)
        log.debug('---' + topic)
        for op in self.ops:
            self.score[str(op)][topic] = op.compute(ranking,
                                                    topic=topic,
                                                    qrels=self.qrels[topic])

    def zero(self, topic):
        self.topics.add(topic)
        for op in self.ops:
            self.score[str(op)][topic] = 0

    def comp_means(self):
        for op in self.ops:
            self.means[str(op)] = op.reduce(self.score[str(op)])

    def print_scores_for(self, topic):
        for op in self.ops:
            opname = str(op)
            if topic in self.score[opname]:
                output = op.pretty(topic, self.score[opname][topic])
                if output:
                    print(output)

    def print_scores(self):
        for topic in sorted(self.topics):
            self.print_scores_for(topic)

    def print_means(self):
        for op in self.ops:
            opname = str(op)
            if self.means[opname] is not None:
                output = op.pretty_mean(self.means[opname])
                if output:
                    print(output)


class Precision(Measure):
    '''Set precision, with an optional cutoff.'''
    def __init__(self, cutoff=None, min_rel_level=1):
        super().__init__()
        self.cutoff = cutoff
        self.min_rel_level = min_rel_level

    def __str__(self):
        if self.cutoff:
            return f'prec_{self.cutoff}'
        else:
            return 'prec'

    def compute(self, resp, qrels, **kwargs):
        if len(resp) == 0:
            return 0.0
        if self.cutoff:
            resp = resp[:self.cutoff]
        rel_ret = sum(1 for doc in resp
                      if doc in qrels and qrels[doc] >= self.min_rel_level)
        if self.cutoff:
            return float(rel_ret) / self.cutoff
        else:
            return float(rel_ret) / len(resp)


class RPrecision(Measure):
    '''Set precision with a cutoff at the number of relevant documents.'''
    def __init__(self, min_rel_level=1):
        super().__init__()
        self.min_rel_level = min_rel_level

    def __str__(self):
        return 'Rprec'

    def compute(self, resp, qrels, **kwargs):
        if len(resp) == 0:
            return 0.0
        cutoff = sum(1 for doc in qrels if qrels[doc] >= self.min_rel_level)
        if cutoff == 0:
            return 0.0
        resp = resp[:cutoff]
        rel_ret = sum(1 for doc in resp
                      if doc in qrels and qrels[doc] >= self.min_rel_level)
        return float(rel_ret) / cutoff


class Recall(Measure):
    '''Set recall, with an optional cutoff.'''
    def __init__(self, cutoff=None, min_rel_level=1):
        super().__init__()
        self.cutoff = cutoff
        self.min_rel_level = min_rel_level

    def __str__(self):
        if self.cutoff:
            return f'recl_{self.cutoff}'
        else:
            return 'recl'

    def compute(self, resp, qrels, **kwargs):
        if len(resp) == 0:
            return 0.0
        if self.cutoff:
            resp = resp[:self.cutoff]
        num_rel = sum([1 for doc in resp
                       if doc in qrels and qrels[doc] >= self.min_rel_level])
        tot_rel = sum([1 for doc in qrels if qrels[doc] >= self.min_rel_level])
        return float(num_rel) / tot_rel


class NDCG(Measure):
    '''Compute nDCG measure down to the given cutoff.
    Reference: Jarvelin and Kekalainen, "Cumulated gain-based evaluation of IR
    techniques", ACM TOIS 20(4):422-446, 2002.
    '''
    def __init__(self, cutoff=None,
                 gain_mapping={0: 0, 1: 1, 2: 2, 3: 5, 4: 10}):
        super().__init__()
        self.cutoff = cutoff
        self.gains = gain_mapping

    def __str__(self):
        if self.cutoff:
            return f'nDCG_{self.cutoff}'
        else:
            return 'nDCG'

    def _gain(self, doc, qrels):
        if doc not in qrels:
            return 0.0
        rel = qrels[doc]
        return self.gains[rel]

    def _discount(self, rank):
        from math import log2
        return log2(rank + 1)

    def _add_gains(self, resp, qrels):
        cutoff = self.cutoff or sum(1 for doc in qrels if qrels[doc] > 0)
        gain_vec = []
        for i in range(cutoff):
            if i >= len(resp):
                break
            doc = resp[i]
            gain = self._gain(doc, qrels)
            gain_vec.append({'doc': doc,
                             'gain': gain})
        return gain_vec

    def _rank_discount(self, gain_vec, start_rank=1):
        for i in range(len(gain_vec)):
            discount = self._discount(i + start_rank)
            gain_vec[i]['gain'] /= discount
        return gain_vec

    def compute(self, resp, qrels, **kwargs):
        gain_vec = self._add_gains(resp, qrels)
        gain_vec = self._rank_discount(gain_vec)
        dcg = sum(d['gain'] for d in gain_vec)

        ideal_resp = [doc for doc in qrels if qrels[doc] > 0]
        ideal_resp = sorted(ideal_resp, key=lambda doc: self.gains[qrels[doc]], reverse=True)
        ideal_gain_vec = self._add_gains(ideal_resp, qrels)
        ideal_gain_vec = self._rank_discount(ideal_gain_vec)
        idcg = sum(d['gain'] for d in ideal_gain_vec)

        if idcg > 0.0:
            return dcg / idcg
        else:
            return 0.0


class AlphaNDCG(NDCG):
    '''Compute alpha-nDCG measure down to a given cutoff.  Alpha is a decay
    parameter for the value of documents retrieved covering an already-seen
    subtopic.
    Reference: Clarke et al., "A Comparative Analysis of Cascade Measures for
    Novelty and Diversity", WSDM 2011.
    NB: this version of a-nDCG is unusual in supporting alpha discounting with
    graduated gain values.  If the gain for all documents is 1, then this
    should be equivalent to (WSDM 2011).
    '''
    def __init__(self, extractions, subtopic_qrels,
                 gain_mapping={0: 0.0, 1: 1.0, 2: 2.0, 3: 5.0, 4: 10.0},
                 cutoff=None, alpha=0.5):
        super().__init__()
        self.subtopic_qrels = subtopic_qrels
        self.extractions = extractions
        self.gains = {int(rel): float(gain) for rel, gain in gain_mapping.items()}
        self.cutoff = cutoff
        self.alpha = alpha
        self.idcg = {}

    def __str__(self):
        if self.cutoff:
            return f'anDCG_{self.cutoff}'
        else:
            return 'anDCG'

    def _subtopic_discount_gain(self, gain_obj, topic, subtopic_discounts):
        gain = gain_obj['gain']
        doc = gain_obj['doc']
        if gain == 0.0 or doc not in self.subtopic_qrels[topic]:
            return gain
        sg = 0.0
        for subtopic in self.subtopic_qrels[topic][doc]:
            sg += gain * subtopic_discounts[subtopic]
        return sg

    def _update_subtopic_discounts(self, topic, doc, subtopic_discounts):
        if doc in self.subtopic_qrels[topic]:
            for subtopic in self.subtopic_qrels[topic][doc]:
                subtopic_discounts[subtopic] *= 1 - self.alpha

    def _subtopic_discount(self, gain_vec, topic):
        subtopic_discounts = {extr: 1.0 for extr in self.extractions[topic]}
        for gain_obj in gain_vec:
            gain_obj['gain'] = self._subtopic_discount_gain(gain_obj, topic,
                                                            subtopic_discounts)
            self._update_subtopic_discounts(topic, gain_obj['doc'], subtopic_discounts)
        return gain_vec

    def _ideal_gain_vec(self, qrels, topic):
        '''This takes into account subtopic discounting to find the ideal gain vector.'''
        rel_set = set(qrels.keys())
        rank = 1
        ideal_gain_vec = []

        while len(rel_set) > 0:
            gains = []
            discounts = {subtopic: 1.0 for subtopic in self.extractions[topic]}
            for doc in rel_set:
                # Compute the gain for each document if it was ranked here.
                gain_obj = {'doc': doc, 'gain': self._gain(doc, qrels)}
                gain_obj['gain'] = self._subtopic_discount_gain(gain_obj, topic, discounts)
                gain_obj['gain'] /= self._discount(rank)
                gains.append(gain_obj)
            # The document with the largest discounted gain at this rank is the one
            # to append to the ideal ranking.
            biggest = max(gains, key=lambda o: o['gain'])
            ideal_gain_vec.append(biggest)
            rank += 1
            self._update_subtopic_discounts(topic, biggest['doc'], discounts)
            rel_set.remove(biggest['doc'])

        return ideal_gain_vec

    def compute(self, resp, qrels, topic, **kwargs):
        gain_vec = self._add_gains(resp, qrels)
        gain_vec = self._subtopic_discount(gain_vec, topic)
        gain_vec = self._rank_discount(gain_vec)
        dcg = sum(d['gain'] for d in gain_vec)

        if topic in self.idcg:
            idcg = self.idcg[topic]
        else:
            ideal_gain_vec = self._ideal_gain_vec(qrels, topic)
            idcg = sum(d['gain'] for d in ideal_gain_vec)
            self.idcg[topic] = idcg

        if idcg > 0.0:
            return dcg / idcg
        else:
            return 0.0


class RelString(Measure):
    '''Print a nice string representation of the ranked list.'''
    def __init__(self, cutoff=None):
        super().__init__()
        self.cutoff = cutoff
        self.symbol = { 0: '0',
                        1: '1',
                        2: '2',
                        3: '3',
                        4: '4'}

    def __str__(self):
        return 'relstr'

    def compute(self, resp, qrels, **kwargs):
        s = []
        if self.cutoff:
            resp = resp[:self.cutoff]
        for doc in resp:
            if doc in qrels:
                log.debug(f'{doc} {qrels[doc]}')
                s.append(self.symbol[qrels[doc]])
            else:
                print(doc, '-')
                s.append('-')
        return ''.join(s)

    def reduce(self, scores):
        pass

    def pretty(self, topic, value):
        return topic + ' ' + str(self) + ' ' + value

    def pretty_mean(self, value):
        return None


class NumRequests(Measure):
    '''Count the number of requests.'''
    def __init__(self):
        super().__init__()
        self.formatstr = '{:d}'

    def __str__(self):
        return 'num_req'

    def compute(self, resp, topic, **kwargs):
        return 1

    def reduce(self, scores):
        return sum(scores.values())

    def pretty(self, topic, value):
        return None


if __name__ == '__main__':
    import argparse
    import json

    argparser = argparse.ArgumentParser(
        description='BETTER IR scorer',
        formatter_class=argparse.ArgumentDefaultsHelpFormatter
    )
    argparser.add_argument('-d', '--debug',
                           action='store_true',
                           help='Enable debugging output')
    argparser.add_argument('topic_file',
                           help='JSON topics file',
                           default='dry-run-topics.final.json')
    argparser.add_argument('req_qrels',
                           help='Request relevance judgments file',
                           default='req-qrels')
    argparser.add_argument('extr_qrels',
                           help='Critical extractions judgments file',
                           default='extr-qrels')
    argparser.add_argument('submission',
                           help='IR task retrieval output file')

    args = argparser.parse_args()

    logging.basicConfig(format='%(message)s')
    log = logging.getLogger(__name__)

    if args.debug:
        log.setLevel(logging.DEBUG)

    # Load critical extractions from the topics file.
    # This has the complete list of critical extractions, which
    # we need for alpha-ndcg.

    extractions = defaultdict(dict)
    with open(args.topic_file) as fp:
        topics = json.load(fp)
        for topic in topics:
            for request in topic['requests']:
                reqnum = request['req-num']
                for extr in request['req-extr']:
                    extractions[reqnum][str(extr['id'])] = extr['extr']

    # Load document-level relevance judgments

    qrels = defaultdict(dict)
    with open(args.req_qrels, 'r') as fp:
        for line in fp:
            reqnum, docid, rel = line.split()
            qrels[reqnum][docid] = int(rel)

    # Load document-level critical extraction judgments

    erels = defaultdict(lambda: defaultdict(dict))
    with open(args.extr_qrels, 'r') as fp:
        for line in fp:
            reqnum, docid, extrid, rel = line.split()
            erels[reqnum][docid][extrid] = int(rel)

    # Set up the measures

    jig = EvalJig(qrels, erels, extractions)
    jig.add_op(NumRequests())
    jig.add_op(RelString(cutoff=10))
    jig.add_op(Precision(cutoff=10))
    jig.add_op(Recall(cutoff=10))
    jig.add_op(RPrecision())
    jig.add_op(NDCG())
    jig.add_op(AlphaNDCG(extractions, erels, alpha=0.5))

    # Load the submission

    submission = defaultdict(dict)
    with open(args.submission, 'r') as fp:
        for line in fp:
            reqnum, _, docid, _, score, _ = line.split()
            submission[reqnum][docid] = float(score)

    # Score the submission

    for request in qrels:
        jig.zero(request)
        ranking = [k for k, v in sorted(submission[request].items(), reverse=True,
                                        key=lambda x: x[1])]
        jig.compute(request, ranking)

    jig.print_scores()
    jig.comp_means()
    jig.print_means()


"""

python3 better_ir_scorer.py dry-run-topics.final.json req-qrels extr-qrels JHU_BETTER-AUTO-RM3-Results.txt

"""