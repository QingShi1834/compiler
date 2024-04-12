public interface Scope {
    Scope getEnclosingScope();
    SymbolTable getSymbolTable();
    void define(Symbol symbol);
    Symbol getSymbol(String name);
    boolean definedSymbol(String name);
}
