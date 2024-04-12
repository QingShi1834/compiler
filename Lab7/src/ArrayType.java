public class ArrayType implements Type {
    public String currentType = "ArrayType";
    public int elementNum;
    public Type elementType;
    public ArrayType(int elementNum, Type elementType){
        this.elementNum = elementNum;
        this.elementType = elementType;
    }

    @Override
    public String toString() {
        return "array(" + elementType + ")";
    }

    @Override
    public String getTypeName() {
        return currentType;
    }
}
