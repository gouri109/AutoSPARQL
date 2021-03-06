package org.aksw.autosparql.tbsl.algorithm.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;
import org.aksw.autosparql.tbsl.algorithm.sparql.BasicQueryTemplate;
import org.aksw.autosparql.tbsl.algorithm.sparql.Template;
import org.aksw.autosparql.tbsl.algorithm.templator.BasicTemplator;
import org.aksw.autosparql.tbsl.algorithm.templator.Templator;


public class TestFrontend {
	
	// MODE ::= BASIC | LEIPZIG
	static String MODE = "BASIC";  

    public static void main(String[] args) {

        System.out.println("======= SPARQL Templator =================");
        System.out.println("Running in " + MODE + " mode.");
        System.out.println("\nType ':q' to quit.");

    	BasicTemplator btemplator = new BasicTemplator();
    	Templator templator = new Templator();
        
        while (true) {
            String s = getStringFromUser("input > ").trim(); 
            
            if (s.equals(":q")) {
                System.exit(0);
            }
            
            if (MODE.equals("BASIC")) {
            	Set<BasicQueryTemplate> querytemps = btemplator.buildBasicQueries(s);
            	for (BasicQueryTemplate temp : querytemps) {
            		System.out.println(temp.toString());
            	}
            }
            else if (MODE.equals("LEIPZIG")) {
            	Set<Template> temps = templator.buildTemplates(s);           
            	for (Template temp : temps) {
            		System.out.println(temp.toString());
            	}
            }
        }
    }

    public static String getStringFromUser(String msg) {
        String str = "";
        try {
        	System.out.println("\n===========================================\n");
            System.out.print(msg);
            str = new BufferedReader(new InputStreamReader(System.in)).readLine();
        } catch (IOException e) {
        }
        return str;
    }
}
