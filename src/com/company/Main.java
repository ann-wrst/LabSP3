package com.company;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static class Transition {
        public int from;
        public int to;
        public char letter;

        public Transition(int f, char l, int t) {
            from = f;
            to = t;
            letter = l;
        }
    }

    public static String getLexemeType(int finalState) {
        return switch (finalState) {
            case (-1) -> "DECIMAL";
            case (-2) -> "HEXADECIMAL";
            case (-3) -> "FLOAT";
            case (-4) -> "STRING";
            case (-5) -> "IDENTIFIER";
            case (-6) -> "BRACKETS";
            case (-100) -> "ERROR";
            default -> "UNKNOWN";
        };
    }

    public static HashSet<Transition> FSMachine = new HashSet<>();

    public static HashSet<String> Reserved = new HashSet<>(Arrays.asList("var", "abstract", "arguments", "await", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "debugger", "default", "delete", "do", "double", "else", "enum",
            "eval", "export", "extends", "false", "final", "finally", "float", "for", "function", "goto", "if", "implements", "import", "in", "int", "interface", "let", "long", "new", "null", "package", "private", "protected",
            "public", "return", "short", "static", "switch", "this", "throw", "throws", "true", "try", "typeof", "void", "while", "async"));

    public static ArrayList<String> Operators = new ArrayList<>(Arrays.asList("===", "!==", "**=", "**", "++", "--", "&&", "||", "=", "+=", "-=", "*=", ">=", "<=", "==", "<<", ">>", "!=", "/=", "%=", "+", "-", "*", "/", "%", ">", "<",
            "?", "!", "&", "|", "~", "^", ",", ";", "."));

    public static ArrayList<String> lexemes;
    public static ArrayList<Lexeme> lexemesList = new ArrayList<>();

    static Pattern p = Pattern.compile("(===|!==|\\*\\*=|==|!=|\\+\\+|\\+=|-=|\\*=|\\/=|%=|>=|<=|&&|<<|>>|\\|\\||\\(|\\)|\\[|\\]|\\{|\\}|\\+|-|\\/|%|\\*—|=|>|<|\\?|!|&|~|\\^|,|;)");
    public static int currentState = 0;
    static StringBuilder code = new StringBuilder();
    static int First_State;
    static int CountOfTrans;
    static int finalStatesCount;
    static ArrayList<Integer> finalStates = new ArrayList<>();

    public static class Lexeme {
        public StringBuilder value;
        public String type;

        public Lexeme(String t, StringBuilder v) {
            type = t;
            value = v;
        }

        public void PrintInfo() {
            System.out.println(type + " " + value);
        }
    }

    public static void replaceAll(StringBuilder sb, String find, String replace) {
        Pattern p = Pattern.compile(find);
        Matcher matcher = p.matcher(sb);
        int startIndex = 0;
        while (matcher.find(startIndex)) {
            sb.replace(matcher.start(), matcher.end(), replace);
            startIndex = matcher.start() + replace.length();
        }
    }

    public static void main(String[] args) throws FileNotFoundException {
        initFSMachine("in_states.txt");
        read("input.txt");

        for (var l : lexemesList) {
            l.PrintInfo();
        }
    }

    public static void initFSMachine(String fileName) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(fileName));
        First_State = scanner.nextInt();
        CountOfTrans = scanner.nextInt();
        finalStatesCount = scanner.nextInt();
        for (int i = 0; i < finalStatesCount; i++) {
            finalStates.add(scanner.nextInt());
        }
        while (CountOfTrans-- > 0) {
            FSMachine.add(new Transition(scanner.nextInt(), scanner.next().charAt(0), scanner.nextInt()));

        }
        scanner.close();
    }

    public static void read(String fileName) {
        try (FileReader fileReader = new FileReader(fileName)) {
            Lexeme lexeme;
            int c;
            boolean isFound = false;
            while (-1 != (c = fileReader.read())) {
                code.append((char) c);
            }
            removeComments(code);
            processDelimiters(code);
            for (String lex : lexemes) {
                isFound = searchReserved(lex) || searchOperator(lex);
                if (isFound) continue;
                lex = lex.concat(" ");
                char[] Token = lex.toCharArray();
                char a;
                for (Character ch : Token) {
                    a = detectCharClass(ch);
                    Execute(a);
                }
                String type = getLexemeType(currentState);
                lexeme = new Lexeme(type, new StringBuilder(lex));
                lexemesList.add(lexeme);
                currentState = 0;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void removeComments(StringBuilder code) {
        char previous = ' ';
        int startOneLineComment = -1, startManyLinesComment = -1;
        for (int i = 0; i < code.length(); i++) {
            if (startOneLineComment == -1 && code.charAt(i) == '/' && previous == '/') {
                startOneLineComment = i;
            }
            if (startManyLinesComment == -1 && code.charAt(i) == '*' && previous == '/') {
                startManyLinesComment = i;
            }
            if (startManyLinesComment != -1 && ((code.charAt(i) == '/' && previous == '*') || (i == code.length() - 1))) {
                code.delete(startManyLinesComment - 1, i + 1);
                i = startManyLinesComment - 2;
                startManyLinesComment = -1;
            }
            if (startOneLineComment != -1 && (code.charAt(i) == '\n')) {
                code.delete(startOneLineComment - 1, i);
                i = startOneLineComment - 1;
                startOneLineComment = -1;
            }
            previous = code.charAt(i);
        }
    }

    public static void processDelimiters(StringBuilder code) {
        Matcher m = p.matcher(code);
        int poss = 0;
        while (m.find(poss)) {
            String replacement = " ";
            code.insert(m.end(), replacement);
            code.insert(m.start(), replacement);
            poss = m.end() + replacement.length();
        }
        replaceAll(code, "[ \t\r\n]+", " ");
        code = new StringBuilder(code.toString().trim());
        lexemes = new ArrayList<>(Arrays.asList(code.toString().split(" ")));

        for (var lex : lexemes) {
            lex = lex.trim();
        }
    }

    public static boolean searchReserved(String lex) {
        boolean isFound = false;
        for (String reserved : Reserved) {
            if (lex.equals(reserved)) {
                Lexeme lexeme = new Lexeme("RESERVED", new StringBuilder(lex));
                lexemesList.add(lexeme);
                isFound = true;
                break;
            }
        }
        return isFound;
    }

    public static boolean searchOperator(String lex) {
        boolean isFound = false;
        for (String operator : Operators) {
            if (lex.equals(operator)) {
                Lexeme lexeme = new Lexeme("OPERATOR", new StringBuilder(lex));
                lexemesList.add(lexeme);
                isFound = true;
                break;
            }
        }
        return isFound;
    }

    public static char detectCharClass(char ch) {
        if (ch >= '1' && ch <= '9') return 'n'; // a - from 1 to 9
        if ((ch >= 'A' && ch <= 'F') || (ch >= 'a' && ch <= 'f')) return 'l'; // l - from A(a) to F(f)
        if ((ch >= 'G' && ch <= 'W') || (ch >= 'g' && ch <= 'w') || (ch >= 'Y' && ch <= 'Z') || (ch >= 'y' && ch <= 'z')) return 't'; //t - other letters
            return switch (ch) {
                case '0' -> '0';
                case '.' -> '.';
                case '"', '`', '\'' -> '`';
                case 'x' -> 'x';
                case '}', '{', '(', ')', '[', ']' -> '(';
                case ' ' -> 'e';
                default -> 'D';
            };
    }

    public static void Execute(char a) {
        for (Transition tr : FSMachine) {
            if (tr.from == currentState && tr.letter == a) {
                currentState = tr.to;
                break;
            }
        }
    }
}
