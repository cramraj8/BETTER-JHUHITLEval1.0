package RMSearchSimple;

// implementation of weighted term (term, score) pairs
public class Gram implements WeightedTerm {

    public String term;
    public double score;

    public Gram(String t) {
        term = t;
        score = 0.0;
    }

    @Override
    public String getTerm() {
        return term;
    }

    @Override
    public double getWeight() {
        return score;
    }

    // The secondary sort is to have defined behavior for statistically tied samples.
    // @Override
    public int compareTo(WeightedTerm other) {
        Gram that = (Gram) other;
        int result = this.score > that.score ? -1 : (this.score < that.score ? 1 : 0);
        if (result != 0) {
            return result;
        }
        result = (this.term.compareTo(that.term));
        return result;
    }

    @Override
    public String toString() {
        return "<" + term + "," + score + ">";
    }
}