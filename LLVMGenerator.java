import java.util.*;

class LLVMGenerator {
   static String header_text = "";
   static String main_text = "";
   static int reg = 1;
   static int br = 0;
   static String buffer = "";
   static int main_reg = 1;

   static Stack<Integer> br_stack = new Stack<>();

   public static String generate() {
      String text = "";
      text += "declare i32 @printf(i8*, ...)\n";
      text += "declare i32 @__isoc99_scanf(i8*, ...)\n";
      text += "@strpi = constant [4 x i8] c\"%d\\0A\\00\"\n";
      text += "@strpd = constant [4 x i8] c\"%f\\0A\\00\"\n";
      text += "@strsi = constant [3 x i8] c\"%d\\00\"\n";
      text += "@strsd = constant [4 x i8] c\"%lf\\00\"\n";
      text += header_text;
      text += "define i32 @main() nounwind{\n";
      text += main_text;
      text += "ret i32 0 }\n";
      return text;
   }

   public static void allocateInteger(String id) {
      main_text += "%" + id + " = alloca i32\n";
   } 

   public static void allocateReal(String id) {
      main_text += "%" + id + " = alloca double\n";
   }

   public static void allocateBool(String id) {
      main_text += "%" + id + " = alloca i1\n";
   }

   public static void assignInteger(String id, String value) {
      main_text += "store i32 " + value + ", i32* %" + id + "\n";
   }

   public static void assignReal(String id, String value) {
      main_text += "store double " + value + ", double* %" + id + "\n";
   }

   public static void assignBool(String id, String value) {
      main_text += "store i1 " + value + ", i1* %" + id + "\n";
   }

   public static void loadInteger(String id) {
      main_text += "%" + reg + "= load i32, i32* %" + id + "\n";
      reg++;
   }

   public static void loadReal(String id) {
      main_text += "%" + reg + "= load double, double* %" + id + "\n";
      reg++;
   }

   public static void loadBool(String id) {
      main_text += "%" + reg + "= load i1, i1* %" + id + "\n";
      reg++;
   }

   public static void printInteger(String id) {
      main_text += "%" + reg + "= load i32, i32* %" + id + "\n";
      reg++;
      main_text += "%" + reg + " = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strpi, i32 0, i32 0), i32 %" + (reg-1) + ")\n";
      reg++;
   }

   public static void printReal(String id) {
      main_text += "%" + reg + "= load double, double* %" + id + "\n";
      reg++;
      main_text += "%" + reg + " = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strpd, i32 0, i32 0), double %" + (reg-1) + ")\n";
      reg++;
   }

   public static void printBool(String id) {
      main_text += "%" + reg + "= load i1, i1* %" + id + "\n";
      reg++;
      main_text += "%" + reg + " = call i32 (i8*, ...) @printf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strpi, i1 0, i1 0), i1 %" + (reg-1) + ")\n";
      reg++;
   }

   public static void readInteger(String id) {
      main_text += "%" + reg + " = call i32 (i8*, ...) @__isoc99_scanf(i8* getelementptr inbounds ([3 x i8], [3 x i8]* @strsi, i32 0, i32 0), i32* %" + id + ")\n";
      reg++;  
   }

   public static void readReal(String id) {
      main_text += "%" + reg + " = call i32 (i8*, ...) @__isoc99_scanf(i8* getelementptr inbounds ([4 x i8], [4 x i8]* @strsd, i32 0, i32 0), double* %" + id + ")\n";
      reg++;
   }

   public static void addIntegers(String value1, String value2) {
      main_text += "%" + reg + " = add i32 " + value1 + ", " + value2 + "\n";
      reg++;
   }

   public static void addReals(String value1, String value2) {
      main_text += "%" + reg + " = fadd double " + value1 + ", " + value2 + "\n";
      reg++;
   }

   public static void mulIntegers(String value1, String value2) {
      main_text += "%" + reg + " = mul i32 " + value1 + ", " + value2 + "\n";
      reg++;
   }

   public static void mulReals(String value1, String value2) {
      main_text += "%" + reg + " = fmul double " + value1 + ", " + value2 + "\n";
      reg++;
   }

   public static void subIntegers(String value1, String value2) {
      main_text += "%" + reg + " = sub i32 " + value1 + ", " + value2 + "\n";
      reg++;
   }

   public static void subReals(String value1, String value2) {
      main_text += "%" + reg + " = fsub double " + value1 + ", " + value2 + "\n";
      reg++;
   }

   public static void divIntegers(String value1, String value2) {
      main_text += "%" + reg + " = div i32 " + value1 + ", " + value2 + "\n";
      reg++;
   }

   public static void divReals(String value1, String value2) {
      main_text += "%" + reg + " = fdiv double " + value1 + ", " + value2 + "\n";
      reg++;
   }

   static void ifStart(String conditionVariable) {
      br++;
      main_text += "br i1" + conditionVariable + ", label %true" + br + ", label %false" + br + "\n";
      main_text += "true" + br + ":\n";
      br_stack.push(br);
   }

   static void ifEnd() {
      int b = br_stack.pop();
      main_text += "br label %false" + b + "\n";
      main_text += "false" + b + ":\n";
   }

   static void functionStart(String id, String params){
      buffer = main_text;
      main_reg = reg;
      main_text = "define i32 @" + id + "(" + params + ") nounwind {\n";
      reg = 1;
   }

   static void functionEnd(){
      main_text += "ret i32 0\n"; 
      main_text += "}\n";
      header_text += main_text;
      main_text = buffer;
      reg = main_reg;
   }

   static void call(String id, String params){
      main_text += "%" + reg + " = call i32 @" + id + "("+params+")\n";
      reg++;
   }
}