import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.IOException;

public class Main
{
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("input path is required");
        }
        String source = args[0];
        CharStream input = CharStreams.fromFileName(source);
        SysYLexer sysYLexer = new SysYLexer(input);
//        sysYLexer.removeErrorListeners();;
        CommonTokenStream tokens = new CommonTokenStream(sysYLexer);
        SysYParser sysYParser = new SysYParser(tokens);

//        sysYParser.removeErrorListeners();
//        MyErrorListener myErrorListener = new MyErrorListener();
//        myErrorListener.syntaxError = false;
//        sysYParser.addErrorListener(myErrorListener);

        ParseTree tree = sysYParser.program();
        Visitor visitor = new Visitor();
        visitor.visit(tree);

        if (!visitor.err){//如果没有语义错误
            for (String obj : Visitor.syntaxTree){
                System.err.print(obj);
            }
        }

    }
}