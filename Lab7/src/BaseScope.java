public class BaseScope implements Scope {
    private final Scope enclosingScope;
    private final SymbolTable symbolTable = new SymbolTable(this);
//    private String name;

    public BaseScope(Scope enclosingScope) {
        this.enclosingScope = enclosingScope;
    }

    @Override
    public boolean definedSymbol(String name) {
        return symbolTable.findSymbol(name);
    }



    @Override
    public Scope getEnclosingScope() {
        return this.enclosingScope;
    }

    @Override
    public SymbolTable getSymbolTable() {
        return this.symbolTable;
    }

    @Override
    public void define(Symbol symbol) {
        symbolTable.addSymbol(symbol);
    }

    @Override
    public Symbol getSymbol(String type_name) {

        Symbol symbol = symbolTable.getSymbol(type_name);

        if (symbol != null){
            return symbol;
        }

        if (enclosingScope != null) {
            return enclosingScope.getSymbol(type_name);
        }

        return null;
    }
}
