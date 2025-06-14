package me.jonghyeon.election_result_8values.models;

import java.io.Serializable;

public class VoteCounted implements Serializable {
  public String voteType;
  public String regionArea;
  public String regionCity;
  public String regionWard;
  public String candidate;
  public long count;

  public VoteCounted(
    String voteType,
    String regionArea,
    String regionCity,
    String regionWard,
    String candidate,
    long count
  ) {
    this.voteType = voteType;
    this.regionArea = regionArea;
    this.regionCity = regionCity;
    this.regionWard = regionWard;
    this.candidate = candidate;
    this.count = count;
  }

  public String getCandidate() {
    return candidate;
  }
  public long getCount() {
    return count;
  }

  @Override
  public String toString() {
    return "VoteCounted{" +
      "voteType='" + voteType + '\'' +
      ", regionArea='" + regionArea + '\'' +
      ", regionCity='" + regionCity + '\'' +
      ", regionWard='" + regionWard + '\'' +
      ", candidate='" + candidate + '\'' +
      ", count=" + count +
      '}';
  }

  public String getRegionName() {
    return regionWard;
  }
}
