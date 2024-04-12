import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;

public class Visitor extends SysYParserBaseVisitor<Void>{
    private static int depth = 0;

    private Scope globalScope = null;

    private Scope currentScope = null;

    public boolean err = false;

    public static List<String> syntaxTree = new ArrayList<>();
    private void errReport(int typeNum, ParserRuleContext ctx, String message) {
        System.err.println("Error type " + typeNum + " at Line " + ctx.getStart().getLine() + ": " + message + ".");
        err = true;
    }

    private String getIndent() {
        return "  ".repeat(depth);
    }

    private String getHighlight(int type_id){
        if (type_id > 0 && type_id < 10){
            return "[orange]";
        }else if (type_id > 9 && type_id < 25){
            return "[blue]";
        }else if (type_id == 33){
            return "[red]";
        }else if (type_id == 34){
            return "[green]";
        }
        return "none";
    }

    private String capitalize(String word){
        return word.substring(0,1).toUpperCase() + word.substring(1);
    }

    public int parseInt(String str){
        int decimal;
        if (str.startsWith("0x")||str.startsWith("0X")) { decimal = Integer.parseInt(str.substring(2), 16); }
        else if (str.startsWith("0") && str.length() != 1) { decimal = Integer.parseInt(str.substring(1), 8); }
        else { decimal = Integer.parseInt(str); }
        return decimal;
    }

    public BasicType findBasicType(String type_name){
        return new BasicType(type_name);
    }

    @Override
    public Void visitChildren(RuleNode node) {
        RuleContext ctx = node.getRuleContext();

        String ruleName = capitalize(SysYParser.ruleNames[ctx.getRuleIndex()]);

        syntaxTree.add(getIndent() + ruleName + "\n");
//        depth = ctx.depth();
        depth++;
        Void ret = super.visitChildren(node);
        depth--;

        return ret;
    }

    @Override
    public Void visitTerminal(TerminalNode node) {
        // 一些前置工作
        Token token = node.getSymbol();
        if (token.getType() - 1 < 0){
            return super.visitTerminal(node);
        }
        String ruleName = SysYLexer.ruleNames[token.getType() - 1];
        String tokenText = node.getText();
        String color = getHighlight(token.getType());

        Symbol symbol = currentScope.getSymbol(tokenText);

        if (ruleName.equals("INTEGER_CONST")) {//转化为十进制数
            tokenText = String.valueOf(parseInt(tokenText));
        } else if (ruleName.equals("IDENT")) {//变量名
            int rowNO = token.getLine();
            int columnNO = token.getCharPositionInLine();

            if (symbol != null) {
                symbol.addCase(rowNO, columnNO);
            }
        }

        if (!color.equals("none")) {
            // 在 msgToPrint 中加入需要输出的语法树的内容
            syntaxTree.add(getIndent());
            if (symbol == null){
                syntaxTree.add(tokenText);
            }else {
                syntaxTree.add(symbol.getName());
            }
            syntaxTree.add(" " + ruleName + color + "\n");
        }

        return super.visitTerminal(node);
    }


    @Override
    public Void visitProgram(SysYParser.ProgramContext ctx) {
        // 进入新的 Scope
        currentScope = globalScope = new BaseScope(null);

        //定义两个基本类型
//        currentScope.define(new BaseSymbol("int"));
//        currentScope.define(new BasicType("void"));
        Void ret = super.visitProgram(ctx);
        currentScope = currentScope.getEnclosingScope();
        // 回到上一层 Scope
        return ret;
    }

    @Override
    public Void visitFuncDef(SysYParser.FuncDefContext ctx) {
        // 报告 Error type 4

        String functionName = ctx.IDENT().getText();

        if (currentScope.definedSymbol(functionName)){
            errReport(4,ctx,"Redefined function: " + functionName);
            return null;
        }

        String retTypeName = ctx.funcType().getText();

        Type retType = (Type) findBasicType(retTypeName);
        FunctionType functionType = new FunctionType(retType,new ArrayList<>());

//        currentScope.define(new FunctionSymbol(functionName,functionType));
        FunctionSymbol funcSym = new FunctionSymbol(functionName,currentScope,functionType);

        currentScope.define(funcSym);

        currentScope = funcSym;
        // 进入新的 Scope，定义新的 Symbol
        Void ret = super.visitFuncDef(ctx);
        currentScope = currentScope.getEnclosingScope();
        // 回到上一层 Scope
        return ret;
    }

    @Override
    public Void visitBlock(SysYParser.BlockContext ctx) {
        BaseScope localScope = new BaseScope(currentScope);
//        String localScopeName = localScope.getName() + localScopeNum;
//        localScope.setName(localScopeName);
//        localScopeNum++;
        currentScope = localScope;
        // 进入新的 Scope
        Void ret = super.visitBlock(ctx);
        currentScope = currentScope.getEnclosingScope();
        // 回到上一层 Scope
        return ret;
    }


    @Override
    public Void visitVarDecl(SysYParser.VarDeclContext ctx) {
        String typeName = ctx.bType().getText();

        for (SysYParser.VarDefContext varDefContext : ctx.varDef()) {
            // 报告 Error type 3
            Type varType = (Type) globalScope.getSymbol(typeName);
            String varName = varDefContext.IDENT().getText();

            if (currentScope.definedSymbol(varName)){
                errReport(3,varDefContext, "Redefined variable: " + varName );
                continue;
            }

            for (SysYParser.ConstExpContext constExpContext : varDefContext.constExp()){
                int elementNum = parseInt(constExpContext.getText());
                varType = new ArrayType(elementNum, varType);
            }

            if (varDefContext.ASSIGN() != null) {
                // 报告 Error type 5
                SysYParser.ExpContext expContext = varDefContext.initVal().exp();
                if (expContext != null ){
                    Type initValType = getExpType(expContext);
                    if (!initValType.toString().equals("noType")
                            && !varType.toString().equals(initValType.toString())){
                        errReport(5,varDefContext,"Type mismatched for assignment");
                    }
                }
            }

            VariableSymbol variableSymbol = new VariableSymbol(varName, varType,false);
            currentScope.define(variableSymbol);
            // 定义新的 Symbol
        }

        return super.visitVarDecl(ctx);
    }

    @Override
    public Void visitConstDecl(SysYParser.ConstDeclContext ctx) {
        // 结构同 visitVarDecl
        String typeName = ctx.bType().getText();
        for (SysYParser.ConstDefContext varDefContext : ctx.constDef()) {
            Type constType = (Type) globalScope.getSymbol(typeName);
            String constVarName = varDefContext.IDENT().getText();
            if (currentScope.definedSymbol(constVarName)){
                errReport(3,varDefContext,"Redefined variable: " + constVarName);
                continue;
            }

            for (SysYParser.ConstExpContext constExpContext : varDefContext.constExp()) {
                int elementNum = parseInt(constExpContext.getText());
                constType = new ArrayType(elementNum,constType);
            }

            SysYParser.ConstExpContext expContext = varDefContext.constInitVal().constExp();
            if (expContext != null) {
                Type initVarType = getExpType(expContext.exp());
                if (!initVarType.toString().equals("noType")
                        && !constType.toString().equals(initVarType.toString())){
                    errReport(5,varDefContext,"Type mismatched for assignment");
                }
            }

            VariableSymbol constSymbol = new VariableSymbol(constVarName,constType,true);
            currentScope.define(constSymbol);
        }

        return super.visitConstDecl(ctx);
    }

    @Override
    public Void visitFuncFParam(SysYParser.FuncFParamContext ctx) {
        // 报告 Error type 3
        // 定义新的 Symbol
        String varTypeName = ctx.bType().getText();
        Type varType = (Type) globalScope.getSymbol(varTypeName);
        for (TerminalNode ignored : ctx.L_BRACKT()) {
            varType = new ArrayType(0, varType);
        }
        String varName = ctx.IDENT().getText();
        VariableSymbol varSymbol = new VariableSymbol(varName, varType,false);

        if (currentScope.definedSymbol(varName)) {
            errReport(3, ctx, "Redefined variable: " + varName);
        } else {
            currentScope.define(varSymbol);
            ((FunctionSymbol) currentScope).getType().getParamsType().add(varType);
        }
        return super.visitFuncFParam(ctx);
    }

    private Type getLValType(SysYParser.LValContext ctx) {
        String varName = ctx.IDENT().getText();
        Symbol symbol = currentScope.getSymbol(varName);
        if (symbol == null) {
            return new BasicTypeSymbol("noType");
        }
        Type varType = symbol.getType();
        for (SysYParser.ExpContext ignored : ctx.exp()) {
            if (varType instanceof ArrayType) {
                varType = ((ArrayType) varType).elementType;
            } else {
                return new BasicTypeSymbol("noType");
            }
        }
        return varType;
    }


    @Override
    public Void visitLVal(SysYParser.LValContext ctx) {
        // 报告 Error type 1
        String varName = ctx.IDENT().getText();
        Symbol symbol = currentScope.getSymbol(varName);
        if (symbol == null) {
            errReport(1, ctx, "Undefined variable: " + varName);
            return null;
        }

        Type varType = symbol.getType();
        int arrayDimension = ctx.exp().size();
        for (int i = 0; i < arrayDimension; ++i) {
            // 报告 Error type 9
            if (varType instanceof ArrayType) {
                varType = ((ArrayType) varType).elementType;
                SysYParser.ExpContext expContext = ctx.exp(i);
                varName += "[" + expContext.getText() + "]";
            } else {
                TerminalNode node = ctx.L_BRACKT(i);
                errReport(9, ctx, "Not an array: " + varName);
                break;
            }
        }

        return super.visitLVal(ctx);
    }

    @Override
    public Void visitStmt(SysYParser.StmtContext ctx) {
        if (ctx.ASSIGN() != null) {
            Type lValType = getLValType(ctx.lVal());
            Type rValType = getExpType(ctx.exp());
            if (lValType instanceof FunctionType) {
                // 报告 Error type 11
                errReport(11, ctx, "The left-hand side of an assignment must be a variable");
            } else if (!lValType.toString().equals("noType") && !rValType.toString().equals("noType") && !lValType.toString().equals(rValType.toString())) {
                // 报告 Error type 5
                errReport(5, ctx, "Type mismatched for assignment");
            }
        } else if (ctx.RETURN() != null) {
            // 报告 Error type 7
            Type retType = new BasicTypeSymbol("void");
            if (ctx.exp() != null) {
                retType = getExpType(ctx.exp());
            }

            Scope tmpScope = currentScope;
            while (!(tmpScope instanceof FunctionSymbol)) {
                tmpScope = tmpScope.getEnclosingScope();
            }

            Type expectedType = ((FunctionSymbol) tmpScope).getType().getReturnType();
            if (!retType.toString().equals("noType") && !expectedType.toString().equals("noType") && !retType.toString().equals(expectedType.toString())) {
                errReport(7, ctx, "Type mismatched for return");
            }
        }
        return super.visitStmt(ctx);
    }

    private Type getExpType(SysYParser.ExpContext ctx) {
        if (ctx.IDENT() != null) { // IDENT L_PAREN funcRParams? R_PAREN
            String funcName = ctx.IDENT().getText();
            Symbol symbol = currentScope.getSymbol(funcName);
            if (symbol != null && symbol.getType() instanceof FunctionType) {
                FunctionType functionType = (FunctionType) currentScope.getSymbol(funcName).getType();
                ArrayList<Type> paramsType = functionType.getParamsType(), argsType = new ArrayList<>();
                if (ctx.funcRParams() != null) {
                    for (SysYParser.ParamContext paramContext : ctx.funcRParams().param()) {
                        argsType.add(getExpType(paramContext.exp()));
                    }
                }
                if (paramsType.equals(argsType)) {
                    return functionType.getReturnType();
                }
            }
        } else if (ctx.L_PAREN() != null) { // L_PAREN exp R_PAREN
            return getExpType(ctx.exp(0));
        } else if (ctx.unaryOp() != null) { // unaryOp exp
            return getExpType(ctx.exp(0));
        } else if (ctx.lVal() != null) { // lVal
            return getLValType(ctx.lVal());
        } else if (ctx.number() != null) { // number
            return new BasicTypeSymbol("int");
        } else if (ctx.MUL() != null || ctx.DIV() != null || ctx.MOD() != null || ctx.PLUS() != null || ctx.MINUS() != null) {
            Type op1Type = getExpType(ctx.exp(0));
            Type op2Type = getExpType(ctx.exp(1));
            if (op1Type.toString().equals("int") && op2Type.toString().equals("int")) {
                return op1Type;
            }
        }
        return new BasicTypeSymbol("noType");
    }

    private boolean checkArgsTyps(ArrayList<Type> paramsType, ArrayList<Type> argsType) {
        int len1 = paramsType.size();
        int len2 = argsType.size();

        for (Type type : paramsType) {
            if (type.toString().equals("noType")) {
                return true;
            }
        }

        for (Type type : argsType) {
            if (type.toString().equals("noType")) {
                return true;
            }
        }

        if (len1 != len2) {
            return false;
        }

        for (int i = 0; i < len1; ++i) {
            Type paramType = paramsType.get(i);
            Type argType = argsType.get(i);
            if (!paramType.toString().equals(argType.toString())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Void visitExp(SysYParser.ExpContext ctx) {
        if (ctx.IDENT() != null) { // IDENT L_PAREN funcRParams? R_PAREN
            String funcName = ctx.IDENT().getText();
            Symbol symbol = currentScope.getSymbol(funcName);
            if (symbol == null) {// 报告 Error type 2
                errReport(2, ctx, "Undefined function: " + funcName);
            } else if (!(symbol.getType() instanceof FunctionType)) {// 报告 Error type 10
                errReport(10, ctx, "Not a function: " + funcName);
            } else {// 报告 Error type 8
                FunctionType functionType = (FunctionType) symbol.getType();
                ArrayList<Type> paramsType = functionType.getParamsType();
                ArrayList<Type> argsType = new ArrayList<>();
                if (ctx.funcRParams() != null) {
                    for (SysYParser.ParamContext paramContext : ctx.funcRParams().param()) {
                        argsType.add(getExpType(paramContext.exp()));
                    }
                }
                if (!checkArgsTyps(paramsType, argsType)) {
                    errReport(8, ctx, "Function is not applicable for arguments");
                }
            }

        } else if (ctx.unaryOp() != null) { // unaryOp exp
            // 报告 Error type 6
            Type expType = getExpType(ctx.exp(0));
            if (!expType.toString().equals("int")) {
                errReport(6, ctx, "Type mismatched for operands");
            }
        } else if (ctx.MUL() != null || ctx.DIV() != null || ctx.MOD() != null || ctx.PLUS() != null || ctx.MINUS() != null) {
            // 报告 Error type 6
            Type op1Type = getExpType(ctx.exp(0)), op2Type = getExpType(ctx.exp(1));
            if (op1Type.toString().equals("noType") || op2Type.toString().equals("noType")) {

            } else if (op1Type.toString().equals("int") && op2Type.toString().equals("int")) {

            } else {
                errReport(6, ctx, "Type mismatched for operands");
            }
        }
        return super.visitExp(ctx);
    }

    private Type getCondType(SysYParser.CondContext ctx) {
        if (ctx.exp() != null) {
            return getExpType(ctx.exp());
        }

        Type cond1 = getCondType(ctx.cond(0));
        Type cond2 = getCondType(ctx.cond(1));
        if (cond1.toString().equals("int") && cond2.toString().equals("int")) {
            return cond1;
        }
        return new BasicTypeSymbol("noType");
    }

    @Override
    public Void visitCond(SysYParser.CondContext ctx) {
        // 报告 Error type 6
        if (ctx.exp() == null && !getCondType(ctx).toString().equals("int")) {
            errReport(6, ctx, "Type mismatched for operands");
        }
        return super.visitCond(ctx);
    }
}
