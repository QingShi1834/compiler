import org.antlr.v4.runtime.*;

public class MyErrorListener extends BaseErrorListener {
    boolean syntaxError = false;

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg, RecognitionException e) {
        syntaxError = true;
        System.err.printf("Error type B at Line %d: %s\n", line, msg);
    }
}
