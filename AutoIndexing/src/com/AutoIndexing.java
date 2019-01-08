package com;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class AutoIndexing {

	public static void main(String[] args) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter("test.txt"));
		bw.write("test txt");
		bw.write(", ");
		bw.write("right");
		bw.newLine();
		bw.write("2nd line");
		bw.close();
		System.out.println("done");
	}

}
