package david.i.am.panels;

public class ParseError extends RuntimeException {
    public ParseError(String s, Exception e) {
        super(s, e);
    }
}
