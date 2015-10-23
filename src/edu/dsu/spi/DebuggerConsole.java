package edu.dsu.spi;

import java.util.Scanner;

public class DebuggerConsole {
    Interpreter interp;

    public DebuggerConsole(Interpreter interp, boolean debug) {
        this.interp = interp;
        runProgram(debug);
    }

    private void runProgram(boolean debug) {
        StringBuilder sbPrint = new StringBuilder();
        boolean step = false;
        String input;

        try (
                Scanner in = new Scanner(System.in)
        ) {
            System.out.println("Labels: " + ((interp.getLabelsEnabled()) ? "ENABLED" : "DISABLED"));
            while (!interp.programComplete()) {
                runProgram(sbPrint, step, debug);

                if (!interp.programComplete()) { // if the program halts before completion (breakpoint)
                    step = true; // enable stepping after breakpoint is triggered
                    do {
                        input = in.nextLine(); // wait for input when stepping

                        if (input.equalsIgnoreCase("d")) { // stop stepping and continue debugging
                            System.out.println("Continue with debugging.");
                            step = false;
                            debug = true;
                        } else if (input.equalsIgnoreCase("c")) { // stop stepping and continue without debugging
                            System.out.println("Continue without debugging.");
                            step = false;
                            debug = false;
                        } else if (input.equalsIgnoreCase("q")) {
                            System.out.println("End program.");
                            return; // force quit
                        } else {
                            debug = true;
                            try {
                                int breakPoint = Integer.parseInt(input);
                                if (interp.breakpointIsSet(breakPoint)) // if breakpoint exists...
                                    System.out.println(interp.removeBreakpoint(breakPoint)); // remove the breakpoint
                                else
                                    System.out.println(interp.setBreakpoint(breakPoint)); // set the breakpoint
                            } catch (NumberFormatException e) { // if input string isn't an integer
                                // do nothing
                            }
                        }
                    } while (step && !input.isEmpty()); // take input, allowing use to set/unset multiple breakpoints before continuing
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void runProgram(StringBuilder sbPrint, boolean step, boolean debug) throws Exception {
        ResultData resultData;

        try {
            while (!interp.programComplete()) {
                resultData = interp.runNextInstruction();

                if (debug) {
                    String debugString = Interpreter.getResultOutput(resultData);
                    System.out.println(interp.getLastInstructionPointer() + ":\t" + debugString);
                }

                Instruction inst = resultData.getInstruction();
                if (!inst.getPositive() && inst.getOp() == 8) {
                    // preserve result of print statement for later
                    appendString(sbPrint, Long.toString(resultData.getResult()[0]));
                }

                if (interp.breakpointSetOnInstructionPointer()) {
                    step = true; // force a break
                    System.out.println("Breakpoint triggered on row " + interp.getInstructionPointer());
                    break;
                }

                if (step)
                    break;
            }

            if (interp.programComplete() && sbPrint.length() > 0) { // full print to console
                if (debug) {
                    System.out.println("----------------");
                    System.out.println("Full output:");
                }

                System.out.println(sbPrint);
            }

        } catch (Exception e) {
            throw new Exception("Error: " + e.getMessage());
        }
    }

    private void appendString(StringBuilder sb, String string) {
        if (sb.length() > 0)
            sb.append(System.getProperty("line.separator"));

        sb.append(string);
    }
}
