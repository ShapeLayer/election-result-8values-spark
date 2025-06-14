package me.jonghyeon.election_result_8values;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;
import me.jonghyeon.election_result_8values.commons.Defaults;
import me.jonghyeon.election_result_8values.commons.Env;
import me.jonghyeon.election_result_8values.commons.PartySupports;
import me.jonghyeon.election_result_8values.controllers.*;
import me.jonghyeon.election_result_8values.models.VoteCounted;
import me.jonghyeon.election_result_8values.models.Struct8Values;
import org.apache.hadoop.fs.FileSystem;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

class MainArgs {
  public static final String USAGE = "ElectionResult8Values --input=<input-path> [--hadoop] [--spark] [--output=<hdfs-output-path>] [--namenode=<hdfs-namenode-url>]";

  public boolean useHadoop = false;
  public boolean useSpark = false;
  public String[] inputPaths;
  public String outputPath;
  public String nameNode;

  public static MainArgs from(String[] args) {
    MainArgs mainArgs = new MainArgs();
    for (String arg : args) {
      if (arg.startsWith("--input=")) {
        mainArgs.inputPaths = parsePathArr(
            arg.split("=", 2)[1]
        );
      } else if (arg.equals("--hadoop")) {
        mainArgs.useHadoop = true;
      } else if (arg.equals("--spark")) {
        mainArgs.useSpark = true;
      } else if (arg.startsWith("--output=")) {
        mainArgs.outputPath = arg.split("=", 2)[1];
      } else if (arg.startsWith("--namenode=")) {
        mainArgs.nameNode = arg.split("=", 2)[1];
      }
    }

    if (!mainArgs.validate()) {
      System.err.println(USAGE);
      System.exit(1);
    }

    mainArgs.fillDefaults();
    mainArgs.validate();
    return mainArgs;
  }

  public void fillDefaults() {
    if (this.outputPath == null) {
      this.outputPath = this.useHadoop ? Defaults.DEFAULT_OUTPUT_PATH : "." + Defaults.DEFAULT_LOCAL_OUTPUT_PATH;
      this.outputPath += "/" + Defaults.DEFAULT_RESULT_REPORT_NAME;
    }
    if (this.nameNode == null) {
      this.nameNode = Defaults.DEFAULT_HDFS_NAMENODE;
    }
  }

  public static String[] parsePathArr(String path) {
    return path.split(",");
  }

  public boolean validate() {
    boolean result = true;
    result &= inputPaths != null;

    return result;
  }
}

public class Main {
  public static Env env = new Env();

  public static void main(String[] args) throws IOException {
    // Parse args
    MainArgs mainArgs = MainArgs.from(args);

    List<List<VoteCounted>> parsed = null;

    // Input
    FileSystem fs = null;  // used when mainArgs.useHadoop is true only

    System.out.println("Input paths: " + String.join(", ", mainArgs.inputPaths));
    if (!mainArgs.useHadoop) {
      System.out.println("Running in local mode. Use --hadoop to run on HDFS.");
      parsed = LocalExcelHandler.parseAll(mainArgs.inputPaths);
    } else {
      System.out.println("Using HDFS NameNode: " + mainArgs.nameNode);
      fs = HdfsConfig.getFileSystem(mainArgs.nameNode);
      parsed = HDFSExcelHandler.parseAll(mainArgs.inputPaths, fs);
    }

    // Output
    System.out.println("Output path: " + mainArgs.outputPath);

    if (mainArgs.useSpark) {
      System.out.println("Using Apache Spark for data processing...");
      SparkHandler sparkHandler = new SparkHandler();
      try {
        Map<String, Map<String, Map<String, Struct8Values>>> results = sparkHandler.proc(parsed);
        System.out.println("=== Processing Complete ===");
        System.out.println("Final calculated Struct8Values results by region:");
        for (Entry<String, Map<String, Map<String, Struct8Values>>> entry : results.entrySet()) {
          for (Entry<String, Map<String, Struct8Values>> subEntry : entry.getValue().entrySet()) {
            System.out.println("Region: " + entry.getKey() + ", Area: " + subEntry.getKey());
            for (Entry<String, Struct8Values> valuesEntry : subEntry.getValue().entrySet()) {
              System.out.println("  Candidate: " + valuesEntry.getKey() + ", Struct8Values: " + valuesEntry.getValue());
            }
          }
          System.out.println("Region " + entry.getKey() + ": " + entry.getValue().toString());
        }
        
        // Write results to file
        PrintWriter writer = null;
        try {
          if (!mainArgs.useHadoop) {
            java.nio.file.Path outputPath = Paths.get(mainArgs.outputPath);
            if (!Files.exists(outputPath.getParent())) {
              Files.createDirectories(outputPath.getParent());
            }
            writer = new PrintWriter(Files.newBufferedWriter(outputPath));
          } else {
            org.apache.hadoop.fs.Path outputPath = new org.apache.hadoop.fs.Path(mainArgs.outputPath);
            if (!fs.exists(outputPath.getParent())) {
              fs.mkdirs(outputPath.getParent());
            }
            writer = new PrintWriter(fs.create(outputPath, true));
          }
          
          writer.println("Region,Economic,Diplomatic,Civil,Society");
            // Flatten and write all entries to CSV
            for (Entry<String, Map<String, Map<String, Struct8Values>>> regionEntry : results.entrySet()) {
              String region = regionEntry.getKey();
              
              for (Entry<String, Map<String, Struct8Values>> areaEntry : regionEntry.getValue().entrySet()) {
                String area = areaEntry.getKey();
                
                for (Entry<String, Struct8Values> candidateEntry : areaEntry.getValue().entrySet()) {
                  String candidate = candidateEntry.getKey();
                  Struct8Values values = candidateEntry.getValue();
                  
                  writer.println(String.format("%s,%.4f,%.4f,%.4f,%.4f",
                    "\"" + region + "," + area + "," + candidate + "\"",
                    values.economic, 
                    values.diplomatic, 
                    values.civil, 
                    values.society));
                }
              }
            }
        } finally {
          if (writer != null) {
            writer.close();
          }
        }
      } finally {
        sparkHandler.close();
      }
    } else {
      // Original CSV output logic
      PrintWriter writer = null;
      if (!mainArgs.useHadoop) {
        java.nio.file.Path outputPath = Paths.get(mainArgs.outputPath);

        if (!Files.exists(outputPath.getParent())) {
          Files.createDirectories(outputPath.getParent());
        }
        writer = new PrintWriter(Files.newBufferedWriter(outputPath));
      } else {
        org.apache.hadoop.fs.Path outputPath = new org.apache.hadoop.fs.Path(mainArgs.outputPath);

        if (!fs.exists(outputPath.getParent())) {
          fs.mkdirs(outputPath.getParent());
        }
        writer = new PrintWriter(fs.create(outputPath, true));
      }
      
      writer.println("VoteType,RegionArea,RegionCity,RegionWard,Candidate,Count");
      
      for (List<VoteCounted> votesCountedList : parsed) {
        for (VoteCounted voteCounted : votesCountedList) {
          writer.println(String.format("%s,%s,%s,%s,%s,%d",
              voteCounted.voteType,
              voteCounted.regionArea,
              voteCounted.regionCity,
              voteCounted.regionWard,
              voteCounted.candidate,
              voteCounted.count));
        }
      }
      
      writer.close();
    }

    if (mainArgs.useHadoop && fs != null) {
      fs.close();
    }
    System.out.println("Processing completed. Results written to: " + mainArgs.outputPath);
  }
}
