ANTLR=./AntlrJARs/antlr-4.9.3-complete.jar
GRAMMAR=./Huginn.g4

EXAMPLE_FILENAME=example
EXAMPLE_EXTENSION=.hg

build: gen compile

gen:
	java -jar $(ANTLR) -o out_gen $(GRAMMAR)   

compile:
	javac -cp $(ANTLR):out_gen:. Main.java -d out_compile

test:
	java -cp $(ANTLR):out_compile:. Main $(EXAMPLE_FILENAME)$(EXAMPLE_EXTENSION) > $(EXAMPLE_FILENAME).ll
	lli $(EXAMPLE_FILENAME).ll

clear:
	rm -R out_gen
	rm -R out_compile
	rm $(EXAMPLE_FILENAME).ll
