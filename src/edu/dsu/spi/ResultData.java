package edu.dsu.spi;

public class ResultData {
    private Instruction instruction;
    private long[] result;
    private boolean continueProgram;

    public ResultData(Instruction inst, long[] res, boolean cont) {
        this.instruction = inst;
        this.result = res;
        this.continueProgram = cont;
    }

    public ResultData(Instruction inst, long[] res) {
        this.instruction = inst;
        this.result = res;
        this.continueProgram = true;
    }

    public Instruction getInstruction() {
        return instruction;
    }

    public long[] getResult() {
        return result;
    }

    public boolean getContinueProgram() {
        return continueProgram;
    }
}