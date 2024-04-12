import java.util.HashMap;
import java.util.Map;

public class SymbolTable {

    private Scope currentScope;
    private Map<String,Symbol> table;
    //    private
    public SymbolTable(Scope scope){
        currentScope = scope;
        table = new HashMap<>();
    }

    public boolean findSymbol(String sym_name){
        return table.containsKey(sym_name);
    }

    public void addSymbol(Symbol symbol){
        table.put(symbol.getName(),symbol);
    }

    public Symbol getSymbol(String name){
        return table.get(name);
    }

    public Map<String, Symbol> getTable() {
        return table;
    }
}
