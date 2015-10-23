package edu.dsu.spi;

public class Instruction {
    private boolean positive;
    private int op, opn1, opn2, opn3;

    public Instruction(boolean pos, int op, int opn1, int opn2, int opn3)
    {
        this.positive = pos;
        this.op = op;
        this.opn1 = opn1;
        this.opn2 = opn2;
        this.opn3 = opn3;
    }

    public boolean getPositive() {
        return positive;
    }

    public int getOp() {
        return op;
    }

    public int getOpn1() {
        return opn1;
    }

    public int getOpn2() {
        return opn2;
    }

    public int getOpn3() {
        return opn3;
    }

    public String opToString() {
        return ((positive) ? "+" : "-") + Integer.toString(op);
    }

    public String opn1ToString() {
        return String.format("%03d", opn1);
    }

    public String opn2ToString() {
        return String.format("%03d", opn2);
    }

    public String opn3ToString() {
        return String.format("%03d", opn3);
    }
}