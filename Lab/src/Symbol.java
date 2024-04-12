public interface Symbol {
    String getName();

    Type getType();

    void addCase(int rowNum, int columnNum);

    boolean findCase(int rowNum, int columnNum);
}
