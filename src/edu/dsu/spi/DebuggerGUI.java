package edu.dsu.spi;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.TreeMap;

public class DebuggerGUI {
    private Interpreter interp;
    private DebugWindow debugWindow;
    private HelpWindow helpWindow;
    private AboutDialog aboutDialog;

    // Table Models
    private AbstractTableModel atmDataModel;
    private AbstractTableModel atmProgramModel;
    private AbstractTableModel atmInputModel;
    private AbstractTableModel atmDataSymbolModel;
    private AbstractTableModel atmProgramLabelModel;

    private StringBuilder sbOutput;

    private boolean brokeWithError;
    private StringBuilder sbPrint;
    private boolean enableLabels;

    public DebuggerGUI(Interpreter interp) {
        this.interp = interp;
        this.debugWindow = new DebugWindow();
        this.helpWindow = new HelpWindow();
        this.aboutDialog = new AboutDialog(debugWindow);
        clearOutput();
        debugWindow.getRefreshItem().setEnabled(false);
        disableInterpreterActions();
        setLabelsEnabled(false);
        initializeTableModels();
        initializeRenderers();
        setMenuHandlers();
        setMouseListeners();
    }

    private void initializeTableModels() {
        atmDataModel = new AbstractTableModel() {
            String[] header = {"#", "Value"};
            Integer[] usedDataLocations = null;

            @Override
            public String getColumnName(int index) {
                return header[index];
            }

            @Override
            public int getRowCount() {
                usedDataLocations = interp.getUsedDataLocations().toArray(new Integer[interp.getUsedDataLocations().size()]);
                return usedDataLocations.length;
            }

            @Override
            public int getColumnCount() {
                return header.length;
            }

            @Override
            public String getValueAt(int rowIndex, int columnIndex) {
                if (interp == null)
                    return null;
                else if (columnIndex == 0)
                    return Integer.toString(usedDataLocations[rowIndex]);
                else {
                    long cell = interp.accessData(usedDataLocations[rowIndex]);

                    if (cell == Long.MIN_VALUE)
                        return "";
                    else
                        return Long.toString(cell);
                }
            }
        };
        debugWindow.getDataTable().setModel(atmDataModel);
        debugWindow.getDataTable().getColumnModel().getColumn(0).setPreferredWidth(20);

        atmProgramModel = new AbstractTableModel() {
            String[] header = {"Line", "Instr", "Opn1", "Opn2", "Opn3"};

            @Override
            public String getColumnName(int index) {
                return header[index];
            }

            @Override
            public int getRowCount() {
                return interp.getProgramSize();
            }

            @Override
            public int getColumnCount() {
                return header.length;
            }

            @Override
            public String getValueAt(int rowIndex, int columnIndex) {
                if (interp == null)
                    return null;
                else if (columnIndex == 0)
                    return Integer.toString(rowIndex);
                else {
                    Instruction instruction;
                    try {
                        instruction = interp.getProgramInstruction(rowIndex);

                        switch(columnIndex) {
                            case 1:
                                return instruction.opToString();
                            case 2:
                                return instruction.opn1ToString();
                            case 3:
                                return instruction.opn2ToString();
                            case 4:
                                return instruction.opn3ToString();
                        }
                    } catch (Exception e) {
                        updateOutput("Error: " + e.getMessage());
                    }

                    return null;
                }
            }
        };
        debugWindow.getProgramTable().setModel(atmProgramModel);

        atmInputModel = new AbstractTableModel() {
            String[] header = {"#", "Value"};

            @Override
            public String getColumnName(int index) {
                return header[index];
            }

            @Override
            public int getRowCount() {
                return interp.getInputSize();
            }

            @Override
            public int getColumnCount() {
                return header.length;
            }

            @Override
            public String getValueAt(int rowIndex, int columnIndex) {
                if (interp == null)
                    return null;
                else if (columnIndex == 0)
                    return Integer.toString(rowIndex);
                else {
                    return Long.toString(interp.accessInput(rowIndex));
                }
            }
        };
        debugWindow.getInputTable().setModel(atmInputModel);
        debugWindow.getInputTable().getColumnModel().getColumn(0).setPreferredWidth(20);

        atmDataSymbolModel = new AbstractTableModel() {
            String[] header = {"Label", "Data #", "Size"};

            @Override
            public String getColumnName(int index) {
                return header[index];
            }

            @Override
            public int getRowCount() {
                return interp.getDataSymbolMapSize();
            }

            @Override
            public int getColumnCount() {
                return header.length;
            }

            @Override
            public String getValueAt(int rowIndex, int columnIndex) {
                if (interp == null)
                    return null;

                TreeMap<Integer, Integer> dataSymbolMap = interp.getDataSymbolMap();
                int size = interp.getDataSymbolMapSize();
                Integer[] keyArray, valueArray;

                if (columnIndex == 0 || columnIndex == 2) {
                    keyArray = dataSymbolMap.keySet().toArray(new Integer[size]);
                    if (columnIndex == 0)
                        return Integer.toString(keyArray[rowIndex]); // label column
                    else
                        return Integer.toString(interp.getDataSymbolSize(keyArray[rowIndex])); // size column
                } else {
                    valueArray = dataSymbolMap.values().toArray(new Integer[size]);
                    return Integer.toString(valueArray[rowIndex]);
                }
            }
        };
        debugWindow.getDataSymbolTable().setModel(atmDataSymbolModel);

        atmProgramLabelModel = new AbstractTableModel() {
            String[] header = {"Label", "Line"};

            @Override
            public String getColumnName(int index) {
                return header[index];
            }

            @Override
            public int getRowCount() {
                return interp.getProgramLabelMapSize();
            }

            @Override
            public int getColumnCount() {
                return header.length;
            }

            @Override
            public String getValueAt(int rowIndex, int columnIndex) {
                if (interp == null)
                    return null;

                TreeMap<Integer, Integer> programLabelMap = interp.getProgramLabelMap();
                Integer[] keyArray, valueArray;
                if (columnIndex == 0) {
                    keyArray = programLabelMap.keySet().toArray(new Integer[programLabelMap.size()]);
                    return Integer.toString(keyArray[rowIndex]);
                } else {
                    valueArray = programLabelMap.values().toArray(new Integer[programLabelMap.size()]);
                    return Integer.toString(valueArray[rowIndex]);
                }
            }
        };
        debugWindow.getProgramLabelTable().setModel(atmProgramLabelModel);

    }

    private void initializeRenderers() {
        debugWindow.getDataTable().setDefaultRenderer(Object.class, new DataTableRenderer());
        debugWindow.getProgramTable().setDefaultRenderer(Object.class, new ProgramTableRenderer());
        debugWindow.getInputTable().setDefaultRenderer(Object.class, new InputTableRenderer());
        debugWindow.getProgramLabelTable().setDefaultRenderer(Object.class, new ProgramLabelTableRenderer());
        debugWindow.getDataSymbolTable().setDefaultRenderer(Object.class, new DataSymbolTableRenderer());
    }

    private void setMenuHandlers() {
        debugWindow.getOpenItem().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                StringBuilder returnOutput = new StringBuilder();
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setCurrentDirectory(new File("."));
                fileChooser.setFileFilter(new ProgramFiler());
                fileChooser.setAcceptAllFileFilterUsed(false);
                int result = fileChooser.showOpenDialog(debugWindow);

                if (result == JFileChooser.APPROVE_OPTION) {
                    String filePath = fileChooser.getSelectedFile().getAbsolutePath();
                    int n = JOptionPane.showConfirmDialog(
                            debugWindow,
                            "Does the program use labels or symbols? Choose 'No' if you don't know.",
                            "Labels and Symbols",
                            JOptionPane.YES_NO_OPTION);

                    enableLabels = n == 0; // set interpreter type
                    try {
                        debugWindow.getRefreshItem().setEnabled(true); // allow refreshing the selected file
                        interp.readProgram(filePath, returnOutput, enableLabels);
                        enableInterpreterActions();
                        resetExecution();
                    } catch (IOException e1) {
                        JOptionPane.showMessageDialog(null, e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        disableInterpreterActions();
                    } catch (Exception e) {
                        appendString(returnOutput, "Error: " + e.getMessage());
                        disableInterpreterActions();
                    } finally {
                        clearOutput();
                        updateOutput(returnOutput);
                        updateTables();
                        setLabelsEnabled(interp.getLabelsEnabled());
                    }
                }
            }
        });

        debugWindow.getRefreshItem().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                StringBuilder returnOutput = new StringBuilder();
                try {
                    interp.loadProgram(returnOutput, enableLabels);
                    enableInterpreterActions();
                    resetExecution();
                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(null, e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    disableInterpreterActions();
                } catch (Exception e) {
                    appendString(returnOutput, "Error: " + e.getMessage());
                    disableInterpreterActions();
                } finally {
                    clearOutput();
                    updateOutput(returnOutput);
                    updateTables();
                    setLabelsEnabled(interp.getLabelsEnabled());
                }
            }
        });

        debugWindow.getExportItem().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setCurrentDirectory(new File("."));
                fileChooser.setSelectedFile(new File("outfile.dat"));
                fileChooser.setFileFilter(new ProgramFiler());
                fileChooser.setAcceptAllFileFilterUsed(false);
                int result = fileChooser.showSaveDialog(debugWindow);

                if (result == JFileChooser.APPROVE_OPTION) {
                    // ensure valid export extension
                    File exportFile = fileChooser.getSelectedFile();
                    String fileName = fileChooser.getSelectedFile().getName();
                    int dotPosition = fileName.lastIndexOf(".");
                    if (dotPosition == -1) {
                        exportFile = new File(exportFile.toString() + ".dat");
                    }

                    try{
                        interp.exportProgram(exportFile.toString());
                        updateOutput("The file " + fileChooser.getSelectedFile().getAbsoluteFile() + " was exported successfully.");
                    } catch (IOException e1) {
                        JOptionPane.showMessageDialog(null,
                                "The file " + fileChooser.getSelectedFile().getAbsoluteFile() + " could not be saved to.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                    } catch (Exception e1) { // this should never happen
                        JOptionPane.showMessageDialog(null,
                                "Parsing Error: " + e1.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        debugWindow.getSaveOutputItem().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setCurrentDirectory(new File("."));
                fileChooser.setSelectedFile(new File("output.txt"));
                fileChooser.setFileFilter(new OutputLogFilter());
                fileChooser.setAcceptAllFileFilterUsed(false);
                int result = fileChooser.showSaveDialog(debugWindow);

                if (result == JFileChooser.APPROVE_OPTION) {
                    // ensure valid output extension
                    File outputFile = fileChooser.getSelectedFile();
                    String fileName = fileChooser.getSelectedFile().getName();
                    int dotPosition = fileName.lastIndexOf(".");
                    if (dotPosition == -1) {
                        outputFile = new File(outputFile.toString() + ".txt");
                    }

                    try (
                            FileWriter fw = new FileWriter(outputFile);
                            BufferedWriter bw = new BufferedWriter(fw)
                    ) {
                        bw.write(sbOutput.toString()); // write output stringbuilder directly to file
                    } catch (IOException e1) {
                        JOptionPane.showMessageDialog(null,
                                "The file " + fileChooser.getSelectedFile().getAbsoluteFile() + " could not be saved to.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        debugWindow.getExitItem().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                System.exit(0);
            }
        });

        debugWindow.getRunItem().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                runProgram(false, false);
            }
        });

        debugWindow.getRunDebugItem().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                runProgram(false, true);
            }
        });

        debugWindow.getStepItem().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                runProgram(true, true);
            }
        });

        debugWindow.getResetItem().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                resetProgram();
            }
        });

        debugWindow.getHelpItem().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    helpWindow.showWindow();
                } catch (Exception e1) {
                    JOptionPane.showMessageDialog(null,
                            e1.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        debugWindow.getAboutItem().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                aboutDialog.show();
            }
        });
    }

    private void setMouseListeners() {
        debugWindow.getProgramTable().addMouseListener(new MouseAdapter() {
            private int clickStartRow = -1;

            // use manual pressed/released rather than mouseClicked due to mouseClicked being unreliable
            @Override
            public void mousePressed(MouseEvent e) {
                super.mousePressed(e);
                JTable target = (JTable) e.getSource();
                clickStartRow = target.getSelectedRow();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                super.mouseReleased(e);

                if (!interp.breakpointSetOnRow(clickStartRow))
                    interp.setBreakpoint(clickStartRow);
                else
                    interp.removeBreakpoint(clickStartRow);

                atmProgramModel.fireTableDataChanged();

                clickStartRow = -1;
            }
        });
    }

    private void resetProgram() {
        resetExecution();
        StringBuilder loadResult = new StringBuilder();
        try {
            interp.parseProgram(loadResult);
        } catch (Exception e) {
            appendString(loadResult, e.getMessage());
        } finally {
            clearOutput();
            updateOutput(loadResult);
            updateTables();
            setRunActionsEnabled(!interp.programComplete()); // prevent running or stepping when program is complete
        }
    }

    private void runProgram(boolean step, boolean debug) {
        ResultData resultData;

        try {
            while (!interp.programComplete()) {
                resultData = interp.runNextInstruction();

                if (debug) {
                    String debugString = Interpreter.getResultOutput(resultData);
                    updateOutput(interp.getLastInstructionPointer() + ":\t" + debugString);
                }

                Instruction inst = resultData.getInstruction();
                if (!inst.getPositive() && inst.getOp() == 8) {
                    // preserve result of print statement for later
                    appendString(sbPrint, Long.toString(resultData.getResult()[0]));
                }

                if (interp.breakpointSetOnInstructionPointer()) {
                    step = true; // force a break
                    updateOutput("Breakpoint triggered on row " + interp.getInstructionPointer());
                }

                if (step)
                    break;
            }

            if (interp.programComplete() && sbPrint.length() > 0) { // full print to console
                if (debug) {
                    updateOutput("----------------");
                    updateOutput("Full output:");
                }

                updateOutput(sbPrint);
            }

            updateTables();
            autoScrollTables();
            setRunActionsEnabled(!interp.programComplete()); // prevent running or stepping when program is complete
        } catch (Exception e) {
            updateOutput("Error: " + e.getMessage());
            brokeWithError = true;
            autoScrollTables();
            setRunActionsEnabled(false); // disable running and stepping until program is reset
        }
    }

    private void autoScrollTables() {
        Rectangle cellRect;
        JTable table;

        if (interp.getLastWrittenDataLocation() != -1) {
            table = debugWindow.getDataTable();

            int i = 0;
            for (int x : interp.getUsedDataLocations())
            {
                if (x == interp.getLastWrittenDataLocation())
                    break;

                i++;
            }

            cellRect = table.getCellRect(i, 0, true);
            table.scrollRectToVisible(cellRect);
        }

        if (interp.getLastInstructionPointer() != -1) {
            table = debugWindow.getProgramTable();
            cellRect = table.getCellRect(interp.getLastInstructionPointer(), 0, true);
            table.scrollRectToVisible(cellRect);
        }

        if (interp.getLastReadInputCard() != -1) {
            table = debugWindow.getInputTable();
            cellRect = table.getCellRect(interp.getLastReadInputCard(), 0, true);
            table.scrollRectToVisible(cellRect);
        }

        if (interp.getLastReadProgramLabel() != -1) {
            table = debugWindow.getProgramLabelTable();
            Integer[] indexArray = interp.getProgramLabelMap().keySet().toArray(new Integer[interp.getProgramLabelMapSize()]);
            int x = -1;

            for (int i = 0; i < indexArray.length; i++) {
                if (indexArray[i] == interp.getLastReadProgramLabel()) {
                    x = i;
                    break;
                }
            }

            cellRect = table.getCellRect(x, 0, true);
            table.scrollRectToVisible(cellRect);
        }

        if (interp.getLastWrittenDataSymbol() != -1) {
            table = debugWindow.getDataSymbolTable();
            Integer[] indexArray = interp.getDataSymbolMap().keySet().toArray(new Integer[interp.getDataSymbolMapSize()]);
            int x = -1;

            for (int i = 0; i < indexArray.length; i++) {
                if (indexArray[i] == interp.getLastWrittenDataSymbol()) {
                    x = i;
                    break;
                }
            }

            cellRect = table.getCellRect(x, 0, true);
            table.scrollRectToVisible(cellRect);
        } else {
            for (int symbol : interp.getLastReadDataSymbolSet()) {
                table = debugWindow.getDataSymbolTable();
                Integer[] indexArray = interp.getDataSymbolMap().keySet().toArray(new Integer[interp.getDataSymbolMapSize()]);
                int x = -1;

                for (int i = 0; i < indexArray.length; i++) {
                    if (indexArray[i] == symbol) {
                        x = i;
                        break;
                    }
                }

                cellRect = table.getCellRect(x, 0, true);
                table.scrollRectToVisible(cellRect);
                break; // just do the first one
            }
        }
    }

    // Helper Methods
    private void updateTables() {
        atmDataModel.fireTableDataChanged();
        atmProgramModel.fireTableDataChanged();
        atmInputModel.fireTableDataChanged();
        atmProgramLabelModel.fireTableDataChanged();
        atmDataSymbolModel.fireTableDataChanged();
    }

    private void updateOutput(String s) {
        appendString(sbOutput, s);
        Document doc = debugWindow.getOutputPane().getDocument();
        try {
            if (doc.getLength() != 0)
                doc.insertString(doc.getLength(), System.getProperty("line.separator"), null);

            doc.insertString(doc.getLength(), s, null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        } finally {
            enableSaveOutput();
        }
    }

    private void updateOutput(StringBuilder sb) {
        appendString(sbOutput, sb);
        Document doc = debugWindow.getOutputPane().getDocument();
        try {
            if (doc.getLength() != 0)
                doc.insertString(doc.getLength(), System.getProperty("line.separator"), null);

            doc.insertString(doc.getLength(), sb.toString(), null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        } finally {
            enableSaveOutput();
        }
    }

    private void updateOutput() {
        debugWindow.getOutputPane().setText(sbOutput.toString());
        enableSaveOutput();
    }

    private void enableSaveOutput() {
        if (!debugWindow.getSaveOutputItem().isEnabled() && sbOutput.length() != 0)
            debugWindow.getSaveOutputItem().setEnabled(true);
    }

    private void clearOutput() {
        sbOutput = new StringBuilder();
        updateOutput();
        debugWindow.getSaveOutputItem().setEnabled(false);
    }

    private void setLabelsEnabled(boolean enabled) {
        debugWindow.setLabelsEnabled(enabled);
    }

    private void setRunActionsEnabled(boolean enabled) {
        debugWindow.getRunItem().setEnabled(enabled);
        debugWindow.getRunDebugItem().setEnabled(enabled);
        debugWindow.getStepItem().setEnabled(enabled);
    }

    private void disableInterpreterActions() {
        debugWindow.getExportItem().setEnabled(false);
        debugWindow.getRunItem().setEnabled(false);
        debugWindow.getRunDebugItem().setEnabled(false);
        debugWindow.getStepItem().setEnabled(false);
        debugWindow.getResetItem().setEnabled(false);
    }

    private void enableInterpreterActions() {
        debugWindow.getExportItem().setEnabled(true);
        debugWindow.getRunItem().setEnabled(true);
        debugWindow.getRunDebugItem().setEnabled(true);
        debugWindow.getStepItem().setEnabled(true);
        debugWindow.getResetItem().setEnabled(true);
    }

    private void resetExecution() {
        sbPrint = new StringBuilder();
        brokeWithError = false;
    }

    private void appendString(StringBuilder sb, String string) {
        if (sb.length() > 0)
            sb.append(System.getProperty("line.separator"));

        sb.append(string);
    }

    private void appendString(StringBuilder sb1, StringBuilder sb2) {
        if (sb1.length() > 0)
            sb1.append(System.getProperty("line.separator"));

        sb1.append(sb2);
    }

    //// Custom Table Cell Renderers ////
    public class DataTableRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (Integer.parseInt(atmDataModel.getValueAt(row, 0).toString()) == interp.getLastWrittenDataLocation())
                setForeground(Color.red);
            else
                setForeground(Color.black);

            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    public class ProgramTableRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (interp.breakpointSetOnRow(row))
                this.setBackground(Color.pink);
            else
                this.setBackground(Color.white);

            if (Integer.parseInt(atmProgramModel.getValueAt(row, 0).toString()) == interp.getInstructionPointer())
                table.setForeground(Color.blue);
            else if (Integer.parseInt(atmProgramModel.getValueAt(row, 0).toString()) == interp.getLastInstructionPointer()) {
                table.setForeground(Color.red);
            } else
                table.setForeground(Color.black);

            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    public class InputTableRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (Integer.parseInt(atmInputModel.getValueAt(row, 0).toString()) == interp.getLastReadInputCard())
                setForeground(Color.blue);
            else
                setForeground(Color.black);

            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    public class DataSymbolTableRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Integer[] symbolArray = interp.getDataSymbolMap().keySet().toArray(new Integer[interp.getDataSymbolMapSize()]);
            if (interp.getLastWrittenDataSymbol() == symbolArray[row] && interp.getLastReadDataSymbolSet().contains(symbolArray[row]))
                setForeground(Color.magenta);
            else if (interp.getLastWrittenDataSymbol() == symbolArray[row])
                setForeground(Color.red);
            else if (interp.getLastReadDataSymbolSet().contains(symbolArray[row]))
                setForeground(Color.blue);
            else
                setForeground(Color.black);

            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    public class ProgramLabelTableRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Integer[] labelArray = interp.getProgramLabelMap().keySet().toArray(new Integer[interp.getProgramLabelMapSize()]);
            if (labelArray[row] == interp.getLastReadProgramLabel())
                setForeground(Color.blue);
            else
                setForeground(Color.black);

            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    // File filters
    public class ProgramFiler extends FileFilter {

        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            } else {
                return f.getName().toLowerCase().endsWith(".dat")
                        || f.getName().toLowerCase().endsWith(".txt");
            }
        }

        @Override
        public String getDescription() {
            return "All supported program formats(*.dat,*.txt)";
        }
    }

    public class OutputLogFilter extends FileFilter {

        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            } else {
                return f.getName().toLowerCase().endsWith(".txt")
                        || f.getName().toLowerCase().endsWith(".rtf")
                        || f.getName().toLowerCase().endsWith(".log");
            }
        }

        @Override
        public String getDescription() {
            return "All supported output formats(*.txt,*.rtf,*.log)";
        }
    }
}
