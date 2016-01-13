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

import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * abstract base for all format specific publishers. Note that this interface
 * also has to work for non-stream based reporting, i.e. within Eclipse
 * (we don't want to re-parse from text reports there)
 */
public abstract class Publisher {

  // output phases
  public static final int START = 1;
  public static final int TRANSITION = 2;
  public static final int PROPERTY_VIOLATION = 3;
  public static final int FINISHED = 4;

  protected Config conf;
  protected Reporter reporter; // our master

  protected String[] startTopics = {};
  protected String[] transitionTopics = {};
  protected String[] propertyViolationTopics = {};
  protected String[] constraintTopics = {};
  protected String[] finishedTopics = {};

  DateFormat dtgFormatter = DateFormat.getDateTimeInstance(DateFormat.SHORT,
      DateFormat.SHORT);

  ArrayList<PublisherExtension> extensions;

  /**
   * to be initialized in openChannel
   * NOTE - not all publishers need to have one
   */
  protected PrintWriter out;

  public PrintWriter getOut () {
    return out;
  }

  protected Publisher (Config conf, Reporter reporter){
    this.conf = conf;
    this.reporter = reporter;

    setTopics();
  }

  public void setTopics (int category, String[] newTopics){
    switch (category){
    case START:
      startTopics = newTopics; break;
    case TRANSITION:
      transitionTopics = newTopics; break;
    case PROPERTY_VIOLATION:
      propertyViolationTopics = newTopics; break;
    case FINISHED:
      finishedTopics = newTopics; break;
    default:
      Reporter.log.warning("unknown publisher topic category: " + category);
    }
  }

  public abstract String getName();

  protected void setTopics () {
    setTopics(getName());
  }
  
  protected void setTopics (String name) {
    String prefix = "report." + name;
    startTopics = conf.getStringArray(prefix + ".start", startTopics);
    transitionTopics = conf.getStringArray(prefix + ".transition", transitionTopics);
    propertyViolationTopics = conf.getStringArray(prefix + ".property_violation", propertyViolationTopics);
    constraintTopics = conf.getStringArray(prefix + ".constraint", constraintTopics);
    finishedTopics = conf.getStringArray(prefix + ".finished", finishedTopics);    
  }
  
  public void addExtension (PublisherExtension ext) {
    if (extensions == null) {
      extensions = new ArrayList<PublisherExtension>();
    }
    extensions.add(ext);
  }

  // <2do> should not be a list we can add to
  private static final List<PublisherExtension> EMPTY_LIST = new ArrayList<PublisherExtension>(0);
  
  public List<PublisherExtension> getExtensions(){
    if (extensions != null){
      return extensions;
    } else {
      return EMPTY_LIST; // make life easier for callers
    }
  }
  
  public String getLastErrorId() {
    return reporter.getLastErrorId();
  }

  public boolean hasTopic (String topic) {
    for (String s : startTopics) {
      if (s.equalsIgnoreCase(topic)){
        return true;
      }
    }
    for (String s : transitionTopics) {
      if (s.equalsIgnoreCase(topic)){
        return true;
      }
    }
    for (String s : constraintTopics) {
      if (s.equalsIgnoreCase(topic)){
        return true;
      }
    }
    for (String s : propertyViolationTopics) {
      if (s.equalsIgnoreCase(topic)){
        return true;
      }
    }
    for (String s : finishedTopics) {
      if (s.equalsIgnoreCase(topic)){
        return true;
      }
    }

    return false;
  }

  public String formatDTG (Date date) {
    return dtgFormatter.format(date);
  }

  /**
  static public String _formatHMS (long t) {
    t /= 1000; // convert to sec

    long s = t % 60;
    long h = t / 3600;
    long m = (t % 3600) / 60;

    StringBuilder sb = new StringBuilder();
    sb.append(h);
    sb.append(':');
    if (m < 10){
      sb.append('0');
    }
    sb.append(m);
    sb.append(':');
    if (s < 10){
      sb.append('0');
    }
    sb.append(s);

    return sb.toString();
  }
  **/

  static char[] tBuf = { '0', '0', ':', '0', '0', ':', '0', '0' };
  
  static synchronized public String formatHMS (long t) {
    int h = (int) (t / 3600000);
    int m = (int) ((t / 60000) % 60);
    int s = (int) ((t / 1000) % 60);
    
    tBuf[0] = (char) ('0' + (h / 10));
    tBuf[1] = (char) ('0' + (h % 10));
    
    tBuf[3] = (char) ('0' + (m / 10));
    tBuf[4] = (char) ('0' + (m % 10));
    
    tBuf[6] = (char) ('0' + (s / 10));
    tBuf[7] = (char) ('0' + (s % 10));
    
    return new String(tBuf);
  }
  
  public String getReportFileName (String key) {
    String fname = conf.getString(key);
    if (fname == null){
      fname = conf.getString("report.file");
      if (fname == null) {
        fname = "report";
      }
    }

    return fname;
  }

  public void publishTopicStart (String topic) {
    // to be done by subclasses
  }

  public void publishTopicEnd (String topic) {
    // to be done by subclasses
  }

  public boolean hasToReportStatistics() {
    for (String s : finishedTopics) {
      if ("statistics".equalsIgnoreCase(s)){
        return true;
      }
    }
    return false;
  }

  //--- open/close streams etc
  protected void openChannel(){}
  protected void closeChannel(){}

  //--- if you have different preferences about when to report things, override those
  public void publishStart() {
    for (String topic : startTopics) {
      if ("jpf".equalsIgnoreCase(topic)){
        publishJPF();
      } else if ("platform".equalsIgnoreCase(topic)){
        publishPlatform();
      } else if ("user".equalsIgnoreCase(topic)) {
      } else if ("dtg".equalsIgnoreCase(topic)) {
        publishDTG();
      } else if ("config".equalsIgnoreCase(topic)){
        publishJPFConfig();
      } else if ("sut".equalsIgnoreCase(topic)){
        publishSuT();
      }
    }

    if (extensions != null) {
      for (PublisherExtension e : extensions) {
        e.publishStart(this);
      }
    }
  }

  public void publishTransition() {
    // nothing here, probably just for non-stream publishers (updating statistics etc.)
    if (extensions != null) {
      for (PublisherExtension e : extensions) {
        e.publishTransition(this);
      }
    }
  }

  public void publishConstraintHit() {
    for (String topic : constraintTopics) {
      if ("constraint".equalsIgnoreCase(topic)) {
        publishConstraint();
      } else if ("trace".equalsIgnoreCase(topic)){
        publishTrace();
      } else if ("snapshot".equalsIgnoreCase(topic)){
        publishSnapshot();
      } else if ("output".equalsIgnoreCase(topic)){
        publishOutput();
      } else if ("statistics".equalsIgnoreCase(topic)){
        publishStatistics(); // not sure if that is good for anything
      }
    }

    if (extensions != null) {
      for (PublisherExtension e : extensions) {
        e.publishConstraintHit(this);
      }
    }
  }

  public void publishPropertyViolation() {

    for (String topic : propertyViolationTopics) {
      if ("error".equalsIgnoreCase(topic)) {
        publishError();
      } else if ("trace".equalsIgnoreCase(topic)){
        publishTrace();
      } else if ("snapshot".equalsIgnoreCase(topic)){
        publishSnapshot();
      } else if ("output".equalsIgnoreCase(topic)){
        publishOutput();
      } else if ("statistics".equalsIgnoreCase(topic)){
        publishStatistics(); // not sure if that is good for anything
      }
    }

    if (extensions != null) {
      for (PublisherExtension e : extensions) {
        e.publishPropertyViolation(this);
      }
    }

  }

  public void publishFinished() {
    if (extensions != null) {
      for (PublisherExtension e : extensions) {
        e.publishFinished(this);
      }
    }

    for (String topic : finishedTopics) {
      if ("result".equalsIgnoreCase(topic)){
        publishResult();
      } else if ("statistics".equalsIgnoreCase(topic)){
        publishStatistics();
      }
    }
  }

  protected void publishProlog() {} // XML headers etc
  protected void publishEpilog() {} // likewise at the end

  //--- our standard topics (only placeholders here)
  protected void publishJPF() {}
  protected void publishJPFConfig() {}
  protected void publishPlatform() {}
  protected void publishUser() {}
  protected void publishDTG() {}
  protected void publishJava() {}
  protected void publishSuT() {}
  protected void publishResult() {}
  protected void publishError() {}
  protected void publishConstraint() {}
  protected void publishTrace() {}
  protected void publishOutput() {}
  protected void publishSnapshot() {}
  protected void publishStatistics() {}

  //--- internal helpers
}
