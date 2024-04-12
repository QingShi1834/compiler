import org.bytedeco.llvm.LLVM.LLVMTypeRef;
import org.bytedeco.llvm.LLVM.LLVMValueRef;

import java.util.Map;

public interface Scope {
    String getName();
    void setName(String name);
    Scope getFatherScope();
    Map<String, LLVMValueRef> getSymbols();
    void define(String name, LLVMValueRef type, LLVMTypeRef varType);
    LLVMValueRef findSymbol(String name);
    boolean definedSymbol(String name);

    LLVMTypeRef getTypeOfVar(String varName);
}
