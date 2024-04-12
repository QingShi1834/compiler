import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

public class Visitor extends SysYParserBaseVisitor{
    int indent = 0;
    String[] table = {
        "CONST[orange]", "INT[orange]", "VOID[orange]", "IF[orange]", "ELSE[orange]",
        "WHILE[orange]", "BREAK[orange]", "CONTINUE[orange]", "RETURN[orange]",
        "PLUS[blue]", "MINUS[blue]", "MUL[blue]", "DIV[blue]", "MOD[blue]", "ASSIGN[blue]",
        "EQ[blue]", "NEQ[blue]", "LT[blue]", "GT[blue]",
        "LE[blue]", "GE[blue]", "NOT[blue]", "AND[blue]", "OR[blue]",
        "L_PAREN", "R_PAREN", "L_BRACE", "R_BRACE",
        "L_BRACKT", "R_BRACKT", "COMMA", "SEMICOLON", "IDENT[red]", "INTEGER_CONST[green]",
        "WS", "LINE_COMMENT", "MULTILINE_COMMENT"
    };
    String[] temp = {
        "Program", "CompUnit", "Decl", "ConstDecl", "BType", "ConstDef", "ConstInitVal",
                "VarDecl", "VarDef", "InitVal", "FuncDef", "FuncType", "FuncFParams",
                "FuncFParam", "Block", "BlockItem", "Stmt", "Exp", "Cond", "LVal", "Number",
                "UnaryOp", "FuncRParams", "Param", "ConstExp"
    };
    @Override
    public Object visitChildren(RuleNode node) {
//        String className = node.getClass().getSimpleName().replaceAll("Context$","");
        String className = temp[node.getRuleContext().getRuleIndex()];
//        node.getRuleContext().
        System.err.println(getIndent() + className);
        if (node.getChildCount() == 0) {
//            System.err.println();
        } else {
            indent += 2;
            for (int i = 0; i < node.getChildCount(); i++) {
                node.getChild(i).accept(this);
            }
            indent -= 2;
        }
        return null;
    }

    String getIndent() {
        return " ".repeat(indent);
    }

    @Override
    public Object visitTerminal(TerminalNode node) {
        int id = node.getSymbol().getType();
        if ((id >=1 && id <=24) || id == 33){
            System.err.println(getIndent() + node.getText()+" "+table[id-1]);
        }else if (id == 34){
            System.err.println(getIndent() + trans(node.getText())+" "+table[id-1]);
        }
        return null;
    }
    public int trans(String str){
        int decimal = 0;
        if (str.startsWith("0x")||str.startsWith("0X")) { decimal = Integer.parseInt(str.substring(2), 16); }
        else if (str.startsWith("0") && str.length() != 1) { decimal = Integer.parseInt(str.substring(1), 8); }
        else { decimal = Integer.parseInt(str); }
        return decimal;
    }
}
