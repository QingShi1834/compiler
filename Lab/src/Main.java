import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.bytedeco.javacpp.BytePointer;

import java.io.IOException;

import static org.bytedeco.llvm.global.LLVM.LLVMDisposeMessage;
import static org.bytedeco.llvm.global.LLVM.LLVMPrintModuleToFile;

public class Main
{
    public static final BytePointer error = new BytePointer();

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
        NewVisitor llvmVisitor = new NewVisitor();
        llvmVisitor.visit(tree);

        if (LLVMPrintModuleToFile(llvmVisitor.module, args[1], error) != 0) {    // moudle是你自定义的LLVMModuleRef对象
            LLVMDisposeMessage(error);
        }


    }
}