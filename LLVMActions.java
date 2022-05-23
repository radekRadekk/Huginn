import java.util.*;
import java.util.regex.*;

public class LLVMActions extends HuginnBaseListener {

    HashSet<Variable> variables = new HashSet<Variable>();
    Stack<Variable> stack = new Stack<Variable>();

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

        if (ctx.BOOL_NAME() != null && ctx.BOOL() == null) {
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

        if (ctx.BOOL() != null)
        {
            variables.add(new Variable(ID, VariableType.BOOL));
            LLVMGenerator.allocateBool(ID);
            LLVMGenerator.assignBool(ID, ctx.BOOL().getText());
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

        if (ctx.BOOL() != null && var.get().variableType != VariableType.BOOL) {
            raiseError(ctx.getStart().getLine(), "Mismatched declared type and assigned value of variable " + ID + ".");
        }

        if (ctx.expression() != null && stack.peek().variableType != var.get().variableType) {
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

        if (ctx.BOOL() != null) {
            LLVMGenerator.assignBool(ID, ctx.BOOL().getText());
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

                case BOOL:
                    LLVMGenerator.loadBool(sourceVar.get().name);
                    LLVMGenerator.assignBool(ID, "%" + (LLVMGenerator.reg - 1));
                break;
            }
        }

        if (ctx.expression() != null) {
            Variable storedVariable = stack.pop();
            switch (storedVariable.variableType)
            {
                case INTEGER:
                    LLVMGenerator.assignInteger(ID, "%" + storedVariable.name);
                break;

                case REAL:
                    LLVMGenerator.assignReal(ID, "%" + storedVariable.name);
                break;

                case BOOL:
                    LLVMGenerator.assignBool(ID, "%" + storedVariable.name);
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

            case BOOL:
                LLVMGenerator.printBool(ID);
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

    @Override public void exitExpression_base_add(HuginnParser.Expression_base_addContext ctx) {
        if (ctx.INTEGER().size() == 2) {
            LLVMGenerator.addIntegers(ctx.INTEGER(0).getText(), ctx.INTEGER(1).getText());
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.INTEGER));
        }

        if (ctx.REAL().size() == 2) {
            LLVMGenerator.addReals(ctx.REAL(0).getText(), ctx.REAL(1).getText());
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.REAL));                
        }

        if (ctx.ID().size() == 1 && ctx.INTEGER().size() == 1) {
            Optional<Variable> var = variables.stream().filter(v -> v.name.equals(ctx.ID(0).getText())).findFirst();

            System.out.println("dupa");
            if (!var.isPresent()) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(0).getText() + "> is not defined.");
            }

            if (var.get().variableType != VariableType.INTEGER) {
                raiseError(ctx.getStart().getLine(), "Mismatched types in the expression. <" + var.get().name + "> must be INTEGER.");
            }

            LLVMGenerator.loadInteger(var.get().name);
            LLVMGenerator.addIntegers(ctx.INTEGER(0).getText(), "%" + (LLVMGenerator.reg - 1));
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.INTEGER));
        }

        if (ctx.ID().size() == 1 && ctx.REAL().size() == 1) {
            Optional<Variable> var = variables.stream().filter(v -> v.name.equals(ctx.ID(0).getText())).findFirst();

            if (!var.isPresent()) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(0).getText() + "> is not defined.");
            }

            if (var.get().variableType != VariableType.REAL) {
                raiseError(ctx.getStart().getLine(), "Mismatched types in the expression. <" + var.get().name + "> must be REAL.");
            }

            LLVMGenerator.loadReal(var.get().name);
            LLVMGenerator.addReals(ctx.REAL(0).getText(), "%" + (LLVMGenerator.reg - 1));
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.REAL));
        }

        if (ctx.ID().size() == 2) {
            Optional<Variable> var0 = variables.stream().filter(v -> v.name.equals(ctx.ID(0).getText())).findFirst();
            Optional<Variable> var1 = variables.stream().filter(v -> v.name.equals(ctx.ID(1).getText())).findFirst();

            if (!var0.isPresent()) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(0).getText() + "> is not defined.");
            }

            if (!var1.isPresent()) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(1).getText() + "> is not defined.");
            }

            if (var0.get().variableType != var1.get().variableType) {
                raiseError(ctx.getStart().getLine(), "Mismatched types in expression. Variables <" + ctx.ID(0).getText() + "> and <" + ctx.ID(1).getText() + "> have different types.");
            }

            switch (var0.get().variableType) {
                case INTEGER:
                    LLVMGenerator.loadInteger(var0.get().name);
                    LLVMGenerator.loadInteger(var1.get().name);
                    LLVMGenerator.addIntegers("%" + (LLVMGenerator.reg - 2), "%" + (LLVMGenerator.reg - 1));
                    stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.INTEGER));
                break;

                case REAL:
                    LLVMGenerator.loadReal(var0.get().name);
                    LLVMGenerator.loadReal(var1.get().name);
                    LLVMGenerator.addReals("%" + (LLVMGenerator.reg - 2), "%" + (LLVMGenerator.reg - 1));
                    stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.REAL));
                break;
            }
        }
    }

    @Override public void exitExpression_base_mul(HuginnParser.Expression_base_mulContext ctx) {
        if (ctx.INTEGER().size() == 2) {
            LLVMGenerator.mulIntegers(ctx.INTEGER(0).getText(), ctx.INTEGER(1).getText());
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.INTEGER));
        }

         if (ctx.REAL().size() == 2) {
            LLVMGenerator.mulReals(ctx.REAL(0).getText(), ctx.REAL(1).getText());
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.REAL));                
        }

         if (ctx.ID().size() == 1 && ctx.INTEGER().size() == 1) {
            Optional<Variable> var = variables.stream().filter(v -> v.name.equals(ctx.ID(0).getText())).findFirst();

             if (!var.isPresent()) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(0).getText() + "> is not defined.");
            }

             if (var.get().variableType != VariableType.INTEGER) {
                raiseError(ctx.getStart().getLine(), "Mismatched types in the expression. <" + var.get().name + "> must be INTEGER.");
            }

             LLVMGenerator.loadInteger(var.get().name);
            LLVMGenerator.mulIntegers(ctx.INTEGER(0).getText(), "%" + (LLVMGenerator.reg - 1));
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.INTEGER));
        }

         if (ctx.ID().size() == 1 && ctx.REAL().size() == 1) {
            Optional<Variable> var = variables.stream().filter(v -> v.name.equals(ctx.ID(0).getText())).findFirst();

             if (!var.isPresent()) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(0).getText() + "> is not defined.");
            }

             if (var.get().variableType != VariableType.REAL) {
                raiseError(ctx.getStart().getLine(), "Mismatched types in the expression. <" + var.get().name + "> must be REAL.");
            }

             LLVMGenerator.loadReal(var.get().name);
            LLVMGenerator.mulReals(ctx.REAL(0).getText(), "%" + (LLVMGenerator.reg - 1));
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.REAL));
        }

         if (ctx.ID().size() == 2) {
            Optional<Variable> var0 = variables.stream().filter(v -> v.name.equals(ctx.ID(0).getText())).findFirst();
            Optional<Variable> var1 = variables.stream().filter(v -> v.name.equals(ctx.ID(1).getText())).findFirst();

             if (!var0.isPresent()) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(0).getText() + "> is not defined.");
            }

             if (!var1.isPresent()) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(1).getText() + "> is not defined.");
            }

             if (var0.get().variableType != var1.get().variableType) {
                raiseError(ctx.getStart().getLine(), "Mismatched types in expression. Variables <" + ctx.ID(0).getText() + "> and <" + ctx.ID(1).getText() + "> have different types.");
            }

             switch (var0.get().variableType) {
                case INTEGER:
                    LLVMGenerator.loadInteger(var0.get().name);
                    LLVMGenerator.loadInteger(var1.get().name);
                    LLVMGenerator.mulIntegers("%" + (LLVMGenerator.reg - 2), "%" + (LLVMGenerator.reg - 1));
                    stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.INTEGER));
                break;

                 case REAL:
                    LLVMGenerator.loadReal(var0.get().name);
                    LLVMGenerator.loadReal(var1.get().name);
                    LLVMGenerator.mulReals("%" + (LLVMGenerator.reg - 2), "%" + (LLVMGenerator.reg - 1));
                    stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.REAL));
                break;
            }
        }
    }

    @Override public void exitExpression_base_sub(HuginnParser.Expression_base_subContext ctx) {
        Pattern pattern = Pattern.compile("^[a-z_].*$", Pattern.CASE_INSENSITIVE);

        if (ctx.INTEGER().size() == 2) {
            LLVMGenerator.subIntegers(ctx.INTEGER(0).getText(), ctx.INTEGER(1).getText());
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.INTEGER));
        }

         if (ctx.REAL().size() == 2) {
            LLVMGenerator.subReals(ctx.REAL(0).getText(), ctx.REAL(1).getText());
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.REAL));                
        }

         if (ctx.ID().size() == 1 && ctx.INTEGER().size() == 1) {
            Optional<Variable> var = variables.stream().filter(v -> v.name.equals(ctx.ID(0).getText())).findFirst();

             if (!var.isPresent()) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(0).getText() + "> is not defined.");
            }

             if (var.get().variableType != VariableType.INTEGER) {
                raiseError(ctx.getStart().getLine(), "Mismatched types in the expression. <" + var.get().name + "> must be INTEGER.");
            }

            Matcher matcher = pattern.matcher(ctx.getText());

            LLVMGenerator.loadInteger(var.get().name);
            if (matcher.find()) {
                LLVMGenerator.subIntegers("%" + (LLVMGenerator.reg - 1), ctx.INTEGER(0).getText());
            }
            else {
                LLVMGenerator.subIntegers(ctx.INTEGER(0).getText(), "%" + (LLVMGenerator.reg - 1));
            }
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.INTEGER));
        }

         if (ctx.ID().size() == 1 && ctx.REAL().size() == 1) {
            Optional<Variable> var = variables.stream().filter(v -> v.name.equals(ctx.ID(0).getText())).findFirst();

             if (!var.isPresent()) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(0).getText() + "> is not defined.");
            }

             if (var.get().variableType != VariableType.REAL) {
                raiseError(ctx.getStart().getLine(), "Mismatched types in the expression. <" + var.get().name + "> must be REAL.");
            }


            Matcher matcher = pattern.matcher(ctx.getText());

            LLVMGenerator.loadReal(var.get().name);
            if (matcher.find()) {
                LLVMGenerator.subReals("%" + (LLVMGenerator.reg - 1), ctx.REAL(0).getText());
            }
            else {    
                LLVMGenerator.subReals(ctx.REAL(0).getText(), "%" + (LLVMGenerator.reg - 1));
            }
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.REAL));
        }

         if (ctx.ID().size() == 2) {
            Optional<Variable> var0 = variables.stream().filter(v -> v.name.equals(ctx.ID(0).getText())).findFirst();
            Optional<Variable> var1 = variables.stream().filter(v -> v.name.equals(ctx.ID(1).getText())).findFirst();

             if (!var0.isPresent()) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(0).getText() + "> is not defined.");
            }

             if (!var1.isPresent()) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(1).getText() + "> is not defined.");
            }

             if (var0.get().variableType != var1.get().variableType) {
                raiseError(ctx.getStart().getLine(), "Mismatched types in expression. Variables <" + ctx.ID(0).getText() + "> and <" + ctx.ID(1).getText() + "> have different types.");
            }

             switch (var0.get().variableType) {
                case INTEGER:
                    LLVMGenerator.loadInteger(var0.get().name);
                    LLVMGenerator.loadInteger(var1.get().name);
                    LLVMGenerator.subIntegers("%" + (LLVMGenerator.reg - 2), "%" + (LLVMGenerator.reg - 1));
                    stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.INTEGER));
                break;

                 case REAL:
                    LLVMGenerator.loadReal(var0.get().name);
                    LLVMGenerator.loadReal(var1.get().name);
                    LLVMGenerator.subReals("%" + (LLVMGenerator.reg - 2), "%" + (LLVMGenerator.reg - 1));
                    stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.REAL));
                break;
            }
        }
    }

    @Override public void exitExpression_base_div(HuginnParser.Expression_base_divContext ctx) {
        Pattern pattern = Pattern.compile("^[a-z_].*$", Pattern.CASE_INSENSITIVE);

        if (ctx.INTEGER().size() == 2) {
            LLVMGenerator.divIntegers(ctx.INTEGER(0).getText(), ctx.INTEGER(1).getText());
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.INTEGER));
        }

         if (ctx.REAL().size() == 2) {
            LLVMGenerator.divReals(ctx.REAL(0).getText(), ctx.REAL(1).getText());
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.REAL));                
        }

         if (ctx.ID().size() == 1 && ctx.INTEGER().size() == 1) {
            Optional<Variable> var = variables.stream().filter(v -> v.name.equals(ctx.ID(0).getText())).findFirst();

             if (!var.isPresent()) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(0).getText() + "> is not defined.");
            }

             if (var.get().variableType != VariableType.INTEGER) {
                raiseError(ctx.getStart().getLine(), "Mismatched types in the expression. <" + var.get().name + "> must be INTEGER.");
            }

            Matcher matcher = pattern.matcher(ctx.getText());

            LLVMGenerator.loadInteger(var.get().name);
            if (matcher.find()) {
                LLVMGenerator.divIntegers("%" + (LLVMGenerator.reg - 1), ctx.INTEGER(0).getText());
            }
            else {
                LLVMGenerator.divIntegers(ctx.INTEGER(0).getText(), "%" + (LLVMGenerator.reg - 1));
            }
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.INTEGER));
        }

         if (ctx.ID().size() == 1 && ctx.REAL().size() == 1) {
            Optional<Variable> var = variables.stream().filter(v -> v.name.equals(ctx.ID(0).getText())).findFirst();

             if (!var.isPresent()) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(0).getText() + "> is not defined.");
            }

             if (var.get().variableType != VariableType.REAL) {
                raiseError(ctx.getStart().getLine(), "Mismatched types in the expression. <" + var.get().name + "> must be REAL.");
            }


            Matcher matcher = pattern.matcher(ctx.getText());

            LLVMGenerator.loadReal(var.get().name);
            if (matcher.find()) {
                LLVMGenerator.divReals("%" + (LLVMGenerator.reg - 1), ctx.REAL(0).getText());
            }
            else {    
                LLVMGenerator.divReals(ctx.REAL(0).getText(), "%" + (LLVMGenerator.reg - 1));
            }
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.REAL));
        }

         if (ctx.ID().size() == 2) {
            Optional<Variable> var0 = variables.stream().filter(v -> v.name.equals(ctx.ID(0).getText())).findFirst();
            Optional<Variable> var1 = variables.stream().filter(v -> v.name.equals(ctx.ID(1).getText())).findFirst();

             if (!var0.isPresent()) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(0).getText() + "> is not defined.");
            }

             if (!var1.isPresent()) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(1).getText() + "> is not defined.");
            }

             if (var0.get().variableType != var1.get().variableType) {
                raiseError(ctx.getStart().getLine(), "Mismatched types in expression. Variables <" + ctx.ID(0).getText() + "> and <" + ctx.ID(1).getText() + "> have different types.");
            }

             switch (var0.get().variableType) {
                case INTEGER:
                    LLVMGenerator.loadInteger(var0.get().name);
                    LLVMGenerator.loadInteger(var1.get().name);
                    LLVMGenerator.divIntegers("%" + (LLVMGenerator.reg - 2), "%" + (LLVMGenerator.reg - 1));
                    stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.INTEGER));
                break;

                 case REAL:
                    LLVMGenerator.loadReal(var0.get().name);
                    LLVMGenerator.loadReal(var1.get().name);
                    LLVMGenerator.divReals("%" + (LLVMGenerator.reg - 2), "%" + (LLVMGenerator.reg - 1));
                    stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.REAL));
                break;
            }
        }
    }

    @Override public void enterIf(HuginnParser.IfContext ctx) {
        if (ctx.BOOL() != null) {
            LLVMGenerator.ifStart(ctx.BOOL().getText());
        }

        if (ctx.ID() != null) {
            Optional<Variable> var = variables.stream().filter(v -> v.name.equals(ctx.ID().getText())).findFirst();

            if (!var.isPresent()) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID().getText() + "> is not defined.");
            }

            if (var.get().variableType != VariableType.BOOL) {
                raiseError(ctx.getStart().getLine(), "Mismatched types in the expression. <" + var.get().name + "> must be BOOL.");
            }

            LLVMGenerator.loadBool(var.get().name);
            LLVMGenerator.ifStart("%" + (LLVMGenerator.reg - 1));
        }
    }

	@Override public void exitIf(HuginnParser.IfContext ctx) {
        LLVMGenerator.ifEnd();
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
        REAL,
        BOOL
    }
}
