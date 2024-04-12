import org.antlr.v4.runtime.tree.TerminalNode;
import org.bytedeco.javacpp.Pointer;
import org.bytedeco.javacpp.PointerPointer;
import org.bytedeco.llvm.LLVM.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;

import static org.bytedeco.llvm.global.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.LLVMBuildZExt;

public class NewVisitor extends SysYParserBaseVisitor<LLVMValueRef>{
    //创建module
    LLVMModuleRef module = LLVMModuleCreateWithName("module");

    //初始化IRBuilder，后续将使用这个builder去生成LLVM IR
    LLVMBuilderRef builder = LLVMCreateBuilder();

    //考虑到我们的语言中仅存在int一个基本类型，可以通过下面的语句为LLVM的int型重命名方便以后使用
    LLVMTypeRef i32Type = LLVMInt32Type();

    LLVMTypeRef voidType = LLVMVoidType();

    LLVMTypeRef pointerToIntType = LLVMPointerType(i32Type, 0);

    LLVMValueRef zero = LLVMConstInt(i32Type,0,0);

    LocalScope globalScope;
    Scope currentScope;

    private LLVMValueRef currentFunction;

    private boolean returnState = false;

    private boolean isArrayAddr = false;

    private Stack<LLVMBasicBlockRef> whileConditionStack = new Stack<>();
    private  Stack<LLVMBasicBlockRef> afterWhileStack = new Stack<>();

    private final Map<String, String> funcAndRetTypeMap = new LinkedHashMap<>();

    public NewVisitor(){
        //初始化LLVM
        LLVMInitializeCore(LLVMGetGlobalPassRegistry());
        LLVMLinkInMCJIT();
        LLVMInitializeNativeAsmPrinter();
        LLVMInitializeNativeAsmParser();
        LLVMInitializeNativeTarget();
    }

    public int parseInt(String str){
        int decimal;
        if (str.startsWith("0x")||str.startsWith("0X")) { decimal = Integer.parseInt(str.substring(2), 16); }
        else if (str.startsWith("0") && str.length() != 1) { decimal = Integer.parseInt(str.substring(1), 8); }
        else { decimal = Integer.parseInt(str); }
        return decimal;
    }

    @Override
    public LLVMValueRef visitTerminal(TerminalNode node) {
        if (node.getSymbol().getType() == 34){//整数
            int value = parseInt(node.getText());
            return LLVMConstInt(i32Type,value,1);
        }
        return super.visitTerminal(node);
    }

    @Override
    public LLVMValueRef visitProgram(SysYParser.ProgramContext ctx) {
        currentScope = globalScope = new LocalScope(null);
        LLVMValueRef ret = super.visitProgram(ctx);
        currentScope = currentScope.getFatherScope();
        return ret;
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
        LLVMTypeRef returnType = ctx.funcType().getText().equals("int")?i32Type:voidType;
        //生成函数参数类型
        int paramsNum = 0;
        if (ctx.funcFParams() != null){
            paramsNum = ctx.funcFParams().funcFParam().size();
        }
        PointerPointer<Pointer> argumentTypes = new PointerPointer<>(paramsNum);
        for (int i = 0; i < paramsNum; i++) {
//            argumentTypes.put(i,i32Type);

            SysYParser.FuncFParamContext fParamContext =  ctx.funcFParams().funcFParam(i);
            LLVMTypeRef paramType = fParamContext.bType().getText().equals("int")?i32Type:voidType;
            if (fParamContext.L_BRACKT().size() > 0){
                paramType = LLVMPointerType(paramType,0);
            }
            argumentTypes.put(i,paramType);
        }


        //生成函数类型
        LLVMTypeRef ft = LLVMFunctionType(returnType, argumentTypes, /* argumentCount */ paramsNum, /* isVariadic */ 0);

        String funcName = ctx.IDENT().getText();
        funcAndRetTypeMap.put(funcName,ctx.funcType().getText());

        LLVMValueRef function = LLVMAddFunction(module, /*functionName:String*/ funcName, ft);
        currentFunction = function;
//        funcAndRetType.put(funcName,ctx.funcType().getText());
        currentScope.define(funcName,function,ft);

        //通过如下语句在函数中加入基本块，一个函数可以加入多个基本块
        LLVMBasicBlockRef block = LLVMAppendBasicBlock(function, /*blockName:String*/ funcName+"Entry");

        //选择要在哪个基本块后追加指令
        LLVMPositionBuilderAtEnd(builder, block);//后续生成的指令将追加在block的后面
        currentScope = new LocalScope(currentScope);//参数的作用域
        for (int i = 0; i < paramsNum; i++) {
            SysYParser.FuncFParamContext paramContext = ctx.funcFParams().funcFParam(i);
            String paramName = paramContext.IDENT().getText();
            LLVMTypeRef paramType = paramContext.bType().getText().equals("int")?i32Type:voidType;
            if (paramContext.L_BRACKT().size() != 0 ){
                paramType = LLVMPointerType(paramType,0);
            }
            LLVMValueRef paramPointer = LLVMBuildAlloca(builder,paramType, "pointerTo" + paramName);
            currentScope.define(paramName,paramPointer,paramType);
            LLVMValueRef argVal = LLVMGetParam(function,i);
            LLVMBuildStore(builder,argVal,paramPointer);
        }
        returnState = false;//初始化是没有完成return
        super.visitFuncDef(ctx);

        currentScope = currentScope.getFatherScope();
//        if (!returnState){//如果没有完成return
//            LLVMBuildRetVoid(builder);
//        }
        //if (retType.equals(voidType)) {
        //			LLVMBuildRet(builder, null);
        //		} else {
        //			LLVMBuildRet(builder, zero);
        //		}
        if (returnType.equals(voidType)){
            LLVMBuildRet(builder,null);
        }
        if (returnType.equals(i32Type)){
            LLVMBuildRet(builder,zero);
        }
        returnState = true;
        return function;

    }


    @Override
    public LLVMValueRef visitBlock(SysYParser.BlockContext ctx) {
        LocalScope localScope = new LocalScope(currentScope);
//        if (ctx.blockItem().size() == 0){
//            return LLVMBuildRetVoid(builder);
//        }
        currentScope = localScope;
        LLVMValueRef ret = super.visitBlock(ctx);
        currentScope = currentScope.getFatherScope();
        return ret;
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

        }else if (ctx.lVal() != null){//lVal exp
            LLVMValueRef lValPointer = this.visitLVal(ctx.lVal());
            if (isArrayAddr) {
                isArrayAddr = false;
                return lValPointer;
            }
            return LLVMBuildLoad(builder, lValPointer, ctx.lVal().getText());

        } else if (ctx.number() != null) {//num exp

            int constVal = parseInt(ctx.number().INTEGER_CONST().getText());

            return LLVMConstInt(i32Type,constVal,0);

        } else if (ctx.MUL() != null || ctx.DIV() != null
                   || ctx.MOD() != null || ctx.PLUS() != null || ctx.MINUS() != null) {// + - * / %

            LLVMValueRef leftVal = visit(ctx.exp().get(0));
            LLVMValueRef rightVal = visit(ctx.exp().get(1));

            if (ctx.MUL() != null){
               return LLVMBuildMul(builder,leftVal,rightVal,"result_");
            }else if (ctx.DIV() != null){
               return LLVMBuildSDiv(builder,leftVal,rightVal,"result_");
            }else if (ctx.MOD() != null){
               return LLVMBuildSRem(builder,leftVal,rightVal,"result_");
            }else if (ctx.PLUS() != null){
               return LLVMBuildAdd(builder,leftVal,rightVal,"result_");
            }else {
               return LLVMBuildSub(builder,leftVal,rightVal,"result_");
            }
        }else if (ctx.IDENT() != null){//IDENT L_PAREN funcRParams? R_PAREN
            String funcName = ctx.IDENT().getText();
            LLVMValueRef func = currentScope.findSymbol(funcName);
            int argsNum = 0;
            if (ctx.funcRParams() != null){
                argsNum = ctx.funcRParams().param().size();
            }

            PointerPointer<Pointer> args = new PointerPointer<>(argsNum);
            for (int i = 0; i < argsNum; i++) {
                SysYParser.ParamContext param = ctx.funcRParams().param(i);
                args.put(i,visitExp(param.exp()));
            }
            if (funcAndRetTypeMap.get(funcName).equals("void")){
                funcName = "";
            }
            return LLVMBuildCall(builder,func,args,argsNum,funcName);
        }else if (ctx.L_PAREN() != null){
            return visitExp(ctx.exp(0));
        }
        return super.visitExp(ctx);
    }

    @Override
    public LLVMValueRef visitStmt(SysYParser.StmtContext ctx) {
        if (ctx.RETURN() != null){
            LLVMValueRef ret = null;
            if (ctx.exp() != null){
                ret = visitExp(ctx.exp());
            }
            returnState = true;
            return LLVMBuildRet(builder,ret);
        }

        if (ctx.ASSIGN() != null){
//            String lVarName = ctx.lVal().getText();
            LLVMValueRef lVal = visitLVal(ctx.lVal());
            LLVMValueRef rVal = visit(ctx.exp());
            return LLVMBuildStore(builder,rVal,lVal);
        }

        if (ctx.IF() != null){//IF L_PAREN cond R_PAREN stmt (ELSE stmt)?
            LLVMValueRef condBoolean = visitCond(ctx.cond());
            LLVMValueRef condition = LLVMBuildICmp(builder, LLVMIntNE, zero, condBoolean, "cmp_result");

            LLVMBasicBlockRef ifTrue = LLVMAppendBasicBlock(currentFunction, "trueBlock");
            LLVMBasicBlockRef ifFalse = LLVMAppendBasicBlock(currentFunction, "falseBlock");
            LLVMBasicBlockRef afterBlock = LLVMAppendBasicBlock(currentFunction, "afterBlock");

            //条件跳转指令，选择跳转到哪个块
            LLVMBuildCondBr(builder,
                    /*condition:LLVMValueRef*/ condition,
                    /*ifTrue:LLVMBasicBlockRef*/ ifTrue,
                    /*ifFalse:LLVMBasicBlockRef*/ ifFalse);
            //if true
            LLVMPositionBuilderAtEnd(builder,ifTrue);
            visitStmt(ctx.stmt(0));
            LLVMBuildBr(builder,afterBlock);
            //if false
            LLVMPositionBuilderAtEnd(builder, ifFalse);
            if (ctx.ELSE() != null){
                visitStmt(ctx.stmt(1));
            }
            LLVMBuildBr(builder,afterBlock);
            LLVMPositionBuilderAtEnd(builder, afterBlock);
            return null;
        }

        if (ctx.WHILE() != null){
            LLVMBasicBlockRef whileCondition = LLVMAppendBasicBlock(currentFunction, "whileCondition");
            LLVMBasicBlockRef whileBody = LLVMAppendBasicBlock(currentFunction, "whileBody");
            LLVMBasicBlockRef next = LLVMAppendBasicBlock(currentFunction, "next");

            LLVMBuildBr(builder,whileCondition);

            LLVMPositionBuilderAtEnd(builder, whileCondition);
            LLVMValueRef conditionBool = visitCond(ctx.cond());
            LLVMValueRef cmpResult = LLVMBuildICmp(builder, LLVMIntNE, zero, conditionBool, "cmp_result");
            LLVMBuildCondBr(builder, cmpResult, whileBody, next);

            LLVMPositionBuilderAtEnd(builder, whileBody);
            whileConditionStack.push(whileCondition);
            afterWhileStack.push(next);
            visitStmt(ctx.stmt(0));

            LLVMBuildBr(builder, whileCondition);
            whileConditionStack.pop();
            afterWhileStack.pop();
            LLVMBuildBr(builder, next);

            LLVMPositionBuilderAtEnd(builder, next);
            return null;
        }

        if (ctx.BREAK() != null){
            return LLVMBuildBr(builder, afterWhileStack.peek());
        }

        if (ctx.CONTINUE() != null){
            return LLVMBuildBr(builder, whileConditionStack.peek());
        }
        return super.visitStmt(ctx);
    }



    @Override
    public LLVMValueRef visitCond(SysYParser.CondContext ctx) {
        if (ctx.exp() != null){
            return visitExp(ctx.exp());
        } else if (ctx.AND() != null) {
//            LLVMValueRef bool;

            LLVMBasicBlockRef cond1_block = LLVMAppendBasicBlock(currentFunction,"cond1_block");
            LLVMBasicBlockRef cond2_block = LLVMAppendBasicBlock(currentFunction,"cond2_block");
            LLVMBasicBlockRef condAll_block = LLVMAppendBasicBlock(currentFunction,"condAll_block");

            LLVMValueRef bool = LLVMBuildAlloca(builder,i32Type,"judgeAllCond_i32");
            LLVMBuildBr(builder,cond1_block);
            LLVMPositionBuilderAtEnd(builder,cond1_block);
            LLVMValueRef cond1 = visitCond(ctx.cond(0));
            LLVMValueRef judgeCond1_i1 = LLVMBuildICmp(builder,LLVMIntNE,zero,cond1,"judgeCond1_i1");
            LLVMValueRef judgeCond1_i32 = LLVMBuildZExt(builder,judgeCond1_i1,i32Type,"judgeCond1_i32");
//            bool = judgeCond1_i32;
            LLVMBuildStore(builder,judgeCond1_i32,bool);
            LLVMBuildCondBr(builder,judgeCond1_i1,cond2_block,condAll_block);

            LLVMPositionBuilderAtEnd(builder,cond2_block);
            LLVMValueRef cond2 = visitCond(ctx.cond(1));
            LLVMValueRef judgeCond2_i1 = LLVMBuildICmp(builder,LLVMIntNE,zero,cond2,"judgeCond2_i1");
            LLVMValueRef judgeCond2_i32 = LLVMBuildZExt(builder,judgeCond2_i1,i32Type,"judgeCond2_i32");
//            bool = LLVMBuildAnd(builder,judgeCond1_i32,judgeCond2_i32,"judgeAll_i32");
            LLVMBuildStore(builder,judgeCond2_i32,bool);
            LLVMBuildBr(builder,condAll_block);

            LLVMPositionBuilderAtEnd(builder,condAll_block);

            return LLVMBuildLoad(builder,bool,"logicBool");

        } else if (ctx.OR() != null) {

            LLVMBasicBlockRef cond1_block = LLVMAppendBasicBlock(currentFunction,"cond1_block");
            LLVMBasicBlockRef cond2_block = LLVMAppendBasicBlock(currentFunction,"cond2_block");
            LLVMBasicBlockRef condAll_block = LLVMAppendBasicBlock(currentFunction,"condAll_block");

            LLVMValueRef bool = LLVMBuildAlloca(builder,i32Type,"judgeAllCond_i32");

            LLVMBuildBr(builder,cond1_block);
            LLVMPositionBuilderAtEnd(builder,cond1_block);
            LLVMValueRef cond1 = visitCond(ctx.cond(0));
            LLVMValueRef judgeCond1_i1 = LLVMBuildICmp(builder,LLVMIntNE,zero,cond1,"judgeCond1_i1");
            LLVMValueRef judgeCond1_i32 = LLVMBuildZExt(builder,judgeCond1_i1, i32Type,"judgeCond1_i32");
            LLVMBuildStore(builder,judgeCond1_i32,bool);
            LLVMBuildCondBr(builder,judgeCond1_i1,condAll_block,cond2_block);

            LLVMPositionBuilderAtEnd(builder,cond2_block);
//            LLVMValueRef judgeCond1_i32 = LLVMBuildZExt(builder,bool,i32Type,"judgeCond1_i32");
            LLVMValueRef cond2 = visitCond(ctx.cond(1));
            LLVMValueRef judgeCond2_i1 = LLVMBuildICmp(builder,LLVMIntNE,zero,cond2,"judgeCond2_i1");
            LLVMValueRef judgeCond2_i32 = LLVMBuildZExt(builder,judgeCond2_i1,i32Type,"judgeCond2_i32");
//            bool = LLVMBuildOr(builder,judgeCond1_i32,judgeCond2_i32,"judgeAll_i32");
            LLVMBuildStore(builder,judgeCond2_i32,bool);
            LLVMBuildBr(builder,condAll_block);

            LLVMPositionBuilderAtEnd(builder,condAll_block);
//            bool = LLVMBuildZExt(builder,bool,i32Type,"exp_bool");

            return LLVMBuildLoad(builder,bool,"logicBool");

        } else if (ctx.EQ() != null || ctx.NEQ() != null) {
            LLVMValueRef lvalue = visitCond(ctx.cond(0));
            LLVMValueRef rvalue = visitCond(ctx.cond(1));
            LLVMValueRef result = null;

            if (ctx.EQ() != null){ // ==
                result = LLVMBuildICmp(builder,LLVMIntEQ,lvalue,rvalue,"cmp_result");
            }else { // !=
                result = LLVMBuildICmp(builder,LLVMIntNE,lvalue,rvalue,"cmp_result");
            }

            return LLVMBuildZExt(builder, result, i32Type, "zext_res");
        } else if (ctx.LT() != null || ctx.GT() != null || ctx.LE() != null || ctx.GE() != null) {
            LLVMValueRef lvalue = visitCond(ctx.cond(0));
            LLVMValueRef rvalue = visitCond(ctx.cond(1));
            LLVMValueRef result = null;
            if (ctx.LT() != null){
                result = LLVMBuildICmp(builder,LLVMIntSLT,lvalue,rvalue,"cmp_result");
            } else if (ctx.GT() != null) {
                result = LLVMBuildICmp(builder,LLVMIntSGT,lvalue,rvalue,"cmp_result");
            } else if (ctx.LE() != null) {
                result = LLVMBuildICmp(builder,LLVMIntSLE,lvalue,rvalue,"cmp_result");
            }else if (ctx.GE() != null){
                result = LLVMBuildICmp(builder,LLVMIntSGE,lvalue,rvalue,"cmp_result");
            }

            return LLVMBuildZExt(builder, result, i32Type, "zext_res");
        }

        return super.visitCond(ctx);
    }

    @Override

    public LLVMValueRef visitVarDecl(SysYParser.VarDeclContext ctx) {

        for (SysYParser.VarDefContext varDefContext : ctx.varDef()) {
            String varName = varDefContext.IDENT().getText();

            LLVMTypeRef varType = i32Type;
            
            int elementNum = 0;

            for (SysYParser.ConstExpContext constExpContext : varDefContext.constExp()) {
                elementNum = parseInt(constExpContext.getText());
                varType = LLVMArrayType(varType,elementNum);
            }

            LLVMValueRef varPointer = null;
            if (currentScope == globalScope){
                varPointer = LLVMAddGlobal(module,varType,varName);
                if (elementNum == 0){
                    LLVMSetInitializer(varPointer, zero);
                }else {
                    PointerPointer<Pointer> pointerPointer = new PointerPointer<>(elementNum);
                    for (int i = 0; i < elementNum; i++) {
                        pointerPointer.put(i,zero);
                    }
                    LLVMValueRef arraylist = LLVMConstArray(varType,pointerPointer,elementNum);
                    LLVMSetInitializer(varPointer,arraylist);
                }

            }else {
                varPointer = LLVMBuildAlloca(builder,varType,varName);
            }

            if (varDefContext.ASSIGN() != null){
                SysYParser.ExpContext initValExp = varDefContext.initVal().exp();
//                LLVMValueRef varVal = visitExp(varDefContext.initVal().exp());
                if (initValExp != null){//直接赋一个表达式的值

                    LLVMValueRef varInitVal = visitExp(initValExp);

                    if (currentScope == globalScope){
                        LLVMSetInitializer(varPointer,varInitVal);
                    }else {
                        LLVMBuildStore(builder,varInitVal,varPointer);
                    }

                }else {//数组赋值
                    int initValCount = varDefContext.initVal().initVal().size();
                    SysYParser.InitValContext initValContext = varDefContext.initVal();
                    if (currentScope == globalScope){//全局数组
                        PointerPointer<Pointer> pointerPointer = new PointerPointer<>(elementNum);//这里是所有的元素组成一个集合
                        //把已在初始{}中出现的赋值

                        for (int i = 0; i < initValCount; i++) {
                            pointerPointer.put(i,visitExp(initValContext.initVal(i).exp()));
                        }
                        for (int i = initValCount; i < elementNum; i++) {
                            pointerPointer.put(i,zero);
                        }

                        LLVMValueRef assignArray = LLVMConstArray(varType,pointerPointer,elementNum);
                        LLVMSetInitializer(varPointer,assignArray);

                    } else {
                        LLVMValueRef[] assignArray = new LLVMValueRef[elementNum];
                        for (int i = 0; i < initValCount; i++) {
                            assignArray[i] = visitExp(initValContext.initVal(i).exp());
                        }
                        for (int i = initValCount; i < elementNum; i++) {
                            assignArray[i] = zero;
                        }

                        buildGEP(elementNum,varPointer,assignArray);
                    }
                }


            }

            currentScope.define(varName,varPointer,varType);
        }

        return null;
    }

    @Override
    public LLVMValueRef visitConstDecl(SysYParser.ConstDeclContext ctx) {
        for (SysYParser.ConstDefContext constDefContext : ctx.constDef()) {

            String constVarName = constDefContext.IDENT().getText();

            LLVMTypeRef constVarType = i32Type;

            int elementNum = 0;

            for (SysYParser.ConstExpContext constExpContext : constDefContext.constExp()) {
                elementNum = parseInt(constExpContext.getText());
                constVarType = LLVMArrayType(constVarType,elementNum);
            }

            LLVMValueRef constVarPointer;
            if (currentScope == globalScope){
                constVarPointer = LLVMAddGlobal(module,constVarType,constVarName);
                if (elementNum == 0){
                    LLVMSetInitializer(constVarPointer, zero);
                }else {
                    PointerPointer<Pointer> pointerPointer = new PointerPointer<>(elementNum);
                    for (int i = 0; i < elementNum; i++) {
                        pointerPointer.put(i,zero);
                    }
                    LLVMValueRef arraylist = LLVMConstArray(constVarType,pointerPointer,elementNum);
                    LLVMSetInitializer(constVarPointer,arraylist);
                }

            }else {
                constVarPointer = LLVMBuildAlloca(builder,constVarType,constVarName);
            }
            //assign一定存在
            SysYParser.ConstExpContext initValExp = constDefContext.constInitVal().constExp();
//                LLVMValueRef varVal = visitExp(varDefContext.initVal().exp());
            if (initValExp != null){//直接赋一个表达式的值

                LLVMValueRef varInitVal = visitConstExp(initValExp);

                if (currentScope == globalScope){
                    LLVMSetInitializer(constVarPointer,varInitVal);
                }else {
                    LLVMBuildStore(builder,varInitVal,constVarPointer);
                }

            }else {//数组赋值
                int initValCount = constDefContext.constInitVal().constInitVal().size();
                SysYParser.ConstInitValContext initValContext = constDefContext.constInitVal();
                if (currentScope == globalScope){//全局数组
                    PointerPointer<Pointer> pointerPointer = new PointerPointer<>(elementNum);//这里是所有的元素组成一个集合
                    //把已在初始{}中出现的赋值

                    for (int i = 0; i < initValCount; i++) {
                        pointerPointer.put(i,visitConstExp(initValContext.constInitVal(i).constExp()));
                    }
                    for (int i = initValCount; i < elementNum; i++) {
                        pointerPointer.put(i,zero);
                    }

                    LLVMValueRef assignArray = LLVMConstArray(constVarType,pointerPointer,elementNum);
                    LLVMSetInitializer(constVarPointer,assignArray);

                } else {
                    LLVMValueRef[] assignArray = new LLVMValueRef[elementNum];
                    for (int i = 0; i < initValCount; i++) {
                        assignArray[i] = visitConstExp(initValContext.constInitVal(i).constExp());
                    }
                    for (int i = initValCount; i < elementNum; i++) {
                        assignArray[i] = zero;
                    }

                    buildGEP(elementNum,constVarPointer,assignArray);
                }
            }

//            String constVarName = constDefContext.IDENT().getText();
//
//            LLVMTypeRef constVarType = i32Type;

//            LLVMValueRef constVarPointer;
//            if (currentScope == globalScope){
//                constVarPointer = LLVMAddGlobal(module,constVarType,constVarName);
//                LLVMSetInitializer(constVarPointer, zero);
//            }else {
//                constVarPointer = LLVMBuildAlloca(builder,constVarType,constVarName);
//            }
//
//            if (constDefContext.ASSIGN() != null){
//                LLVMValueRef constVarVal = visit(constDefContext.constInitVal().constExp());
//                if (currentScope == globalScope){
//                    LLVMSetInitializer(constVarPointer,constVarVal);
//                }else {
//                    LLVMBuildStore(builder,constVarVal,constVarPointer);
//                }
//            }

            currentScope.define(constVarName,constVarPointer,constVarType);
        }

        return null;
    }

    @Override
    public LLVMValueRef visitLVal(SysYParser.LValContext ctx) {
        String varName = ctx.IDENT().getText();
        LLVMValueRef varPointer = currentScope.findSymbol(varName);
        LLVMTypeRef varType = currentScope.getTypeOfVar(varName);
        if (varType.equals(i32Type)){
            return varPointer;
        }else if (varType.equals(pointerToIntType)){
            if (ctx.exp().size()>0){
                LLVMValueRef[] arrayPointer = new LLVMValueRef[1];
                arrayPointer[0] = this.visit(ctx.exp(0));
                PointerPointer<LLVMValueRef> indexPointer = new PointerPointer<>(arrayPointer);
                LLVMValueRef pointer = LLVMBuildLoad(builder, varPointer, varName);
                return LLVMBuildGEP(builder, pointer, indexPointer, 1, "PointerTo" + varName);
            }else {
                return varPointer;
            }
        }else {
            LLVMValueRef[] arrayPointer = new LLVMValueRef[2];
            arrayPointer[0] = zero;
            if (ctx.exp().size()>0) {
                arrayPointer[1] = this.visitExp(ctx.exp(0));
            } else {
                isArrayAddr = true;
                arrayPointer[1] = zero;
            }
            PointerPointer<LLVMValueRef> indexPointer = new PointerPointer<>(arrayPointer);
            return LLVMBuildGEP(builder, varPointer, indexPointer, 2, "PointerTo" + varName);
        }
    }

    public void buildGEP(int elementCount, LLVMValueRef varPointer, LLVMValueRef[] initArray) {
        for (int i = 0; i < elementCount; i++) {
            PointerPointer<LLVMValueRef> indexPointer = new PointerPointer<>(zero, LLVMConstInt(i32Type, i, 0));
            LLVMValueRef elementPtr = LLVMBuildGEP(builder, varPointer, indexPointer, 2, "pointerToElement_" + i);
            LLVMBuildStore(builder, initArray[i], elementPtr);
        }
    }
}
