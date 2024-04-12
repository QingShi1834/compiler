import java.util.ArrayList;

public class FunctionType implements Type {
    Type returnType;
    ArrayList<Type> paramsType;

    public FunctionType(Type retType, ArrayList<Type> paramsType){
        this.returnType = retType;
        this.paramsType = paramsType;
    }

    public Type getReturnType(){
        return returnType;
    }

    public ArrayList<Type> getParamsType(){
        return paramsType;
    }

    @Override
    public String toString() {
        StringBuilder function = new StringBuilder(returnType + "(");
        for (Type paramType : paramsType) {
            function.append(paramType.toString());
        }
        function.append(")");
        return function.toString();
    }

    @Override
    public boolean equals(Object obj) {
        FunctionType functionObj = (FunctionType) obj;
        if (toString().equals(functionObj.toString())){
            return true;
        }
        return false;
    }
}
