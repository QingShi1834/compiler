import java.util.Map;

public interface Scope {
    String getName();
    void setName(String name);
    Scope getFatherScope();
    Map<String, Symbol> getSymbols();
    void define(Symbol symbol);
    Symbol findSymbol(String name);
    boolean definedSymbol(String name);
}
