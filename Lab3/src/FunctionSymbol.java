import org.antlr.v4.runtime.misc.Pair;

import java.util.ArrayList;

public class FunctionSymbol extends BaseScope implements Symbol{
    public FunctionType type;
    public ArrayList<Position> casePositions;

    public FunctionSymbol(String name, Scope enclosingScope, FunctionType type){
        super(name, enclosingScope);
        this.casePositions = new ArrayList<>();
        this.type = type;
    }

    @Override
    public FunctionType getType() {
        return type;
    }

    @Override
    public void addCase(int rowNum, int columnNum) {
        casePositions.add(new Position(rowNum,columnNum));
    }

    @Override
    public boolean findCase(int rowNum, int columnNum) {
        for (int i = 0; i < casePositions.size(); i++) {
            if (casePositions.get(i).compareTo(rowNum, columnNum)){
                return true;
            }
        }
        return false;
//        return casePositions.contains(new Position(rowNum,columnNum));
    }
}
