import org.antlr.v4.runtime.*;

import java.io.IOException;
import java.util.List;

public class Main
{
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("input path is required");
        }
        String source = args[0];
        CharStream input = CharStreams.fromFileName(source);
        SysYLexer sysYLexer = new SysYLexer(input);

        MyErrorListener myErrorListener = new MyErrorListener();
        myErrorListener.err = false;
        sysYLexer.removeErrorListeners();
        sysYLexer.addErrorListener(myErrorListener);
        List<? extends Token> tokens = sysYLexer.getAllTokens();
        if (!myErrorListener.err){
            for (Token token:
                    tokens) {
                String temp = token.getText();
                int val = 0;
                if (token.getType() == 34){
                    val = trans(temp);
                }
                System.err.println(sysYLexer.getRuleNames()[token.getType() - 1]+ " " + (token.getType() == 34?val:temp) + " " + "at Line " + token.getLine() + ".");
            }
        }
    }
    public static int trans(String str){
        int decimal = 0;
        if (str.startsWith("0x")||str.startsWith("0X")) { decimal = Integer.parseInt(str.substring(2), 16); }
        else if (str.startsWith("0") && str.length() != 1) { decimal = Integer.parseInt(str.substring(1), 8); }
        else { decimal = Integer.parseInt(str); }
        return decimal;
    }
}