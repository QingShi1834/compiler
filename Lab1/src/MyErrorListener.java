import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class MyErrorListener extends BaseErrorListener {
    public boolean err = false;
    // 当遇到错误时会调用这个方法
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        // 打印错误信息
//        System.err.println("line " + line + ":" + charPositionInLine + " " + msg);
        err = true;
        System.err.println("Error type A at Line " + line + ": "+msg+".");
        // 抛出异常，停止解析过程
//        throw new RuntimeException(msg);
    }
}
