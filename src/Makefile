JFLAGS = -g
JC = javac
.SUFFIXES: .java .class
.java.class:
	$(JC) $(JFLAGS) $*.java

CLASSES = \
					CreateInitialSolution.java \
					Dijkstra.java \
					Driver.java \
					EndPoint.java \
					Graph.java \
					OverlayMapping.java \
					Solutions.java \
					Tuple.java

default: classes

classes: $(CLASSES:.java=.class)

jar:
	@echo "Manifest-Version: 1.0" > manifest.txt
	@echo "Class-Path: ." >> manifest.txt
	@echo "Main-Class: Driver" >> manifest.txt
	@echo "" >> manifest.txt
	jar -cmf manifest.txt fast-mule.jar $(classes) 

clean:
	$(RM) *.class
