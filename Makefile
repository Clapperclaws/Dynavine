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

jar: $(classes)
	jar cvf fast-mule.jar $(classes) 

clean:
	$(RM) *.class
