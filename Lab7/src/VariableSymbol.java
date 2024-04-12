public class VariableSymbol extends BaseSymbol{
    public final boolean isConstVar;
    public VariableSymbol(String name, Type type, boolean isConst){
        super(name, type);
        isConstVar = isConst;
    }
}
