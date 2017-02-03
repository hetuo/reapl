package cs652.repl;

import com.sun.source.util.JavacTask;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Scanner;

import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;

public class JavaREPL {

    public static int classNum = 0;
    public static URLClassLoader cloader;
    public static DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();

	/*public static void main(String[] args) throws IOException {
		exec(new InputStreamReader(System.in));
	}

	public static void exec(Reader r) throws IOException {
		BufferedReader stdin = new BufferedReader(r);
		NestedReader reader = new NestedReader(stdin);
		int classNumber = 0;
		while (true) {
			System.out.print("> ");
			String java = reader.getNestedString();
			// TODO
		}
	}*/



    public static void main(String[] args) throws Exception
    {
        //exec(new InputStreamReader(System.in));

    }

    public static void exec(StringReader r) throws IOException{
        try {
            createTempDirectory();
            BufferedReader bf = new BufferedReader(r);
            NestedReader reader = new NestedReader(bf);
            URL[] urls = new URL[]{new URL("file:/" + System.getProperty("user.dir") + "/temp")};
            cloader = new URLClassLoader(urls);
            while (true) {
                System.out.print("> ");
                String line = reader.getNestedString();
                // System.out.print(line + "xxx");
                //System.out.println(line.length());
                // System.out.println("xxxxxxxxxxxx");
                if (line == null) {
                    // System.out.println("hi");
                    break;
                }
                while (line.contains("//")) {
                    int i = line.indexOf("//");
                    int j = i;
                    for (; i < line.length(); i++)
                        if (line.charAt(i) == '\n')
                            break;
                    String subline = line.substring(j, i);
                    line = line.replace(subline, "");
                }
                if (line.equals(""))
                    continue;
                //System.out.print(line);
                if ((int) line.charAt(0) == 10 || (int) line.charAt(0) == 13) {
                    continue;
                }
                if (line.length() >= 6) {
                    String subline = line.substring(0, 6);

                    if (subline.equals("print ")) {

                        line = "System.out.println(" + line.substring(6, line.length() - 1) + ");";
                    }
                }

                String code = null;
                if (classNum == 0) {
                    code = getCode(line, "interp_" + classNum, null, true);
                } else
                    code = getCode(line, "interp_" + classNum, "interp_" + (classNum - 1), true);
                //System.out.println(code);
                diagnostics = new DiagnosticCollector<JavaFileObject>();
                if (isDeclaration(code)) {
                    compilerSetup(code, false);
                    Class<?> c = cloader.loadClass("interp_" + classNum);
                    classNum += 1;
                    continue;
                } else {
                    if (classNum == 0) {
                        code = getCode(line, "interp_" + classNum, null, false);
                    } else
                        code = getCode(line, "interp_" + classNum, "interp_" + (classNum - 1), false);
                    //System.out.println(code);
                    if (diagnostics.getDiagnostics().size() != 0)
                        diagnostics = new DiagnosticCollector<JavaFileObject>();
                    compilerSetup(code, false);
                    if (diagnostics.getDiagnostics().size() == 0) {
                        Class<?> c = cloader.loadClass("interp_" + classNum);
                        Method m = c.getDeclaredMethod("exec", null);
                        m.invoke(null, null);
                        //exec(cloader, classNum, "exec");
                        classNum += 1;
                    } else {
                        //System.out.println(diagnostics.getDiagnostics().toString());
                        for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
                            System.err.print("line " + diagnostic.getLineNumber() + ": " + diagnostic.getMessage(null));
                            System.err.println();
                        }
                    }
                }

            }
        }catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void createTempDirectory()
    {
        try {
            File tempDir = new File("temp");
            if (tempDir.exists())
                tempDir.delete();
            tempDir.mkdir();
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        //System.out.println(tempDir.getAbsolutePath());
    }

    public static String getCode(String srcCode, String className, String extendSuper, boolean isDef)
    {
        StringBuffer sb = new StringBuffer();
        sb.append("import java.io.*;\nimport java.util.*;\npublic class " + className);
        if (extendSuper == null)
            sb.append("{\n");
        else
            sb.append(" extends " + extendSuper + "{\n");
        if (isDef)
        {
            sb.append("public static " + srcCode + "\npublic static void exec()\n{}\n}");
        }
        else
        {
            sb.append( "\npublic static void exec()\n{\n" + srcCode +"}\n}");
        }
        return sb.toString();
    }

    public static boolean isDeclaration(String line) throws Exception
    {
        compilerSetup(line, true);
        //System.out.println("####" + diagnostics.getDiagnostics().toString());
        //System.out.println(diagnostics.getDiagnostics().toString());

        return diagnostics.getDiagnostics().size() == 0;
		/*Class<?> c = cloader.loadClass("interp_0");
		cloader.close();
		Method m = c.getDeclaredMethod("f", null);
		m.invoke(null, null);
		return false;*/

    }

    public static void compilerSetup(String line, boolean parse) throws Exception
    {
        String currentDir = System.getProperty("user.dir");
        String filename = currentDir + "/temp/interp_" + classNum + ".java";
        File file = new File(filename);
        FileWriter fw = new FileWriter(file);
        fw.write(line);
        fw.flush();
        fw.close();
        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = jc.getStandardFileManager(null, null, null);
        String path = Class.class.getClass().getResource("/").getPath();
        Iterable<String> options = Arrays.asList("-d", path);
        Iterable<? extends JavaFileObject> fileObjects = fileManager.getJavaFileObjects(filename);
        JavacTask task = (JavacTask)jc.getTask(null, fileManager, diagnostics, options, null, fileObjects);
        if (parse == false)
            task.call();
        else
            task.parse();

        fileManager.close();
    }

    public static void exec(URLClassLoader loader, int classNum, String methodName) throws Exception
    {
        //Class<?>[] cs = new Class<?>[classNum + 1];
        //for (int i = 0; i <= classNum; i++)
        //	cs[i] = loader.loadClass("interp_" + i);
        Class<?> c = loader.loadClass("interp_" + classNum);

        //cloader.close();
        //Method m = cs[classNum].getDeclaredMethod(methodName, null);
        Method m = c.getDeclaredMethod(methodName, null);
       // System.out.println("execed");
        m.invoke(null, null);
    }
}
