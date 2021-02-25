package ArabicSearch;

public class TermWrapper implements Comparable{

    String text;
    long freq;
    double score;
    long docCount;

    public TermWrapper(){
        this.text = "";
        this.freq = -1;
        this.score = -1;
    }
    public TermWrapper(String text,long freq){
        this.text = text;
        this.freq = freq;
        this.score = -1;
        this.docCount = -1;
    }
    @Override
    public int compareTo(Object o) {
        TermWrapper other = (TermWrapper) o ;
        if (this.text.equals(other.text)==true ){
            return 0;
        }else{
            if(this.freq >= other.freq){
                return 1;
            }else{
                return -1 ;
            }
        }
    }
    @Override
    public String toString(){
        return this.text + " " + this.freq;
    }
    @Override
    public int hashCode(){
        return this.text.hashCode();
    }
    @Override
    public boolean equals(Object o){
        TermWrapper other = (TermWrapper) o;
        return this.text.equals(other.text);
    }
}
