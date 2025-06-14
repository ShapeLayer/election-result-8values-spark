# 8values Analysis of Election Results

_A project task for &lt;Distributed Systems&gt; lecture in computer engineering undergraduate course program._

## Getting Started

### Requirements

- Apache Spark
- Apache Hadoop (optional)

- Korea National assembly election results  
    - [2020 National Assembly Election Results](https://www.nec.go.kr/portal/bbs/view/B0000101/20200410/)

### Build
```
./gradlew shadowJar
```

### Run
```
java -jar election-result-8values-1.0-SNAPSHOT-all.jar --input=<path> --spark
```
