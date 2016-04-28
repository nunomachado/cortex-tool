//Copyright (C) 2006 United States Government as represented by the
//Administrator of the National Aeronautics and Space Administration
//(NASA).  All Rights Reserved.

//This software is distributed under the NASA Open Source Agreement
//(NOSA), version 1.3.  The NOSA has been approved by the Open Source
//Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
//directory tree for the complete NOSA document.

//THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
//KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
//LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
//SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
//A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
//THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
//DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.

package gov.nasa.jpf.util;

import gov.nasa.jpf.Config;

import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.JTextComponent;
import javax.swing.text.Keymap;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 * this is a root class for standalone swing-based JPF
 * applications that are script driven and produce traces
 * 
 * FIX:
 *  - expandAll,collapseAll -> tree empty
 *  - terminate,launch -> JPF component map not reset
 *  - browse button -> no new file
 *   
 * IMPROVE:
 *  - add mode property file
 *  - output + trace should be visible at the same time
 *  - add slider for replay delay
 */
public class Inspector extends JFrame {
  
  final static int TAB_PROP = 0;
  final static int TAB_SCRIPT = 1;
  final static int TAB_TRACE = 2;
  final static int TAB_OUTPUT = 3;
  
  protected static Logger log; // we need Config to initialize
  
  protected static PrintStream sysOut = System.out;
  protected static PrintStream sysErr = System.err;

  protected static Color BACKGROUND_CLR = new Color(220, 220, 250);
  
  protected Config config;

  protected String application;
  protected String[] appArguments;

  protected int step;

  protected boolean propChanged;
  protected boolean scriptChanged;
  protected boolean traceChanged;
  protected int stepDelay; // do we want a delay when during trace replay?

  protected JTextField cmdTextField;
  //JTree componentTree;
  protected JTextField propTextField;
  protected JTextArea propTextPane;
  protected JTextField scriptTextField;
  protected JTextArea scriptTextPane;
  protected JTextField traceTextField;
  protected JTextArea traceTextPane;
  
  protected JButton bStep;
  
  protected JTextArea console;
  protected JTabbedPane tab;

  protected TreeModel treeModel;
  
  protected ConsoleStream cout;

  
  protected Inspector (String title, Config config, String application, String[] arguments){
    super(title);
    
    this.config = config;
    this.application = application;
    this.appArguments = arguments;
    
    addWindowListener( new WindowAdapter() {
      public void windowClosing (WindowEvent e) {
        setVisible(false);
        disposeAllFrames();

        System.exit(0);
      }
    });

    getTargets();

    Container c = getContentPane();
    c.setBackground(BACKGROUND_CLR);
    
    BoxLayout layout = new BoxLayout(c, BoxLayout.Y_AXIS);
    c.setLayout(layout);
    if (c instanceof JComponent) { // create some margin around the panels
      ((JComponent)c).setBorder(new EmptyBorder(5,10,10,10));
    }

    c.add(createCommandPanel());
    c.add(createApplicationPanel());
    c.add(createPropPanel());
    c.add(createScriptPanel());
    c.add(createTracePanel());    
    c.add( createSplitPane());    

    pack();

    String propName = config.getString("inspect.properties");
    if (propName != null) {
      propTextField.setText(propName);
      setPropContents();
    }
    
    String scriptName = config.getString("inspect.script");
    if (scriptName != null) {
      scriptTextField.setText(scriptName);
      setScriptContents();
      checkEventScript();
    }

    String traceName = config.getString("inspect.trace");
    if (traceName != null) {
      traceTextField.setText(traceName);
      setTraceContents();
    }

    stepDelay = config.getInt("inspect.step_delay",800);
    
    redirectOutput();    
  }
  
  //--- misc helpers
  protected String getAppCommand () {
    String cmd = "";
    if (application != null) {
      cmd += application;
    }
    if (appArguments != null) {
      for (String a : appArguments) {
        cmd += ' ';
        cmd += a;
      }
    }
    return cmd;
  }

  protected void parseAppCommand () {
    String cmd = cmdTextField.getText().trim();

    if (cmd.length() == 0) {
      application = null;
      appArguments = null;
    } else {
      String[] a = cmd.split(" ");
      if (a.length > 0) {
        application = a[0];
        if (a.length > 1) {
          appArguments = new String[a.length-1];
          System.arraycopy(a, 1, appArguments, 0, appArguments.length);
        }
      }
    }
  }

  
  protected void redirectOutput() {
    ConsoleStream out = new ConsoleStream(console);
    System.setOut(out);
    System.setErr(out);
  }

  protected void disposeAllFrames() {
    Frame[] frames = Frame.getFrames();
    for (Frame f : frames) {
      f.setVisible(false);
      f.dispose();
    }
  }
  
  protected boolean saveFile (String fileName, String content) {
    try {
      BufferedWriter w = new BufferedWriter( new FileWriter(fileName));
      w.write(content, 0, content.length());
      w.close();
      return true;
    } catch (Throwable t) {
      System.err.println("error writing file: " + fileName);
      return false;
    }
  }

  protected String readFile (File file) {
    int len = (int)file.length();
    StringBuilder sb = new StringBuilder(len);
    char[] buf = new char[256];
    int n;
    try {
      FileReader r = new FileReader(file);
      while ((n = r.read(buf)) >= 0) {
        sb.append(buf, 0, n);
      }
    } catch (IOException x) {
      System.err.println("error reading file: " + file.getName());
    }

    return sb.toString();
  }

  protected String chooseFile (String title, int type) {
    FileDialog chooser = new FileDialog(this, title, type);
    chooser.setVisible(true);

    String dir = chooser.getDirectory();
    String file = chooser.getFile();

    if (file != null) {
      return (dir + file);
    }

    return null;
  }

  protected boolean save (JTextField nameField, JTextComponent contentPane, String msg) {
    String fileName = nameField.getText();
    
    if (fileName.length() == 0) {
      fileName = chooseFile(msg, FileDialog.SAVE);
      if (fileName == null) {
        return false;
      }
      nameField.setText(fileName);
    }

    return saveFile( fileName, contentPane.getText());
  }

  protected void error (String msg){
    
    // avoid cascaded dialogs
    System.err.print("ERROR: ");
    System.err.println(msg);
  }

  protected void errorPopup (String msg) {
    JOptionPane.showMessageDialog(null, msg, "Error", JOptionPane.WARNING_MESSAGE);    
  }

  protected void delay (int ms) {
    if (ms > 0){
      try { Thread.sleep(ms); } catch (InterruptedException ix) {}
    }
  }

  
  //--- general UI utilities
  protected void setTextFieldSizeConstraints (JTextField tf) {
    Dimension d = tf.getPreferredSize();
    tf.setMinimumSize( new Dimension(50, d.height));
    tf.setMaximumSize( new Dimension(10000, d.height));
  }

  protected void setButtonSizeConstraints (JButton btn) {
    Dimension d = btn.getPreferredSize();
    btn.setMinimumSize(d);
    btn.setMaximumSize(d);
  }

  protected void setSizeConstraints (JComponent c, int prefLines, int minLines) {
    Font font = c.getFont();
    FontMetrics fm = c.getFontMetrics(font);
    int fontHeight = fm.getHeight();
    int prefHeight = fontHeight * prefLines;
    int minHeight = fontHeight * minLines;

    c.setPreferredSize( new Dimension(300, prefHeight));
    c.setMinimumSize( new Dimension(100, minHeight));
  }
  
  protected void setLabelSizeConstraints (JLabel label) {
    Dimension d = label.getPreferredSize();
    Dimension fixed = new Dimension(90, d.height);
    label.setPreferredSize( fixed);
    label.setMinimumSize( fixed);
    label.setMaximumSize( fixed);    
  }

  protected void extendTextKeyMap (JTextComponent c, KeyStroke k, AbstractAction a) {
    Keymap km = c.getKeymap();
    km.addActionForKeyStroke( k, a);
  }

  //--- subclass interface
  // (we don't do this abstract so that we can instantiate and test)
  
  protected void loadApplication() {
    sysOut.println("loadApplication");    
  }

  protected void launchApplication() {
    sysOut.println("launchApplication");    
  }

  protected void checkApplication() {
    sysOut.println("checkApplication");    
  }
  
  protected void terminateApplication() {
    sysOut.println("terminateApplication");    
  }
  
  protected void replayTrace() {
    sysOut.println("replayTrace");    
  }
  
  protected void replayStep() {
    sysOut.println("replayStep");    
  }
  
  protected void checkEventScript() {
    sysOut.println("setEventGeneratorFactory");
  }
  
  protected void getTargets () {
    sysOut.println("getTargets");    
  }
  
  protected JComponent createTargetPanel() {
    // just a dummy
    return new JTextArea();
  }
  
  //--- common panel creation
  
  protected JComponent createCommandPanel () {

    Box box = Box.createHorizontalBox();
    //box.setBorder( new TitledBorder("Commands"));

    addSpecificCommands(box);
    
    JButton check = new JButton("Check");
    check.addActionListener( new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        checkApplication();
      }
    });
    setButtonSizeConstraints(check);
    box.add(check);    
    
    JButton bExit = new JButton("Exit");
    bExit.addActionListener( new ActionListener() {
      public void actionPerformed (ActionEvent e) {      
        disposeAllFrames();
        delay(500);
        System.exit(0);
      }
    });
    setButtonSizeConstraints(bExit);
    box.add(bExit);

    return box;
  }

  
  protected JComponent createApplicationPanel () {
    Box box = Box.createHorizontalBox();
    //box.setBorder(new TitledBorder("Application"));

    JLabel label = new JLabel("Application ", JLabel.RIGHT);
    setLabelSizeConstraints(label);
    box.add( label);

    cmdTextField = new JTextField(getAppCommand());
    cmdTextField.addActionListener( new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        loadApplication();
      }
    });
    setTextFieldSizeConstraints(cmdTextField);
    box.add(cmdTextField);

    JButton browse = new JButton("...");
    setButtonSizeConstraints(browse);
    box.add(browse);

    return box;
  }

  protected JComponent createPropPanel () {
    Box box = Box.createHorizontalBox();

    JLabel label = new JLabel("Properties ", JLabel.RIGHT);
    setLabelSizeConstraints(label);
    box.add( label);

    propTextField = new JTextField();
    propTextField.addActionListener( new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        setPropContents();
      }
    });
    setTextFieldSizeConstraints(propTextField);
    box.add(propTextField);

    JButton browse = new JButton("...");
    browse.addActionListener( new ActionListener() {
      public void actionPerformed (ActionEvent e) {      
        selectProp();
      }
    });
    setButtonSizeConstraints(browse);
    box.add(browse);

    return box;
  }

  
  protected JComponent createScriptPanel () {
    Box box = Box.createHorizontalBox();
    //box.setBorder(new TitledBorder("Script"));

    JLabel label = new JLabel("Script ", JLabel.RIGHT);
    setLabelSizeConstraints(label);
    box.add( label);

    scriptTextField = new JTextField();
    scriptTextField.addActionListener( new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        setScriptContents();
        checkEventScript();
      }
    });
    setTextFieldSizeConstraints(scriptTextField);
    box.add(scriptTextField);

    JButton browse = new JButton("...");
    browse.addActionListener( new ActionListener() {
      public void actionPerformed (ActionEvent e) {      
        selectScript();
        checkEventScript();
      }
    });
    setButtonSizeConstraints(browse);
    box.add(browse);

    return box;
  }

  protected JComponent createTracePanel () {
    Box box = Box.createHorizontalBox();
    //box.setBorder(new TitledBorder("Trace"));

    JLabel label = new JLabel("Trace ", JLabel.RIGHT);
    setLabelSizeConstraints(label);
    box.add( label);

    traceTextField = new JTextField();
    traceTextField.addActionListener( new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        setTraceContents();
      }
    });
    setTextFieldSizeConstraints(traceTextField);
    box.add(traceTextField);

    JButton browse = new JButton("...");
    browse.addActionListener( new ActionListener() {
      public void actionPerformed (ActionEvent e) {      
        selectTrace();
      }
    });
    setButtonSizeConstraints(browse);
    box.add(browse);

    return box;
  }

  
  protected JComponent createTextPanel() {
    JScrollPane scroll;

    tab = new JTabbedPane();
    
    Font def = tab.getFont();
    Font mono = new Font("Monospaced", Font.PLAIN, def.getSize());

    //--- properties
    propTextPane = new JTextArea();
    propTextPane.setFont(mono);
    propTextPane.setLineWrap(false);
    propTextPane.getDocument().addDocumentListener( new DocumentListener() {
      public void changedUpdate (DocumentEvent e) {}
      public void insertUpdate (DocumentEvent e) {
        markPropChanged();
      }
      public void removeUpdate (DocumentEvent e) {
        markPropChanged();
      }
    });
   
    AbstractAction propSaver = new AbstractAction(){
      public void actionPerformed (ActionEvent e) {
        if (save(propTextField, propTextPane, "Save Properties File")) {
          markPropSaved();
        }
      }
    };
    extendTextKeyMap( propTextPane, KeyStroke.getKeyStroke('S', KeyEvent.META_MASK, true), propSaver);
    createTextPopup( propTextPane, propSaver);
    scroll = new JScrollPane(propTextPane);
    tab.addTab( "Properties", scroll);
    
    //--- action script
    scriptTextPane = new JTextArea();
    scriptTextPane.setFont(mono);
    scriptTextPane.setLineWrap(false);
    scriptTextPane.getDocument().addDocumentListener( new DocumentListener() {
      public void changedUpdate (DocumentEvent e) {}
      public void insertUpdate (DocumentEvent e) {
        markScriptChanged();
      }
      public void removeUpdate (DocumentEvent e) {
        markScriptChanged();
      }
    });
   
    AbstractAction scriptSaver = new AbstractAction(){
      public void actionPerformed (ActionEvent e) {
        if (save(scriptTextField, scriptTextPane, "Save Script File")) {
          markScriptSaved();
          checkEventScript();
        }
      }
    };
    extendTextKeyMap( scriptTextPane, KeyStroke.getKeyStroke('S', KeyEvent.META_MASK, true), scriptSaver);
    createTextPopup( scriptTextPane, scriptSaver);
    scroll = new JScrollPane(scriptTextPane);
    tab.addTab( "Script", scroll);

    //--- trace
    traceTextPane = new JTextArea();
    traceTextPane.setFont(mono);
    traceTextPane.setLineWrap(false);
    traceTextPane.getDocument().addDocumentListener( new DocumentListener() {
      public void changedUpdate (DocumentEvent e) {}
      public void insertUpdate (DocumentEvent e) {
        markTraceChanged();
      }
      public void removeUpdate (DocumentEvent e) {
        markTraceChanged();
      }
    });    
    AbstractAction traceSaver = new AbstractAction(){
      public void actionPerformed (ActionEvent e) {
        if (save(traceTextField, traceTextPane, "Save Trace File")) {
          markTraceSaved();
        }
      }
    };
    // <2do> that for some strange reason overwrites the keymap for the scriptTextPane
    //extendTextKeyMap( traceTextPane, KeyStroke.getKeyStroke('S', KeyEvent.META_MASK, true), traceSaver);    
    createTextPopup( traceTextPane, traceSaver);
    scroll = new JScrollPane(traceTextPane);
    tab.addTab("Trace", scroll);

    //--- output
    console = new JTextArea();
    console.setFont(mono);
    console.setEditable(false);
    console.setLineWrap(false);
    scroll = new JScrollPane(console);
    tab.addTab("Output", scroll);

    addSpecificTextPanes(tab);
    
    setSizeConstraints(tab, 20, 10);

    return tab;
  }

  protected void addSpecificTextPanes (JTabbedPane tab) {
    // can be overriden by subclass
  }
  
  protected void addSpecificCommands (Box box) {
    // can be overriden by subclass    
  }
  
  void createTextPopup (final JTextComponent target, ActionListener saver) {
    final JPopupMenu popUp = new JPopupMenu();

    JMenuItem item = new JMenuItem( "cut");
    item.addActionListener( new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        target.cut();
      }
    });
    popUp.add(item);

    item = new JMenuItem( "copy");
    item.addActionListener( new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        target.copy();
      }
    });
    popUp.add(item);

    item = new JMenuItem( "paste");
    item.addActionListener( new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        target.paste();
      }
    });
    popUp.add(item);

    //popUp.addSeparator();    

    item = new JMenuItem( "save");
    item.addActionListener( saver);
    popUp.add(item);

    popUp.setCursor(Cursor.getDefaultCursor()); // that should have taken care of it..
    popUp.setBorder(new BevelBorder(BevelBorder.RAISED));

    // you've got to be kidding me - that's STILL an issue?
    // MouseEvents are still passed down into the popup owner??
    popUp.addPopupMenuListener( new PopupMenuListener() {
      Cursor cur;
      public void popupMenuCanceled (PopupMenuEvent e) {        
        target.setCursor(cur);
      }
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        target.setCursor(cur);
      }
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        cur = target.getCursor();
        target.setCursor(Cursor.getDefaultCursor());
      }
    });

    JPopupMenu.setDefaultLightWeightPopupEnabled(false);
    
    target.setComponentPopupMenu(popUp);
  }

  
  protected JComponent createSplitPane() {
    Box box = Box.createHorizontalBox();

    JComponent tgt = createTargetPanel();
    JComponent txt = createTextPanel();

    JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tgt, txt);
    split.setOpaque(false); // default is true, but we want to inherit the background
    split.setBorder(null);
    split.setOneTouchExpandable(true);
    box.add(split);

    return box;
  }

  //--- UI actions

  protected void expandAll(JTree tree, boolean expand) {
    TreeNode root = (TreeNode)tree.getModel().getRoot();
    expandAll(tree, new TreePath(root), expand);
  }

  protected void expandAll(JTree tree, TreePath parent, boolean expand) {
    TreeNode node = (TreeNode)parent.getLastPathComponent();
    if (node.getChildCount() >= 0) {
      for (Enumeration<?> e=node.children(); e.hasMoreElements(); ) {
        TreeNode n = (TreeNode)e.nextElement();
        TreePath path = parent.pathByAddingChild(n);
        expandAll(tree, path, expand);
      }
    }

    if (expand) {
      tree.expandPath(parent);
    } else {
      tree.collapsePath(parent);
    }
  }

  protected void selectProp() {
    String sel = chooseFile("Choose Properties File", FileDialog.LOAD);
    if (sel != null) {
      propTextField.setText(sel);
      setPropContents();
    }    
  }
  
  protected void selectScript() {
    String sel = chooseFile("Choose Script File", FileDialog.LOAD);
    if (sel != null) {
      scriptTextField.setText(sel);
      setScriptContents();
    }    
  }

  protected void selectTrace() {
    String sel = chooseFile("Choose Trace File", FileDialog.LOAD);
    if (sel != null) {
      traceTextField.setText(sel);
      setTraceContents();
    }
  }

  protected void setTraceContents() {
    String fileName = traceTextField.getText();
    File file = new File(fileName);
    if (file.exists()) {
      String contents = readFile(file);
      traceTextPane.setText(contents);

      traceChanged = false;
      tab.setTitleAt(TAB_TRACE, "Trace");
      tab.setSelectedIndex(TAB_TRACE);
    }
  }

  protected void setPropContents() {
    String fileName = propTextField.getText();
    File file = new File(fileName);
    if (file.exists()) {
      String contents = readFile(file);
      propTextPane.setText(contents);

      propChanged = false;
      tab.setTitleAt(TAB_PROP, "Properties");
      tab.setSelectedIndex(TAB_PROP);
    }    
  }
  
  protected void setScriptContents() {
    String fileName = scriptTextField.getText();
    
    if (fileName.length() > 0) {
      File file = new File(fileName);
      if (file.exists()) {
        String contents = readFile(file);
        scriptTextPane.setText(contents);
      }
    } else {
      scriptTextPane.setText(null);
    }

    scriptChanged = false;
    tab.setTitleAt(TAB_SCRIPT, "Script");
    tab.setSelectedIndex(TAB_SCRIPT);
  }

  public void updateTraceContents () {
    String traceName = traceTextField.getText();    
    if (traceName.length() > 0) {
      File file = new File(traceName);
      if (file.exists()) {
        String contents = readFile(file);
        traceTextPane.setText(contents);
      }
    }
  }
  
  protected void showResetOutput() {
    console.setText(null);
    //tab.setSelectedIndex(2);
  }
  
  protected void markPropChanged () {
    if (!propChanged) {
      propChanged = true;
      tab.setTitleAt(TAB_PROP, "* Properties");
    }
  }

  protected void markPropSaved () {
    if (propChanged) {
      propChanged = false;
      tab.setTitleAt(TAB_PROP, "Properties");
    }
  }
  
  protected void markScriptChanged () {
    if (!scriptChanged) {
      scriptChanged = true;
      tab.setTitleAt(TAB_SCRIPT, "* Script");
    }
  }

  protected void markScriptSaved () {
    if (scriptChanged) {
      scriptChanged = false;
      tab.setTitleAt(TAB_SCRIPT, "Script");
    }
  }

  protected void markTraceChanged () {
    if (!traceChanged) {
      traceChanged = true;
      tab.setTitleAt(TAB_TRACE, "* Trace");
    }
  }
  
  protected void markTraceSaved () {
    if (traceChanged) {
      traceChanged = false;
      tab.setTitleAt(TAB_TRACE, "Trace");
    }
  }

}
