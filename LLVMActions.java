import java.util.*;

public class LLVMActions extends HuginnBaseListener {

    HashSet<Variable> variables = new HashSet<Variable>();
    Stack<String> stack = new Stack<String>();

    public LLVMActions()
    {
        
    }

	@Override public void exitProg(HuginnParser.ProgContext ctx) { 
        System.out.println(LLVMGenerator.generate());
    }

	@Override public void exitDeclaration(HuginnParser.DeclarationContext ctx) {
        String ID = ctx.assignment().ID().getText();
        boolean isInteger = ctx.INTEGER_NAME() != null;
        boolean isReal = ctx.REAL_NAME() != null;

        if (variables.stream().filter(v -> v.name == ID).count() == 1)
        {
            raiseError(ctx.getStart().getLine(), "Variable <" + ID + "> is already defined.");
        }

        if (isInteger)
        {
            String value = ctx.assignment().INTEGER().getText();
            variables.add(new Variable(ID, VariableType.INTEGER));
            LLVMGenerator.allocateInteger(ID);
            LLVMGenerator.assignInteger(ID, value);
        }
        else if (isReal)
        {
            String value = ctx.assignment().REAL().getText();
            variables.add(new Variable(ID, VariableType.REAL));
            LLVMGenerator.allocateReal(ID);
            LLVMGenerator.assignReal(ID, value);
        }
    }

	@Override public void exitAssignment(HuginnParser.AssignmentContext ctx) {
        if (ctx.getParent() instanceof HuginnParser.DeclarationContext)
        {
            return;
        }

        String ID = ctx.ID().getText();
        VariableType variableType = ctx.INTEGER() != null ? VariableType.INTEGER : VariableType.REAL;

        if (variables.stream().filter(v -> v.name == ID).count() != 1)
        {
            raiseError(ctx.getStart().getLine(), "Variable <" + ID + "> is not defined. Cannot assign value to it.");
        }

        if (variables.stream().filter(v -> v.name == ID && v.variableType == variableType).count() != 1)
        {
            raiseError(ctx.getStart().getLine(), "Variable <" + ID + "> has incorrect type. Cannot assign value to it.");
        }

        switch (variableType)
        {
            case INTEGER:
                LLVMGenerator.assignInteger(ID, ctx.INTEGER().getText());
            break;

            case REAL:
                LLVMGenerator.assignReal(ID, ctx.REAL().getText());
            break;
        }
    }

	@Override public void exitPrint(HuginnParser.PrintContext ctx) {
        String ID = ctx.ID().getText();

        Optional<Variable> variable = variables.stream().filter(v -> v.name == ID).findFirst();

        if (!variable.isPresent())
        {
            raiseError(ctx.getStart().getLine(), "Variable <" + ID + "> is not defined.");
        }

        switch (variable.get().variableType)
        {
            case INTEGER:
                LLVMGenerator.printInteger(ID);
            break;

            case REAL:
                LLVMGenerator.printReal(ID);
            break;
        }
    }

	@Override public void exitRead(HuginnParser.ReadContext ctx) { }

    private void raiseError(int line, String msg) {
       System.err.println("Error in line " + line + ", " + msg);
       System.exit(1);
    }

    private class Variable {
        public String name;
        public VariableType variableType;

        public Variable(String name, VariableType variableType)
        {
            this.name = name;
            this.variableType = variableType;
        } 
    }

    private enum VariableType {
        INTEGER,
        REAL
    }
}
