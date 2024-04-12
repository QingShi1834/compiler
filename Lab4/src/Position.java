import org.antlr.v4.runtime.misc.Pair;

import java.util.ArrayList;
import java.util.List;

public class Position {
    private int row;
    private int column;

    public Position(int lineNo, int columnNo){
        row = lineNo;
        column = columnNo;
    }

    @Override
    public boolean equals(Object obj) {
        Position o = (Position) obj;
        if (row == o.getRow() && column == o.getColumn()){
            return true;
        }
        return false;
    }

    public int getRow() {
        return row;
    }

    public int getColumn() {
        return column;
    }

    public Pair<Integer,Integer> getPosition(){
        return new Pair<>(row,column);
    }

    public boolean compareTo(int rowNo, int columnNo){
        if (row == rowNo && column == columnNo){
            return true;
        }
        return false;
    }
}
