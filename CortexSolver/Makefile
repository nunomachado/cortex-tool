CC=g++
CFLAGS=-std=c++11 -c
LDFLAGS=
SOURCES=Common.cpp ConstraintModelGenerator.cpp GraphvizGenerator.cpp JPFParser.cpp KQueryParser.cpp main.cpp Operations.cpp Parameters.cpp Types.cpp TraceAnalyzer.cpp Trie.cpp Util.cpp Schedule.cpp Z3Solver.cpp 
OBJECTS=$(SOURCES:.cpp=.o)
EXECUTABLE=cortexSolver


all: $(SOURCES) $(EXECUTABLE)

$(EXECUTABLE): $(OBJECTS)
	$(CC) $(LDFLAGS) $(OBJECTS) -o $@

.cpp.o:
	$(CC) $(CFLAGS) $< -o $@

clean:
	rm *.o $(EXECUTABLE)
