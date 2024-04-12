import org.antlr.v4.runtime.misc.Pair;

import java.util.ArrayList;

public class FunctionSymbol extends BaseScope implements Symbol{
    public String functionName;

    public FunctionType type;
    public ArrayList<Pair<Integer,Integer>> casePositions;

    public FunctionSymbol(String name,Scope enclosingScope, FunctionType type){
        super(enclosingScope);
        functionName = name;
        this.casePositions = new ArrayList<>();
        this.type = type;
    }

    @Override
    public FunctionType getType() {
        return type;
    }

    @Override
    public void addCase(int rowNum, int columnNum) {
        casePositions.add(new Pair<Integer,Integer>(rowNum,columnNum));
    }

    @Override
    public boolean findCase(int rowNum, int columnNum) {
        return casePositions.contains(new Pair<>(rowNum,columnNum));
    }
}
