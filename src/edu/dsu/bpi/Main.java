package edu.dsu.bpi;

import javax.swing.*;
import java.awt.*;

public class Main {

    private final static String HELP_MESSAGE = "Usage: BabelfishPI -i \"infile.txt\" [-o \"outfile.dat\"] [-l] [-d] [-b #line]\n\n" +
            "Options:\n" +
            "\t-i \"infile.txt\"\t\tFile to be opened by the interpreter.\n" +
            "\t-o \"outfile.dat\"\tRather than opening the interpreter, clean and output the parsed file.\n" +
            "\t-l\t\t\tInput file uses symbols/labels (default: no).\n" +
            "\t-d\t\t\tOpen the interpreter in debug mode.\n" +
            "\t-b #line\t\tBreak on specified line number.\n\n" +
            "After the program halts on a breakpoint, enter the following commands:\n" +
            "\t \t\t\tEnter nothing to step the program forward.\n" +
            "\tc\t\t\tResume program without debugging.\n" +
            "\td\t\t\tResume program with debugging.\n" +
            "\t#line\t\t\tEnter program line number to set/unset breakpoint. Setting and unsetting a breakpoint won't resume the program.\n" +
            "\tq\t\t\tQuit the debugging session and end the program.";

    public static void main(String[] args) {
        if (args.length == 0) {
            openGUIDebugger();
        } else {
            String inFile = null;
            String outFile = null;
            boolean labels = false;
            boolean debug = false;
            boolean help = false;
            int breakLine = -1;

            for (int i = 0; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("-i") && args.length > i+1) {
                    i++;
                    inFile = args[i];
                } else if (args[i].equalsIgnoreCase("-o") && args.length > i+1) {
                    i++;
                    outFile = args[i];
                } else if (args[i].equalsIgnoreCase("-b") && args.length > i+1) {
                    i++;
                    try {
                        breakLine = Integer.parseInt(args[i]);
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid breakpoint number: " + args[i]);
                    }
                } else if (args[i].equalsIgnoreCase("-d"))
                    debug = true;
                else if (args[i].equalsIgnoreCase("-l"))
                    labels = true;
                else if (args[i].equalsIgnoreCase("-h") || args[i].equalsIgnoreCase("-?") || args[i].equalsIgnoreCase("-help")
                        || args[i].equalsIgnoreCase("/h") || args[i].equalsIgnoreCase("/help") || args[i].equalsIgnoreCase("/?"))
                    help = true;
            }

            if (help || inFile == null)
                System.out.println(HELP_MESSAGE);
            else {

                try {
                    openConsoleDebugger(inFile, outFile, labels, debug, breakLine);
                }  catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    private static void openGUIDebugger() {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    new DebuggerGUI(new Interpreter());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, e.getMessage());
                }
            }
        });
    }

    private static void openConsoleDebugger(String inFile, String outFile, boolean labels, boolean debug, int breakLine) throws Exception {
        if (outFile != null) {
            Interpreter.exportProgram(inFile, outFile);
            System.out.println("Parsed program exported to " + outFile);
        } else {
            Interpreter interp = null;
            try {
                interp = new Interpreter(inFile, labels);
            } catch (Exception e) {
                System.out.println("Loading Error: " + e.getMessage());
            }

            if (interp != null) {
                if (breakLine != -1)
                    System.out.println(interp.setBreakpoint(breakLine));

                try {
                    new DebuggerConsole(interp, debug);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }

    }
}