package RMSearchSimple;


/**
 * A generic interface for weighted 'terms', although it may also apply to
 * phrases.
 */
public interface WeightedTerm extends Comparable<WeightedTerm> {
    public String getTerm();
    public double getWeight();
    public int compareTo(WeightedTerm other);
}