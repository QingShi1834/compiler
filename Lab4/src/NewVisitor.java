import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.bytedeco.llvm.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.*;

public class NewVisitor extends SysYParserBaseVisitor<LLVMValueRef>{
    //创建module
    LLVMModuleRef module = LLVMModuleCreateWithName("module");

    //初始化IRBuilder，后续将使用这个builder去生成LLVM IR
    LLVMBuilderRef builder = LLVMCreateBuilder();

    //考虑到我们的语言中仅存在int一个基本类型，可以通过下面的语句为LLVM的int型重命名方便以后使用
    LLVMTypeRef i32Type = LLVMInt32Type();

    public int parseInt(String str){
        int decimal;
        if (str.startsWith("0x")||str.startsWith("0X")) { decimal = Integer.parseInt(str.substring(2), 16); }
        else if (str.startsWith("0") && str.length() != 1) { decimal = Integer.parseInt(str.substring(1), 8); }
        else { decimal = Integer.parseInt(str); }
        return decimal;
    }
//    @Override
//    public LLVMValueRef visit(ParseTree tree) {
//        return
//    }

    @Override
    public LLVMValueRef visitTerminal(TerminalNode node) {
        if (node.getSymbol().getType() == 34){//整数
            int value = parseInt(node.getText());
            return LLVMConstInt(i32Type,value,1);
        }
        return super.visitTerminal(node);
    }

    /**
     * 本次实验需要完成对main函数的翻译，要求如下
     * main函数无参数且返回值为int类型
     * 仅包含return语句
     * @param ctx the parse tree
     * @return main函数
     */
    @Override
    public LLVMValueRef visitFuncDef(SysYParser.FuncDefContext ctx) {

        //生成返回值类型
        LLVMTypeRef returnType = i32Type;

        //生成函数参数类型
        LLVMTypeRef argumentTypes = LLVMVoidType();

        //生成函数类型
        LLVMTypeRef ft = LLVMFunctionType(returnType, argumentTypes, /* argumentCount */ 0, /* isVariadic */ 0);

        String funcName = ctx.IDENT().getText();

        LLVMValueRef function = LLVMAddFunction(module, /*functionName:String*/ funcName, ft);

        //通过如下语句在函数中加入基本块，一个函数可以加入多个基本块
        LLVMBasicBlockRef block = LLVMAppendBasicBlock(function, /*blockName:String*/ "mainEntry");

        //选择要在哪个基本块后追加指令
        LLVMPositionBuilderAtEnd(builder, block);//后续生成的指令将追加在block的后面

        super.visitFuncDef(ctx);

        return function;

    }



    @Override
    public LLVMValueRef visitExp(SysYParser.ExpContext ctx) {
        if (ctx.unaryOp() != null) { // unaryOp exp
            String operator = ctx.unaryOp().getText();
            if (operator.equals("+")){// + exp
                return visit(ctx.exp().get(0));
            }else if (operator.equals("-")){// - exp
                int exp_value = (int) LLVMConstIntGetZExtValue(visitExp(ctx.exp().get(0)));
                return LLVMConstInt(i32Type,-exp_value,1);
            }else if (operator.equals("!")) {// ! exp
                LLVMValueRef val = visit(ctx.exp().get(0));
                // 生成icmp
                LLVMValueRef tmp_ = LLVMBuildICmp(builder, LLVMIntNE, LLVMConstInt(i32Type, 0, 0), val, "tmp_");
                // 生成xor
                tmp_ = LLVMBuildXor(builder, tmp_, LLVMConstInt(LLVMInt1Type(), 1, 0), "tmp_");
                // 生成zext
                tmp_ = LLVMBuildZExt(builder, tmp_, i32Type, "tmp_");

                return tmp_;
            }

        }else if (ctx.MUL() != null || ctx.DIV() != null
                   || ctx.MOD() != null || ctx.PLUS() != null || ctx.MINUS() != null) {// + - * / %

                LLVMValueRef leftVal = visit(ctx.exp().get(0));
                LLVMValueRef rightVal = visit(ctx.exp().get(1));

                if (ctx.MUL() != null){
                   return LLVMBuildMul(builder,leftVal,rightVal,"result");
                }else if (ctx.DIV() != null){
                   return LLVMBuildSDiv(builder,leftVal,rightVal,"result");
                }else if (ctx.MOD() != null){
                   return LLVMBuildSRem(builder,leftVal,rightVal,"result");
                }else if (ctx.PLUS() != null){
                   return LLVMBuildAdd(builder,leftVal,rightVal,"result");
                }else {
                   return LLVMBuildSub(builder,leftVal,rightVal,"result");
                }
            }
        return super.visitExp(ctx);
    }

    @Override
    public LLVMValueRef visitStmt(SysYParser.StmtContext ctx) {
        if (ctx.RETURN() != null){
            LLVMValueRef ret = visitExp(ctx.exp());
            return LLVMBuildRet(builder,ret);
        }
        return super.visitStmt(ctx);
    }
}
