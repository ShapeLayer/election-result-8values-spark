package me.jonghyeon.election_result_8values.commons;

public class Defaults {
  public static final String DEFAULT_HDFS_NAMENODE = "hdfs://localhost:9000";
  public static final String DEFAULT_OUTPUT_PATH = "/election_result/output";
  public static final String DEFAULT_RESULT_REPORT_NAME = "result.txt";
  public static final String DEFAULT_INPUT_PATH = "/election_result/input";
  public static final String DEFAULT_HDFS_INPUT_PATH = "/election_result/input/hdfs";
  public static final String DEFAULT_LOCAL_INPUT_PATH = "/election_result/input/local";
  public static final String DEFAULT_LOCAL_OUTPUT_PATH = "/election_result/output/local";

  public static final String DEFAULT_SPARK_MASTER = "local";
  public static final String DEFAULT_SPARK_APPNAME = "ElectionAnalyzer";
  public static final String DEFAULT_SPARK_HOME = "/usr/lib/spark/";
}
