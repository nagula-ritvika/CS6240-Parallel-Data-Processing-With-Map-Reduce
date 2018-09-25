HADOOP_VERSION=2.8.1
SCALA_HOME = /usr/local/scala-2.11.8
MY_CLASSPATH=${HADOOP_HOME}/share/hadoop/common/hadoop-common-${HADOOP_VERSION}.jar:${SPARK_HOME}/jars/*:${HADOOP_HOME}/share/hadoop/mapreduce/*:out:.
SPARK_HOME= /usr/local/spark
CLASSNAME=neu.pdpmr.project.Model

PROJECT_BASE=neu/pdpmr/project/src/main/scala
INPUT_FOLDER=input
OUTPUT_FOLDER=output
JAR_NAME=model-futai-ritvika-shikha.jar
MAIN_CLASS=ParseSongsResultsCollect

all: build run

build:
	${SCALA_HOME}/bin/scalac -cp ${MY_CLASSPATH} -d ${JAR_NAME} ${PROJECT_BASE}/*.scala

run:
	$(SPARK_HOME)/bin/spark-submit --class $(CLASSNAME) $(JAR_NAME)

report:
	Rscript -e "rmarkdown::render('Report.Rmd')"

clean:
	rm -rf out

gzip:
	-gzip output/*

gunzip:
	-gunzip input/MillionSongSubset/*