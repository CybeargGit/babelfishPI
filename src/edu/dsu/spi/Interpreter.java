package edu.dsu.spi;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Interpreter {
    private static int MAX_MEM = 1000;

    private List<String> inputInstructionList;
    private long[] _data;
    private char[][] _program;
    private long[] _input;
    private TreeSet<Integer> usedDataLocations;
    private TreeMap<Integer, Integer> programLabelMap;
    private TreeMap<Integer, Integer> dataSymbolMap;
    private TreeMap<Integer, Integer> dataSymbolSizeMap;
    private TreeMap<Integer, Integer> programLineMap;
    private HashSet<Integer> lastReadDataSymbolSet;
    private HashSet<Integer> breakpointSet;

    private String fileName = null;

    private int instructionPointer, lastInstructionPointer, inputPointer, dataSymbolPointer;
    private int programSize, inputSize;
    private int lastWrittenDataLocation, lastReadInputCard, lastReadProgramLabel, lastWrittenDataSymbol;
    private boolean labelsEnabled;

    public TreeSet<Integer> getUsedDataLocations() { return usedDataLocations;}
    public TreeMap<Integer, Integer> getProgramLabelMap() { return programLabelMap; }
    public TreeMap<Integer, Integer> getDataSymbolMap() { return dataSymbolMap; }

    public int getInstructionPointer() { return instructionPointer; }
    public int getLastInstructionPointer() { return lastInstructionPointer; }

    public int getProgramSize() { return programSize;}
    public int getInputSize() { return inputSize; }
    public int getProgramLabelMapSize() { return programLabelMap.size(); }
    public int getDataSymbolMapSize() { return dataSymbolMap.size(); }
    public int getDataSymbolSize(int symbol) { return dataSymbolSizeMap.get(symbol); }

    public int getLastWrittenDataLocation() { return lastWrittenDataLocation; }
    public int getLastReadInputCard() { return lastReadInputCard; }
    public int getLastReadProgramLabel() { return lastReadProgramLabel; }
    public int getLastWrittenDataSymbol() { return lastWrittenDataSymbol; }
    public HashSet<Integer> getLastReadDataSymbolSet() { return lastReadDataSymbolSet; }
    public boolean breakpointSetOnRow(int row) { return breakpointSet.contains(row); }
    public boolean breakpointSetOnInstructionPointer() {return breakpointSet.contains(instructionPointer);}

    public boolean getLabelsEnabled() { return labelsEnabled; }

    public long accessData(int index) { return _data[index]; }
    public long accessInput(int index) {
        return _input[index];
    }

    private enum LoadState { DATA, PROGRAM, INPUT };

    public Interpreter() {
        _data = new long[MAX_MEM];
        _program = new char[MAX_MEM][];
        _input = new long[MAX_MEM];
        usedDataLocations = new TreeSet<>();
        inputInstructionList = new LinkedList<>();
        programLabelMap = new TreeMap<>();
        programLineMap = new TreeMap<>();
        dataSymbolMap = new TreeMap<>();
        dataSymbolSizeMap = new TreeMap<>();
        lastReadDataSymbolSet = new HashSet<>();
        breakpointSet = new HashSet<>();

        clear();
    }

    public Interpreter(String fileName, boolean hasLabels) throws Exception {
        this();
        readProgram(fileName, hasLabels);
    }

    private void clear() {
        lastInstructionPointer = -1;
        instructionPointer = 0;
        inputPointer = 0;
        lastReadProgramLabel = -1;
        lastReadDataSymbolSet.clear();
        lastWrittenDataSymbol = -1;
        lastWrittenDataLocation = -1;
        lastReadInputCard = -1;
        programSize = 0;
        inputSize = 0;
        dataSymbolPointer = 0;

        for (int i = 0; i < MAX_MEM; i++) {
            _data[i] = Long.MIN_VALUE;
            _input[i] = Long.MIN_VALUE;
        }

        _program = new char[MAX_MEM][];
        usedDataLocations.clear();
        programLabelMap.clear();
        programLineMap.clear();
        dataSymbolMap.clear();
        dataSymbolSizeMap.clear();
    }

    private void readProgram(String file, boolean enableLabels) throws Exception {
        StringBuilder noSB = null;
        readProgram(file, noSB, enableLabels, false);
    }

    public void readProgram(String file, StringBuilder report, boolean enableLabels) throws Exception {
        readProgram(file, report, enableLabels, true);
    }

    private void readProgram(String file, StringBuilder report, boolean enableLabels, boolean autoLabelDetection) throws Exception {
        if (this.fileName == null || !this.fileName.equals(file)) {
            this.fileName = file;
            clearBreakpoints(); // clear breakpoints if a new file is being read
        }

        loadProgram(report, enableLabels, autoLabelDetection);
    }

    public void loadProgram(StringBuilder report, boolean enableLabels) throws Exception {
        loadProgram(report, enableLabels, true);
    }

    private void loadProgram(StringBuilder report, boolean enableLabels, boolean autoLabelDetection) throws Exception {
        inputInstructionList.clear();
        boolean labelsDetected = false; // assume labels are disabled
        LoadState preLoadState = LoadState.DATA;
        int dataCardCount = 0;

        if (fileName == null)
            throw new Exception("A program must be read before it can be refreshed");

        try {
            reportLn(report, "--Reading Program File--");

            try (
                    FileReader fr = new FileReader(fileName);
                    BufferedReader br = new BufferedReader(fr)
            ) {
                String currLine;

                while ((currLine = br.readLine()) != null) {

                    // test to see if the program uses labels
                    try {
                        String parsedCard = parseCard(currLine);
                        if (parsedCard.equals("+9999999999")) {
                            switch (preLoadState) {
                                case DATA: preLoadState = LoadState.PROGRAM; break;
                                case PROGRAM: preLoadState = LoadState.INPUT; break;
                            }
                        } else if (preLoadState == LoadState.PROGRAM && parsedCard.substring(0, 2).equals("-7"))
                            labelsDetected = true;

                        if (preLoadState == LoadState.DATA && !parsedCard.isEmpty())
                            dataCardCount++; // tally number of data cards

                    } catch (Exception e1) {
                        // ignore -- we're just checking if the cards are valid
                    }

                    // add the line to the program instruction list
                    inputInstructionList.add(currLine);
                }

                // for labels to be enabled, a label card must be encountered and there must be an even number of data cards
                labelsEnabled = enableLabels | (autoLabelDetection & labelsDetected & (dataCardCount % 2 == 0));
                reportLn(report, "--Program Successfully Read--");
                reportLn(report);

                parseProgram(report);
                clearUnusedBreakpoints();
            }
        } catch (IOException e) {
            clear(); // make sure that interpreter is cleared after failure
            labelsEnabled = false;
            throw new IOException("The file " + fileName + " could not be read");
        }
    }

    public void parseProgram(StringBuilder report) throws Exception {
        clear();
        String parsedCardString;
        int fileLine = 1;
        LoadState loadState = LoadState.DATA;
        int lineCount = 0;
        dataSymbolPointer = 0;

        try {
            String currLine;
            reportLn(report, "Labels: " + ((labelsEnabled) ? "ENABLED" : "DISABLED"));
            reportLn(report, "--Loading Data--");
            for (int i = 0; i < inputInstructionList.size(); i++) {
                currLine = inputInstructionList.get(i);
                parsedCardString = parseCard(currLine);

                if (parsedCardString.isEmpty()) { // skip empty/commented-out lines and continue
                    fileLine++;
                    continue;
                }

                if (parsedCardString.equals("+9999999999")) { // advance program load state
                    fileLine++;

                    // increment load state (throw error if more than 2 boundary cards are encountered)
                    int state = loadState.ordinal();
                    if (state == 2)
                        throw new Exception();
                    else
                        loadState = LoadState.values()[state + 1];

                    switch (loadState) {
                        case PROGRAM:
                            reportLn(report);
                            reportLn(report, "--Loading Program--");
                            break;
                        case INPUT:
                            reportLn(report);
                            reportLn(report, "--Loading Input--");
                            break;
                    }

                    lineCount = 0;
                    continue;
                }

                switch (loadState) {
                    case DATA: {
                        if (lineCount == 0)
                            reportLn(report, "Index\tData");

                        if (labelsEnabled) {
                            // get data symbol
                            Instruction instruction = parseInstruction(parsedCardString.toCharArray());

                            // get paired data card
                            int skippedLines = 0;
                            do {
                                i++;
                                if (inputInstructionList.size() <= i) {
                                    throw new UnsupportedOperationException("In label/symbol mode, data cards must come in pairs.");
                                }

                                currLine = inputInstructionList.get(i);
                                skippedLines++;
                                try {
                                    parsedCardString = parseCard(currLine);
                                } catch (Exception e) {
                                    fileLine += skippedLines; // in case of an error, make sure the right line is indicated
                                    throw e;
                                }
                            } while (parsedCardString.isEmpty());

                            long dataValue = Long.parseLong(parsedCardString);

                            if (dataSymbolMap.containsKey(instruction.getOpn1())) {
                                throw new UnsupportedOperationException("Data symbol " + instruction.getOpn1() + " has already been set");
                            }

                            if (instruction.getOpn2() == 0) {
                                throw new UnsupportedOperationException("Can't allocate zero words for data symbol " + instruction.getOpn1());
                            } else if (dataSymbolPointer + instruction.getOpn2() > MAX_MEM) {
                                int excess = dataSymbolPointer + instruction.getOpn2() - MAX_MEM;
                                throw new UnsupportedOperationException("Memory limit exceeded by " + excess + " word" + ((excess == 1) ? "" : "s"));
                            } else {
                                dataSymbolMap.put(instruction.getOpn1(), dataSymbolPointer);
                                dataSymbolSizeMap.put(instruction.getOpn1(), instruction.getOpn2());

                                fileLine += skippedLines;

                                // initialize memory locations to paired card value
                                for (int j = dataSymbolPointer; j < dataSymbolPointer + instruction.getOpn2(); j++) {
                                    _data[j] = dataValue;
                                    usedDataLocations.add(j);
                                    lineCount++;
                                }

                                dataSymbolPointer += instruction.getOpn2();
                            }
                        } else {
                            _data[lineCount] = Long.parseLong(parsedCardString);
                            usedDataLocations.add(lineCount);
                        }
                        break;
                    }
                    case PROGRAM: {
                        if (lineCount == 0)
                            reportLn(report, "Line#\tInstruction");

                        char[] parsedCardChars = parsedCardString.toCharArray();

                        if (labelsEnabled) {
                            Instruction instruction = parseInstruction(parsedCardChars);
                            if (!instruction.getPositive() && instruction.getOp() == 7) {
                                if (programLabelMap.containsKey(instruction.getOpn1())) {
                                    throw new UnsupportedOperationException("label " + instruction.opn1ToString() + " has already been defined");
                                } else if (instruction.getOpn1() > 99)
                                    throw new UnsupportedOperationException("label name exceeds maximum of 99");
                                else if (programLabelMap.size() >= 100)
                                    throw new UnsupportedOperationException("maximum of 100 labels have already been defined");
                                else {
                                    programLabelMap.put(instruction.getOpn1(), lineCount);
                                    reportLn(report, lineCount + "\t" + parsedCardString);
                                    fileLine++;
                                    continue;
                                }
                            }
                        }
                        _program[lineCount] = parsedCardChars;
                        programLineMap.put(lineCount, fileLine); // preserve relationship between program line and file line
                        break;
                    }
                    case INPUT: {
                        if (lineCount == 0)
                            reportLn(report, "Index\tData");

                        _input[lineCount] = Long.parseLong(parsedCardString);
                        break;
                    }
                }

                if (loadState == LoadState.PROGRAM)
                    reportLn(report, lineCount + "\t" + parsedCardString);
                else
                    reportLn(report, lineCount + "\t" + Long.parseLong(parsedCardString));

                lineCount++;
                fileLine++;

                if (loadState == LoadState.PROGRAM)
                    programSize = lineCount; // keep tally of program cards right away, in case of missing final separator card
            }
            if (loadState == LoadState.INPUT)
                inputSize = lineCount;
        } catch (UnsupportedOperationException e) { // used to indicate specific problem with card operation
            throw new Exception("Invalid card format on line " + fileLine + " of " + fileName + ": " + e.getMessage());
        } catch (Exception e) {
            throw new Exception("Invalid card format on line " + fileLine + " of " + fileName);
        }

        if (programSize == 0)
            throw new Exception("No program cards were loaded");

        reportLn(report);
        reportLn(report, "--Program Loaded Successfully--");
    }

    public static void exportProgram(String inFile, String outFile) throws Exception {
        List<String> unparsedList = new LinkedList<String>();
        String currLine;
        try {
            try (
                    FileReader fr = new FileReader(inFile);
                    BufferedReader br = new BufferedReader(fr)
            )
            {
                while ((currLine = br.readLine()) != null) {
                    unparsedList.add(currLine);
                }
            }
        } catch (IOException e) {
            throw new Exception("The file " + inFile + " could not be read");
        }

        exportProgram(unparsedList, inFile, outFile);
    }

    private static void exportProgram(List<String> unparsedList, String inFile, String outFile) throws Exception {
        List<String> parsedList = new LinkedList<String>();
        String parsedCard;
        int fileLine = 0;
        try {
            try (
                    FileWriter fw = new FileWriter(outFile, false);
                    BufferedWriter bw = new BufferedWriter(fw)
            )
            {
                for (String currLine : unparsedList) {
                    parsedCard = parseCard(currLine);
                    fileLine++;

                    if (parsedCard.isEmpty())
                        continue;

                    parsedList.add(parsedCard);
                }

                int i = 1;
                int separatorCount = 0;
                for (String s : parsedList) {

                    if (s.equals("+9999999999"))
                        separatorCount++;

                    bw.write(s);
                    if (i != parsedList.size()) // exclude empty new line at end of file
                        bw.newLine();

                    i++;
                }

                while (separatorCount < 2) {
                    bw.newLine();
                    bw.write("+9999999999");

                    separatorCount++;
                }
            }
        } catch (IOException e) {
            throw new IOException("The file " + outFile + " could not be written");
        } catch (Exception e) {
            throw new Exception("Invalid card format on line " + (fileLine+1) + " of " + inFile);
        }
    }

    public void exportProgram(String outFile) throws Exception {
        exportProgram(inputInstructionList, fileName, outFile);
    }

    public void clearBreakpoints() {
        breakpointSet.clear();
    }

    public void clearUnusedBreakpoints() {
        LinkedList<Integer> unusedBreakpoints = new LinkedList<>();

        // find breakpoints beyond the span of the program
        for (int row : breakpointSet) {
            if (row >= programSize)
                unusedBreakpoints.add(row);
        }

        // remove the unused breakpoints
        for (int row : unusedBreakpoints) {
            breakpointSet.remove(row);
        }
    }

    public String setBreakpoint(int row) {
        if (row >= 0 && row < programSize) {
            breakpointSet.add(row);
            return "Breakpoint set on row " + row;
        } else {
            return "Breakpoint not set because line " + row + " doesn't exist in the program";
        }
    }

    public String removeBreakpoint(int row) {
        if (breakpointSet.contains(row)) {
            breakpointSet.remove(row);
            return "Breakpoint unset on row " + row;
        } else {
            return "Breakpoint " + row + " not unset because it doesn't exist";
        }
    }

    public boolean breakpointIsSet(int row) {
        return breakpointSet.contains(row);
    }

    private void reportLn(StringBuilder sb) {
        if (sb != null)
            sb.append(System.getProperty("line.separator"));
    }

    private void reportLn(StringBuilder sb, String output) {
        if (sb != null) {
            if (sb.length() != 0) // pre-append new lines to prevent empty line at end of output file
                sb.append(System.getProperty("line.separator"));

            sb.append(output);
        }
    }

    private static String parseCard(String inputString) throws Exception { // alt regex (doesn't allow spaces): ^([-+]{1})(\d{10})
        if (inputString.isEmpty())
            return inputString; // ignore blank lines

        Pattern pattern = Pattern.compile("^([-+]{1})(\\d{1})(\\d{3})(\\d{3})(\\d{3})");// pattern of correct instruction
        String instructionString = inputString.split(";")[0].replaceAll("\\s", ""); // split on comment and remove whitespace
        Matcher matcher = pattern.matcher(instructionString);

        if (!matcher.matches() && !instructionString.isEmpty())
            throw new Exception(); // indicate a problem with this line

        return instructionString;
    }

    private long getDataValue(int index) throws Exception {
        if (index > 999)
            throw new Exception("Data index " + index + " exceeds memory range of " + (MAX_MEM-1));

        if (_data[index] == Long.MIN_VALUE)
            throw new Exception("Attempted to access uninitialized memory location " + index);
        else
            return _data[index];
    }

    private long getData(int index) throws Exception {
        return getData(index, 0);
    }

    private long getData(int index, int offset) throws Exception {
        if (!labelsEnabled)
            return getDataValue(index + offset);
        else {
            if (!dataSymbolMap.containsKey(index))
                throw new Exception("Data symbol " + index + " has not been declared");
            else if (dataSymbolSizeMap.get(index) <= offset)
                throw new Exception("Offset " + offset + " is out of bounds for data symbol " + index);
            else {
                lastReadDataSymbolSet.add(index);
                return getDataValue(dataSymbolMap.get(index) + offset);
            }
        }
    }

    private void setDataValue(long value, int index) throws Exception {
        if (index > 999)
            throw new Exception("Data index " + index + " exceeds maximum memory range of " + (MAX_MEM-1));
        else {
            _data[index] = value;
            lastWrittenDataLocation = index;
            usedDataLocations.add(index);
        }
    }

    private void setData(long value, int index) throws Exception {
        setData(value, index, 0);
    }

    private void setData(long value, int index, int offset) throws Exception {
        if (!labelsEnabled)
            setDataValue(value, index + offset);
        else {
            if (!dataSymbolMap.containsKey(index))
                throw new Exception("Data symbol " + index + " has not been declared");
            else if (dataSymbolSizeMap.get(index) <= offset)
                throw new Exception("Offset " + offset + " is out of bounds for data symbol " + index);
            else {
                lastWrittenDataSymbol = index;
                setDataValue(value, dataSymbolMap.get(index) + offset);
            }
        }
    }

    public static String instructionToString(long instruction) {
        return ((instruction >= 0) ? "+" : "-") + String.format("%010d", Math.abs(instruction));
    }

    public Instruction getProgramInstruction(int index) throws Exception {
        if (_program[index] == null)
            throw new UnsupportedOperationException("Program line " + index + " has not been set. Missing end card?");
        else {
            return parseInstruction(_program[index]);
        }
    }

    private Instruction parseInstruction(char[] instruction) {
        boolean positive;
        int op, opn1, opn2, opn3;

        positive = instruction[0] == '+';
        op = (instruction[1] - '0');
        opn1 = Integer.parseInt(new String(instruction, 2, 3));
        opn2 = Integer.parseInt(new String(instruction, 5, 3));
        opn3 = Integer.parseInt(new String(instruction, 8, 3));

        return new Instruction(positive, op, opn1, opn2, opn3);
    }

    public boolean programComplete() {
        return (lastInstructionPointer > 0) && (String.valueOf(_program[lastInstructionPointer]).equals("+9000000000"));
    }

    public ResultData runNextInstruction() throws Exception {
        lastWrittenDataLocation = -1;
        lastReadInputCard = -1;
        lastReadProgramLabel = -1;
        lastWrittenDataSymbol = -1;
        lastReadDataSymbolSet.clear();
        boolean positive, continueProgram = true;
        Integer op, opn1, opn2, opn3;
        long[] result = null;
        Instruction instruction = null;

        try {
            instruction = getProgramInstruction(instructionPointer);
            positive = instruction.getPositive();
            op = instruction.getOp();
            opn1 = instruction.getOpn1();
            opn2 = instruction.getOpn2();
            opn3 = instruction.getOpn3();
            lastInstructionPointer = instructionPointer;
            instructionPointer++;

            switch (op) {
                case 0:
                default:
                    if (positive) result = opMove(opn1, opn2, opn3);
                    else throw new Exception("Called unsupported operation -0");
                    break;
                case 1:
                    if (positive) result = opAdd(opn1, opn2, opn3);
                    else result = opSubtract(opn1, opn2, opn3);
                    break;
                case 2:
                    if (positive) result = opMultiply(opn1, opn2, opn3);
                    else result = opDivide(opn1, opn2, opn3);
                    break;
                case 3:
                    if (positive) result = opSquare(opn1, opn2, opn3);
                    else result = opRoot(opn1, opn2, opn3);
                    break;
                case 4:
                    if (positive) result = opEqual(opn1, opn2, opn3);
                    else result = opUnequal(opn1, opn2, opn3);
                    break;
                case 5:
                    if (positive) result = opGreaterThanEqual(opn1, opn2, opn3);
                    else result = opLessThan(opn1, opn2, opn3);
                    break;
                case 6:
                    if (positive) result = opFromArray(opn1, opn2, opn3);
                    else result = opToArray(opn1, opn2, opn3);
                    break;
                case 7:
                    if (positive)
                        result = opIncrementAndTest(opn1, opn2, opn3);
                    else if (!labelsEnabled)
                        throw new Exception("Called unsupported operation -7");

                    break; // nothing special when arriving at a valid label
                case 8:
                    if (positive) result = opRead(opn1, opn2, opn3);
                    else result = opPrint(opn1, opn2, opn3);
                    break;
                case 9:
                    if (positive) continueProgram = false;
                    else throw new Exception("Called unsupported operation -9");
            }

        } catch (UnsupportedOperationException e) {
            throw e; // special error case where the " on line ..." addition wouldn't make sense
        } catch (Exception e){
            throw new Exception(e.getMessage() + " (line " + programLineMap.get(lastInstructionPointer) + " of " + fileName + ")");
        }

        return new ResultData(instruction, result, continueProgram);
    }

    public static String getResultOutput(ResultData resultData) {
        Instruction inst = resultData.getInstruction();
        long[] result = resultData.getResult();

        switch (inst.getOp()) {
            case 0:
                if (inst.getPositive()) { // move
                    return("Move " + result[0] + " into " + inst.getOpn3());
                }
                break;
            case 1:
                if (inst.getPositive()) { // add
                    return(result[0] + " + " + result[1] + " = " + result[2] + " into " + inst.getOpn3());
                } else { // subtract
                    return(result[0] + " - " + result[1] + " = " + result[2] + " into " + inst.getOpn3());
                }
            case 2:
                if (inst.getPositive()) { // multiply
                    return(result[0] + " * " + result[1] + " = " + result[2] + " into " + inst.getOpn3());
                } else { // divide
                    return(result[0] + " / " + result[1] + " = " + result[2] + " into " + inst.getOpn3());
                }
            case 3:
                if (inst.getPositive()) { // square
                    return(result[0] + " squared = " + result[1] + " into " + inst.getOpn3());
                } else { // square root
                    return("Square root of " + result[0] + " = " + result[1] + " into " + inst.getOpn3());
                }
            case 4:
                if (inst.getPositive()) { // equal
                    return(result[0] + " == " + result[1] + " is " + ((result[2] == 1) ? "true:" : "false: don't") + " go to "
                            + ((result[3] == Long.MIN_VALUE) ? "line " + inst.getOpn3() : "label " + inst.getOpn3() + " (line " + result[3] + ")"));
                } else { // unequal
                    return(result[0] + " != " + result[1] + " is " + ((result[2] == 1) ? "true:" : "false: don't") + " go to "
                            + ((result[3] == Long.MIN_VALUE) ? "line " + inst.getOpn3() : "label " + inst.getOpn3() + " (line " + result[3] + ")"));
                }
            case 5:
                if (inst.getPositive()) { // greater than or equal to
                    return(result[0] + " >= " + result[1] + " is " + ((result[2] == 1) ? "true:" : "false: don't") + " go to "
                            + ((result[3] == Long.MIN_VALUE) ? "line " + inst.getOpn3() : "label " + inst.getOpn3() + " (line " + result[3] + ")"));
                } else { // less than
                    return(result[0] + " < " + result[1] + " is " + ((result[2] == 1) ? "true:" : "false: don't") + " go to "
                            + ((result[3] == Long.MIN_VALUE) ? "line " + inst.getOpn3() : "label " + inst.getOpn3() + " (line " + result[3] + ")"));
                }
            case 6:
                if (inst.getPositive()) { // from array
                    return("Move " + result[2] + " from " + inst.getOpn1() + "[" + result[0] + "] into " + inst.getOpn3());
                } else { // to array
                    return("Move " + result[0] + " from " + inst.getOpn1() + " into " + inst.getOpn2() + "[" + result[1] + "]");
                }
            case 7:
                if (inst.getPositive()) { // increment and test
                    return("Increment " + inst.getOpn1() + " and test: "
                            + result[0] + " < " + result[1] + " is " + ((result[2] == 1) ? "true:" : "false: don't") + " go to "
                            + ((result[3] == Long.MIN_VALUE) ? "line " + inst.getOpn3() : "label " + inst.getOpn3() + " (line " + result[3] + ")"));
                } else { // arrived at label
                    return ("--- Label " + inst.getOpn1() + " ---");
                }
            case 8:
                if (inst.getPositive()) { // read
                    return("Read " + result[0] + " into " + inst.getOpn3());
                } else { // print
                    return("Print " + result[0] + " from " + inst.getOpn1());
                }
            case 9:
                if (inst.getPositive()) { // program end
                    return("End program");
                }
                break;
        }
        return "";
    }

    private long setArithResult(long result, int index) throws Exception { // enforce numeric size boundaries
        if (result > 9999999999L)
            throw new Exception("Program memory overflow: the result (" + result + ") exceeds the maximum value of 9999999999");
        else if (result < -9999999999L)
            throw new Exception("Program memory underflow: the result (" + result + ") exceeds the minimum value of -9999999999");
        else
            setData(result, index);

        return result;
    }

    // Operation methods
    long[] opMove(int opn1, int opn2, int opn3) throws Exception // +0 put data[opn1] in opn3
    {
        long[] returnArray = new long[]{getData(opn1)};
        setData(getData(opn1), opn3);
        return returnArray;
    }

    long[] opAdd(int opn1, int opn2, int opn3) throws Exception // +1 add data[opn1] to data[opn2] and put in opn3
    {
        long result = getData(opn1) + getData(opn2);
        long[] returnArray = new long[]{getData(opn1), getData(opn2), result};
        setArithResult(result, opn3);
        return returnArray;
    }

    long[] opSubtract(int opn1, int opn2, int opn3) throws Exception // -1 subtract data[opn2] from data[opn1] and put in opn3
    {
        long result = getData(opn1) - getData(opn2);
        long[] returnArray = new long[]{getData(opn1), getData(opn2), result};
        setArithResult(result, opn3);
        return returnArray;
    }

    long[] opMultiply(int opn1, int opn2, int opn3) throws Exception // +2 multiply data[opn1] by data[opn2] and put in opn3
    {
        long result = getData(opn1) * getData(opn2);
        long[] returnArray = new long[]{getData(opn1), getData(opn2), result};
        setArithResult(result, opn3);
        return returnArray;
    }

    long[] opDivide(int opn1, int opn2, int opn3) throws Exception // -2 divide data[opn1] by data[opn2] and put in opn3
    {
        long result = getData(opn1) / getData(opn2);
        long[] returnArray = new long[]{getData(opn1), getData(opn2), result};
        setArithResult(result, opn3);
        return returnArray;
    }

    long[] opSquare(int opn1, int opn2, int opn3) throws Exception // +3 square data[opn1] and put in opn3
    {
        long result = getData(opn1) * getData(opn1);
        long[] returnArray = new long[]{getData(opn1), result};
        setArithResult(result, opn3);
        return returnArray;
    }

    long[] opRoot(int opn1, int opn2, int opn3) throws Exception // -3 square root data[opn1] and put in opn3
    {
        long result = (long)Math.sqrt(getData(opn1));
        long[] returnArray = new long[]{getData(opn1), result};
        setArithResult(result, opn3);
        return returnArray;
    }

    long[] opEqual(int opn1, int opn2, int opn3) throws Exception // +4 if data[opn1] == data[opn2] go to opn3
    {
        long result;
        if (getData(opn1) == getData(opn2)) {
            if (labelsEnabled) {
                if (programLabelMap.containsKey(opn3)) {
                    lastReadProgramLabel = opn3;
                    instructionPointer = programLabelMap.get(opn3);
                } else
                    throw new Exception("Program label " + opn3 + " has not been defined");
            } else
              instructionPointer = opn3;

            result = 1L;
        } else
            result = 0L;

        return new long[]{getData(opn1), getData(opn2), result, ((labelsEnabled && programLabelMap.containsKey(opn3)) ?  programLabelMap.get(opn3) : Long.MIN_VALUE)};
    }

    long[] opUnequal(int opn1, int opn2, int opn3) throws Exception // -4 if data[opn1] != data[opn2] goto opn3
    {
        long result;
        if (getData(opn1) != getData(opn2)) {
            if (labelsEnabled) {
                if (programLabelMap.containsKey(opn3)) {
                    lastReadProgramLabel = opn3;
                    instructionPointer = programLabelMap.get(opn3);
                } else
                    throw new Exception("Program label " + opn3 + " has not been defined");
            } else
                instructionPointer = opn3;

            result = 1L;
        } else
            result = 0L;

        return new long[]{getData(opn1), getData(opn2), result, ((labelsEnabled && programLabelMap.containsKey(opn3)) ?  programLabelMap.get(opn3) : Long.MIN_VALUE)};
    }

    long[] opGreaterThanEqual(int opn1, int opn2, int opn3) throws Exception // +5 if data[opn1] >= data[opn2] goto opn3
    {
        long result;
        if (getData(opn1) >= getData(opn2)) {
            if (labelsEnabled) {
                if (programLabelMap.containsKey(opn3)) {
                    lastReadProgramLabel = opn3;
                    instructionPointer = programLabelMap.get(opn3);
                } else
                    throw new Exception("Program label " + opn3 + " has not been defined");
            } else
                instructionPointer = opn3;

            result = 1L;
        } else
            result = 0L;

        return new long[]{getData(opn1), getData(opn2), result, ((labelsEnabled && programLabelMap.containsKey(opn3)) ?  programLabelMap.get(opn3) : Long.MIN_VALUE)};
    }

    long[] opLessThan(int opn1, int opn2, int opn3) throws Exception // -5 if data[opn1] < data[opn3] goto opn3
    {
        long result;
        if (getData(opn1) < getData(opn2)) {
            if (labelsEnabled) {
                if (programLabelMap.containsKey(opn3)) {
                    lastReadProgramLabel = opn3;
                    instructionPointer = programLabelMap.get(opn3);
                } else
                    throw new Exception("Program label " + opn3 + " has not been defined");
            } else
                instructionPointer = opn3;

            result = 1L;
        } else
            result = 0L;

        return new long[]{getData(opn1), getData(opn2), result, ((labelsEnabled && programLabelMap.containsKey(opn3)) ?  programLabelMap.get(opn3) : Long.MIN_VALUE)};
    }

    long[] opFromArray(int opn1, int opn2, int opn3) throws Exception // +6 opn1[opn2] -> opn3
    {
        long result;
        result = getData(opn1, (int) getData(opn2));
        long[] returnArray = new long[]{getData(opn2), opn1 + getData(opn2), result};
        setData(result, opn3);
        return returnArray;
    }

    long[] opToArray(int opn1, int opn2, int opn3) throws Exception // -6 opn1 -> opn2[opn3]
    {
        long[] returnArray = new long[]{getData(opn1), getData(opn3), opn2 + getData(opn3)};
        setData(getData(opn1), opn2, (int) getData(opn3));
        return returnArray;
    }

    long[] opIncrementAndTest(int opn1, int opn2, int opn3) throws Exception // +7 auto-increment opn1, then if opn1 is less than opn2, goto opn3
    {
        long result;
        setData(getData(opn1) + 1, opn1);
        if (getData(opn1) < getData(opn2)) {
            if (labelsEnabled) {
                if (programLabelMap.containsKey(opn3)) {
                    lastReadProgramLabel = opn3;
                    instructionPointer = programLabelMap.get(opn3);
                } else
                    throw new Exception("Program label " + opn3 + " has not been defined");
            } else
                instructionPointer = opn3;

            result = 1L;
        } else
            result = 0L;

        return new long[]{getData(opn1), getData(opn2), result, ((labelsEnabled && programLabelMap.containsKey(opn3)) ?  programLabelMap.get(opn3) : Long.MIN_VALUE)};
    }

    long[] opRead(int opn1, int opn2, int opn3) throws Exception // +8 read from card into opn3
    {
        long result;
        if (_input[inputPointer] != Long.MIN_VALUE) {
            result = _input[inputPointer];
            lastReadInputCard = inputPointer;
            inputPointer++;
            setData(result, opn3);
        }  else
            throw new Exception("Attempted to read beyond bounds of input cards");

        return new long[]{result};
    }

    long[] opPrint(int opn1, int opn2, int opn3) throws Exception // -8 print value of opn1
    {
        return new long[]{getData(opn1)};
    }
}