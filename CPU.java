import java.io.*;
import java.util.Random;
import java.util.Scanner;

//CPU Class
public class CPU {
    //required registers
    static int PC = 0, SP = 1000, IR, AC, X, Y, timerFlag, instruction_count = 0;
    static int systemStack_top = 2000, userStack_top = 1000;
    
    static boolean userMode = true; //initially set to true. On interrupt, set it to false for kernel mode
    static boolean isInterrupted = false; //flag to avoid nested interrupt execution
    
    public static void main(String args[]) {
        String fileName = null;
        //check the command line argument length
        if(args.length == 2) {
             fileName = args[0];
             timerFlag = Integer.parseInt(args[1]); // set timer interrupt value
        }
        //else exit
        else {
            System.out.println("Incorrect number of parameters, try again.");
            System.exit(0);
        } 

        try {            
            //Create child process and set up I/O streams
            Runtime runtime = Runtime.getRuntime();
            Process process = runtime.exec("java Memory");

            OutputStream output = process.getOutputStream();
            PrintWriter printwrite = new PrintWriter(output);

            InputStream input = process.getInputStream();
            Scanner memoryReader = new Scanner(input);
            
            //Send file name to child
            fileNameToMemory(printwrite, input, output, fileName);
            
            //communication between CPU and memory.
            while (true) {
                
                //check if timer interrupt has occurred.
                if(instruction_count > 0 && (instruction_count % timerFlag) == 0 
                		&& isInterrupted == false) {
                    //process interrupt
                    isInterrupted = true;
                    interruptFromTimer(printwrite, input, memoryReader, output);
                }
                
                //read instruction from memory
                int value = readFromMemory(printwrite, input, memoryReader, output, PC);
                
                if (value != -1)
                	processInstruction(value, printwrite, input, memoryReader, output);
                else
                    break;
            }
            
            process.waitFor();
            int exitVal = process.exitValue();
            System.out.println("Process exited: " + exitVal);
        } 
        catch (IOException | InterruptedException t) {
           t.printStackTrace();
        }

    }

    //send file name to memory
    private static void fileNameToMemory(PrintWriter pw, InputStream is, OutputStream os, String fileName) {
        pw.printf(fileName + "\n");  //send filename to memory
        pw.flush();
    }

    // read data from given address
    private static int readFromMemory(PrintWriter pw, InputStream is, Scanner memory_reader, OutputStream os, int address) {
        checkMemoryViolation(address);
        pw.printf("1," + address + "\n"); //1 indicates write
        pw.flush();
        if (memory_reader.hasNext()) {
            String temp = memory_reader.next();
            if(!temp.isEmpty()) {
                int temp2 = Integer.parseInt(temp);
                return (temp2); 
            }
        }
        return -1;
    }
    
    //tell child process to write data to given address
    private static void writeToMemory(PrintWriter pw, InputStream is, OutputStream os, int address, int data) {
        pw.printf("2," + address + "," + data + "\n"); //2 indicates write
        pw.flush();
    }

    // function to process an instruction received from the memory
    private static void processInstruction(int value, PrintWriter pw, InputStream is, Scanner memory_reader, OutputStream os) {
        IR = value; //store instruction in IR
        int op;
        
        switch(IR) {
        
            case 1: //Load the value into the AC
                PC++; // increment counter to get operand
                op = readFromMemory(pw, is, memory_reader, os, PC);
                AC = op;
                if(isInterrupted == false) 
                    instruction_count++;
                PC++;
                break;
                
            case 2: //Load the value at the address into the AC
                PC++;
                op = readFromMemory(pw, is, memory_reader, os, PC);
                AC = readFromMemory(pw, is, memory_reader, os, op);
                if(isInterrupted == false) 
                    instruction_count++;
                PC++;
                break;

            case 3: //Load the value from the address found in the address into the AC
                PC++;
                op = readFromMemory(pw, is, memory_reader, os, PC);
                op = readFromMemory(pw, is, memory_reader, os, op);
                AC = readFromMemory(pw, is, memory_reader, os, op);
                if(isInterrupted == false) 
                    instruction_count++;
                PC++;
                break;
                
                
            case 4: //Load the value at (address+X) into the AC
                PC++;
                op = readFromMemory(pw, is, memory_reader, os, PC);
                AC = readFromMemory(pw, is, memory_reader, os, op + X);
                if(isInterrupted == false) 
                    instruction_count++;
                PC++;
                break;
                
            case 5: //Load the value at (address+Y) into the AC
                PC++;
                op = readFromMemory(pw, is, memory_reader, os, PC);
                AC = readFromMemory(pw, is, memory_reader, os, op + Y);
                if(isInterrupted == false) 
                    instruction_count++;
                PC++;
                break;
                
            case 6: //Load from (Sp+X) into the AC
                AC = readFromMemory(pw, is, memory_reader, os, SP + X);
                if(isInterrupted == false) 
                    instruction_count++;
                PC++;
                break;
                
            case 7: //Store the value in the AC into the address
                PC++;
                op = readFromMemory(pw, is, memory_reader, os, PC);
                writeToMemory(pw, is, os, op, AC);
                if(isInterrupted == false) 
                    instruction_count++;
                PC++;
                break;
                
            case 8: //Gets a random int from 1 to 100 into the AC
                Random r = new Random();
                int i = r.nextInt(100) + 1;
                AC = i;
                if(isInterrupted == false) 
                    instruction_count++;
                PC++;
                break;
                
            case 9: //If port=1, writes AC as an int to the screen
                    //If port=2, writes AC as a char to the screen
                PC++;
                op = readFromMemory(pw, is, memory_reader, os, PC);
                if(op == 1) {
                    System.out.print(AC);
                    if(isInterrupted == false) 
                        instruction_count++;
                    PC++;
                    break;

                }
                else if (op == 2) {
                    System.out.print((char)AC);
                    if(isInterrupted == false) 
                        instruction_count++;
                    PC++;
                    break;
                }
                else {
                    System.out.println("Error: Port = " + op);
                    if(isInterrupted == false) 
                        instruction_count++;
                    PC++;
                    System.exit(0);
                    break;
                }
                
            case 10: //Add the value in X to the AC
                AC = AC + X;
                if(isInterrupted == false) 
                    instruction_count++;
                PC++;
                break;
                
            case 11: //Add the value in Y to the AC
                AC = AC + Y;
                if(isInterrupted == false) 
                    instruction_count++;
                PC++;
                break;
                
            case 12: //Subtract the value in X from the AC
                AC = AC - X;
                if(isInterrupted == false) 
                    instruction_count++;
                PC++;
                break;
                
            case 13: //Subtract the value in Y from the AC
                AC = AC - Y;
                if(isInterrupted == false) 
                    instruction_count++;
                PC++;
                break;
                
            case 14: //Copy the value in the AC to X
                X = AC;
                if(isInterrupted == false) 
                    instruction_count++;
                PC++;
                break;
                
            case 15: //Copy the value in X to the AC
                AC = X;
                if(isInterrupted == false) 
                    instruction_count++;
                PC++;
                break;
                
            case 16: //Copy the value in the AC to Y
                Y = AC;
                if(isInterrupted == false) 
                    instruction_count++;
                PC++;
                break;
                
            case 17: //Copy the value in Y to the AC
                AC = Y;
                if(isInterrupted == false) 
                    instruction_count++;
                PC++;
                break;
                
            case 18: //Copy the value in AC to the SP
                SP = AC;
                if(isInterrupted == false) 
                    instruction_count++;
                PC++;
                break;
                
            case 19: //Copy the value in SP to the AC 
                AC = SP;
                if(isInterrupted == false) 
                    instruction_count++;
                PC++;
                break;
                
            case 20: // Jump to the address
                PC++;
                op = readFromMemory(pw, is, memory_reader, os, PC);
                PC = op;
                if(isInterrupted == false) 
                    instruction_count++;
                break;
                
            case 21: //Jump to the address only if the value in the AC is zero
                PC++;
                op = readFromMemory(pw, is, memory_reader, os, PC);
                if (AC == 0) 
                {
                    PC = op;
                    if(isInterrupted == false) 
                        instruction_count++;
                    break;
                }
                if(isInterrupted == false) 
                    instruction_count++;
                PC++;
                break;
                
            case 22: //Jump to the address only if the value in the AC is not zero
                PC++;
                op = readFromMemory(pw, is, memory_reader, os, PC);
                if (AC != 0) 
                {
                    PC = op;
                    if(isInterrupted == false) 
                        instruction_count++;
                    break;
                }
                if(isInterrupted == false) 
                    instruction_count++;
                PC++;
                break;
                
            case 23:// Push return address onto stack, jump to the address
                PC++;
                op = readFromMemory(pw, is, memory_reader, os, PC);
                pushValueToStack(pw, is, os,PC+1);
                userStack_top = SP;
                PC = op;
                if(isInterrupted == false) 
                    instruction_count++;
                break;
                
            case 24: //Pop return address from the stack, jump to the address
                op = popValueFromStack(pw, is, memory_reader, os);
                PC = op;
                if(isInterrupted == false) 
                    instruction_count++;
                break;
                
            case 25: //Increment the value in X
                X++;
                if(isInterrupted == false) 
                    instruction_count++;
                PC++;
                break;
            
            case 26: //Decrement the value in X
                X--;
                if(isInterrupted == false) 
                    instruction_count++;
                PC++;
                break;
            
            case 27: //Push AC onto stack
                pushValueToStack(pw, is, os,AC);
                PC++;
                if(isInterrupted == false) 
                    instruction_count++;
                break;
                
            case 28: //Pop from stack into AC
                AC = popValueFromStack(pw, is, memory_reader, os);
                PC++;
                if(isInterrupted == false) 
                    instruction_count++;
                break;
                
            case 29: //Perform system call
                isInterrupted = true;
                userMode = false;
                op = SP;
                SP = 2000;
                pushValueToStack(pw, is, os, op);
                
                op = PC + 1;
                PC = 1500;
                pushValueToStack(pw, is, os, op);
                
                if(isInterrupted == false) 
                    instruction_count++;
                
                break;
                
            case 30: //Return from system call
                
                PC = popValueFromStack(pw, is, memory_reader, os);
                SP = popValueFromStack(pw, is, memory_reader, os);
                userMode = true;
                instruction_count++;
                isInterrupted = false;
                break;
                
            case 50: //End Execution
                if(isInterrupted == false) 
                    instruction_count++;
                System.exit(0);
                break;
            
            default:
                System.out.println("Error: Unknown command");
                System.exit(0);
        }
    }
    //handle interrupts caused by the timer
    private static void interruptFromTimer(PrintWriter pw, InputStream is, Scanner memory_reader, OutputStream os) 
    {
        int op;
        userMode = false;
        op = SP;
        SP = systemStack_top;
        pushValueToStack(pw, is, os, op);

        op = PC;
        PC = 1000;
        pushValueToStack(pw, is, os, op);
    }

    //check if user program is trying to access system memory and stack correctly
    private static void checkMemoryViolation(int address) 
    {
        if(userMode && address > 1000)
        {
            System.out.println("Error: User tried to access system stack. Process exiting.");
            System.exit(0);
        }
    }

    //pop a value from stack
    private static int popValueFromStack(PrintWriter pw, InputStream is, Scanner memory_reader, OutputStream os) 
    {
        int temp = readFromMemory(pw, is, memory_reader, os, SP);
        writeToMemory(pw, is, os, SP, 0);
        SP++;
        return temp;
    }

    //push a value to stack
    private static void pushValueToStack(PrintWriter pw, InputStream is, OutputStream os, int value) 
    {
        SP--;
        writeToMemory(pw, is, os, SP, value);
    }
}