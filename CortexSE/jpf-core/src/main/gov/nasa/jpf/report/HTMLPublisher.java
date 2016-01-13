//
// Copyright (C) 2006 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
// (NASA).  All Rights Reserved.
//
// This software is distributed under the NASA Open Source Agreement
// (NOSA), version 1.3.  The NOSA has been approved by the Open Source
// Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
// directory tree for the complete NOSA document.
//
// THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
// KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
// LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
// SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
// A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
// THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
// DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//
package gov.nasa.jpf.report;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.Error;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.jvm.ClassInfo;
import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.FieldInfo;
import gov.nasa.jpf.jvm.JVM;
import gov.nasa.jpf.jvm.LocalVarInfo;
import gov.nasa.jpf.jvm.MethodInfo;
import gov.nasa.jpf.jvm.Path;
import gov.nasa.jpf.jvm.ReturnInstruction;
import gov.nasa.jpf.jvm.StackFrame;
import gov.nasa.jpf.jvm.Step;
import gov.nasa.jpf.jvm.ThreadInfo;
import gov.nasa.jpf.jvm.Transition;
import gov.nasa.jpf.jvm.bytecode.Instruction;
import gov.nasa.jpf.util.ObjectList;
import gov.nasa.jpf.util.RepositoryEntry;
import gov.nasa.jpf.util.Source;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/*
 * Outputs the report in HTML format.  Currently only Firefox 3.0.7 through 3.0.10 have been tested.
 */

// TODO - Add support for Internet Explorer.
// TODO BUG - Exiting static void [VM].[clinit]<clinit>() doesn't always return the tree back to the previous level.
// TODO BUG - Trace - The unexecuted code between initializing member variables and the construction should not be written.

public class HTMLPublisher extends Publisher {

  private final static byte s_bullet[] = {
    (byte) 0x47, (byte) 0x49, (byte) 0x46, (byte) 0x38, (byte) 0x39, (byte) 0x61, (byte) 0x13, (byte) 0x00,
    (byte) 0x13, (byte) 0x00, (byte) 0x80, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
    (byte) 0xEE, (byte) 0xEE, (byte) 0xEE, (byte) 0x21, (byte) 0xF9, (byte) 0x04, (byte) 0x01, (byte) 0x00,
    (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x2C, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
    (byte) 0x13, (byte) 0x00, (byte) 0x13, (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x15, (byte) 0x8C,
    (byte) 0x8F, (byte) 0xA9, (byte) 0xCB, (byte) 0xED, (byte) 0x0F, (byte) 0xA3, (byte) 0x9C, (byte) 0xB4,
    (byte) 0x2E, (byte) 0x80, (byte) 0x29, (byte) 0x06, (byte) 0x3A, (byte) 0xDB, (byte) 0x0F, (byte) 0x86,
    (byte) 0xE2, (byte) 0x48, (byte) 0x42, (byte) 0x05, (byte) 0x00, (byte) 0x3B
  };

  private final static byte s_plus[] = {
    (byte) 0x47, (byte) 0x49, (byte) 0x46, (byte) 0x38, (byte) 0x39, (byte) 0x61, (byte) 0x13, (byte) 0x00,
    (byte) 0x13, (byte) 0x00, (byte) 0x91, (byte) 0x03, (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
    (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xEE, (byte) 0xEE,
    (byte) 0xEE, (byte) 0x21, (byte) 0xF9, (byte) 0x04, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x03,
    (byte) 0x00, (byte) 0x2C, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x13, (byte) 0x00,
    (byte) 0x13, (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x2A, (byte) 0x9C, (byte) 0x8F, (byte) 0xA9,
    (byte) 0xCB, (byte) 0xED, (byte) 0x0F, (byte) 0x63, (byte) 0x0B, (byte) 0xB4, (byte) 0x56, (byte) 0x17,
    (byte) 0x80, (byte) 0xDE, (byte) 0x20, (byte) 0xE0, (byte) 0x2D, (byte) 0x6C, (byte) 0xDE, (byte) 0x04,
    (byte) 0x8A, (byte) 0x9F, (byte) 0x80, (byte) 0xA2, (byte) 0xDD, (byte) 0xA7, (byte) 0x85, (byte) 0xDA,
    (byte) 0xC8, (byte) 0x64, (byte) 0xAD, (byte) 0x49, (byte) 0x72, (byte) 0x2F, (byte) 0x66, (byte) 0x59,
    (byte) 0xD2, (byte) 0xCE, (byte) 0xF7, (byte) 0xFE, (byte) 0x3F, (byte) 0x28, (byte) 0x00, (byte) 0x00,
    (byte) 0x3B
  };

  private final static byte s_minus[] = {
    (byte) 0x47, (byte) 0x49, (byte) 0x46, (byte) 0x38, (byte) 0x39, (byte) 0x61, (byte) 0x13, (byte) 0x00,
    (byte) 0x13, (byte) 0x00, (byte) 0x91, (byte) 0x03, (byte) 0x00, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
    (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xEE, (byte) 0xEE,
    (byte) 0xEE, (byte) 0x21, (byte) 0xF9, (byte) 0x04, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x03,
    (byte) 0x00, (byte) 0x2C, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x13, (byte) 0x00,
    (byte) 0x13, (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x28, (byte) 0x9C, (byte) 0x8F, (byte) 0xA9,
    (byte) 0xCB, (byte) 0xED, (byte) 0x0F, (byte) 0x63, (byte) 0x0B, (byte) 0xB4, (byte) 0x56, (byte) 0x17,
    (byte) 0x80, (byte) 0xDE, (byte) 0x20, (byte) 0x60, (byte) 0xBE, (byte) 0x79, (byte) 0x13, (byte) 0xA8,
    (byte) 0x89, (byte) 0x4C, (byte) 0x26, (byte) 0xA4, (byte) 0x69, (byte) 0xF7, (byte) 0x91, (byte) 0xE6,
    (byte) 0x92, (byte) 0xB9, (byte) 0x2D, (byte) 0xF8, (byte) 0x2A, (byte) 0xD6, (byte) 0x2D, (byte) 0xE5,
    (byte) 0xFA, (byte) 0xCE, (byte) 0xF7, (byte) 0x43, (byte) 0x01, (byte) 0x00, (byte) 0x3B
  };

  private final static String XML_VERSION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
  private final static String DOCTYPE     = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml11.dtd\">";
  private final static String HTML        = "<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:svg=\"http://www.w3.org/2000/svg\" xml:lang=\"en\" lang=\"en\">";
  private final static Logger s_logger    = JPF.getLogger("gov.nasa.jpf.report.HTMLPublisher");

  private final String m_pathName;
  private final HashMap<String, Pair<BitSet, BitSet>> m_sourceCoverage = new HashMap<String, Pair<BitSet, BitSet>>();
  private final ArrayList<TabInfo> m_tabs = new ArrayList<TabInfo>();
  private int m_noSource;
  private int m_treeNodeMethodId;
  private int m_treeNodeLineId;

  private static class Pair<T1, T2> {

    private T1 m_item1;
    private T2 m_item2;

    public Pair(T1 item1, T2 item2) {
      m_item1 = item1;
      m_item2 = item2;
    }

    public T1 getItem1() {
      return (m_item1);
    }

    public void setItem1(T1 item1) {
      m_item1 = item1;
    }

    public T2 getItem2() {
      return (m_item2);
    }

    public void setItem2(T2 item2) {
      m_item2 = item2;
    }
  }

  private static class TabInfo
  {
     public final String title;
     public final String fileName;

     private TabInfo(String title, String fileName)
     {
        this.title    = title;
        this.fileName = fileName;
     }
  }

  public HTMLPublisher(Config config, Reporter reporter) {
    super(config, reporter);

    m_pathName = getPathName();
  }

  public String getName() {
    return ("html");
  }

  private String getPathName() {
    String pathName;

    pathName = conf.getString("report.html.path");

    if (pathName == null) {
      pathName = ".";
    }

    if (!pathName.endsWith(File.separator)) {
      pathName += File.separator;
    }

    new File(pathName).mkdirs();

    return (pathName);
  }

  public void publishStart() {
    super.publishStart();

    writeIndex();
  }

  private void writeIndex() {
    PrintWriter index;

    try {
      index = new PrintWriter(m_pathName + "index.html");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return;
    }

    writeDocStart(index);
    index.println("   <head>");
    writeTitle(index, "");
    index.println("   </head>");
    index.println("   <frameset rows=\"20,*\">");
    index.println("      <frame frameborder=\"0\" scrolling=\"no\"  name=\"header\" src=\"header.html\" />");
    index.println("      <frame frameborder=\"0\" scrolling=\"yes\" name=\"body\"   src=\"main.html\" />");
    index.println("   </frameset>");
    index.println("   <noframes>");
    index.println("      Please use a frame enabled browser.");
    index.println("   </noframes>");
    index.println("</html>");

    index.flush();
    index.close();
  }

  private void writeHeader(int internalTabs) {
    PrintWriter header;
    int i;

    try {
      header = new PrintWriter(m_pathName + "header.html");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return;
    }

    writeDocStart(header);
    header.println("   <head>");
    header.println("      <base target=\"body\"/>");
    header.println("      <style type=\"text/css\">");
    header.println("         a          { color: #FFFFFF; background-color: #0060C0; text-decoration: none; padding-left: 10px; padding-right: 10px; margin-left: 1px; -moz-border-radius-topleft: 10px; -moz-border-radius-topright: 10px; }");
    header.println("         a.selected { color: #0060C0; background-color: #FFFFFF; }");
    header.println("      </style>");
    header.println("      <script type=\"text/javascript\">");
    header.println("         //<![CDATA[");
    header.println("         function doClick(anchor)");
    header.println("         {");
    header.println("            var i, anchors;");
    header.println();
    header.println("            anchor.blur();");
    header.println("            parent.frames[1].location = anchor.rel;");
    header.println();
    header.println("            anchors = document.getElementsByTagName(\"a\");");
    header.println();
    header.println("            for (i = anchors.length - 1; i >= 0; i--)");
    header.println("               anchors[i].className = \"\";");
    header.println();
    header.println("            anchor.className = \"selected\";");
    header.println();
    header.println("            return false;");
    header.println("         }");
    header.println();
    header.println("         function doLoad()");
    header.println("         {");
    header.println("            var anchors;");
    header.println();
    header.println("            anchors = document.getElementsByTagName(\"a\");");
    header.println("            doClick(anchors[0]);");
    header.println("         }");
    header.println("         //]]>");
    header.println("      </script>");
    header.println("   </head>");
    header.print("   <body style=\"background-color: #B0D0FF; margin: 0px;\" onload=\"doLoad();\">");

    for (i = internalTabs; i < m_tabs.size(); i++)
      writeHeaderTab(header, m_tabs.get(i));

    for (i = 0; i < internalTabs; i++)
      writeHeaderTab(header, m_tabs.get(i));

    header.println("</body>");
    header.println("</html>");

    header.flush();
    header.close();
  }

  private void writeHeaderTab(PrintWriter output, TabInfo tabInfo)
  {
     output.print("<a rel=\"");
     output.print(tabInfo.fileName);
     output.print("\"           onclick=\"doClick(this);\"><b>");
     output.print(tabInfo.title);
     output.print("</b></a>");
  }

  public void publishFinished() {
    int internalTabs;

    super.publishFinished();

    internalTabs = m_tabs.size();

    // Hmm, this bypasses topic configuration
    writeMain();
    writeSources();
    writeArray("bullet.gif", s_bullet);
    writeArray("plus.gif", s_plus);
    writeArray("minus.gif", s_minus);
    writeError();
    writeOutput();
    writeSnapshot();
    writeTrace();
    writeLoadedClasses();
    writeJPFConfig();
    writeJVMConfig();

    writeHeader(internalTabs);            // Must be last so that all tabs will be included.

      // <todo> Publishers are not supposed to print anything outside their realm
    System.out.println("Report file:  " + m_pathName + "index.html");
  }

  // <todo> not good - this is the standard PW access method for PublisherExtensions
  public PrintWriter getOut() {
    return (new PrintWriter(new Writer() {
      public void close() {}
      public void flush() {}
      public void write(char cbuf[], int off, int len) {}
    }));
  }

   public PrintWriter getOut(String title)
   {
      PrintWriter output;
      String fileName;

      fileName  = title.replaceAll("[^A-Za-z0-9]", " ");
      fileName  = fileName.replaceAll(" +", "_");
      fileName += ".html";

      try
      {
         output = new PrintWriter(m_pathName + fileName)
         {
            private boolean m_closed = false;

            public void close()
            {
               if (!m_closed)
               {
                  m_closed = true;
                  println("   </body>");
                  println("</html>");
               }

               flush();
               super.close();
            }
         };
      }
      catch (FileNotFoundException e)
      {
         e.printStackTrace();
         return(new PrintWriter(System.out));
      }

      writeDocStart(output);
      output.println("   <head>");
      writeTitle(output, title);
      output.println("   </head>");
      output.println("   <body>");

      m_tabs.add(new TabInfo(title, fileName));

      return(output);
   }

  private void writeJVMConfig() {
    PrintWriter output;

    output = getOut("JVM Config");
    writeJVMProperties(output);
    output.flush();
    output.close();
  }

  private void writeJVMProperties(PrintWriter output) {
    String keys[];

    output.println("      <p><b>Properties</b></p>");
    output.println("      <table cellpadding=\"5\" style=\"border-collapse: collapse;\">");
    output.println("         <tr bgcolor=\"#0080FF\" border=\"1\" rules=\"rows\"><th>Key</th><th>Value</th></tr>");

    keys = new String[System.getProperties().size()];
    keys = System.getProperties().keySet().toArray(keys);
    Arrays.sort(keys);

    for (String key : keys) {
      output.print("         <tr><td>");
      output.print(escape(key));
      output.print(":&#160;&#160;</td><td>");
      output.print(escape(System.getProperty(key)));
      output.println("</td></tr>");
    }

    output.println("      </table>");
  }

  private void writeJPFConfig() {
    PrintWriter output;

    output = getOut("JPF Config");

    writeJPFFiles(output);
    //writeJPFArguments(output);
    writeJPFProperties(output);

    output.flush();
    output.close();
  }

  private void writeJPFFiles(PrintWriter output) {
    output.println("      <p><b>Files</b></p>");
    output.println("      <table cellpadding=\"5\">");

    for (Object src: conf.getSources()){
      output.print("         <tr><td>Source:&#160;&#160;</td><td>");
      output.print(conf.getSourceName(src));
      output.println("</td></tr>");
    }

    output.println("      </table>");
    output.println("      <br/>");
    output.println("      <br/>");
  }

  private void writeJPFProperties(PrintWriter output) {
    output.println("      <hr/>");
    output.println("      <p><b>Properties</b></p>");
    output.println("      <table cellpadding=\"5\" style=\"border-collapse: collapse;\">");
    output.println("         <tr bgcolor=\"#0080FF\" border=\"1\" rules=\"rows\"><th>Key</th><th>Value</th></tr>");

    for (Map.Entry<Object, Object> entry : conf.asOrderedMap().entrySet()) {
      output.print("         <tr><td>");
      output.print(escape(entry.getKey().toString()));
      output.print(":&#160;&#160;</td><td>");
      output.print(escape(entry.getValue().toString()));
      output.println("</td></tr>");
    }

    output.println("      </table>");
  }

  private void writeMain() {
    PrintWriter output;

    output = getOut("Main");

    writeTableTreeScript(output, 0);

    writeTime(output);
    writeStatistics(output);
    writeConstraint(output);
    writeSystemUnderTest(output);
    writeMainClassArguments(output);

    output.print("         <p>Machine Name:&#160;&#160;");
    output.print(reporter.getHostName());
    output.println("</p>");

    output.print("      <p>");
    output.print(reporter.getJPFBanner());
    output.println("</p>");

    output.flush();
    output.close();
  }

  private void writeTime(PrintWriter output) {
    output.println("      <p><b>Time</b></p>");
    output.println("      <table cellpadding=\"5\">");

    output.print("         <tr><td>Started:&#160;&#160;</td><td>");
    output.print(formatDTG(reporter.getStartDate()));
    output.println("</td></tr>");

    output.print("         <tr><td>Finished:&#160;&#160;</td><td>");
    output.print(formatDTG(reporter.getFinishedDate()));
    output.println("</td></tr>");

    output.print("         <tr><td>Elapsed:&#160;&#160;</td><td>");
    output.print(formatHMS(reporter.getElapsedTime()));
    output.println("</td></tr>");

    output.println("      </table>");
    output.println("      <br/>");
    output.println("      <br/>");
  }

  private void writeStatistics(PrintWriter output) {
    Statistics stats;

    stats = reporter.getStatistics();

    output.println("      <hr/>");
    output.println("      <p><b>Statistics</b></p>");

    if (stats == null) {
      output.println("      <i>Statistics not collected</i>");
      output.println("      <br/>");
      output.println("      <br/>");
      return;
    }

    output.println("      <table cellpadding=\"5\" style=\"border-collapse: collapse;\">");
    output.println("         <tr bgcolor=\"#0080FF\"><th>Statistic</th><th>Value</th></tr>");

    output.println("         <tr><td colspan=\"2\"><u>States:</u>&#160;&#160;</td></tr>");

    output.print("         <tr><td>&#160;&#160;&#160;New:&#160;&#160;</td><td align=\"right\">");
    output.print(stats.newStates);
    output.println("</td></tr>");

    output.print("         <tr><td>&#160;&#160;&#160;Visited:&#160;&#160;</td><td align=\"right\">");
    output.print(stats.visitedStates);
    output.println("</td></tr>");

    output.print("         <tr><td>&#160;&#160;&#160;Backtracked:&#160;&#160;</td><td align=\"right\">");
    output.print(stats.backtracked);
    output.println("</td></tr>");

    output.print("         <tr><td>&#160;&#160;&#160;End:&#160;&#160;</td><td align=\"right\">");
    output.print(stats.endStates);
    output.println("</td></tr>");

    output.print("         <tr><td><u>Instructions:</u>&#160;&#160;</td><td align=\"right\">");
    output.print(stats.insns);
    output.println("</td></tr>");

    output.println("         <tr><td colspan=\"2\"><u>Search:</u>&#160;&#160;</td></tr>");

    output.print("         <tr><td>&#160;&#160;&#160;Max Depth:&#160;&#160;</td><td align=\"right\">");
    output.print(stats.maxDepth);
    output.println("</td></tr>");

    output.print("         <tr><td>&#160;&#160;&#160;Constraints:&#160;&#160;</td><td align=\"right\">");
    output.print(stats.constraints);
    output.println("</td></tr>");

    output.println("         <tr><td colspan=\"2\"><u>Choice Generators:</u>&#160;&#160;</td></tr>");

    output.print("         <tr><td>&#160;&#160;&#160;Thread:&#160;&#160;</td><td align=\"right\">");
    output.print(stats.threadCGs);
    output.println("</td></tr>");

    output.print("         <tr><td>&#160;&#160;&#160;Data:&#160;&#160;</td><td align=\"right\">");
    output.print(stats.dataCGs);
    output.println("</td></tr>");

    output.println("         <tr><td colspan=\"2\"><u>Heap:</u>&#160;&#160;</td></tr>");

    output.print("         <tr><td>&#160;&#160;&#160;GC:&#160;&#160;</td><td align=\"right\">");
    output.print(stats.gcCycles);
    output.println("</td></tr>");

    output.print("         <tr><td>&#160;&#160;&#160;New Objects:&#160;&#160;</td><td align=\"right\">");
    output.print(stats.nNewObjects);
    output.println("</td></tr>");

    output.print("         <tr><td>&#160;&#160;&#160;Free Objects:&#160;&#160;</td><td align=\"right\">");
    output.print(stats.nReleasedObjects);
    output.println("</td></tr>");

    output.print("         <tr><td>&#160;&#160;&#160;Max Memory:&#160;&#160;</td><td align=\"right\">");
    output.print(stats.maxUsed >> 20);
    output.println(" MB</td></tr>");

    output.println("         <tr><td colspan=\"2\"><u>Loaded Code:</u>&#160;&#160;</td></tr>");

    // TODO Output the number of instructions loaded.

    output.print("         <tr><td>&#160;&#160;&#160;Methods:&#160;&#160;</td><td align=\"right\">");
    output.print(MethodInfo.getNumberOfLoadedMethods());
    output.println("</td></tr>");

    output.print("         <tr><td>&#160;&#160;&#160;Classes:&#160;&#160;</td><td align=\"right\">");
    output.print(ClassInfo.getNumberOfLoadedClasses());
    output.println("</td></tr>");

    output.println("      </table>");
    output.println("      <br/>");
    output.println("      <br/>");
  }

  private void writeConstraint(PrintWriter output) {
    String constraint;

    output.println("      <hr/>");
    output.println("      <p><b>Search Constraint:&#160;&#160;</b></p>");
    output.print("      ");

    constraint = reporter.getLastSearchConstraint();
    if (constraint == null) {
      constraint = "<i>None</i>";
    }

    output.print(constraint);
    output.println("<br/>");
    output.println("      <br/>");

  }

  private void writeSystemUnderTest(PrintWriter output) {
    RepositoryEntry entry;
    String mainPath;

    output.println("      <hr/>");
    output.println("      <p><b>System Under Test</b></p>");
    output.println("      <table cellpadding=\"5\">");

    output.print("         <tr><td>Main Class:&#160;&#160;</td><td>");
    output.print(conf.getTarget());
    output.println("</td></tr>");

    mainPath = reporter.getSuT();

    if (mainPath != null) {
      output.print("         <tr><td>Application:&#160;&#160;</td><td>");
      output.print(mainPath);
      output.println("</td></tr>");

      entry = RepositoryEntry.getRepositoryEntry(mainPath);

      if (entry != null) {
        output.print("         <tr><td>Repository:&#160;&#160;</td><td>");
        output.print(entry.getRepository());
        output.println("</td></tr>");

        output.print("         <tr><td>Revision:&#160;&#160;</td><td>");
        output.print(entry.getRevision());
        output.println("</td></tr>");

        output.print("         <tr><td>Repository Type:&#160;&#160;</td><td>");
        output.print(entry.getRepositoryType());
        output.println("</td></tr>");

        output.print("         <tr><td>Repository File Name:&#160;&#160;</td><td>");
        output.print(entry.getFileName());
        output.println("</td></tr>");
      }
    }

    output.println("      </table>");
    output.println("      <br/>");
    output.println("      <br/>");
  }

  private void writeMainClassArguments(PrintWriter output) {
    String args[];
    int i;

    output.println("      <hr/>");
    output.println("      <p><b>Main Class Arguments</b></p>");

    args = conf.getTargetArgs();
    if (args.length <= 0) {
      output.println("      <i>None</i>");
      output.println("      <br/>");
      output.println("      <br/>");
      return;
    }

    output.println("      <table cellpadding=\"5\" style=\"border-collapse: collapse;\">");
    output.println("         <tr bgcolor=\"#0080FF\"><th>#</th><th>Argument</th></tr>");

    for (i = 0; i < args.length; i++) {
      output.print("         <tr><td>");
      output.print(i);
      output.print("</td><td>");
      output.print(args[i]);
      output.println("</td></tr>");
    }

    output.println("      </table>");
    output.println("      <br/>");
    output.println("      <br/>");
  }

  private void writeError() {
    PrintWriter output;

    super.publishError();

    output = getOut("Errors");

    output.println("      <div style=\"white-space: nowrap;\">");
    writeErrors(output);
    output.println("      </div>");

    output.flush();
    output.close();
  }

  private void writeErrors(PrintWriter output) {
    List<Error> errors;
    Error error;
    String details;
    int i;

    errors = reporter.getErrors();

    if (errors.isEmpty()) {
      output.println("      <i>No errors detected.</i>");
      output.println("      <br/>");
      output.println("      <br/>");

      System.out.println("No errors detected.");
      return;
    }

    System.err.println("Errors detected.");

    output.println("      <table border=\"1\" rules=\"all\" cellpadding=\"5\" style=\"border-collapse: collapse;\">");
    output.println("         <tr bgcolor=\"#0080FF\"><th>Id</th><th>Description</th><th>Details</th></tr>");

    for (i = 0; i < errors.size(); i++) {
      error = errors.get(i);
      details = error.getDetails();
      
      if (details == null)
        details = "";
      else
        details = details.trim();       

      output.print("         <tr><td>");
      output.print(error.getId());
      output.print("</td><td>");
      output.print(escape(error.getDescription()));
      output.println("</td><td><pre>");
      output.print(escape(details));
      output.println("</pre></td></tr>");
    }

    output.println("      </table>");
    output.println("      <br/>");
    output.println("      <br/>");
  }

  private void writeOutput() {
    PrintWriter output;

    output = getOut("Output");

    output.println("     <div style=\"white-space: nowrap;\">");
    writeOutput(output);
    output.println("     </div>");

    output.flush();
    output.close();
  }

  private void writeOutput(PrintWriter output) {
    Path path;
    String data;
    int index, last;
    boolean first;

    path = reporter.getPath();

    if ((path.isEmpty()) || (!path.hasOutput())) {
      output.println("      <i>No output</i>");
      output.println("      <br/>");
      output.println("      <br/>");
      return;
    }

    first = true;
    last = -1;

    for (Transition trans : path) {
      data = trans.getOutput();
      if (data == null) {
        continue;
      }

      index = trans.getThreadIndex();

      if (last != index) {
        last = index;

        if (first) {
          first = false;
        } else {
          output.println("</pre>");
        }

        output.println("      <hr/>");

        output.print("      <p><b>Thread #");
        output.print(index);
        output.print("</b> - ");
        output.print(escape(trans.getThreadInfo().getName()));
        output.println("</p>");

        output.print("<pre>");
      }

      output.print(escape(data));
    }

    if (!first) {
      output.println("</pre>");
    }
  }

  private void writeSnapshot() {
    PrintWriter output;

    output = getOut("Threads");
    output.flush();
    output.close();

    try {
      output = new PrintWriter(m_pathName + "threads.html");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return;
    }

    writeDocStart(output);
    output.println("   <head>");
    writeTitle(output, "Threads");
    output.println("   </head>");

    output.println("   <frameset rows=\"200,*\" style=\"overflow: hidden;\">");
    output.println("      <frame frameborder=\"1\" name=\"thread_list\" src=\"thread_list.html\" style=\"overflow-y: hidden; overflow-x: auto;\" />");
    output.println("      <frameset cols=\"50%,50%\">");
    output.println("         <frame frameborder=\"1\" scrolling=\"yes\" name=\"thread_stacks\" src=\"thread_stacks.html\" />");
    output.println("         <frame frameborder=\"0\" scrolling=\"yes\" name=\"thread_locks\"  src=\"thread_locks.html\" />");
    output.println("      </frameset>");
    output.println("   </frameset>");
    output.println("   <noframes>");
    output.println("      Please use a frame enabled browser.");
    output.println("   </noframes>");

    output.println("</html>");

    output.flush();
    output.close();

    writeThreadList();
    writeThreadStacks();
    writeThreadLocks();
  }

  private void writeThreadList() {
    PrintWriter threadList;
    JVM jvm;
    ThreadInfo threads[];
    int i;

    try {
      threadList = new PrintWriter(m_pathName + "thread_list.html");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return;
    }

    writeDocStart(threadList);
    threadList.println("   <head>");
    writeTitle(threadList, "Thread List");
    threadList.println("      <script type=\"text/javascript\">");
    threadList.println("         //<![CDATA[");
    threadList.println("         function resize()");
    threadList.println("         {");
    threadList.println("            var bodyHeight, windowHeight, tbody;");
    threadList.println();
    threadList.println("            bodyHeight = document.body.parentNode.scrollHeight;");
    threadList.println("            windowHeight = window.innerHeight;");
    threadList.println("            tbody = document.getElementsByTagName(\"tbody\")[0];");
    threadList.println("            tbody.style.height = (windowHeight - (200 - 162)) + \"px\";");
    threadList.println("         }");
    threadList.println();
    threadList.println("         function doClick(anchor)");
    threadList.println("         {");
    threadList.println("            parent.frames[1].location = \"thread_stacks.html#\" + anchor.rel;");
    threadList.println("            parent.frames[2].location = \"thread_locks.html#\" + anchor.rel;");
    threadList.println();
    threadList.println("            return false;");
    threadList.println("         }");
    threadList.println("         //]]>");
    threadList.println("      </script>");
    threadList.println("   </head>");
    threadList.println("   <body onload=\"resize()\" onresize=\"resize()\">");

    jvm = reporter.getVM();

    if (jvm.getPathLength() <= 0) {
      threadList.println("      <i>Initial program state.</i>");
    } else {
      threads = jvm.getLiveThreads();

      if (threads.length == 0) {
        threadList.println("      <i>No live threads.</i>");
      } else {
        threadList.println("      <table id=\"list\" cellpadding=\"5\" style=\"border-collapse: collapse;\">");
        threadList.println("         <thead>");
        threadList.println("            <tr bgcolor=\"#0080FF\">");
        threadList.println("               <th>#</th>");
        threadList.println("               <th>Name</th>");
        threadList.println("               <th>Status</th>");
        threadList.println("               <th>Locks Owned</th>");
        threadList.println("               <th>Daemon</th>");
        threadList.println("               <th>Priority</th>");
        threadList.println("               <th>Lock Count</th>");
        threadList.println("               <th>Atomic</th>");
        threadList.println("               <th>&#160;</th>");    // Space for vertical scroll bar.
        threadList.println("            </tr>");
        threadList.println("         </thead>");
        threadList.println("         <tbody style=\"overflow-y: scroll; overflow-x: hidden;\">");

        for (i = 0; i < threads.length; i++) {
          writeThreadToList(threadList, threads[i]);
        }

        threadList.println("         </tbody>");
        threadList.println("      </table>");
      }
    }

    threadList.println("   </body>");
    threadList.println("</html>");

    threadList.flush();
    threadList.close();
  }

  private void writeThreadToList(PrintWriter output, ThreadInfo thread) {
    String status;

    output.println("           <tr>");

    output.print("              <td align=\"right\">");
    output.print(thread.getId());
    output.println("</td>");

    output.print("              <td align=\"left\"><a href=\"#\" onclick=\"doClick(this);\" rel=\"");
    output.print(thread.getId());
    output.print("\">");
    output.print(thread.getName());
    output.println("</a></td>");

    output.print("              <td align=\"center\">");
    status = thread.getStateName();
    status = status.charAt(0) + status.substring(1).toLowerCase();
    output.print(status);
    output.println("</td>");

    output.print("              <td align=\"right\">");
    output.print(thread.getLockedObjects().size());
    output.println("</td>");

    output.print("              <td align=\"center\">");
    output.print(thread.isDaemon());
    output.println("</td>");

    output.print("              <td align=\"right\">");
    output.print(thread.getPriority());
    output.println("</td>");

    output.print("              <td align=\"right\">");
    output.print(thread.getLockCount());
    output.println("</td>");

    output.print("              <td align=\"center\">");
    output.print(thread.isExecutingAtomically());
    output.println("</td>");

    output.print("              <td></td>");  // Space for vertical scroll bar

    output.println("           </tr>");
  }

  private void writeThreadStacks() {
    JVM jvm;
    ThreadInfo threads[];
    PrintWriter threadStacks;
    int i;

    try {
      threadStacks = new PrintWriter(m_pathName + "thread_stacks.html");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return;
    }

    writeDocStart(threadStacks);
    threadStacks.println("   <head>");
    writeTitle(threadStacks, "Thread Stacks");
    writeTableTreeScript(threadStacks, 0);
    threadStacks.println("   </head>");
    threadStacks.println("   <body style=\"white-space: nowrap;\">");

    threadStacks.println("      <p><b>Thread Stacks</b></p>");

    jvm = reporter.getVM();

    if (jvm.getPathLength() <= 0) {
      threadStacks.println("      <i>Initial program state.</i>");
    } else {
      threads = jvm.getLiveThreads();

      if (threads.length == 0) {
        threadStacks.println("      <i>No live threads.</i>");
      } else {
        for (i = 0; i < threads.length; i++) {
          writeThreadStack(threadStacks, threads[i]);
        }
      }
    }

    threadStacks.println("   </body>");
    threadStacks.println("</html>");

    threadStacks.flush();
    threadStacks.close();
  }

  private void writeThreadStack(PrintWriter output, ThreadInfo thread) {
    String frameID;

    output.println("      <hr/>");
    output.print("      <p id=\"");
    output.print(thread.getId());
    output.print("\"><b>Thread #");
    output.print(thread.getId());
    output.print("</b> - ");
    output.print(thread.getName());
    output.println("</p>");

    output.println("      <p><b>Call Stack</b></p>");

    if (thread.getStackDepth() <= 0) {
      output.println("      <i>No call stack.</i>");
      output.println("      <br/>");
      output.println("      <br/>");
      return;
    }

    writeTableTreeBegin(output);
    writeTableTreeNodeBegin(output, "thread" + thread.getId());
    output.println("            <td></td>");
    writeTableTreeNodeEnd(output);

    int i = thread.getStackDepth()-1;
    for (StackFrame frame : thread){
      if (frame.isDirectCallFrame()) {
        continue;
      }

      frameID = "thread" + thread.getId() + "-frame" + i--;

      writeStackMethod(output, frame, frameID);
      writeLocalVariables(output, frame, frameID);
      writeFields(output, frame, frameID);
      writeOperandStack(output, frame, frameID);
    }

    writeTableTreeEnd(output);
  }

  private void writeStackMethod(PrintWriter output, StackFrame frame, String frameID) {
    ClassInfo klass;
    boolean ref;

    writeTableTreeNodeBegin(output, frameID);

    output.print("<td>at ");

    klass = frame.getClassInfo();
    ref = (klass != null) && (frame.getPC() != null);

    if (ref) {
      writeSourceAnchor(output, klass.getSourceFileName(), frame.getLine());
    }

    if (klass != null) {
      output.print(klass.getName());
      output.print('.');
    }

    output.print(frame.getMethodName());

    output.print(':');

    if (frame.getPC() == null) {
      output.print("Native");
    } else if (klass == null) {
      output.print("Synthetic");
    } else {
      output.print(frame.getLine());
    }

    if (ref) {
      output.print("</a>");
    }

    output.print("</td>");

    writeTableTreeNodeEnd(output);
  }

  private void writeLocalVariables(PrintWriter output, StackFrame frame, String frameID) {
    LocalVarInfo localVars[];
    Object attr;
    int i;

    frameID += "-LocalVariables";

    writeTableTreeNodeBegin(output, frameID);
    output.println("          <td>Local Variables</td>");
    writeTableTreeNodeEnd(output);

    localVars = frame.getLocalVars();
    if (localVars == null) {
      return;
    }

    frameID += "-";

    for (i = 0; i < localVars.length; i++) {
      writeTableTreeNodeBegin(output, frameID + i);

      output.print("          <td>");
      output.print(localVars[i].getType());
      output.print(' ');
      output.print(localVars[i].getName());
      output.print(" = ");
      output.print(frame.getLocalValueObject(localVars[i]));

      attr = frame.getLocalAttr(i);
      if (attr != null) {
        output.print(" (");
        int k=0;
        for (Object a : ObjectList.iterator(attr)){
          if (k++ > 0){
            output.print(',');
          }
          output.print(a);
        }
        output.print(')');
      }

      output.println("</td>");

      writeTableTreeNodeEnd(output);
    }
  }

  private void writeFields(PrintWriter output, StackFrame frame, String frameID) {
    ClassInfo klass;

    frameID += "-Fields";

    writeTableTreeNodeBegin(output, frameID);
    output.println("          <td>Fields</td>");
    writeTableTreeNodeEnd(output);

    klass = frame.getClassInfo();
    writeFields(output, frame, frameID + "-StaticField", klass.getDeclaredStaticFields(), true);
    writeFields(output, frame, frameID + "-InstanceField", klass.getDeclaredInstanceFields(), false);
  }

  private void writeFields(PrintWriter output, StackFrame frame, String frameID, FieldInfo fields[], boolean isStatic) {
    int i;

    for (i = 0; i < fields.length; i++) {
      writeTableTreeNodeBegin(output, frameID + i);

      output.print("             <td>");

      if (isStatic) {
        output.print("<i>");
      }

      output.print(fields[i].getType());
      output.print(' ');
      output.print(fields[i].getName());
      output.print(" = ");
      output.print(frame.getFieldValue(fields[i].getName()));

      if (isStatic) {
        output.print("</i>");
      }

      output.println("</td>");
      writeTableTreeNodeEnd(output);
    }
  }

  private void writeOperandStack(PrintWriter output, StackFrame frame, String frameID) {
    Object attr;
    String number;
    int i, j, max;

    frameID += "-OperandStack";

    writeTableTreeNodeBegin(output, frameID);
    output.println("          <td>Operand Stack</td>");
    writeTableTreeNodeEnd(output);

    max = Integer.toString(frame.getTopPos()).length();

    frameID += "-Operand";

    for (i = 0; i < frame.getTopPos(); i++) {
      writeTableTreeNodeBegin(output, frameID + i);
      output.print("             <td>#");

      number = Integer.toString(i);

      for (j = max - number.length(); j > 0; j--) {
        output.print(' ');
      }

      output.print(number);
      output.print(" = ");
      output.print(frame.peek(i));

      attr = frame.getOperandAttr(i);
      if (attr != null) {
        output.print(" (");
        int k=0;
        for (Object a : ObjectList.iterator(attr)){
          if (k++ > 0){
            output.print(',');
          }
          output.print(a);
        }
        output.print(')');
      }

      output.println("</td>");
      writeTableTreeNodeEnd(output);
    }
  }

  public void writeSourceAnchor(PrintWriter output, String fileName, int line) {
    output.print("<a target=\"_blank\" href=\"source_");
    output.print(cleanFileName(fileName));
    output.print(".html#");
    output.print(line);
    output.print("\">");

    if (!m_sourceCoverage.containsKey(fileName)) {
      m_sourceCoverage.put(fileName, null);
    }
  }

  // <2do> no good - publishers should not be topic specific
  public void setSourceCoverage(String fileName, BitSet executable, BitSet covered) {
    Pair<BitSet, BitSet> coverage;

    coverage = m_sourceCoverage.get(fileName);
    if (coverage == null) {
      coverage = new Pair<BitSet, BitSet>(executable, covered);
      m_sourceCoverage.put(fileName, coverage);
    } else {
      coverage.getItem1().or(executable);
      coverage.getItem2().or(covered);
    }
  }

  private String cleanFileName(String fileName) {
    fileName = fileName.replace('\\', '_');
    fileName = fileName.replace('/', '_');

    return (fileName);
  }

  private void writeThreadLocks() {
    PrintWriter threadDetail;
    JVM jvm;
    ThreadInfo threads[];
    int i;

    try {
      threadDetail = new PrintWriter(m_pathName + "thread_locks.html");
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      return;
    }

    writeDocStart(threadDetail);
    threadDetail.println("   <head>");
    writeTitle(threadDetail, "Thread Locks");
    threadDetail.println("   </head>");
    threadDetail.println("   <body style=\"white-space: nowrap;\">");

    threadDetail.println("      <p><b>Thread Locks</b></p>");

    jvm = reporter.getVM();

    if (jvm.getPathLength() <= 0) {
      threadDetail.println("      <i>Initial program state.</i>");
    } else {
      threads = jvm.getLiveThreads();

      if (threads.length == 0) {
        threadDetail.println("      <i>No live threads.</i>");
      } else {
        for (i = 0; i < threads.length; i++) {
          writeThreadLockedObjects(threadDetail, threads[i]);
        }
      }
    }

    threadDetail.println("   </body>");
    threadDetail.println("</html>");

    threadDetail.flush();
    threadDetail.close();
  }

  private void writeThreadLockedObjects(PrintWriter output, ThreadInfo thread) {
    List<ElementInfo> locks;
    ElementInfo block;
    int i;

    output.println("      <hr/>");
    output.print("      <p id=\"");
    output.print(thread.getId());
    output.print("\"><b>Thread #");
    output.print(thread.getId());
    output.print("</b> - ");
    output.print(thread.getName());
    output.println("</p>");

    output.println("      <p><b>Locks</b></p>");

    block = thread.getLockObject();
    if (block != null) {
      if (thread.isWaiting()) {
        output.print("      <p>Waiting On:  ");
      } else {
        output.print("      <p>Blocked On:  ");
      }

      output.print(thread.getLockObject());
      output.println("</p>");
    }

    locks = thread.getLockedObjects();

    if (locks.isEmpty()) {
      output.println("      <i>No owned locks.</i>");
      output.println("      <br/>");
      output.println("      <br/>");
      return;
    }

    output.println("      <table cellpadding=\"5\" style=\"border-collapse: collapse;\">");
    output.println("         <tr bgcolor=\"#0080FF\"><th>#</th><th>Lock</th></tr>");

    i = 0;
    for (ElementInfo element : locks) {
      i++;

      output.print("         <tr><td>");
      output.print(i);
      output.print("</td><td>");
      output.print(element);
      output.println("</td></tr>");
    }

    output.println("      </table>");
    output.println("      <br/>");
  }

  private void writeSources() {
    PrintWriter output;
    Source source;
    Pair<BitSet, BitSet> coverage;
    String cleanName;

    for (String fileName : m_sourceCoverage.keySet()) {
      cleanName = cleanFileName(fileName);

      try {
        output = new PrintWriter(m_pathName + "source_" + cleanName + ".html");
      } catch (FileNotFoundException e) {
        e.printStackTrace();
        continue;
      }

      writeDocStart(output);
      output.println("   <head>");
      writeTitle(output, "Source Code");
      output.println("      <style type=\"text/css\">");
      output.println("         td.lineNum { text-align: right; border-right: 1px solid #000000; padding-right: 3px; }");
      output.println("         pre.code   { margin-bottom: 0px; margin-top: 0px; }");
      output.println("         td.covered { background-color: #80FF80; }");
      output.println("         tr.missed  { background-color: #FF9090; }");
      output.println("      </style>");
      output.println("   </head>");
      output.println("   <body style=\"font-family: verdana,arial,helvetica;\">");

      source = Source.getSource(fileName);
      if (source == null) {
        output.println("      <i>No source available.</i>");
      } else {
        coverage = m_sourceCoverage.get(fileName);
        writeSourceLines(output, source, coverage != null ? coverage.getItem1() : null, coverage != null ? coverage.getItem2() : null);
      }

      output.println("   </body>");
      output.println("</html>");

      output.flush();
      output.close();
    }
  }

  private void writeSourceLines(PrintWriter output, Source source, BitSet executable, BitSet covered) {
    String number;
    int i, max, width;
    boolean execute, cover;

    execute = false;
    cover = false;
    max = source.getLineCount();
    width = Integer.toString(max).length();

    output.println("      <table style=\"border-collapse: collapse; font-size: 11px;\">");

    for (i = 1; i <= max; i++) {
      number = Integer.toString(i);

      output.print("         <tr id=\"");
      output.print(number);
      output.print('\"');

      if (executable != null) {
        execute = executable.get(i);
        cover = covered.get(i);
      }

      if ((execute) && (!cover)) {
        output.print(" class=\"missed\"");
      }

      output.println('>');

      output.print("            <td class=\"lineNum");

      if ((execute) && (cover)) {
        output.print(" covered");
      }

      output.print("\">");
      output.print(number);
      output.println("</td>");

      output.print("            <td><pre class=\"code\">");
      output.print(escape(source.getLine(i).replaceAll("\t", "   ")));
      output.println("</pre></td>");

      output.println("         </tr>");
    }

    output.println("      </table>");
  }

  private void writeLoadedClasses() {
    PrintWriter output;

    output = getOut("Loaded Classes");

    output.println("      <style type=\"text/css\">");
    output.println("         table             { border-collapse: collapse; white-space: nowrap; border: 1px solid #000000; }");
    output.println("         th                { padding: 5px 5px; border: 1px solid #000000; background-color: #0080FF; }");
    output.println("         td                { padding: 0px 5px; border: none; }");
    output.println("         tr.treeNodeOpened { font-weight: bold; background-color: #A0D0FF; }");
    output.println("         tr.treeNodeClosed { font-weight: bold; background-color: #A0D0FF; }");
    output.println("      </style>");
    writeTableTreeScript(output, 0);

    output.println("      <div style=\"white-space: nowrap;\">");

    writeTableTreeBegin(output);

    // Write header row
    output.println("         <thead>");
    output.println("            <tr>");
    output.println("               <th>Package / Class</th>");
    output.println("               <th>Count</th>");
    output.println("            </tr>");
    output.println("         </thead>");
    output.println("         <tbody>");

    // Write all row
    writeTableTreeNodeBegin(output, "pc", "style=\"background-color: #60A0FF\"");
    output.print("                 <td><i>Root</i></td><td>");
    output.print(ClassInfo.getNumberOfLoadedClasses());
    output.println("</td>");
    writeTableTreeNodeEnd(output);

    writeLoadedClasses(output);

    output.println("         </tbody>");

    writeTableTreeEnd(output);

    output.println("      </div>");

    output.flush();
    output.close();
  }

  private void writeLoadedClasses(PrintWriter output) {
    HashMap<String, Integer> count;
    ClassInfo klass, classes[];
    String name, lastName;
    int i, end;

    classes = ClassInfo.getLoadedClasses();
    end     = classes.length;
    
    for (i = classes.length; --i >= 0; )
    {
      if (classes[i] == null)
         classes[i] = classes[--end];
       
      if (classes[i].getPackageName() == null)
        s_logger.warning("HTMLPublisher problem: class has a null package.  Class = " + classes[i].getName());
    }
    
    if (end != classes.length)
    {
      classes = Arrays.copyOf(classes, end);
      s_logger.warning("HTMLPublisher problem: ClassInfo.getLoadedClasses() returned an array with null elements");
    }

    sortClasses(classes);
    count = computeClassCounts(classes);
    lastName = "";

    for (i = 0; i < classes.length; i++) {
      klass = classes[i];
      
      name = klass.getName();

      writeLoadedClassesPackage(output, name, lastName, count);

      lastName = name;

      if (name.charAt(0) != '[') {
        name = name.replace('.', '-');
      }

      writeTableTreeNodeBegin(output, "pc-" + name);

      output.print("                 <td colspan=\"2\">");

      name = name.substring(name.lastIndexOf('-') + 1);
      output.print(name);

      output.println("</td>");
      writeTableTreeNodeEnd(output);
    }
  }

  private static void writeLoadedClassesPackage(PrintWriter output, String name, String last, HashMap<String, Integer> count) {
    String pack, piece;
    int i, start, len;
    char test;

    start = -1;
    len = Math.min(name.length(), last.length());

    if (len > 0) {
      if ((name.charAt(0) == '[') || (last.charAt(0) == '[')) {
        return;
      }
    }

    for (i = 0; i < len; i++) {
      test = last.charAt(i);

      if (test != name.charAt(i)) {
        break;
      }

      if (test == '.') {
        start = i;
      }
    }

    for (i = name.indexOf('.', start + 1); i >= 0; i = name.indexOf('.', start + 1)) {
      pack = name.substring(0, i);
      writeTableTreeNodeBegin(output, "pc-" + pack.replace('.', '-'), "style=\"background-color: #60A0FF\"");

      piece = name.substring(start + 1, i);
      output.print("                 <td>");
      output.print(piece);
      output.print("</td><td>");
      output.print(count.get(pack));
      output.println("</td>");

      writeTableTreeNodeEnd(output);

      start = i;
    }
  }

  private static void sortClasses(ClassInfo classes[]) {
    Comparator<ClassInfo> comparator;

    comparator = new Comparator<ClassInfo>() {

      public int compare(ClassInfo class1, ClassInfo class2) {
        String item1, item2;
        int result;

        if (class1 == null)
           return(class2 == null ? 0 : -1);

        if (class2 == null)
           return(1);

        item1 = class1.getPackageName();
        item2 = class2.getPackageName();
        
        if (item1 == null)
        {
           if (item2 != null)
              return(-1);

           result = 0;
        }
        else if (item2 == null)
           return(1);
        else
           result = item1.compareTo(item2);
        
        if (result == 0) {
          result = class1.getName().compareTo(class2.getName());
        } else if (item1.length() == 0) { // Sort the classes with no package to the bottom.
          result = 1;
        } else if (item2.length() == 0) { // Sort the classes with no package to the bottom.
          result = -1;
        }

        return (result);
      }
    };

    Arrays.sort(classes, comparator);
  }

  private static HashMap<String, Integer> computeClassCounts(ClassInfo classes[]) {
    HashMap<String, Integer> result;
    String pack;
    int i, index;

    result = new HashMap<String, Integer>();

    for (i = classes.length - 1; i >= 0; i--) {
      pack = classes[i].getPackageName();
      addPackageCount(result, pack);
       
      if (pack == null)
        continue;

      for (index = pack.indexOf('.'); index >= 0; index = pack.indexOf('.', index + 1)) {
        addPackageCount(result, pack.substring(0, index));
      }
    }

    return (result);
  }

  private static void addPackageCount(HashMap<String, Integer> count, String pack) {
    Integer value;

    value = count.get(pack);
    if (value == null) {
      value = Integer.valueOf(0);
    }

    count.put(pack, value + 1);
  }

  private void writeTrace() {
    PrintWriter output;

    output = getOut("Trace");

    writeTraceStyle(output);
    output.println("      <div style=\"white-space: nowrap;\">");
    writeTableTreeScript(output, 1);
    writeTraceScript(output);

    writeTrace(output);

    output.println("      </div>");

    output.flush();
    output.close();
  }

  private static void writeTraceStyle(PrintWriter output) {
    output.println("      <style type=\"text/css\">");
    output.println("         table           { min-width: 100%; }");
    output.println();
    output.println("         tt              { text-decoration: none; white-space: normal; color: #000000; font-size: medium; }");
    output.println("         tt.noexec       { text-decoration: line-through; white-space: pre; color: #808080; }");  // Modify the text of code nodes.
    output.println("         tt.exec         { white-space: pre; }");
    output.println("         tt.no_source    { color: #808080; }");
    output.println();
    output.println("         tr.treeNodeOpened div.treeInner { font-weight: bold; border-top: 1px solid #000000; }");
    output.println("         tr.treeNodeClosed               { font-size: small; color: #808080; }");
    output.println();
    output.println("         td.StateID                      { font-size: small; color: #808080; vertical-align: top; }");
    output.println("      </style>");
  }

  private static void writeTraceScript(PrintWriter output) {
    output.println("      <script type=\"text/javascript\">");
    output.println("         //<![CDATA[");
    output.println("         //addEvent(window, \"load\", rotateStateID);   // During onload event, rotate the State ID cells.");
    output.println();
    output.println("         function addEvent(object, event, method)");
    output.println("         {");
    output.println("            if (object.addEventListener)");
    output.println("               object.addEventListener(event, method, false);");
    output.println("            else if (object.attachEvent)");
    output.println("               return(object.attachEvent(\"on\", event, method));");
    output.println("            else");
    output.println("               return(false);");
    output.println();
    output.println("            return(true);");
    output.println("         }");
    output.println();
    output.println("         function rotateStateID()");
    output.println("         {");
    output.println("            var i, cells, cell, last, count;");
    output.println();
    output.println("            cells = document.getElementsByTagName(\"td\");");
    output.println("            last  = null;");
    output.println("            count = 0;");
    output.println();
    output.println("            for (i = cells.length - 1; i >= 0; i--)");
    output.println("            {");
    output.println("               cell = cells[i];");
    output.println();
    output.println("               if (cell.className != \"StateID\")");
    output.println("                  continue;");
    output.println();
    output.println("               if (last == null)");
    output.println("               {");
    output.println("                  count = 1;");
    output.println("                  last  = cell;");
    output.println("                  continue;");
    output.println("               }");
    output.println();
    output.println("               if (cell.innerHTML == last.innerHTML)");
    output.println("               {");
    output.println("                  count++;");
    output.println("                  last.parentNode.removeChild(last);");
    output.println("                  last = cell;");
    output.println("                  continue;");
    output.println("               }");
    output.println();
    output.println("               rotateCell(last, count);");
    output.println("               count = 1;");
    output.println("               last  = cell;");
    output.println("            }");
    output.println();
    output.println("            if (last != null)");
    output.println("               rotateCell(last, count);");
    output.println("         }");
    output.println();
    output.println("         function rotateCell(cell, rowSpan)");
    output.println("         {");
    output.println("               cell.rowSpan   = rowSpan;");
    output.println("               cell.width     = \"1em\";");
    output.println("               cell.innerHTML = \"<svg:svg width='1.0em' height='45'><svg:text text-anchor='end' y='0.8em' transform='rotate(-90)' font-size='small' fill='#808080'>\" + cell.innerHTML + \"</svg:text></svg:svg>\";");
    output.println("         }");
    output.println("         //]]>");
    output.println("      </script>");
  }

  private void writeTrace(PrintWriter output) {
    HashMap<Integer, ArrayList<Pair<String, String>>> stacks;
    ArrayList<Pair<String, String>> stack;
    HashSet<String> openNodes;
    Path path;
    Instruction lastInst;
    String treeId;
    int index, lastThread, curThread, stateID;

    path = reporter.getPath();

    if (path.isEmpty()) {
      output.println("      <i>No trace information.</i>");
      output.println("      <br/>");
      output.println("      <br/>");
      return;
    }
    
    if (!super.reporter.getVM().hasToRecordSteps()) {
      output.println("      <i>Steps not recorded.</i>");
      output.println("      <i>Enable by setting report.html.finished+=,trace</i>");
      output.println("      <br/>");
      output.println("      <br/>");
      return;
    }

    stacks     = new HashMap<Integer, ArrayList<Pair<String, String>>>();
    openNodes  = new HashSet<String>();
    stack      = null;
    lastInst   = null;
    treeId     = null;
    index      = 0;
    stateID    = -1;
    m_noSource = 0;
    lastThread = -1;

    output.println("      <table><tr><td class=\"StateID\">State #</td></tr></table>");

    for (Transition trans : path) {
      curThread = trans.getThreadIndex();
      if (lastThread != curThread) {
        lastThread = curThread;
        lastInst   = null;
        m_noSource = 0;

        writeTransExit(output, treeId, stack, stateID, openNodes);
        
        treeId  = "tree" + ++index;
        stack   = getStack(stacks, curThread);
        stateID = trans.getStateId();
        
        writeTransEnter(output, treeId, stack, trans, index);
      }

      lastInst = writeSteps(output, trans, lastInst, treeId, stack, openNodes);
    }

    writeTransExit(output, treeId, stack, stateID, openNodes);
  }

  private static ArrayList<Pair<String, String>> getStack(HashMap<Integer, ArrayList<Pair<String, String>>> stacks, int threadIndex) {
    ArrayList<Pair<String, String>> stack;

    stack = stacks.get(threadIndex);
    if (stack == null) {
      stack = new ArrayList<Pair<String, String>>();
      stacks.put(threadIndex, stack);
    }

    return (stack);
  }

  private void writeTransEnter(PrintWriter output, String treeId, ArrayList<Pair<String, String>> stack, Transition trans, int index) {
    int i;

    writeTableTreeBegin(output);

    writeTableTreeNodeBegin(output, treeId);
    output.print("            <td class=\"StateID\">");
    output.print(trans.getStateId());
    output.print("</td><td>");
    output.print("Transition #");
    output.print(index);
    output.print(" - Thread:  ");
    output.print(trans.getThreadIndex());
    output.print(" - Choice:  ");
    output.print(trans.getChoiceGenerator());
    output.println("</td>");
    writeTableTreeNodeEnd(output);

    for (i = 0; i < stack.size(); i++) {
      writeMethod(output, treeId, stack.get(i), trans.getStateId());
    }
  }

  private void writeTransExit(PrintWriter output, String treeId, ArrayList<Pair<String, String>> stack, int stateID, HashSet<String> openNodes) {
    if (stack == null) {
      return;
    }

    if (!stack.isEmpty()) {
      writeNoSource(output, treeId, stack.get(stack.size() - 1).getItem2(), stateID);
    }

    writeTableTreeEnd(output);
    writeTableTreeOpenNodes(output, treeId, openNodes);
    openNodes.clear();
  }

  public static void writeTableTreeOpenNodes(PrintWriter output, String treeId, Collection<String> openNodes) {
    if (openNodes.isEmpty()) {
      return;
    }

    output.println("      <script type=\"text/javascript\">");
    output.println("         //<![CDATA[");
    output.println("         function openLineNodes()");
    output.println("         {");

    for (String node : openNodes) {
      output.print("            treeNodeShowPath(\"");
      output.print(treeId);
      output.print(node);
      output.println("\");");
    }

    output.println("         }");
    output.println();
    output.println("         addEvent(window, \"load\", openLineNodes);");
    output.println();
    output.println("         //]]>");
    output.println("      </script>");
  }

  public static void writeTableTreeOpenNodes(PrintWriter output, Collection<String> nodeIDs)
  {
    if (nodeIDs.isEmpty()) {
       return;
    }

    output.println("      <script type=\"text/javascript\">");
    output.println("         //<![CDATA[");
    output.println("         function openLineNodes()");
    output.println("         {");

    for (String node : nodeIDs)
    {
       output.print("            treeNodeShowPath(\"");
       output.print(node);
       output.println("\");");
    }

    output.println("         }");
    output.println();
    output.println("         addEvent(window, \"load\", openLineNodes);");
    output.println();
    output.println("         //]]>");
    output.println("      </script>");
  }

  private static class WriteLine {

    public static final WriteLine EMPTY = new WriteLine(null, null, -1);
    public final String methodId;
    public final Instruction inst;
    public final int last;

    public WriteLine(String methodId, Instruction inst, int last) {
      this.methodId = methodId;
      this.inst = inst;
      this.last = last;
    }
  }

  private Instruction writeSteps(final PrintWriter output, final Transition trans, Instruction lastInst, final String treeId, final ArrayList<Pair<String, String>> stack, final HashSet<String> openNodes) {
    WriteLine line, nextLine;
    MethodInfo lastMethod;
    Instruction inst;
    StringBuilder comment;
    String methodId, subcomment;
    int last;

    line = null;
    nextLine = null;

    if (lastInst != null) {
      last = lastInst.getLineNumber();
      lastMethod = lastInst.getMethodInfo();
    } else {
      last = Integer.MAX_VALUE - 1;
      lastMethod = null;
    }

    if (stack.isEmpty()) {
      methodId = null;
    } else {
      methodId = stack.get(stack.size() - 1).getItem2();
    }

    comment = new StringBuilder();

    for (Step step : trans) {
      inst = step.getInstruction();

      if (inst.isFirstInstruction()) {
        if (line != null) {
          writeLines(output, treeId, line.methodId, trans.getStateId(), line.inst, comment, line.last);
          line = null;
        }

        methodId = enterMethod(output, treeId, stack, trans.getStateId(), inst);
        last     = Integer.MAX_VALUE - 1;
      } else if (stack.isEmpty()) {
        continue;
      }

      if (step.getLineString() == null) {
        nextLine = WriteLine.EMPTY;

        m_noSource++;
        last = Integer.MAX_VALUE - 1;
        comment.setLength(0);
      } else if ((inst.getLineNumber() != last) || (inst.getMethodInfo() != lastMethod)) {
        lastInst = inst;
        lastMethod = inst.getMethodInfo();
        nextLine = new WriteLine(methodId, inst, last);
        last = inst.getLineNumber();
        openNodes.add(methodId);
      }

      if ((nextLine != null) || (inst instanceof ReturnInstruction)) {
        if (line != null) {
          writeLines(output, treeId, line.methodId, trans.getStateId(), line.inst, comment, line.last);
        }

        if (nextLine != WriteLine.EMPTY) {
          line = nextLine;
        } else {
          line = null;
        }

        nextLine = null;
        comment.setLength(0);
      }

      if (inst instanceof ReturnInstruction) {
        methodId = exitMethod(output, treeId, stack, trans.getStateId());
        last = Integer.MAX_VALUE - 1;
      }

      subcomment = step.getComment();
      if (subcomment != null) {
        if (comment.length() > 0) {
          comment.append("  |  ");
        }

        comment.append(subcomment);
      }
    }

    // TODO BUG - GETFIELD, transition to next state (continue with same thread), store in local - The store in local operation doesn't get added to the comment.

    if (line != null) {
      writeLines(output, treeId, line.methodId, trans.getStateId(), line.inst, comment, line.last);
    }

    writeNoSource(output, treeId, methodId, trans.getStateId());

    return (lastInst);
  }

  private String enterMethod(PrintWriter output, String treeId, ArrayList<Pair<String, String>> stack, int stateID, Instruction inst) {
    Pair<String, String> method;
    String name, parent;

    if (stack.isEmpty()) {
      parent = "";
    } else {
      parent = stack.get(stack.size() - 1).getItem2();
    }

    writeNoSource(output, treeId, parent, stateID);

    name   = getMethod(inst);

    method = new Pair<String, String>(name, parent + "-" + m_treeNodeMethodId++);
    stack.add(method);
    writeMethod(output, treeId, method, stateID);

    return (method.getItem2());
  }

  private String exitMethod(PrintWriter output, String treeId, ArrayList<Pair<String, String>> stack, int stateID) {
    Pair<String, String> method;
    int index;

    index = stack.size() - 1;
    method = stack.remove(index);
    writeNoSource(output, treeId, method.getItem2(), stateID);

    index = stack.size() - 1;
    if (index < 0) {
      return (null);
    }

    method = stack.get(index);

    return (method.getItem2());
  }

  private void writeMethod(PrintWriter output, String treeId, Pair<String, String> method, int stateID) {
    writeTableTreeNodeBegin(output, treeId + method.getItem2());

    output.print("            <td class=\"StateID\">");
    output.print(stateID);
    output.print("</td><td>");
    output.print(method.getItem1());
    output.println("</td>");

    writeTableTreeNodeEnd(output);
  }

  private String getMethod(Instruction inst) {
    MethodInfo methodInfo;
    StringBuilder method;
    boolean first;

    methodInfo = inst.getMethodInfo();

    method = new StringBuilder(50);

    if (methodInfo.isPublic()) {
      method.append("public ");
    }

    if (methodInfo.isStatic()) {
      method.append("static ");
    }

    if (methodInfo.isSynchronized()) {
      method.append("synchronized ");
    }

    method.append(methodInfo.getReturnTypeName());
    method.append(' ');
    method.append(methodInfo.getClassName());

    if (!methodInfo.isCtor()) {
      method.append('.');
      method.append(escape(methodInfo.getName()));
    }

    method.append('(');

    first = true;
    for (String arg : methodInfo.getArgumentTypeNames()) {
      if (first) {
        first = false;
      } else {
        method.append(", ");
      }

      method.append(arg);
    }

    method.append(')');

    return (method.toString());
  }

  private void writeNoSource(PrintWriter output, String treeId, String methodId, int stateID) {
    if (m_noSource <= 0) {
      return;
    }

    writeTableTreeNodeBegin(output, treeId + methodId + "-" + m_treeNodeLineId++);

    output.print("            <td class=\"StateID\">");
    output.print(stateID);
    output.print("</td><td><tt class=\"no_source\">");
    output.print('[');
    output.print(m_noSource);
    output.print(" instruction");

    if (m_noSource != 1) {
      output.print('s');
    }

    output.println(" without source code]</tt></td>");

    writeTableTreeNodeEnd(output);

    m_noSource = 0;
  }

  private void writeLines(PrintWriter output, String treeId, String methodId, int stateID, Instruction inst, StringBuilder comment, int last) {
    String filename;
    int current;

    writeNoSource(output, treeId, methodId, stateID);

    current  = inst.getLineNumber();
    filename = inst.getMethodInfo().getSourceFileName();

    for (last++; last < current; last++) {
      writeLine(output, treeId, methodId, stateID, filename, null, last, false);
    }

    writeLine(output, treeId, methodId, stateID, filename, comment, current, true);
  }

  private void writeLine(PrintWriter output, String treeId, String methodId, int stateID, String filename, StringBuilder comment, int number, boolean executed) {
    Source source;
    String line, num;
    int i, max;

    source = Source.getSource(filename);
    line   = source.getLine(number);

    num    = Integer.toString(number);
    max    = Integer.toString(source.getLineCount()).length();

    writeTableTreeNodeBegin(output, treeId + methodId + "-" + m_treeNodeLineId++);

    output.print("            <td class=\"StateID\">");
    output.print(stateID);
    output.print("</td><td><tt class=\"");

    if (!executed) {
      output.print("no");
    }

    output.print("exec\">");

    for (i = max - num.length(); i > 0; i--)
      output.print(' ');

    writeSourceAnchor(output, filename, number);
    output.print(num);
    output.print("</a> ");

    output.print(escape(line.replaceAll("\t", "   ")));

    if ((comment != null) && (comment.length() > 0)) {
      output.print("            // ");
      output.print(escape(comment.toString()));
    }

    output.println("</tt></td>");

    writeTableTreeNodeEnd(output);
  }

  private void writeDocStart(PrintWriter output) {
    output.println(XML_VERSION);
    output.println(DOCTYPE);
    output.println(HTML);
  }

  private void writeTitle(PrintWriter output, String title) {
    String mainClass, mainPath;

    output.print("      <title>Java PathFinder ");
    output.print(title);

    mainClass = conf.getTarget();

    if (mainClass != null) {
      output.print(" for ");

      mainPath = reporter.getSuT();
      if (mainPath == null) {
        output.print(mainClass);
      } else {
        output.print(mainPath);
      }
    }

    output.println("</title>");
  }

  public static void writeTableTreeScript(PrintWriter output, int columnIndex) {
    output.println("      <style type=\"text/css\">");
    output.println("         tr.treeNodeLeaf   span.treeNodeLeaf { background: url(bullet.gif) center no-repeat; float: left; height: 19px; width: 19px; }");
    output.println("         tr.treeNodeOpened a.treeNode        { background: url(minus.gif)  center no-repeat; float: left; height: 19px; width: 19px; }");
    output.println("         tr.treeNodeClosed a.treeNode        { background: url(plus.gif)   center no-repeat; float: left; height: 19px; width: 19px; }");
    output.println("      </style>");
    output.println("      <script type=\"text/javascript\">");
    output.println("         //<![CDATA[");
    output.println("         addEvent(window, \"load\", initializeTrees);   // During onload event, convert the table rows into tree nodes.");
    output.println();
    output.println("         function addEvent(object, event, method)");
    output.println("         {");
    output.println("            if (object.addEventListener)");
    output.println("               object.addEventListener(event, method, false);");
    output.println("            else if (object.attachEvent)");
    output.println("               return(object.attachEvent(\"on\", event, method));");
    output.println("            else");
    output.println("               return(false);");
    output.println();
    output.println("            return(true);");
    output.println("         }");
    output.println();
    output.println("         function treeNodeShowPath(id)");
    output.println("         {");
    output.println("            var pos;");
    output.println();
    output.println("            pos = -1;");
    output.println();
    output.println("            for (pos = id.indexOf(\"-\", pos + 1); pos >= 0; pos = id.indexOf(\"-\", pos + 1))");
    output.println("               treeNodeShow(id.substring(0, pos));");
    output.println();
    output.println("            treeNodeShow(id);");
    output.println("         }");
    output.println();
    output.println("         function treeNodeShow(id)");
    output.println("         {");
    output.println("            var row, cell, node;");
    output.println();
    output.println("            row = document.getElementById(id);");
    output.println("            if (row == null)");
    output.println("               return;");
    output.println();
    output.println("            if (row.className != \"treeNodeClosed\")");
    output.println("               return;");
    output.println();
    output.println("            cell = row.cells[0];");
    output.println("            node = cell.childNodes[0];");
    output.println();
    output.println("            treeNodeClicked(node);");
    output.println("         }");
    output.println();
    output.println("         function treeNodeClicked(node)");
    output.println("         {");
    output.println("            var i, base, baseID, rows, row, rowOp;");
    output.println();
    output.println("            base   = node.parentNode.parentNode;");
    output.println("            baseID = base.id + \"-\";");
    output.println();
    output.println("            if (base.className == \"treeNodeOpened\")");
    output.println("            {");
    output.println("               base.className = \"treeNodeClosed\";");
    output.println("               rowOp          = hideRow;");
    output.println("            }");
    output.println("            else if (base.className == \"treeNodeClosed\")");
    output.println("            {");
    output.println("               base.className = \"treeNodeOpened\";");
    output.println("               rowOp          = showRow;");
    output.println("            }");
    output.println();
    output.println("            rows = document.getElementsByTagName(\"tr\");");
    output.println();
    output.println("            for (i = rows.length - 1; i >= 0; i--)");
    output.println("            {");
    output.println("               row = rows[i];");
    output.println();
    output.println("               if (row == base)");
    output.println("                  break;");
    output.println();
    output.println("               if (row.className.indexOf(\"treeNode\") != 0)");
    output.println("                  continue;");
    output.println();
    output.println("               if (row.id.indexOf(baseID) != 0)");
    output.println("                  continue;");
    output.println();
    output.println("               rowOp(row, baseID);");
    output.println("            }");
    output.println();
    output.println("            return(false);");
    output.println("         }");
    output.println();
    output.println("         function showRow(row, baseID)");
    output.println("         {");
    output.println("            if (row.id.indexOf(\"-\", baseID.length) < 0)");
    output.println("               row.style.display = \"table-row\";");
    output.println("         }");
    output.println();
    output.println("         function hideRow(row, baseID)");
    output.println("         {");
    output.println("            if (row.className == \"treeNodeOpened\")");
    output.println("               row.className  = \"treeNodeClosed\";");
    output.println();
    output.println("            row.style.display = \"none\";");
    output.println("         }");
    output.println();
    output.println("         function initializeTrees()");
    output.println("         {");
    output.println("            document.getElementsByTagName(\"body\")[0].style.cursor = \"wait\";");
    output.println();
    output.println("            initializeNodes();");
    output.println("            initializeRoots();");
    output.println();
    output.println("            document.getElementsByTagName(\"body\")[0].style.cursor = \"default\";");
    output.println("         }");
    output.println();
    output.println("         function initializeRoots()");
    output.println("         {");
    output.println("            var i, rows, row, root;");
    output.println();
    output.println("            root = null;");
    output.println("            rows = document.getElementsByTagName(\"tr\");");
    output.println();
    output.println("            for (i = 0; i < rows.length; i++)");
    output.println("            {");
    output.println("               row = rows[i];");
    output.println();
    output.println("               if (row.className != \"treeNodeClosed\")");
    output.println("                  continue;");
    output.println();
    output.println("               if ((root != null) && (row.id.indexOf(root.id) == 0))");
    output.println("                  continue;");
    output.println();
    output.println("               root = row;");
    output.println("               row.style.display = \"table-row\";");
    output.println();
    output.println("               treeNodeClicked(row.cells[0].childNodes[0]);");
    output.println("            }");
    output.println("         }");
    output.println();
    output.println("         function initializeNodes()");
    output.println("         {");
    output.println("            var i, rows, last, row, cell, anchor, node, indent;");
    output.println();
    output.println("            last = null;");
    output.println("            rows = document.getElementsByTagName(\"tr\");");
    output.println();
    output.println("            for (i = rows.length - 1; i >= 0; i--)");
    output.println("            {");
    output.println("               row = rows[i];");
    output.println();
    output.println("               if (row.className != \"treeNode\")");
    output.println("                  continue;");
    output.println();
    output.println("               row.style.display = \"none\";");
    output.println();
    output.println("               indent = 14 * (row.id.split(\"-\").length - 1);");
    output.println();
    output.println("               if (((last != null) && (last.id.indexOf(row.id + '-') >= 0)) || (row.id.indexOf(\"-\") < 0))");
    output.println("               {");
    output.println("                  row.className = \"treeNodeClosed\";");
    output.println("                  node = \"<a class='treeNode' onclick='treeNodeClicked(this);' style='margin-left: \" + indent + \"px'></a>\";");
    output.println("               }");
    output.println("               else");
    output.println("               {");
    output.println("                  row.className = \"treeNodeLeaf\";");
    output.println("                  node = \"<span class='treeNodeLeaf' style='margin-left: \" + indent + \"px'></span>\";");
    output.println("               }");
    output.println();
    output.println("               cell = row.cells[" + columnIndex + "];");
    output.println("               cell.innerHTML = node + \"<div class='treeInner' style='margin-left: \" + (indent + 19) + \"px'>\" + cell.innerHTML + \"</div>\";");
    output.println();
    output.println("               last = row;");
    output.println("            }");
    output.println("         }");
    output.println("         //]]>");
    output.println("      </script>");
  }

  public static void writeTableTreeBegin(PrintWriter output) {
    output.println("      <table>");
  }

  public static void writeTableTreeEnd(PrintWriter output) {
    output.println("      </table>");
  }

  public static void writeTableTreeNodeBegin(PrintWriter output, String id) {
    writeTableTreeNodeBegin(output, id, null);
  }

  public static void writeTableTreeNodeBegin(PrintWriter output, String id, String attributes) {
    output.print("         <tr class=\"treeNode\" id=\"");
    output.print(id);
    output.print('\"');

    if (attributes != null) {
      output.print(' ');
      output.print(attributes);
    }

    output.println('>');
  }

  public static void writeTableTreeNodeEnd(PrintWriter output) {
    output.println("         </tr>");
  }

  public static void writeProgressBarStyle(PrintWriter pw) {
    pw.println("      <style type=\"text/css\">");
    pw.println("         div.progressbar              { background-color: #F02020; border: 1px solid #808080; width: 100px; }");
    pw.println("         div.progressbar div.greenbar { background-color: #00F000; }");  // height: 1.3em;
    pw.println("         div.progressbar span.text    { display: block; text-align: center; width: 100px; font-size: 68%; }");
    pw.println("      </style>");
  }

  public static String makeProgressBar(int percent) {
    StringBuilder result = new StringBuilder(115);
    result.append("<div class=\"progressbar\"><div class=\"greenbar\" style=\"width: ");
    result.append(percent);
    result.append("px;\"><span class=\"text\">");
    result.append(percent);
    result.append("%</span></div></div>");

    return result.toString();
  }

  private void writeArray(String filename, byte data[]) {
    FileOutputStream output;

    try {
      output = new FileOutputStream(m_pathName + filename);
      output.write(data);
      output.flush();
      output.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static String escape(String text) {
    text = text.replaceAll("&", "&amp;");
    text = text.replaceAll("<", "&lt;");
    text = text.replaceAll(">", "&gt;");

    return (text);
  }
}
