//import org.bytedeco.llvm.LLVM.LLVMValueRef;
import org.bytedeco.llvm.LLVM.*;
import static org.bytedeco.llvm.global.LLVM.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class BaseScope implements Scope {
    private final Scope fatherScope;
    //    private final Map<String, Symbol> symbols = new LinkedHashMap<>();
    private SymbolTable table;
    private String scope_name;

    private Map<String,LLVMTypeRef> varAndType;


    public BaseScope(String name, Scope fatherScope) {
        this.scope_name = name;
        this.fatherScope = fatherScope;
        varAndType = new LinkedHashMap<>();
        table = new SymbolTable(this);
    }

    @Override
    public boolean definedSymbol(String name) {
        return this.table.findSymbol(name);
    }

    @Override
    public String getName() {
        return this.scope_name;
    }

    @Override
    public void setName(String name) {
        this.scope_name = name;
    }

    @Override
    public Scope getFatherScope() {
        return this.fatherScope;
    }

    @Override
    public Map<String, LLVMValueRef> getSymbols() {
        return this.table.getTable();
    }

    @Override
    public void define(String name, LLVMValueRef type,LLVMTypeRef varType) {
        this.table.addSymbol(name,type);
        this.varAndType.put(name,varType);
    }

    @Override
    public LLVMTypeRef getTypeOfVar(String varName) {
        if (varAndType.containsKey(varName)){
            return varAndType.get(varName);
        }
        if (fatherScope != null){
            return fatherScope.getTypeOfVar(varName);
        }
        return null;
    }

    @Override
    public LLVMValueRef findSymbol(String name) {
        LLVMValueRef symbol = this.table.getSymbol(name);

        if (symbol != null){
            return symbol;
        }

        if (fatherScope != null) {
            return fatherScope.findSymbol(name);
        }

        return null;
    }
}
