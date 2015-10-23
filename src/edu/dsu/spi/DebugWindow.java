package edu.dsu.spi;

import javax.swing.*;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.FormSpecs;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class DebugWindow extends JFrame {
	private JMenuBar mbMainMenu;

	private JMenuItem miOpen;
	private JMenuItem miRefresh;
	private JMenuItem miExport;
    private JMenuItem miSaveOutput;
    private JMenuItem miExit;
    private JMenuItem miRun;
	private JMenuItem miRunDebug;
    private JMenuItem miStep;
    private JMenuItem miReset;
    private JMenuItem miHelp;
    private JMenuItem miAbout;

	private JTable tblData;
	private JTable tblProgram;
	private JTable tblInput;
	private JTable tblDataSymbol;
	private JTable tblProgramLabel;
	private JTextPane tpOutput;

    JLabel lblDataSymbol;
    JLabel lblProgramLabel;

	public DebugWindow() {
        super("Suave Pseudo-Interpreter");
		initialize();
		this.setVisible(true);
	}

	private void initialize() {
        this.setMinimumSize(new Dimension(600, 700));
        this.setResizable(false);
        this.setBounds(100, 100, 552, 413);
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("pipe_icon.png")));
		//this.setIconImage(new ImageIcon(DebugWindow.class.getResource("/pipe2.png")).getImage());
		
		mbMainMenu = new JMenuBar();
        this.setJMenuBar(mbMainMenu);
		
		JMenu mnFile = new JMenu("File");
		mnFile.setMnemonic(KeyEvent.VK_F);
		mbMainMenu.add(mnFile);
		
		miOpen = new JMenuItem("Open");
		miOpen.setToolTipText("Open program in debugger.");
		miOpen.setMnemonic(KeyEvent.VK_O);
		mnFile.add(miOpen);

		miRefresh = new JMenuItem("Refresh");
		miRefresh.setToolTipText("Refreshes the current program from the file it was opened from.");
		miRefresh.setMnemonic(KeyEvent.VK_R);
		miRefresh.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0, true));
		mnFile.add(miRefresh);

		miExport = new JMenuItem("Export");
		miExport.setToolTipText("Export parsed program (removes spaces, comments, etc.).");
		miExport.setMnemonic(KeyEvent.VK_E);
		mnFile.add(miExport);
		
		miSaveOutput = new JMenuItem("Save Output");
		miSaveOutput.setToolTipText("Save contents of Output panel to a text file.");
		miSaveOutput.setMnemonic(KeyEvent.VK_S);
		mnFile.add(miSaveOutput);
		
		JSeparator separator = new JSeparator();
		mnFile.add(separator);
		
		miExit = new JMenuItem("Exit");
		miExit.setMnemonic(KeyEvent.VK_X);
		mnFile.add(miExit);
		
		JMenu mnInterpreter = new JMenu("Interpreter");
		mnInterpreter.setMnemonic(KeyEvent.VK_I);
		mbMainMenu.add(mnInterpreter);
		
		miRun = new JMenuItem("Run");
		miRun.setToolTipText("Run the loaded program.");
		miRun.setMnemonic(KeyEvent.VK_R);
		miRun.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0, true));
		mnInterpreter.add(miRun);

		miRunDebug = new JMenuItem("Run with Debug");
		miRunDebug.setToolTipText("Run the program with debug logging.");
		miRunDebug.setMnemonic(KeyEvent.VK_Q);
		miRunDebug.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, ActionEvent.SHIFT_MASK, true));
		mnInterpreter.add(miRunDebug);
		
		miStep = new JMenuItem("Step");
		miStep.setToolTipText("Advance one step in the loaded program.");
		miStep.setMnemonic(KeyEvent.VK_S);
		miStep.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0, true));
		mnInterpreter.add(miStep);
		
		miReset = new JMenuItem("Reset");
		miReset.setToolTipText("Reset loaded program to its start.");
		miReset.setMnemonic(KeyEvent.VK_E);
		miReset.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0, true));
		mnInterpreter.add(miReset);
		
		JMenu mnHelp = new JMenu("Help");
		mnHelp.setMnemonic(KeyEvent.VK_H);
		mbMainMenu.add(mnHelp);
		//mnHelp.setEnabled(false);
		
		miHelp = new JMenuItem("Help");
		miHelp.setToolTipText("Learn how to use Suave Pseudo-Interpreter.");
		miHelp.setMnemonic(KeyEvent.VK_H);
		miHelp.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0, true));
		mnHelp.add(miHelp);
		
		JSeparator separator_1 = new JSeparator();
		mnHelp.add(separator_1);
		
		miAbout = new JMenuItem("About");
		miAbout.setMnemonic(KeyEvent.VK_A);
		mnHelp.add(miAbout);
        this.getContentPane().setLayout(new FormLayout(new ColumnSpec[]{
				FormSpecs.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormSpecs.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormSpecs.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("default:grow"),
				FormSpecs.RELATED_GAP_COLSPEC,
				ColumnSpec.decode("max(2dlu;default)"),},
				new RowSpec[]{
						FormSpecs.RELATED_GAP_ROWSPEC,
						FormSpecs.DEFAULT_ROWSPEC,
						FormSpecs.RELATED_GAP_ROWSPEC,
						RowSpec.decode("max(124dlu;default)"),
						FormSpecs.RELATED_GAP_ROWSPEC,
						FormSpecs.DEFAULT_ROWSPEC,
						FormSpecs.RELATED_GAP_ROWSPEC,
						RowSpec.decode("default:grow"),
						FormSpecs.RELATED_GAP_ROWSPEC,
						FormSpecs.DEFAULT_ROWSPEC,
						FormSpecs.RELATED_GAP_ROWSPEC,
						RowSpec.decode("default:grow"),
						FormSpecs.RELATED_GAP_ROWSPEC,
						RowSpec.decode("max(2dlu;default)"),}));

		JLabel lblData = new JLabel("Data");
        this.getContentPane().add(lblData, "2, 2");
		
		JLabel lblProgram = new JLabel("Program");
        this.getContentPane().add(lblProgram, "4, 2");
		
		JLabel lblInput = new JLabel("Input");
        this.getContentPane().add(lblInput, "6, 2");

        tblData = new JTable();
		tblData.getTableHeader().setReorderingAllowed(false);
		JScrollPane spData = new JScrollPane(tblData);
		spData.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		spData.setPreferredSize(new Dimension(150, 200));
        this.getContentPane().add(spData, "2, 4, fill, fill");

        tblProgram = new JTable();
		tblProgram.getTableHeader().setReorderingAllowed(false);
		JScrollPane spProgram = new JScrollPane(tblProgram);
		spProgram.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		spProgram.setPreferredSize(new Dimension(250, 350));
		spProgram.setMinimumSize(new Dimension(250, 350));
        this.getContentPane().add(spProgram, "4, 4, 1, 5, fill, fill");

        tblInput = new JTable();
		tblInput.getTableHeader().setReorderingAllowed(false);
		JScrollPane spInput = new JScrollPane(tblInput);
		spInput.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		spInput.setPreferredSize(new Dimension(150, 200));
        this.getContentPane().add(spInput, "6, 4, fill, fill");
		
		lblDataSymbol = new JLabel("Data Symbols");
        this.getContentPane().add(lblDataSymbol, "2, 6");
		
		lblProgramLabel = new JLabel("Program Labels");
        this.getContentPane().add(lblProgramLabel, "6, 6");

        tblDataSymbol = new JTable();
		tblDataSymbol.getTableHeader().setReorderingAllowed(false);
		JScrollPane spDataSymbol = new JScrollPane(tblDataSymbol);
		spDataSymbol.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		spDataSymbol.setPreferredSize(new Dimension(150, 150));
		spDataSymbol.setMinimumSize(new Dimension(150, 150));
        this.getContentPane().add(spDataSymbol, "2, 8, fill, fill");

        tblProgramLabel = new JTable();
		tblProgramLabel.getTableHeader().setReorderingAllowed(false);
		JScrollPane spProgramLabel = new JScrollPane(tblProgramLabel);
		spProgramLabel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		spProgramLabel.setPreferredSize(new Dimension(150, 150));
		spProgramLabel.setMinimumSize(new Dimension(150, 150));
        this.getContentPane().add(spProgramLabel, "6, 8, fill, fill");
		
		JLabel lblOutput = new JLabel("Output");
        this.getContentPane().add(lblOutput, "2, 10");
		
		tpOutput = new JTextPane();
		tpOutput.setEditable(false);
		tpOutput.setPreferredSize(new Dimension(550, 200));
		tpOutput.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane spOutput = new JScrollPane(tpOutput);
        this.getContentPane().add(spOutput, "2, 12, 5, 1, fill, fill");

        this.pack();
	}

	public JMenuBar getMainMenu() {
		return mbMainMenu;
	}

    public JMenuItem getOpenItem() { return miOpen; }

	public JMenuItem getRefreshItem() {
		return miRefresh;
	}

	public JMenuItem getExportItem() {
		return miExport;
	}

    public JMenuItem getSaveOutputItem() {
        return miSaveOutput;
    }

    public JMenuItem getExitItem() {
        return miExit;
    }

	public JMenuItem getRunItem() {
		return miRun;
	}

	public JMenuItem getRunDebugItem() {
		return miRunDebug;
	}

    public JMenuItem getStepItem() {
        return miStep;
    }

    public JMenuItem getResetItem() {
        return miReset;
    }

    public JMenuItem getHelpItem() {
        return miHelp;
    }

    public JMenuItem getAboutItem() {
        return miAbout;
    }

    public JTable getDataTable() {
		return tblData;
	}

	public JTable getProgramTable() {return tblProgram;}

	public JTable getInputTable() {
		return tblInput;
	}

	public JTable getDataSymbolTable() {
		return tblDataSymbol;
	}

	public JTable getProgramLabelTable() {
		return tblProgramLabel;
	}

	public JTextPane getOutputPane() {
		return tpOutput;
	}

    public void setLabelsEnabled(boolean enabled) {
        lblDataSymbol.setEnabled(enabled);
		tblDataSymbol.setEnabled(enabled);
        lblProgramLabel.setEnabled(enabled);
		tblProgramLabel.setEnabled(enabled);
    }

}
