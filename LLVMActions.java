import java.util.*;
import java.util.regex.*;

public class LLVMActions extends HuginnBaseListener {

    HashSet<Variable> variables = new HashSet<Variable>();
    Stack<Variable> stack = new Stack<Variable>();
    HashSet<Function> functions = new HashSet<Function>();

    int blockId = 1;
    HashSet<Block> blocks = new HashSet<Block>();
    Stack<Integer> blockIds = new Stack<Integer>();
    boolean isFunction = false;

    public LLVMActions()
    {
        blockIds.push(0);
    }

    private Variable getVariable(String ID, int blockId, boolean ignoreParents)
    {
        var variable = variables.stream().filter(v -> v.name.equals(ID) && v.blockId == blockId).findFirst();

        if (variable.isPresent())
            return variable.get();
        if (!variable.isPresent() && ignoreParents)
            return null;

        if (!blocks.stream().filter(b -> b.id == blockId).findFirst().isPresent())
            return null;

        var parentBlock = blocks.stream().filter(b -> b.id == blockId).findFirst().get();
        var parentBlockId = parentBlock.parentId;

        if (parentBlockId == -1)
            return null;

        do
        {
            int pbi = parentBlockId;
            variable = variables.stream().filter(v -> v.name.equals(ID) && v.blockId == pbi).findFirst();
            if (variable.isPresent())
                return variable.get();

            if (!blocks.stream().filter(b -> b.id == pbi).findFirst().isPresent())
                break;
            parentBlock = blocks.stream().filter(b -> b.id == pbi).findFirst().get();
            parentBlockId = parentBlock.parentId;
        } while (parentBlockId != 0);

        return null;
    }

	@Override public void exitProg(HuginnParser.ProgContext ctx) { 
        System.out.println(LLVMGenerator.generate());
    }

    @Override public void exitDeclaration(HuginnParser.DeclarationContext ctx) {
        String ID = ctx.ID().getText();

        // if (variables.stream().filter(v -> v.name.equals(ID)).findFirst().isPresent()) {
        if (getVariable(ID, blockIds.peek(), true) != null) {
            raiseError(ctx.getStart().getLine(), "Variable <" + ID + "> is already defined in given block.");
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
            var v = new Variable(ID, VariableType.INTEGER, blockIds.peek());
            variables.add(v);
            LLVMGenerator.allocateInteger(v.getName());
            LLVMGenerator.assignInteger(v.getName(), ctx.INTEGER().getText());
        }

        if (ctx.REAL() != null)
        {
            var v = new Variable(ID, VariableType.REAL, blockIds.peek());
            variables.add(v);
            LLVMGenerator.allocateReal(v.getName());
            LLVMGenerator.assignReal(v.getName(), ctx.REAL().getText());
        }

        if (ctx.BOOL() != null)
        {
            var v = new Variable(ID, VariableType.BOOL, blockIds.peek());
            variables.add(v);
            LLVMGenerator.allocateBool(v.getName());
            LLVMGenerator.assignBool(v.getName(), ctx.BOOL().getText());
        }
    }

	@Override public void exitAssignment(HuginnParser.AssignmentContext ctx) {
        String ID = ctx.ID(0).getText();

        // Optional<Variable> var = variables.stream().filter(v -> v.name.equals(ID)).findFirst();
        var var = getVariable(ID, blockIds.peek(), false);

        if (var == null) {
            raiseError(ctx.getStart().getLine(), "Variable <" + ID + "> is not defined.");
        }

        if (ctx.INTEGER() != null && var.variableType != VariableType.INTEGER) {
            raiseError(ctx.getStart().getLine(), "Mismatched declared type and assigned value of variable " + ID + ".");
        }

        if (ctx.REAL() != null && var.variableType != VariableType.REAL) {
            raiseError(ctx.getStart().getLine(), "Mismatched declared type and assigned value of variable " + ID + ".");
        }

        if (ctx.BOOL() != null && var.variableType != VariableType.BOOL) {
            raiseError(ctx.getStart().getLine(), "Mismatched declared type and assigned value of variable " + ID + ".");
        }

        if (ctx.expression() != null && stack.peek().variableType != var.variableType) {
            raiseError(ctx.getStart().getLine(), "Mismatched declared type and assigned value of variable " + ID + ".");
        }

        String sourceID = ctx.ID(1) != null ? ctx.ID(1).getText() : "";
        // Optional<Variable> sourceVar = variables.stream().filter(v -> v.name.equals(sourceID)).findFirst();
        var sourceVar = getVariable(sourceID, blockIds.peek(), false);
        if (!sourceID.equals("") && sourceVar == null) {
            raiseError(ctx.getStart().getLine(), "Variable <" + sourceID + "> is not defined.");
        }

        if (sourceVar != null && sourceVar.variableType != var.variableType) {
            raiseError(ctx.getStart().getLine(), "Mismatched types of " + ID + " and " + sourceID + ".");
        }

        if (ctx.INTEGER() != null) {
            LLVMGenerator.assignInteger(var.getName(), ctx.INTEGER().getText());
        }

        if (ctx.REAL() != null) {
            LLVMGenerator.assignReal(var.getName(), ctx.REAL().getText());
        }

        if (ctx.BOOL() != null) {
            LLVMGenerator.assignBool(var.getName(), ctx.BOOL().getText());
        }

        if (sourceVar != null) {
            switch (sourceVar.variableType)
            {
                case INTEGER:
                    LLVMGenerator.loadInteger(sourceVar.getName());
                    LLVMGenerator.assignInteger(var.getName(), "%" + (LLVMGenerator.reg - 1));
                break;

                case REAL:
                    LLVMGenerator.loadReal(sourceVar.getName());
                    LLVMGenerator.assignReal(var.getName(), "%" + (LLVMGenerator.reg - 1));
                break;

                case BOOL:
                    LLVMGenerator.loadBool(sourceVar.getName());
                    LLVMGenerator.assignBool(var.getName(), "%" + (LLVMGenerator.reg - 1));
                break;
            }
        }

        if (ctx.expression() != null) {
            Variable storedVariable = stack.pop();
            switch (storedVariable.variableType)
            {
                case INTEGER:
                    LLVMGenerator.assignInteger(var.getName(), "%" + storedVariable.name);
                break;

                case REAL:
                    LLVMGenerator.assignReal(var.getName(), "%" + storedVariable.name);
                break;

                case BOOL:
                    LLVMGenerator.assignBool(var.getName(), "%" + storedVariable.name);
                break;
            }
        }
    }

	@Override public void exitPrint(HuginnParser.PrintContext ctx) {
        String ID = ctx.ID().getText();
    
        // Optional<Variable> var = variables.stream().filter(v -> v.name.equals(ID)).findFirst();
        var var = getVariable(ID, blockIds.peek(), false);

        if (var == null) {
            raiseError(ctx.getStart().getLine(), "Variable <" + ID + "> is not defined.");
        }

        switch (var.variableType)
        {
            case INTEGER:
                LLVMGenerator.printInteger(var.getName());
            break;

            case REAL:
                LLVMGenerator.printReal(var.getName());
            break;

            case BOOL:
                LLVMGenerator.printBool(var.getName());
            break;
        }
    }

	@Override public void exitRead(HuginnParser.ReadContext ctx) {
        String ID = ctx.ID().getText();
    
        // Optional<Variable> var = variables.stream().filter(v -> v.name.equals(ID)).findFirst();
        var var = getVariable(ID, blockIds.peek(), false);

        if (var == null) {
            raiseError(ctx.getStart().getLine(), "Variable <" + ID + "> is not defined.");
        }

        switch (var.variableType)
        {
            case INTEGER:
                LLVMGenerator.readInteger(var.getName());
            break;

            case REAL:
                LLVMGenerator.readReal(var.getName());
            break;
        }
    }

    @Override public void exitExpression_base_add(HuginnParser.Expression_base_addContext ctx) {
        if (ctx.INTEGER().size() == 2) {
            LLVMGenerator.addIntegers(ctx.INTEGER(0).getText(), ctx.INTEGER(1).getText());
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.INTEGER, blockIds.peek()));
        }

        if (ctx.REAL().size() == 2) {
            LLVMGenerator.addReals(ctx.REAL(0).getText(), ctx.REAL(1).getText());
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.REAL, blockIds.peek()));                
        }

        if (ctx.ID().size() == 1 && ctx.INTEGER().size() == 1) {
            // Optional<Variable> var = variables.stream().filter(v -> v.name.equals(ctx.ID(0).getText())).findFirst();
            var var = getVariable(ctx.ID(0).getText(), blockIds.peek(), false);

            if (var == null) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(0).getText() + "> is not defined.");
            }

            if (var.variableType != VariableType.INTEGER) {
                raiseError(ctx.getStart().getLine(), "Mismatched types in the expression. <" + var.name + "> must be INTEGER.");
            }

            LLVMGenerator.loadInteger(var.getName());
            LLVMGenerator.addIntegers(ctx.INTEGER(0).getText(), "%" + (LLVMGenerator.reg - 1));
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.INTEGER, blockIds.peek()));
        }

        if (ctx.ID().size() == 1 && ctx.REAL().size() == 1) {
            // Optional<Variable> var = variables.stream().filter(v -> v.name.equals(ctx.ID(0).getText())).findFirst();
            var var = getVariable(ctx.ID(0).getText(), blockIds.peek(), false);

            if (var == null) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(0).getText() + "> is not defined.");
            }

            if (var.variableType != VariableType.REAL) {
                raiseError(ctx.getStart().getLine(), "Mismatched types in the expression. <" + var.name + "> must be REAL.");
            }

            LLVMGenerator.loadReal(var.getName());
            LLVMGenerator.addReals(ctx.REAL(0).getText(), "%" + (LLVMGenerator.reg - 1));
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.REAL, blockIds.peek()));
        }

        if (ctx.ID().size() == 2) {
            // Optional<Variable> var0 = variables.stream().filter(v -> v.name.equals(ctx.ID(0).getText())).findFirst();
            // Optional<Variable> var1 = variables.stream().filter(v -> v.name.equals(ctx.ID(1).getText())).findFirst();
            var var0 = getVariable(ctx.ID(0).getText(), blockIds.peek(), false);
            var var1 = getVariable(ctx.ID(1).getText(), blockIds.peek(), false);

            if (var0 == null) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(0).getText() + "> is not defined.");
            }

            if (var1 == null) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(1).getText() + "> is not defined.");
            }

            if (var0.variableType != var1.variableType) {
                raiseError(ctx.getStart().getLine(), "Mismatched types in expression. Variables <" + ctx.ID(0).getText() + "> and <" + ctx.ID(1).getText() + "> have different types.");
            }

            switch (var0.variableType) {
                case INTEGER:
                    LLVMGenerator.loadInteger(var0.getName());
                    LLVMGenerator.loadInteger(var1.getName());
                    LLVMGenerator.addIntegers("%" + (LLVMGenerator.reg - 2), "%" + (LLVMGenerator.reg - 1));
                    stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.INTEGER, blockIds.peek()));
                break;

                case REAL:
                    LLVMGenerator.loadReal(var0.getName());
                    LLVMGenerator.loadReal(var1.getName());
                    LLVMGenerator.addReals("%" + (LLVMGenerator.reg - 2), "%" + (LLVMGenerator.reg - 1));
                    stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.REAL, blockIds.peek()));
                break;
            }
        }
    }

    @Override public void exitExpression_base_mul(HuginnParser.Expression_base_mulContext ctx) {
        if (ctx.INTEGER().size() == 2) {
            LLVMGenerator.mulIntegers(ctx.INTEGER(0).getText(), ctx.INTEGER(1).getText());
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.INTEGER, blockIds.peek()));
        }

         if (ctx.REAL().size() == 2) {
            LLVMGenerator.mulReals(ctx.REAL(0).getText(), ctx.REAL(1).getText());
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.REAL, blockIds.peek()));                
        }

         if (ctx.ID().size() == 1 && ctx.INTEGER().size() == 1) {
            // Optional<Variable> var = variables.stream().filter(v -> v.name.equals(ctx.ID(0).getText())).findFirst();
            var var = getVariable(ctx.ID(0).getText(), blockIds.peek(), false);

             if (var == null) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(0).getText() + "> is not defined.");
            }

             if (var.variableType != VariableType.INTEGER) {
                raiseError(ctx.getStart().getLine(), "Mismatched types in the expression. <" + var.name + "> must be INTEGER.");
            }

            LLVMGenerator.loadInteger(var.getName());
            LLVMGenerator.mulIntegers(ctx.INTEGER(0).getText(), "%" + (LLVMGenerator.reg - 1));
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.INTEGER, blockIds.peek()));
        }

         if (ctx.ID().size() == 1 && ctx.REAL().size() == 1) {
            // Optional<Variable> var = variables.stream().filter(v -> v.name.equals(ctx.ID(0).getText())).findFirst();
            var var = getVariable(ctx.ID(0).getText(), blockIds.peek(), false);

            if (var == null) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(0).getText() + "> is not defined.");
            }

             if (var.variableType != VariableType.REAL) {
                raiseError(ctx.getStart().getLine(), "Mismatched types in the expression. <" + var.name + "> must be REAL.");
            }

            LLVMGenerator.loadReal(var.getName());
            LLVMGenerator.mulReals(ctx.REAL(0).getText(), "%" + (LLVMGenerator.reg - 1));
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.REAL, blockIds.peek()));
        }

         if (ctx.ID().size() == 2) {
            // Optional<Variable> var0 = variables.stream().filter(v -> v.name.equals(ctx.ID(0).getText())).findFirst();
            // Optional<Variable> var1 = variables.stream().filter(v -> v.name.equals(ctx.ID(1).getText())).findFirst();
            var var0 = getVariable(ctx.ID(0).getText(), blockIds.peek(), false);
            var var1 = getVariable(ctx.ID(1).getText(), blockIds.peek(), false);

            if (var0 == null) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(0).getText() + "> is not defined.");
            }

             if (var1 == null) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(1).getText() + "> is not defined.");
            }

             if (var0.variableType != var1.variableType) {
                raiseError(ctx.getStart().getLine(), "Mismatched types in expression. Variables <" + ctx.ID(0).getText() + "> and <" + ctx.ID(1).getText() + "> have different types.");
            }

             switch (var0.variableType) {
                case INTEGER:
                    LLVMGenerator.loadInteger(var0.getName());
                    LLVMGenerator.loadInteger(var1.getName());
                    LLVMGenerator.mulIntegers("%" + (LLVMGenerator.reg - 2), "%" + (LLVMGenerator.reg - 1));
                    stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.INTEGER, blockIds.peek()));
                break;

                 case REAL:
                    LLVMGenerator.loadReal(var0.getName());
                    LLVMGenerator.loadReal(var1.getName());
                    LLVMGenerator.mulReals("%" + (LLVMGenerator.reg - 2), "%" + (LLVMGenerator.reg - 1));
                    stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.REAL, blockIds.peek()));
                break;
            }
        }
    }

    @Override public void exitExpression_base_sub(HuginnParser.Expression_base_subContext ctx) {
        Pattern pattern = Pattern.compile("^[a-z_].*$", Pattern.CASE_INSENSITIVE);

        if (ctx.INTEGER().size() == 2) {
            LLVMGenerator.subIntegers(ctx.INTEGER(0).getText(), ctx.INTEGER(1).getText());
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.INTEGER, blockIds.peek()));
        }

         if (ctx.REAL().size() == 2) {
            LLVMGenerator.subReals(ctx.REAL(0).getText(), ctx.REAL(1).getText());
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.REAL, blockIds.peek()));                
        }

         if (ctx.ID().size() == 1 && ctx.INTEGER().size() == 1) {
            // Optional<Variable> var = variables.stream().filter(v -> v.name.equals(ctx.ID(0).getText())).findFirst();
            var var = getVariable(ctx.ID(0).getText(), blockIds.peek(), false);

            if (var == null) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(0).getText() + "> is not defined.");
            }

             if (var.variableType != VariableType.INTEGER) {
                raiseError(ctx.getStart().getLine(), "Mismatched types in the expression. <" + var.name + "> must be INTEGER.");
            }

            Matcher matcher = pattern.matcher(ctx.getText());

            LLVMGenerator.loadInteger(var.getName());
            if (matcher.find()) {
                LLVMGenerator.subIntegers("%" + (LLVMGenerator.reg - 1), ctx.INTEGER(0).getText());
            }
            else {
                LLVMGenerator.subIntegers(ctx.INTEGER(0).getText(), "%" + (LLVMGenerator.reg - 1));
            }
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.INTEGER, blockIds.peek()));
        }

         if (ctx.ID().size() == 1 && ctx.REAL().size() == 1) {
            // Optional<Variable> var = variables.stream().filter(v -> v.name.equals(ctx.ID(0).getText())).findFirst();
            var var = getVariable(ctx.ID(0).getText(), blockIds.peek(), false);

             if (var == null) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(0).getText() + "> is not defined.");
            }

             if (var.variableType != VariableType.REAL) {
                raiseError(ctx.getStart().getLine(), "Mismatched types in the expression. <" + var.name + "> must be REAL.");
            }

            Matcher matcher = pattern.matcher(ctx.getText());

            LLVMGenerator.loadReal(var.getName());
            if (matcher.find()) {
                LLVMGenerator.subReals("%" + (LLVMGenerator.reg - 1), ctx.REAL(0).getText());
            }
            else {    
                LLVMGenerator.subReals(ctx.REAL(0).getText(), "%" + (LLVMGenerator.reg - 1));
            }
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.REAL, blockIds.peek()));
        }

         if (ctx.ID().size() == 2) {
            // Optional<Variable> var0 = variables.stream().filter(v -> v.name.equals(ctx.ID(0).getText())).findFirst();
            // Optional<Variable> var1 = variables.stream().filter(v -> v.name.equals(ctx.ID(1).getText())).findFirst();
            var var0 = getVariable(ctx.ID(0).getText(), blockIds.peek(), false);
            var var1 = getVariable(ctx.ID(1).getText(), blockIds.peek(), false);

             if (var0 == null) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(0).getText() + "> is not defined.");
            }

             if (var1 == null) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(1).getText() + "> is not defined.");
            }

             if (var0.variableType != var1.variableType) {
                raiseError(ctx.getStart().getLine(), "Mismatched types in expression. Variables <" + ctx.ID(0).getText() + "> and <" + ctx.ID(1).getText() + "> have different types.");
            }

             switch (var0.variableType) {
                case INTEGER:
                    LLVMGenerator.loadInteger(var0.getName());
                    LLVMGenerator.loadInteger(var1.getName());
                    LLVMGenerator.subIntegers("%" + (LLVMGenerator.reg - 2), "%" + (LLVMGenerator.reg - 1));
                    stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.INTEGER, blockIds.peek()));
                break;

                 case REAL:
                    LLVMGenerator.loadReal(var0.getName());
                    LLVMGenerator.loadReal(var1.getName());
                    LLVMGenerator.subReals("%" + (LLVMGenerator.reg - 2), "%" + (LLVMGenerator.reg - 1));
                    stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.REAL, blockIds.peek()));
                break;
            }
        }
    }

    @Override public void exitExpression_base_div(HuginnParser.Expression_base_divContext ctx) {
        Pattern pattern = Pattern.compile("^[a-z_].*$", Pattern.CASE_INSENSITIVE);

        if (ctx.INTEGER().size() == 2) {
            LLVMGenerator.divIntegers(ctx.INTEGER(0).getText(), ctx.INTEGER(1).getText());
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.INTEGER, blockIds.peek()));
        }

         if (ctx.REAL().size() == 2) {
            LLVMGenerator.divReals(ctx.REAL(0).getText(), ctx.REAL(1).getText());
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.REAL, blockIds.peek()));                
        }

         if (ctx.ID().size() == 1 && ctx.INTEGER().size() == 1) {
            // Optional<Variable> var = variables.stream().filter(v -> v.name.equals(ctx.ID(0).getText())).findFirst();
            var var = getVariable(ctx.ID(0).getText(), blockIds.peek(), false);

             if (var == null) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(0).getText() + "> is not defined.");
            }

             if (var.variableType != VariableType.INTEGER) {
                raiseError(ctx.getStart().getLine(), "Mismatched types in the expression. <" + var.name + "> must be INTEGER.");
            }

            Matcher matcher = pattern.matcher(ctx.getText());

            LLVMGenerator.loadInteger(var.getName());
            if (matcher.find()) {
                LLVMGenerator.divIntegers("%" + (LLVMGenerator.reg - 1), ctx.INTEGER(0).getText());
            }
            else {
                LLVMGenerator.divIntegers(ctx.INTEGER(0).getText(), "%" + (LLVMGenerator.reg - 1));
            }
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.INTEGER, blockIds.peek()));
        }

         if (ctx.ID().size() == 1 && ctx.REAL().size() == 1) {
            // Optional<Variable> var = variables.stream().filter(v -> v.name.equals(ctx.ID(0).getText())).findFirst();
            var var = getVariable(ctx.ID(0).getText(), blockIds.peek(), false);

             if (var == null) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(0).getText() + "> is not defined.");
            }

             if (var.variableType != VariableType.REAL) {
                raiseError(ctx.getStart().getLine(), "Mismatched types in the expression. <" + var.name + "> must be REAL.");
            }


            Matcher matcher = pattern.matcher(ctx.getText());

            LLVMGenerator.loadReal(var.getName());
            if (matcher.find()) {
                LLVMGenerator.divReals("%" + (LLVMGenerator.reg - 1), ctx.REAL(0).getText());
            }
            else {    
                LLVMGenerator.divReals(ctx.REAL(0).getText(), "%" + (LLVMGenerator.reg - 1));
            }
            stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.REAL, blockIds.peek()));
        }

         if (ctx.ID().size() == 2) {
            // Optional<Variable> var0 = variables.stream().filter(v -> v.name.equals(ctx.ID(0).getText())).findFirst();
            // Optional<Variable> var1 = variables.stream().filter(v -> v.name.equals(ctx.ID(1).getText())).findFirst();
            var var0 = getVariable(ctx.ID(0).getText(), blockIds.peek(), false);
            var var1 = getVariable(ctx.ID(1).getText(), blockIds.peek(), false);

             if (var0 == null) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(0).getText() + "> is not defined.");
            }

             if (var1 == null) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID(1).getText() + "> is not defined.");
            }

             if (var0.variableType != var1.variableType) {
                raiseError(ctx.getStart().getLine(), "Mismatched types in expression. Variables <" + ctx.ID(0).getText() + "> and <" + ctx.ID(1).getText() + "> have different types.");
            }

             switch (var0.variableType) {
                case INTEGER:
                    LLVMGenerator.loadInteger(var0.getName());
                    LLVMGenerator.loadInteger(var1.getName());
                    LLVMGenerator.divIntegers("%" + (LLVMGenerator.reg - 2), "%" + (LLVMGenerator.reg - 1));
                    stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.INTEGER, blockIds.peek()));
                break;

                 case REAL:
                    LLVMGenerator.loadReal(var0.getName());
                    LLVMGenerator.loadReal(var1.getName());
                    LLVMGenerator.divReals("%" + (LLVMGenerator.reg - 2), "%" + (LLVMGenerator.reg - 1));
                    stack.push(new Variable("" + (LLVMGenerator.reg - 1), VariableType.REAL, blockIds.peek()));
                break;
            }
        }
    }

    @Override public void enterIf(HuginnParser.IfContext ctx) {
        if (ctx.BOOL() != null) {
            LLVMGenerator.ifStart(ctx.BOOL().getText());
        }

        if (ctx.ID() != null) {
            // Optional<Variable> var = variables.stream().filter(v -> v.name.equals(ctx.ID().getText())).findFirst();
            var var = getVariable(ctx.ID().getText(), blockIds.peek(), false);

            if (var == null) {
                raiseError(ctx.getStart().getLine(), "Variable <" + ctx.ID().getText() + "> is not defined.");
            }

            if (var.variableType != VariableType.BOOL) {
                raiseError(ctx.getStart().getLine(), "Mismatched types in the expression. <" + var.name + "> must be BOOL.");
            }

            LLVMGenerator.loadBool(var.getName());
            LLVMGenerator.ifStart("%" + (LLVMGenerator.reg - 1));
        }
    }

	@Override public void exitIf(HuginnParser.IfContext ctx) {
        LLVMGenerator.ifEnd();
    }



    @Override public void enterFunction_def(HuginnParser.Function_defContext ctx) {
        Optional<Function> fun = functions.stream().filter(f -> f.name.equals(ctx.ID().getText())).findFirst();
        if (fun.isPresent()) {
            raiseError(ctx.getStart().getLine(), "Function <" + ctx.ID().getText() + "> is already defined.");
        }

        var f = new Function(ctx.ID().getText());
        for (var p: ctx.parameter_def()) {
            VariableType vt = VariableType.BOOL;

            if (p.INTEGER() != null)
                vt = VariableType.INTEGER;
            if (p.REAL() != null)
                vt = VariableType.REAL;
            if (p.BOOL() != null)
                vt = VariableType.BOOL;
        
            f.parameters.add(new Parameter(p.ID().getText(), vt));
        }

        LLVMGenerator.functionStart(ctx.ID().getText());

        isFunction = true;
    }

    @Override public void exitFunction_def(HuginnParser.Function_defContext ctx) {
        LLVMGenerator.functionEnd();

        isFunction = false;
    }

    @Override public void exitFunction_call(HuginnParser.Function_callContext ctx) {
        //TODO checks etc.
        LLVMGenerator.call(ctx.ID().getText());
    }

    @Override public void enterBlock(HuginnParser.BlockContext ctx) {
        int parentId = -1;
        if (!isFunction)
        {
            parentId = blockIds.peek();
        }

        var block = new Block(blockId, parentId);
        blockIds.push(blockId);

        blockId++;

        blocks.add(block);
        isFunction = false;
    }

	@Override public void exitBlock(HuginnParser.BlockContext ctx) {
        blockIds.pop();
    }

    private void raiseError(int line, String msg) {
       System.err.println("Error in line " + line + ", " + msg);
       System.exit(1);
    }

    private class Variable {
        public String name;
        public VariableType variableType;
        public int blockId;

        public Variable(String name, VariableType variableType, int blockId)
        {
            this.name = name;
            this.variableType = variableType;
            this.blockId = blockId;
        }

        public String getName(){
            return name + blockId;
        }
    }

    private enum VariableType {
        INTEGER,
        REAL,
        BOOL
    }

    private class Function {
        public String name;
        public ArrayList<Parameter> parameters;

        public Function(String name)
        {
            this.name = name;
            this.parameters = new ArrayList<Parameter>();
        }
    }

    private class Parameter {
        public String name;
        public VariableType variableType;

        public Parameter(String name, VariableType variableType)
        {
            this.name = name;
            this.variableType = variableType;
        }
    }

    private class Block {
        public int id;
        public int parentId;

        public Block(int id, int parentId) {
            this.id = id;
            this.parentId = parentId;
        }
    }
}
