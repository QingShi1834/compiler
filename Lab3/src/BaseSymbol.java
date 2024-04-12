import org.antlr.v4.runtime.misc.Pair;

import java.util.ArrayList;

public class BaseSymbol implements Symbol{//基本符号
    private final String name;
    private final Type type;
    private ArrayList<Position> casePositions;
    public BaseSymbol(String name, Type type){
        this.name = name;
        this.type = type;
        casePositions = new ArrayList<>();
    }
    @Override
    public String getName() {
        return name;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public void addCase(int rowNum, int columnNum) {
        casePositions.add(new Position(rowNum,columnNum));
    }

    @Override
    public boolean findCase(int rowNum, int columnNum) {
//        return casePositions.contains(new Position(rowNum,columnNum));
        for (int i = 0; i < casePositions.size(); i++) {
            if (casePositions.get(i).compareTo(rowNum, columnNum)){
                return true;
            }
        }
        return false;
    }
}
