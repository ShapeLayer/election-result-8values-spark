package me.jonghyeon.election_result_8values.controllers;

import me.jonghyeon.election_result_8values.Main;
import me.jonghyeon.election_result_8values.commons.Defaults;
import me.jonghyeon.election_result_8values.commons.PartySupports;
import me.jonghyeon.election_result_8values.models.Party;
import me.jonghyeon.election_result_8values.models.VoteCounted;
import me.jonghyeon.election_result_8values.models.Struct8Values;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

public class SparkHandler {
  SparkSession spark;

  public SparkHandler() {
    this.spark = SparkSession
        .builder()
        .appName(Defaults.DEFAULT_SPARK_APPNAME)
        .master(Defaults.DEFAULT_SPARK_MASTER)
        .getOrCreate();
  }

  public Map<String, Map<String, Map<String, Struct8Values>>> proc(
    List<List<VoteCounted>> parsed
  ) {
    Map<Party, Struct8Values> partyValues = PartySupports.partyEnumTo8Values;

    List<VoteCounted> flattenedData = new ArrayList<>();
    for (List<VoteCounted> votesCountedList : parsed) {
      flattenedData.addAll(votesCountedList);
    }

    JavaSparkContext sc = new JavaSparkContext(spark.sparkContext());
    JavaRDD<VoteCounted> voteRDD = sc.parallelize(flattenedData);

    Dataset<Row> voteDF = createVoteRDDDataFrame(voteRDD);

    System.out.println("=== Processing Struct8Values Calculation by Region (Area/City/Ward) ===");
    
    Dataset<Row> regionTotals = voteDF
            .groupBy("regionArea", "regionCity", "regionWard")
            .agg(org.apache.spark.sql.functions.sum("count").alias("totalVotes"));

    Dataset<Row> partyRegionVotes = voteDF
            .groupBy("regionArea", "regionCity", "regionWard", "candidate")
            .agg(org.apache.spark.sql.functions.sum("count").alias("partyVotes"));

    Dataset<Row> partyRegionPercentages = partyRegionVotes
            .join(regionTotals, new String[]{"regionArea", "regionCity", "regionWard"})
            .withColumn("percentage", 
                org.apache.spark.sql.functions.col("partyVotes")
                    .divide(org.apache.spark.sql.functions.col("totalVotes")));

    System.out.println("Party votes by region with percentages:");
    partyRegionPercentages.show();

    List<Row> results = partyRegionPercentages.collectAsList();
    
    Map<String, Map<String, Map<String, Struct8Values>>> regionResults = new java.util.HashMap<>();
    
    for (Row row : results) {
      String regionArea = row.getString(0);
      String regionCity = row.getString(1);
      String regionWard = row.getString(2);
      String candidate = row.getString(3);
      long partyVotes = row.getLong(4);
      long totalVotes = row.getLong(5);
      double percentage = row.getDouble(6);
      
      if (!regionResults.containsKey(regionArea)) {
        regionResults.put(regionArea, new java.util.HashMap<>());
      }
      if (!regionResults.get(regionArea).containsKey(regionCity)) {
        regionResults.get(regionArea).put(regionCity, new java.util.HashMap<>());
      }
      if (!regionResults.get(regionArea).get(regionCity).containsKey(regionWard)) {
        regionResults.get(regionArea).get(regionCity).put(regionWard, new Struct8Values(0, 0, 0, 0));
      }
      
      Party matchingParty = null;
      for (Party party : partyValues.keySet()) {
        if (PartySupports.toString(party).equals(candidate)) {
          matchingParty = party;
          break;
        }
      }
      
      if (matchingParty != null) {
        Struct8Values partyValue = partyValues.get(matchingParty);
        
        Struct8Values weightedValue = partyValue.mul((float) percentage);
        Struct8Values currentWardSum = regionResults.get(regionArea).get(regionCity).get(regionWard);
        regionResults.get(regionArea).get(regionCity).put(regionWard, currentWardSum.add(weightedValue));
        
        System.out.printf("Area: %s, City: %s, Ward: %s, Party: %s, Percentage: %.4f, Struct8Values: %s, Weighted: %s%n",
            regionArea, regionCity, regionWard, candidate, percentage, partyValue.toString(), weightedValue.toString());
      }
    }
    
    sc.close();
    
    System.out.println("=== Final Weighted Sum (sum(v_i * r_i)) by Region Hierarchy ===");
    for (Map.Entry<String, Map<String, Map<String, Struct8Values>>> areaEntry : regionResults.entrySet()) {
      System.out.println("Area: " + areaEntry.getKey());
      for (Map.Entry<String, Map<String, Struct8Values>> cityEntry : areaEntry.getValue().entrySet()) {
        System.out.println("  City: " + cityEntry.getKey());
        for (Map.Entry<String, Struct8Values> wardEntry : cityEntry.getValue().entrySet()) {
          System.out.println("    Ward: " + wardEntry.getKey() + " -> " + wardEntry.getValue().toString());
        }
      }
    }
    
    return regionResults;
  }

  public void processVoteCountedData(List<List<VoteCounted>> parsed, String outputPath, boolean useHadoop) {
    List<VoteCounted> flattenedData = new ArrayList<>();
    for (List<VoteCounted> votesCountedList : parsed) {
      flattenedData.addAll(votesCountedList);
    }

    JavaSparkContext sc = new JavaSparkContext(spark.sparkContext());
    JavaRDD<VoteCounted> voteRDD = sc.parallelize(flattenedData);

    Dataset<Row> voteDF = createVoteRDDDataFrame(voteRDD);

    System.out.println("=== Spark Processing Statistics ===");
    System.out.println("Total records: " + voteDF.count());
    
    Dataset<Row> candidateSummary = voteDF
            .groupBy("candidate")
            .sum("count")
            .orderBy(org.apache.spark.sql.functions.desc("sum(count)"));
    
    System.out.println("Top candidates by total votes:");
    candidateSummary.show(10);

    Dataset<Row> regionSummary = voteDF
            .groupBy("regionArea")
            .sum("count")
            .orderBy(org.apache.spark.sql.functions.desc("sum(count)"));
    
    System.out.println("Top regions by total votes:");
    regionSummary.show(10);

    String finalOutputPath = outputPath;
    if (useHadoop) {
        voteDF.coalesce(1)
                .write()
                .mode("overwrite")
                .option("header", "true")
                .csv(finalOutputPath + "_spark_processed");
    } else {
        voteDF.coalesce(1)
                .write()
                .mode("overwrite")
                .option("header", "true")
                .csv(finalOutputPath + "_spark_processed");
    }

    sc.close();
    System.out.println("Spark processed data saved to: " + finalOutputPath + "_spark_processed");
  }

  public void processAndSaveData(Object data, String outputPath, boolean useHadoop, String dataType) {
    if ("VoteCounted".equals(dataType)) {
      processVoteCountedData((List<List<VoteCounted>>) data, outputPath, useHadoop);
      return;
    }

    JavaSparkContext sc = new JavaSparkContext(spark.sparkContext());
    
    if ("Struct8Values".equals(dataType)) {
      createStruct8ValuesTable(data, outputPath, useHadoop, sc);
    }
    
    sc.close();
  }

  private void createStruct8ValuesTable(Object data, String outputPath, boolean useHadoop, JavaSparkContext sc) {
    System.out.println("=== Creating Struct8Values Table ===");
    
    StructType Struct8ValuesSchema = new StructType(new StructField[]{
      DataTypes.createStructField("economic", DataTypes.FloatType, false),
      DataTypes.createStructField("diplomatic", DataTypes.FloatType, false),
      DataTypes.createStructField("civil", DataTypes.FloatType, false),
      DataTypes.createStructField("society", DataTypes.FloatType, false)
    });

    List<Struct8Values> Struct8ValuesList;
    if (data instanceof List) {
      Struct8ValuesList = (List<Struct8Values>) data;
    } else {
      Struct8ValuesList = new ArrayList<>();
      Struct8ValuesList.add(new Struct8Values(75.5f, -25.3f, 60.2f, -40.1f));
      Struct8ValuesList.add(new Struct8Values(-30.7f, 45.8f, -20.5f, 35.9f));
      Struct8ValuesList.add(new Struct8Values(15.2f, -60.4f, 80.1f, -15.7f));
      Struct8ValuesList.add(new Struct8Values(-50.3f, 20.6f, -35.8f, 70.4f));
    }
    
    JavaRDD<Struct8Values> Struct8ValuesRDD = sc.parallelize(Struct8ValuesList);
    JavaRDD<Row> rowRDD = Struct8ValuesRDD.map(v8 -> 
      org.apache.spark.sql.RowFactory.create(
        v8.economic,
        v8.diplomatic,
        v8.civil,
        v8.society
      )
    );
    
    Dataset<Row> Struct8ValuesDF = spark.createDataFrame(rowRDD, Struct8ValuesSchema);
    
    System.out.println("Struct8Values table created with " + Struct8ValuesDF.count() + " records");
    
    System.out.println("Struct8Values Statistics:");
    Struct8ValuesDF.describe().show();
    
    Dataset<Row> avgValues = Struct8ValuesDF.select(
      org.apache.spark.sql.functions.avg("economic").alias("avg_economic"),
      org.apache.spark.sql.functions.avg("diplomatic").alias("avg_diplomatic"),
      org.apache.spark.sql.functions.avg("civil").alias("avg_civil"),
      org.apache.spark.sql.functions.avg("society").alias("avg_society")
    );
    
    System.out.println("Average Struct8Values:");
    avgValues.show();
    
    Struct8ValuesDF.show();
    
    String Struct8ValuesOutputPath = outputPath + "_Struct8Values";
    if (useHadoop) {
      Struct8ValuesDF.coalesce(1)
              .write()
              .mode("overwrite")
              .option("header", "true")
              .csv(Struct8ValuesOutputPath);
    } else {
      Struct8ValuesDF.coalesce(1)
              .write()
              .mode("overwrite")
              .option("header", "true")
              .csv(Struct8ValuesOutputPath);
    }
    
    System.out.println("Struct8Values table saved to: " + Struct8ValuesOutputPath);
  }

  private Dataset<Row> createStruct8ValuesRDDDataFrame(JavaRDD<Struct8Values> Struct8ValuesRDD) {
    StructType schema = new StructType(new StructField[]{
      DataTypes.createStructField("economic", DataTypes.FloatType, false),
      DataTypes.createStructField("diplomatic", DataTypes.FloatType, false),
      DataTypes.createStructField("civil", DataTypes.FloatType, false),
      DataTypes.createStructField("society", DataTypes.FloatType, false)
    });

    JavaRDD<Row> rowRDD = Struct8ValuesRDD.map(v8 -> 
      org.apache.spark.sql.RowFactory.create(
        v8.economic,
        v8.diplomatic,
        v8.civil,
        v8.society
      )
    );

    return spark.createDataFrame(rowRDD, schema);
  }

  private Dataset<Row> createVoteRDDDataFrame(JavaRDD<VoteCounted> voteRDD) {
    StructType schema = new StructType(new StructField[]{
      DataTypes.createStructField("voteType", DataTypes.StringType, false),
      DataTypes.createStructField("regionArea", DataTypes.StringType, false),
      DataTypes.createStructField("regionCity", DataTypes.StringType, false),
      DataTypes.createStructField("regionWard", DataTypes.StringType, false),
      DataTypes.createStructField("candidate", DataTypes.StringType, false),
      DataTypes.createStructField("count", DataTypes.LongType, false)
    });

    JavaRDD<Row> rowRDD = voteRDD.map(vote -> 
      org.apache.spark.sql.RowFactory.create(
        vote.voteType,
        vote.regionArea,
        vote.regionCity,
        vote.regionWard,
        vote.candidate,
        vote.count
      )
    );

    return spark.createDataFrame(rowRDD, schema);
  }

  public void close() {
    if (spark != null) {
      spark.stop();
    }
  }
}
