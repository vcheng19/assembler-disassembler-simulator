package assembler;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by jiaweizhang on 10/31/2016.
 */
public class Assembler {
    private List<Line> lines;
    private List<Line> data;
    private List<Line> text;
    private ISA isa;
    private List<Error> errors;
    private Map<String, Integer> dataMap;
    private DataMemory dataMemory;

    public Assembler(List<String> lines, ISA isa) {
        this.lines = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            Line line = new Line();
            line.lineNumber = i;
            line.originalLine = lines.get(i);
            line.line = lines.get(i);
        }

        this.isa = isa;
        this.dataMap = new HashMap<>();
        this.dataMemory = new DataMemory();
    }

    public void assemble() {
        // remove comments and whitespace
        lines.forEach(l -> {
            int commentIndex;
            if ((commentIndex = l.line.indexOf("#")) != -1) {
                // comment exists
                l.line = l.line.substring(0, commentIndex);
            }
            l.line = l.line.trim();
        });

        // remove empty lines
        lines = lines.stream().filter(l -> l.line.length() > 0).collect(Collectors.toList());

        // split into text and data
        int textBegin = -1;
        int dataBegin = -1;

        for (int i = 0; i < lines.size(); i++) {
            Line l = lines.get(i);
            if (l.line.startsWith(".text")) {
                if (textBegin == -1) {
                    // first occurrence
                    textBegin = i;
                } else {
                    // second occurrence
                    report(l, "More than one occurrence of .text");
                }
            } else if (l.line.startsWith(".data")) {
                if (dataBegin == -1) {
                    // first occurrence
                    dataBegin = i;
                } else {
                    // second occurrence
                    report(l, "More than one occurrence of .data");
                }
            }
        }

        if (textBegin == 0) {
            text = lines.subList(1, dataBegin);
            data = lines.subList(dataBegin + 1, lines.size());
        } else if (dataBegin == 0) {
            data = lines.subList(1, textBegin);
            text = lines.subList(textBegin + 1, lines.size());
        } else {
            report(lines.get(0), "File must begin with .text or .data");
        }

        // process data
        processData();

        // process text
        processText();

    }

    private void processData() {
        data.forEach(d -> {
            String[] strings = d.line.split("\\s+");
            if (strings.length != 3) {
                report(d, "data must contain label, type, and value");
                return;
            }
            String label = validateDataLabel(strings[0], d);
            switch (strings[1]) {
                case ".char":
                    //
                    if (strings[2].length() != 1) {
                        report(d, "failed to parse char. Not a single char");
                        return;
                    }
                    int intValue = strings[2].charAt(0);
                    int index = dataMemory.addData(Collections.singletonList(intValue));
                    dataMap.put(label, index);
                    break;
                case ".word":
                    //
                    int value = 0;
                    if (strings[2].startsWith("0x") || strings[2].startsWith("0X")) {
                        // parse as hex
                        try {
                            value = Integer.parseInt(strings[2], 16);
                        } catch (Exception e) {
                            report(d, "failed to parse hex word");
                            return;
                        }
                    } else {
                        // parse as dec
                        try {
                            value = Integer.parseInt(strings[2]);
                        } catch (Exception e) {
                            report(d, "failed to parse decimal word");
                            return;
                        }
                    }
                    int index2 = dataMemory.addData(Collections.singletonList(value));
                    dataMap.put(label, index2);
                    break;
                case ".string":
                    //
                    List<Integer> characters = new ArrayList<Integer>();
                    for (int i = 0; i < strings[2].length(); i++) {
                        // for each character
                        characters.add((int) strings[2].charAt(i));
                    }
                    int index3 = dataMemory.addData(characters);
                    dataMap.put(label, index3);
                    break;
                default:
                    report(d, "invalid data type");
                    return;
            }
        });
    }

    private void processText() {

    }

    private String validateDataLabel(String label, Line d) {
        // make sure ends with :
        if (label.charAt(label.length() - 1) != ':') {
            report(d, "Invalid label, must end with :");
        }
        // make sure not taken
        if (dataMap.containsKey(label)) {
            report(d, "Duplicate declaration of key");
        }
        // make sure at least 2 length
        if (label.length() < 2) {
            report(d, "Invalid label");
        }
        return label.substring(0, label.length() - 1);
    }

    private void report(Line line, String message) {
        // make deep copy of line
        Line newLine = new Line();
        newLine.lineNumber = line.lineNumber;
        newLine.line = line.line;
        newLine.originalLine = line.originalLine;
        Error error = new Error(newLine, message);
        errors.add(error);
    }
}
