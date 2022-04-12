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
        String ID = ctx.ID().getText();

        if (variables.stream().filter(v -> v.name.equals(ID)).findFirst().isPresent()) {
            raiseError(ctx.getStart().getLine(), "Variable <" + ID + "> is already defined.");
        }

        if (ctx.INTEGER_NAME() != null && ctx.INTEGER() == null) {
            raiseError(ctx.getStart().getLine(), "Mismatched declared type and assigned value of variable " + ID + ".");
        }

        if (ctx.REAL_NAME() != null && ctx.REAL() == null) {
            raiseError(ctx.getStart().getLine(), "Mismatched declared type and assigned value of variable " + ID + ".");
        }

        if (ctx.INTEGER() != null)
        {
            variables.add(new Variable(ID, VariableType.INTEGER));
            LLVMGenerator.allocateInteger(ID);
            LLVMGenerator.assignInteger(ID, ctx.INTEGER().getText());
        }

        if (ctx.REAL() != null)
        {
            variables.add(new Variable(ID, VariableType.REAL));
            LLVMGenerator.allocateReal(ID);
            LLVMGenerator.assignReal(ID, ctx.REAL().getText());
        }
    }

	@Override public void exitAssignment(HuginnParser.AssignmentContext ctx) {
        String ID = ctx.ID(0).getText();

        Optional<Variable> var = variables.stream().filter(v -> v.name.equals(ID)).findFirst();

        if (!var.isPresent()) {
            raiseError(ctx.getStart().getLine(), "Variable <" + ID + "> is not defined.");
        }

        if (ctx.INTEGER() != null && var.get().variableType != VariableType.INTEGER) {
            raiseError(ctx.getStart().getLine(), "Mismatched declared type and assigned value of variable " + ID + ".");
        }

        if (ctx.REAL() != null && var.get().variableType != VariableType.REAL) {
            raiseError(ctx.getStart().getLine(), "Mismatched declared type and assigned value of variable " + ID + ".");
        }

        String sourceID = ctx.ID(1) != null ? ctx.ID(1).getText() : "";
        Optional<Variable> sourceVar = variables.stream().filter(v -> v.name.equals(sourceID)).findFirst();
        if (!sourceID.equals("") && !sourceVar.isPresent()) {
            raiseError(ctx.getStart().getLine(), "Variable <" + sourceID + "> is not defined.");
        }

        if (sourceVar.isPresent() && sourceVar.get().variableType != var.get().variableType) {
            raiseError(ctx.getStart().getLine(), "Mismatched types of " + ID + " and " + sourceID + ".");
        }

        if (ctx.INTEGER() != null) {
            LLVMGenerator.assignInteger(ID, ctx.INTEGER().getText());
        }

        if (ctx.REAL() != null) {
            LLVMGenerator.assignReal(ID, ctx.REAL().getText());
        }

        if (sourceVar.isPresent()) {
            switch (sourceVar.get().variableType)
            {
                case INTEGER:
                    LLVMGenerator.loadInteger(sourceVar.get().name);
                    LLVMGenerator.assignInteger(ID, "%" + (LLVMGenerator.reg - 1));
                break;

                case REAL:
                    LLVMGenerator.loadReal(sourceVar.get().name);
                    LLVMGenerator.assignReal(ID, "%" + (LLVMGenerator.reg - 1));
                break;
            }
        }
    }

	@Override public void exitPrint(HuginnParser.PrintContext ctx) {
        String ID = ctx.ID().getText();
    
        Optional<Variable> var = variables.stream().filter(v -> v.name.equals(ID)).findFirst();

        if (!var.isPresent()) {
            raiseError(ctx.getStart().getLine(), "Variable <" + ID + "> is not defined.");
        }

        switch (var.get().variableType)
        {
            case INTEGER:
                LLVMGenerator.printInteger(ID);
            break;

            case REAL:
                LLVMGenerator.printReal(ID);
            break;
        }
    }

	@Override public void exitRead(HuginnParser.ReadContext ctx) {
        String ID = ctx.ID().getText();
    
        Optional<Variable> var = variables.stream().filter(v -> v.name.equals(ID)).findFirst();

        if (!var.isPresent()) {
            raiseError(ctx.getStart().getLine(), "Variable <" + ID + "> is not defined.");
        }

        switch (var.get().variableType)
        {
            case INTEGER:
                LLVMGenerator.readInteger(ID);
            break;

            case REAL:
                LLVMGenerator.readReal(ID);
            break;
        }
    }

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
