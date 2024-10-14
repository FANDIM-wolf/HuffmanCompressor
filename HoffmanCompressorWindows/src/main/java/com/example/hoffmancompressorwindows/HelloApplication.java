package com.example.hoffmancompressorwindows;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import com.example.hoffmancompressorwindows.BitArray;
import com.example.hoffmancompressorwindows.CodeTreeNode;


public class HelloApplication extends Application {
    //private Stage primaryStage; // declare it as a class field
    private TextField filePathField; // declare it as a class field
    private Button compressButton;

    private static TreeMap<Character, Integer> countFrequency(String text) {
        TreeMap<Character, Integer> freqMap = new TreeMap<>();
        for (int i = 0; i < text.length(); i++) {
            Character c = text.charAt(i);
            Integer count = freqMap.get(c);
            freqMap.put(c, count != null ? count + 1 : 1);
        }
        return freqMap;
    }
    public ArrayList<CodeTreeNode> codeTreeNodes = new ArrayList<>();

    private static CodeTreeNode huffman(ArrayList<CodeTreeNode> codeTreeNodes) {
        while (codeTreeNodes.size() > 1) {
            Collections.sort(codeTreeNodes);
            CodeTreeNode left = codeTreeNodes.remove(codeTreeNodes.size() - 1);
            CodeTreeNode right = codeTreeNodes.remove(codeTreeNodes.size() - 1);

            CodeTreeNode parent = new CodeTreeNode(null, right.weight + left.weight, left, right);
            codeTreeNodes.add(parent);
        }
        return  codeTreeNodes.get(0);
    }

    private static String huffmanDecode(String encoded, CodeTreeNode tree) {
        StringBuilder decoded = new StringBuilder();

        CodeTreeNode node = tree;
        for (int i = 0; i < encoded.length(); i++) {
            node = encoded.charAt(i) == '0' ? node.left : node.right;
            if (node.content != null) {
                decoded.append(node.content);
                node = tree;
            }
        }
        return decoded.toString();
    }
    // сохранение таблицы частот и сжатой информации в файл
    private static void saveToFile(File output, Map<Character, Integer> frequencies, String bits) {
        try {
            DataOutputStream os = new DataOutputStream(new FileOutputStream(output));
            os.writeInt(frequencies.size());
            for (Character character: frequencies.keySet()) {
                os.writeChar(character);
                os.writeInt(frequencies.get(character));
            }
            int compressedSizeBits = bits.length();
            BitArray bitArray = new BitArray(compressedSizeBits);
            for (int i = 0; i < bits.length(); i++) {
                bitArray.set(i, bits.charAt(i) != '0' ? 1 : 0);
            }

            os.writeInt(compressedSizeBits);
            os.write(bitArray.bytes, 0, bitArray.getSizeInBytes());
            os.flush();
            os.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // загрузка сжатой информации и таблицы частот из файла
    private static void loadFromFile(File input, Map<Character, Integer> frequencies, StringBuilder bits) {
        try {
            DataInputStream os = new DataInputStream(new FileInputStream(input));
            int frequencyTableSize = os.readInt();
            for (int i = 0; i < frequencyTableSize; i++) {
                frequencies.put(os.readChar(), os.readInt());
            }
            int dataSizeBits = os.readInt();
            BitArray bitArray = new BitArray(dataSizeBits);
            os.read(bitArray.bytes, 0, bitArray.getSizeInBytes());
            os.close();

            for (int i = 0; i < bitArray.size; i++) {
                bits.append(bitArray.get(i) != 0 ? "1" : 0);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fileUncompress(String filePath) throws IOException {
        File file = new File(filePath);
        TreeMap<Character, Integer> frequencies2 = new TreeMap<>();
        StringBuilder encoded2 = new StringBuilder();
        this.codeTreeNodes.clear();

        System.out.println("Uncompressing file: " + filePath);

        // извлечение сжатой информации из файла
        loadFromFile(file, frequencies2, encoded2);

        System.out.println("Encoded file content: " + encoded2);

        // генерация листов и постоение кодового дерева Хаффмана на основе таблицы частот сжатого файла
        for (Character c : frequencies2.keySet()) {
            this.codeTreeNodes.add(new CodeTreeNode(c, frequencies2.get(c)));
        }
        CodeTreeNode tree2 = huffman(this.codeTreeNodes);

        System.out.println("Decoding file content...");

        // декодирование обратно исходной информации из сжатой
        String decoded = huffmanDecode(encoded2.toString(), tree2);

        System.out.println("Decoded file content: " + decoded);

        // сохранение в файл декодированной информации
        // Remove the ".huff" extension from the file path
        String originalFilePath = filePath.replace(".huff", "");
        Files.write(Paths.get(originalFilePath), decoded.getBytes());

        System.out.println("Uncompression successful! Original file saved to: " + originalFilePath);
    }
    private void fileCompress(String filePath) throws IOException {
        this.codeTreeNodes.clear();
        try {
            // загрузка содержимого файла в виде строки
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            System.out.println("Original file content: " + content);

            // вычисление таблицы частот с которыми встречаются символы в тексте
            TreeMap<Character, Integer> frequencies = countFrequency(content);

            // генерация листов будущего дерева для символов текста
            for (Character c : frequencies.keySet()) {
                this.codeTreeNodes.add(new CodeTreeNode(c, frequencies.get(c)));
            }
            // построение кодового дерева алгоритмом Хаффмана
            CodeTreeNode tree = huffman(this.codeTreeNodes);

            // постоение таблицы префиксных кодов для символов исходного текста
            TreeMap<Character, String> codes = new TreeMap<>();
            for (Character c : frequencies.keySet()) {
                codes.put(c, tree.getCodeForCharacter(c, ""));
            }

            // кодирование текста префиксными кодами
            StringBuilder encoded = new StringBuilder();
            for (int i = 0; i < content.length(); i++) {
                encoded.append(codes.get(content.charAt(i)));
            }

            System.out.println("Encoded file content: " + encoded);

            // сохранение сжатой информации в файл
            File file = new File(filePath + ".huff");
            saveToFile(file, frequencies, encoded.toString());
            this.codeTreeNodes.clear();

            System.out.println("Compression successful!");
        } catch (IOException e) {
            System.err.println("Error compressing file: " + e.getMessage());
        }
    }
    @Override
    public void start(Stage primaryStage) throws Exception {
        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(10));

        Label filePathLabel = new Label("File Path:");
        filePathField = new TextField();
        filePathField.setPrefWidth(300);

        Button browseButton = new Button("Browse");
        browseButton.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            File file = fileChooser.showOpenDialog(primaryStage);
            if (file != null) {
                filePathField.setText(file.getAbsolutePath());
            }
        });

        compressButton = new Button("Compress");
        compressButton.setOnAction(event -> {
            String filePath = filePathField.getText();
            if (filePath != null && !filePath.isEmpty()) {
                try {
                    fileCompress(filePath);
                    System.out.println("Compression successful!");
                } catch (IOException e) {
                    System.err.println("Error compressing file: " + e.getMessage());
                }
            }
        });

        Button uncompressButton = new Button("Uncompress");
        uncompressButton.setOnAction(event -> {
            String filePath = filePathField.getText();
            if (filePath != null && !filePath.isEmpty()) {
                try {
                    fileUncompress(filePath);
                    System.out.println("Uncompression successful!");
                } catch (IOException e) {
                    System.err.println("Error uncompressing file: " + e.getMessage());
                }
            }
        });



        root.getChildren().addAll(filePathLabel, filePathField, browseButton, compressButton, uncompressButton);

        Scene scene = new Scene(root, 400, 200);
        primaryStage.setScene(scene);
        primaryStage.setTitle("File Compressor");
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch();
    }
}