package correcter;

import java.io.*;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Write a mode: ");
        String mode = scanner.nextLine();
        switch (mode) {
            case "encode" : encode(new File("send.txt"), new File("encoded.txt"));
                break;
            case "send" : send(new File("encoded.txt"), new File("received.txt"));
                break;
            case "decode" : decode(new File("received.txt"), new File("decoded.txt"));
        }

    }

    public static void encode(File inputFile, File outputFile) {
        StringBuilder stringBuilder = new StringBuilder();

        try (FileInputStream inputStream = new FileInputStream(inputFile);
             FileOutputStream outputStream = new FileOutputStream(outputFile, false)) {

            byte[] bytesIn = inputStream.readAllBytes();
            for (byte b : bytesIn) {
                char[] bits = String.format("%8s", Integer.toString(Byte.toUnsignedInt(b), 2)).replace(' ', '0').toCharArray();

                for (int i = 0; i <= bits.length/2; i += 4) {

                    char[] hamming = new char[bits.length];
                    hamming[2] = bits[i];
                    hamming[4] = bits[i+1];
                    hamming[5] = bits[i+2];
                    hamming[6] = bits[i+3];
                    hamming[7] = '0';

                    for (int j = 0; j < 3; j++) {
                        int parityIndex = (int)Math.pow(2, j) - 1;
                        hamming[parityIndex] = getParity(hamming, parityIndex);
                    }

                    stringBuilder.append(hamming);
                }
            }
            String s = stringBuilder.toString();
            // write string sequences of bits as bytes
            while (!s.isBlank()) {
                outputStream.write(Integer.parseInt(s.substring(0, 8), 2));
                s = s.substring(8);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static char getParity(char[] arr, int index) {
        List<Integer> powTwo = new ArrayList<>();
        int oneCount = 0;

        switch (index) {
            case 0 : powTwo.addAll(List.of(2, 4, 6));
                break;
            case 1 : powTwo.addAll(List.of(2, 5, 6));
                break;
            case 3 : powTwo.addAll(List.of(4, 5, 6));
        }

        for (Integer x : powTwo) {
            if (arr[x] == '1') oneCount++;
        }

        return oneCount % 2 == 0 ? '0' : '1';
    }

    public static void send(File inputFile, File outputFile) {
        Random r = new Random();
        try (FileInputStream inputStream = new FileInputStream(inputFile);
             FileOutputStream outputStream = new FileOutputStream(outputFile, false)) {

            byte[] bytesIn = inputStream.readAllBytes();
            byte[] bytesOut = new byte[bytesIn.length];

            for (int i = 0; i < bytesIn.length; i++) {
                // fill out array of bytes with corruption of random bit
                bytesOut[i] = bytesIn[i] ^= 1 << r.nextInt(7);
            }
            outputStream.write(bytesOut);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static char[] correctError(char[] incorrectBits) {
        char[] encoded = new char[4];
        int indexOfError = 0;

        for (int j = 0; j < 3; j++) {
            int parityIndex = (int) Math.pow(2, j) - 1;
            char parity = getParity(incorrectBits, parityIndex);

            if (incorrectBits[parityIndex] != parity) {
                indexOfError += (parityIndex + 1);
            }
        }

        if (indexOfError > 0) {
            //исправляем ошибку
            incorrectBits[(indexOfError - 1)] = incorrectBits[(indexOfError - 1)] == '0' ? '1' : '0';
        }

        encoded[0] = incorrectBits[2];
        encoded[1] = incorrectBits[4];
        encoded[2] = incorrectBits[5];
        encoded[3] = incorrectBits[6];

        return encoded;
    }

    public static void decode(File inputFile, File outputFile) {
        StringBuilder stringBuilder = new StringBuilder();

        try (FileInputStream inputStream = new FileInputStream(inputFile);
             FileOutputStream outputStream = new FileOutputStream(outputFile, false)) {

            byte[] bytesIn = inputStream.readAllBytes();

            for (byte incorrectByte : bytesIn) {
                // перевели кривой байт в массив битов
                char[] incorrectBits = String.format("%8s", Integer.toString(Byte.toUnsignedInt(incorrectByte), 2)).replace(' ', '0').toCharArray();
                //ищем ошибку и исправляем
                stringBuilder.append(correctError(incorrectBits));
            }

            String s = stringBuilder.toString();
            while (!s.isBlank()) {
                outputStream.write(Integer.parseInt(s.substring(0, 8), 2));
                s = s.substring(8);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}