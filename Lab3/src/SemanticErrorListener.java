import java.util.ArrayList;

public class SemanticErrorListener {
    ArrayList<String> errMessage = new ArrayList<>();
    public void add(String errMess){
        errMessage.add(errMess);
    }
    public void printErr(){
        for (int i = 0; i < errMessage.size(); i++) {
            System.err.print(errMessage.get(i));
        }
    }
}
