import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

//Memory Class
public class Memory {   
    final static int [] memory = new int[2000]; // int array for memory (instructions[0-999] and stack[1000-1999])
    public static void main(String args[]) {
        try {
            //Create a File obsplitStringect and a scanner to read it
			Scanner CPU_reader = new Scanner(System.in);
            File file = null;
            if(CPU_reader.hasNextLine())    // read file name from CPU
            {
                file = new File(CPU_reader.nextLine());
                
                if(!file.exists()) //exit if file not found
                {
                    System.out.println("File not found");
                    System.exit(0);
                }
            }
    
            // call function to read file and initialize memory array
            readFile(file);

            //vars
            String line;
            int temp2;
            //This while loop will keep on reading instructions from the CPU process and perform the read or write as needed
            while(true)
            {
                if(CPU_reader.hasNext())
                {
                    line = CPU_reader.nextLine(); //read the comma delimited line sent by the CPU
                    if(!line.isEmpty())
                    {
                        String [] splitString = line.split(","); //split the line to get tokens
                        
                        //  if first token is 1 then CPU is requesting to read from address
                        if(splitString[0].equals("1"))    
                        {
                            temp2 = Integer.parseInt(splitString[1]);
                            System.out.println(memory[temp2]);// send data to CPU 
                        }
                        
                        //  else it will be 2, CPU is requesting to write data to a specific address
                        else
                        {
                            int int1 = Integer.parseInt(splitString[1]);
                            int int2 = Integer.parseInt(splitString[2]);
                            memory[int1] = int2;
                            // invoke write function
                        }
                    }
                    else 
                        break;
                }
                else
                    break;
            }
            
        }
        catch(NumberFormatException e) {
            e.printStackTrace();
        }
    }
    //Read instructions from file and initialize memory
    private static void readFile(File file) {
        try 
        {
            //vars
			Scanner scanner = new Scanner(file);
            String temp;
            int temp_int;
            int i = 0;

            //Read the file to load memory instructions
            while(scanner.hasNext())
            {
                //if integer then write to memory
                if(scanner.hasNextInt())
                {
                    temp_int = scanner.nextInt();
                    memory[i++] = temp_int;
                }
                
                else
                {
                    temp = scanner.next();
                    //if token starts with '.', then move the counter
                    if(temp.charAt(0) == '.')
                        i = Integer.parseInt(temp.substring(1));
                    
                    //the token is a comment then go to next line
                    else if(temp.equals("//"))
                        scanner.nextLine();
                    
                    //else skip line
                    else
                        scanner.nextLine();
                }
            }
        } 
        catch (FileNotFoundException ex) 
        {
            ex.printStackTrace();
        }
    }
}