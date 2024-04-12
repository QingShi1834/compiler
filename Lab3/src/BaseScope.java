import java.util.Map;

public class BaseScope implements Scope {
    private final Scope fatherScope;
//    private final Map<String, Symbol> symbols = new LinkedHashMap<>();
    private SymbolTable table;
    private String name;

    public BaseScope(String name, Scope fatherScope) {
        this.name = name;
        this.fatherScope = fatherScope;
        table = new SymbolTable(this);
    }

//    @Override
//    public boolean definedSymbol(String name) {
//        return this.symbols.containsKey(name);
//    }


    @Override
    public boolean definedSymbol(String name) {
        return this.table.findSymbol(name);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Scope getFatherScope() {
        return this.fatherScope;
    }

    @Override
    public Map<String, Symbol> getSymbols() {
        return this.table.getTable();
    }

//    @Override
//    public void define(Symbol symbol) {
//        this.symbols.put(symbol.getName(), symbol);
//    }


    @Override
    public void define(Symbol symbol) {
        this.table.addSymbol(symbol);
    }

    @Override
    public Symbol findSymbol(String name) {
//        Symbol symbol = this.symbols.get(name);
        Symbol symbol = this.table.getSymbol(name);

        if (symbol != null){
            return symbol;
        }

        if (fatherScope != null) {
            return fatherScope.findSymbol(name);
        }

        return null;
    }
}
