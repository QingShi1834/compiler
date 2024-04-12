import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;

public class Visitor extends SysYParserBaseVisitor<Void>{
    private static int depth = 0;

    private LocalScope globalScope = null;

    private Scope currentScopePointer = null;


    public SemanticErrorListener semanticErrorListener = new SemanticErrorListener();

    String[] terminalTable = {
            "CONST[orange]", "INT[orange]", "VOID[orange]", "IF[orange]", "ELSE[orange]",
            "WHILE[orange]", "BREAK[orange]", "CONTINUE[orange]", "RETURN[orange]",
            "PLUS[blue]", "MINUS[blue]", "MUL[blue]", "DIV[blue]", "MOD[blue]", "ASSIGN[blue]",
            "EQ[blue]", "NEQ[blue]", "LT[blue]", "GT[blue]",
            "LE[blue]", "GE[blue]", "NOT[blue]", "AND[blue]", "OR[blue]",
            "L_PAREN", "R_PAREN", "L_BRACE", "R_BRACE",
            "L_BRACKT", "R_BRACKT", "COMMA", "SEMICOLON", "IDENT[red]", "INTEGER_CONST[green]",
            "WS", "LINE_COMMENT", "MULTILINE_COMMENT"
    };

    public static List<String> syntaxTree = new ArrayList<>();
    private void errReport(int typeNum, ParserRuleContext ctx, String message) {
        semanticErrorListener.add("Error type " + typeNum + " at Line " + ctx.getStart().getLine() + ": " + message + ".\n");
    }

    private String getIndent() {
        return "  ".repeat(depth);
    }

    private String getTerminalHilight(int type_id){
        if (type_id > 0 && type_id < 25){
            return terminalTable[type_id-1];
        }else if (type_id == 33){
            return terminalTable[32];
        }else if (type_id == 34){
            return terminalTable[33];
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
//        String color = getTerminalHilight(token.getType());

        Symbol symbol = currentScopePointer.findSymbol(tokenText);

        if (ruleName.equals("INTEGER_CONST")) {//转化为十进制数
            tokenText = String.valueOf(parseInt(tokenText));

        } else if (ruleName.equals("IDENT")) {//变量名

            int rowNO = token.getLine();
            int columnNO = token.getCharPositionInLine();

            if (symbol != null) {
                symbol.addCase(rowNO, columnNO);
            }
        }

        String ruleNameAndColor = getTerminalHilight(token.getType());
//        if (!color.equals("none")) {
        if ((token.getType() > 0 && token.getType() < 25) || token.getType() == 33 || token.getType() == 34){
            // 在 msgToPrint 中加入需要输出的语法树的内容
            syntaxTree.add(getIndent());
            if (symbol == null){
                syntaxTree.add(tokenText);
            }else {
                syntaxTree.add(symbol.getName());
            }
            syntaxTree.add(" " + ruleNameAndColor + "\n");
        }

        return super.visitTerminal(node);
    }
    @Override
    public Void visitProgram(SysYParser.ProgramContext ctx) {
        // 进入新的 Scope
        currentScopePointer = globalScope = new LocalScope(null);
        currentScopePointer.define(new BasicTypeSymbol("int"));
        currentScopePointer.define(new BasicTypeSymbol("void"));
        Void ret = super.visitProgram(ctx);
        currentScopePointer = currentScopePointer.getFatherScope();
        // 回到上一层 Scope
        return ret;
    }

    public Type getTypeAccordingToName(String name){
        return (Type) globalScope.findSymbol(name);
    }
    @Override
    public Void visitFuncDef(SysYParser.FuncDefContext ctx) {
        // 报告 Error type 4

        String functionName = ctx.IDENT().getText();

        if (currentScopePointer.definedSymbol(functionName)){
            errReport(4,ctx,"Redefined function: " + functionName);
            return null;
        }

        String retTypeName = ctx.funcType().getText();
        Type retType = getTypeAccordingToName(retTypeName);
        FunctionType functionType = new FunctionType(retType,new ArrayList<>());
        FunctionSymbol funcSym = new FunctionSymbol(functionName, currentScopePointer,functionType);
        currentScopePointer.define(funcSym);

        currentScopePointer = funcSym;
        // 进入新的 Scope，定义新的 Symbol
        Void ret = super.visitFuncDef(ctx);
        currentScopePointer = currentScopePointer.getFatherScope();
        // 回到上一层 Scope
        return ret;
    }
    @Override
    public Void visitBlock(SysYParser.BlockContext ctx) {
        LocalScope localScope = new LocalScope(currentScopePointer);
        currentScopePointer = localScope;
        // 进入新的 Scope
        Void ret = super.visitBlock(ctx);
        currentScopePointer = currentScopePointer.getFatherScope();
        // 回到上一层 Scope
        return ret;
    }

    @Override
    public Void visitVarDecl(SysYParser.VarDeclContext ctx) {
        String typeName = ctx.bType().getText();

        for (SysYParser.VarDefContext varDefContext : ctx.varDef()) {
            // 报告 Error type 3
            String varName = varDefContext.IDENT().getText();
            if (currentScopePointer.definedSymbol(varName)){
                errReport(3,varDefContext, "Redefined variable: " + varName );
                continue;
            }

            Type varType = getTypeAccordingToName(typeName);
            for (SysYParser.ConstExpContext constExpContext : varDefContext.constExp()){
                int elementNum = parseInt(constExpContext.getText());
                varType = new ArrayType(elementNum, varType);
            }

            if (varDefContext.ASSIGN() != null) {
                // 报告 Error type 5
                SysYParser.ExpContext expContext = varDefContext.initVal().exp();
                if (expContext != null ){
                    Type initValType = getExpType(expContext);
                    if (!(initValType.toString().equals("noType") || type_Equal(varType,initValType))){
                        errReport(5,varDefContext,"Type mismatched for assignment");
                    }
                }
            }

            VariableSymbol variableSymbol = new VariableSymbol(varName, varType,false);
            currentScopePointer.define(variableSymbol);
            // 定义新的 Symbol
        }

        return super.visitVarDecl(ctx);
    }

    public FunctionType getFuncTypeAccordingToName(String funcName){
        return (FunctionType) currentScopePointer.findSymbol(funcName).getType();
    }


    private Type getExpType(SysYParser.ExpContext ctx) {
        if (ctx.IDENT() != null) { // IDENT L_PAREN funcRParams? R_PAREN
            return getFuncExpType(ctx);
        } else if (ctx.L_PAREN() != null) { // L_PAREN exp R_PAREN
            return getExpType(ctx.exp(0));
        } else if (ctx.unaryOp() != null) { // unaryOp exp
            return getExpType(ctx.exp(0));
        } else if (ctx.lVal() != null) { // lVal
            return getLValType(ctx.lVal());
        } else if (ctx.number() != null) { // number
            return new BasicTypeSymbol("int");
        } else if (isArithmeticOp(ctx)) {
            // 处理算术运算符
            return handleArithmeticOp(ctx);
        }
        return new BasicTypeSymbol("noType");
    }

    private Type getFuncExpType(SysYParser.ExpContext ctx) {
        String funcName = ctx.IDENT().getText();
        Symbol symbol = currentScopePointer.findSymbol(funcName);
        if (symbol == null || !(symbol.getType() instanceof FunctionType)) {
            return new BasicTypeSymbol("noType");
        }
        FunctionType functionType = getFuncTypeAccordingToName(funcName);
        ArrayList<Type> paramsType = functionType.getParamsType();
        ArrayList<Type> argsType = new ArrayList<>();
        if (ctx.funcRParams() != null) {
            for (SysYParser.ParamContext paramContext : ctx.funcRParams().param()) {
                argsType.add(getExpType(paramContext.exp()));
            }
        }
        if (!paramsType.equals(argsType)) {
            return new BasicTypeSymbol("noType");
        }
        return functionType.getReturnType();
    }

    private boolean isArithmeticOp(SysYParser.ExpContext ctx) {
        return ctx.MUL() != null || ctx.DIV() != null || ctx.MOD() != null || ctx.PLUS() != null || ctx.MINUS() != null;
    }

    private Type handleArithmeticOp(SysYParser.ExpContext ctx) {
        Type op1Type = getExpType(ctx.exp(0));
        Type op2Type = getExpType(ctx.exp(1));
        if (op1Type.toString().equals("int") && op2Type.toString().equals("int")) {
            return op1Type;
        }
        return new BasicTypeSymbol("noType");
    }

    private Type getLValType(SysYParser.LValContext ctx) {
        String varName = ctx.IDENT().getText();
        Symbol symbol = currentScopePointer.findSymbol(varName);
        if (symbol == null) {
            return new BasicTypeSymbol("noType");
        }
        Type varType = symbol.getType();
        for (SysYParser.ExpContext expContext : ctx.exp()) {
            if (varType instanceof ArrayType) {
                varType = ((ArrayType) varType).elementType;
            } else {
                return new BasicTypeSymbol("noType");
            }
        }
        return varType;
    }

    public boolean type_Equal(Type t1, Type t2){
        return t1.toString().equals(t2.toString());
    }

    public String getVarName(SysYParser.ConstDefContext varText){
        return varText.IDENT().getText();
    }

    public boolean checkErr3(String name, SysYParser.ConstDefContext varDefContext){
        if (currentScopePointer.definedSymbol(name)){
            errReport(3,varDefContext,"Redefined variable: " + name);
            return true;
//            continue;
        }
        return false;
    }
    @Override
    public Void visitConstDecl(SysYParser.ConstDeclContext ctx) {
        // 结构同 visitVarDecl
        String typeName = ctx.bType().getText();
        for (SysYParser.ConstDefContext varDefContext : ctx.constDef()) {

            String constVarName = getVarName(varDefContext);

            if (checkErr3(constVarName,varDefContext)){
                continue;
            }

            Type constType = getTypeAccordingToName(typeName);
            for (SysYParser.ConstExpContext constExpContext : varDefContext.constExp()) {
                int elementNum = parseInt(constExpContext.getText());
                constType = new ArrayType(elementNum,constType);
            }

            SysYParser.ConstExpContext expContext = varDefContext.constInitVal().constExp();
            if (expContext != null) {
                Type initVarType = getExpType(expContext.exp());
                if (!(initVarType.toString().equals("noType") || type_Equal(constType,initVarType))){
                    errReport(5,varDefContext,"Type mismatched for assignment");
                }
            }

            VariableSymbol constSymbol = new VariableSymbol(constVarName,constType,true);
            currentScopePointer.define(constSymbol);
        }

        return super.visitConstDecl(ctx);
    }

    @Override
    public Void visitFuncFParam(SysYParser.FuncFParamContext ctx) {
        // 报告 Error type 3
        // 定义新的 Symbol
        String varTypeName = ctx.bType().getText();
        Type varType = getTypeAccordingToName(varTypeName);
        for (TerminalNode node : ctx.L_BRACKT()) {
            varType = new ArrayType(0, varType);
        }
        String varName = ctx.IDENT().getText();
        VariableSymbol varSymbol = new VariableSymbol(varName, varType,false);

        if (currentScopePointer.definedSymbol(varName)) {
            errReport(3, ctx, "Redefined variable: " + varName);
        } else {
            currentScopePointer.define(varSymbol);
            ((FunctionSymbol) currentScopePointer).getType().getParamsType().add(varType);
        }
        return super.visitFuncFParam(ctx);
    }

    @Override
    public Void visitLVal(SysYParser.LValContext ctx) {
        // 报告 Error type 1
        String varName = ctx.IDENT().getText();
        Symbol symbol = currentScopePointer.findSymbol(varName);

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

    public Void visitStmt(SysYParser.StmtContext ctx) {
        if (ctx.ASSIGN() != null) {
            handleAssign(ctx);
        } else if (ctx.RETURN() != null) {
            handleReturn(ctx);
        }
        return super.visitStmt(ctx);
    }

    private void handleAssign(SysYParser.StmtContext ctx) {
        Type leftType = getLValType(ctx.lVal());
        Type rightType = getExpType(ctx.exp());
        if (leftType instanceof FunctionType) {
            // 报告 Error type 11
            errReport(11, ctx, "The left-hand side of an assignment must be a variable");
        } else {
            checkTypeMatch(leftType, rightType, 5, ctx);
        }
    }

    private void handleReturn(SysYParser.StmtContext ctx) {
        // 报告 Error type 7
        Type retType = new BasicTypeSymbol("void");
        if (ctx.exp() != null) {
            retType = getExpType(ctx.exp());
        }

        Scope tmpScope = currentScopePointer;
        while (!(tmpScope instanceof FunctionSymbol)) {
            tmpScope = tmpScope.getFatherScope();
        }

        Type expectedType = ((FunctionSymbol) tmpScope).getType().getReturnType();
        checkTypeMatch(retType, expectedType, 7, ctx);
    }

    private void checkTypeMatch(Type type1, Type type2, int errorCode, SysYParser.StmtContext ctx) {
        if (!type1.toString().equals("noType") && !type2.toString().equals("noType") && !type1.toString().equals(type2.toString())) {
            errReport(errorCode, ctx, "Type mismatched");
        }
    }

    public boolean checkParamsType(ArrayList<Type> param){
        for (int i = 0; i < param.size(); i++) {
            if (param.get(i).toString().equals("noType")){
                return true;
            }
        }
        return false;
    }

    private boolean checkArgsTyps(ArrayList<Type> paramsType, ArrayList<Type> argsType) {
        int len1 = paramsType.size();
        int len2 = argsType.size();

        if (checkParamsType(paramsType)){
            return true;
        }

        if (checkParamsType(argsType)){
            return true;
        }

        if (len1 != len2) {
            return false;
        }

        for (int i = 0; i < len1; ++i) {

            if (!type_Equal(paramsType.get(i),argsType.get(i))){
                return false;
            }
        }

        return true;
    }

    public void checkFunc(String funcName,SysYParser.ExpContext ctx){
        Symbol symbol = currentScopePointer.findSymbol(funcName);
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
    }

    @Override
    public Void visitExp(SysYParser.ExpContext ctx) {
        if (ctx.IDENT() != null) { // IDENT L_PAREN funcRParams? R_PAREN
            checkFunc(ctx.IDENT().getText(),ctx);
        } else if (ctx.unaryOp() != null) { // unaryOp exp
            // 报告 Error type 6
            Type expType = getExpType(ctx.exp(0));
            if (!expType.toString().equals("int")) {
                errReport(6, ctx, "Type mismatched for operands");
            }
        } else if (ctx.MUL() != null || ctx.DIV() != null || ctx.MOD() != null || ctx.PLUS() != null || ctx.MINUS() != null) {
            // 报告 Error type 6
            Type op1Type = getExpType(ctx.exp(0)), op2Type = getExpType(ctx.exp(1));

            if ((!op1Type.toString().equals("noType") && !op2Type.toString().equals("noType"))
                    && !(op1Type.toString().equals("int") && op2Type.toString().equals("int"))){
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
            return cond2;
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
