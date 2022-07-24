package org.example;

import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;


public class Main {
    public static void main(String[] args) throws IOException {
        Terminal terminal = TerminalBuilder.builder()
                .build();

        LineReader lineReader = LineReaderBuilder.builder()
                .terminal(terminal)
                .build();

        String prompt = "fog> ";
        while (true) {
            String line;
            try {
                line = lineReader.readLine(prompt);
                System.out.println(line);
            } catch (UserInterruptException e) {
                // Do nothing
            } catch (EndOfFileException e) {
                System.out.println("\nBye.");
                return;
            }
        }
    }
}