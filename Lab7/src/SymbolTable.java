//import org.bytedeco.llvm.LLVM.LLVMValueRef;
import org.bytedeco.llvm.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.*;

import java.util.HashMap;
import java.util.Map;

public class SymbolTable {

    private Scope currentScope;
    private Map<String, LLVMValueRef> table;
    //    private
    public SymbolTable(Scope scope){
        currentScope = scope;
        table = new HashMap<>();
    }

    public boolean findSymbol(String sym_name){
        return table.containsKey(sym_name);
    }

    public void addSymbol(String sym_name, LLVMValueRef sym_type){
        table.put(sym_name, sym_type);
    }

    public LLVMValueRef getSymbol(String name){
        return table.get(name);
    }

    public Map<String, LLVMValueRef> getTable() {
        return table;
    }
}
