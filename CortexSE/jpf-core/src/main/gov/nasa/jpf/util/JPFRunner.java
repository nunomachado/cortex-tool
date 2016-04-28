package gov.nasa.jpf.util;

import gov.nasa.jpf.Error;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.ListenerAdapter;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.SystemState;
import gov.nasa.jpf.search.Search;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.SoftBevelBorder;

public class JPFRunner extends JFrame {
  Inspector inspector;
  JPF jpf;
  JPFAdapter adapter;
  boolean stop;
  boolean foundErrors;
  long totalActions;
  
  String application;
  String script;
    
  JLabel lStatus;
  JLabel lNew;
  JLabel lVisited;
  JLabel lInsn;
  JLabel lBack;
  JLabel lResult;
  JLabel lTime;
    
  class JPFAdapter extends ListenerAdapter {
    int nNew;
    int nVisited;
    int nBacktrack;
    int nInsn;
    long tStart;
    
    void checkStop (Search search) {
      if (stop) {
        search.terminate();
        lStatus.setText("canceled..");
      }
    }
  
    void setStartTime() {
      tStart = System.currentTimeMillis();
    }
    
    void updateTime () {
      long t = System.currentTimeMillis();
      int elapsed = (int)(t - tStart) / 1000; // in sec
      int s = elapsed % 60;
      int h = elapsed / 3600;
      int m = (elapsed - h * 3600) / 60;
      
      StringBuilder sb = new StringBuilder(10);
      if (h < 10){
        sb.append('0');
      }
      sb.append(h);
      sb.append(':');
      if (m < 10) {
        sb.append('0');
      }
      sb.append(m);
      sb.append(':');
      if (s < 10) {
        sb.append('0');
      }
      sb.append(s);
      lTime.setText(sb.toString());
    }
    
    public void stateAdvanced (Search search) {
      if (search.isNewState()) {
        nNew++;
        lNew.setText(Integer.toString(nNew));
      } else {
        nVisited++;
        lVisited.setText(Integer.toString(nVisited));
      }
      
      JVM vm = search.getVM();
      SystemState ss = vm.getSystemState();
      
      nInsn += ss.getTrail().getStepCount();
      lInsn.setText(Integer.toString(nInsn));
            
      checkStop(search);
      updateTime();
    }
    
    public void stateBacktracked (Search search) {
      nBacktrack++;
      lBack.setText(Integer.toString(nBacktrack));

      checkStop(search);
      updateTime();
    }
    
    public void stateRestored (Search search) {
      checkStop(search);
      updateTime();
    }
    
    public void searchStarted (Search search) {
      lStatus.setText("running..");
      updateTime();
    }
    
    public void searchFinished (Search search) {
      if (!foundErrors) {
        lStatus.setText("finished");
        lResult.setText("no defect found");
      }
      updateTime();
      
      inspector.updateTraceContents();
    }
    
    public void propertyViolated (Search search) {
      List<Error> errors = search.getErrors();
      StringBuilder sb = new StringBuilder();
      int i=0;
      for (gov.nasa.jpf.Error e : errors) {
        if (i++ > 0) sb.append(',');
        sb.append(e.getDescription());
      }
      lResult.setForeground(Color.RED);
      lResult.setText(sb.toString());
      lStatus.setText("finished");
      foundErrors = true;
    }

    public void searchConstraintHit (Search search) {
      updateTime();
    }

    public void stateProcessed (Search search) {
      updateTime();
    }
  }
  
  public JPFRunner (Inspector inspector, JPF jpf, String application, String script) {
    super("Checking: " + application);
    
    this.inspector = inspector;
    this.jpf = jpf;
    this.application = application;
    this.script = script;
    
    Container c = getContentPane();
    BoxLayout layout = new BoxLayout(c, BoxLayout.Y_AXIS);
    c.setLayout(layout);

    if (c instanceof JComponent) { // create some margin around the panels
      ((JComponent)c).setBorder(new EmptyBorder(5,10,10,10));
    }
    
    c.add(createFieldsPanel());
    c.add(Box.createVerticalStrut(10));
    c.add(Box.createVerticalStrut(10));
    c.add(createCommandPanel());

    adapter = new JPFAdapter();
    jpf.addSearchListener(adapter);

    c.setBackground(inspector.getContentPane().getBackground());
    
    setLocation( 300, 400);
    pack();
    setResizable(false);
    setVisible(true);
  }
  
  JComponent fixedWidth (JComponent c, int w) {
    Dimension d = c.getPreferredSize();
    d.width = w;
    d.height += 5;
    c.setPreferredSize(d);
    c.setMinimumSize(d);
    c.setMaximumSize(d);
    return c;
  }
  
  JComponent bordered (JComponent c) {
    Border b = new SoftBevelBorder(BevelBorder.LOWERED);
    c.setBorder(b);
    c.setOpaque(true);
    c.setBackground(new Color(240,240,240));
    c.setForeground(Color.BLUE);
    return c;
  }
  
  static final int L_WIDTH=150;
  static final int F_WIDTH=200;
  
  JComponent createFieldsPanel() {
    Box box = Box.createHorizontalBox();
    
    Box labels = Box.createVerticalBox();
    labels.add(fixedWidth(new JLabel("application", JLabel.RIGHT), L_WIDTH));
    labels.add(fixedWidth(new JLabel("script", JLabel.RIGHT), L_WIDTH));
    labels.add(fixedWidth(new JLabel("status", JLabel.RIGHT), L_WIDTH));
    labels.add(fixedWidth(new JLabel("result", JLabel.RIGHT), L_WIDTH));
    labels.add(Box.createVerticalStrut(10));
    labels.add(fixedWidth(new JLabel("new states", JLabel.RIGHT), L_WIDTH));
    labels.add(fixedWidth(new JLabel("visited states", JLabel.RIGHT), L_WIDTH));
    labels.add(fixedWidth(new JLabel("backtracked states", JLabel.RIGHT), L_WIDTH));
    labels.add(fixedWidth(new JLabel("instructions", JLabel.RIGHT), L_WIDTH));
    labels.add(fixedWidth(new JLabel("time", JLabel.RIGHT), L_WIDTH));
    box.add(labels);
    
    box.add(Box.createHorizontalStrut(10));
    
    Box fields = Box.createVerticalBox();
    fields.add(bordered(fixedWidth(new JLabel(application), F_WIDTH)));
    fields.add(bordered(fixedWidth(new JLabel(script), F_WIDTH)));
    fields.add(bordered(fixedWidth((lStatus = new JLabel("?")),F_WIDTH)));
    fields.add(bordered(fixedWidth((lResult = new JLabel("?")),F_WIDTH)));
    fields.add(Box.createVerticalStrut(10));
    fields.add(bordered(fixedWidth((lNew = new JLabel("0")),F_WIDTH)));
    fields.add(bordered(fixedWidth((lVisited = new JLabel("0")),F_WIDTH)));
    fields.add(bordered(fixedWidth((lBack = new JLabel("0")),F_WIDTH)));
    fields.add(bordered(fixedWidth((lInsn = new JLabel("0")),F_WIDTH)));
    fields.add(bordered(fixedWidth((lTime = new JLabel("00:00:00")),F_WIDTH)));
    box.add(fields);
    
    box.add(Box.createHorizontalStrut(10));
        
    return box;
  }
  
  
  JComponent createCommandPanel() {
    Box box = Box.createHorizontalBox();
    
    JButton bStop = new JButton("Stop");
    bStop.addActionListener( new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        stop = true;
        lStatus.setText("stopping..");
      }
    });
    box.add(bStop);
    
    JButton bExit = new JButton("Exit");
    bExit.addActionListener( new ActionListener() {
      public void actionPerformed (ActionEvent e) {
        stop = true;
        setVisible(false);
        dispose();
      }
    });
    box.add(bExit);
    
    return box;
  }
  
  
  public void run() {
    Thread t = new Thread(jpf);

    t.start();
    adapter.setStartTime();
  }

}
