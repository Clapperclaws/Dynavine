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

clean:
	$(RM) *.class
