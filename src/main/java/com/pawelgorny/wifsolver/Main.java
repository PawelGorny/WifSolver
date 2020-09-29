package com.pawelgorny.wifsolver;

import org.bitcoinj.core.Base58;

import javax.mail.Session;
import java.io.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class Main {

    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, IOException, InterruptedException {
        if (args.length<1 || "--help".equals(args[0])){
            showFile("help.txt");
            showFile("footer.txt");
            System.exit(0);
        }
        Configuration configuration = readConfiguration(args[0]);
        if (args.length>1){
            readEmailConfiguration(configuration, args[1]);
        }
        Worker worker = new Worker(configuration);
        worker.run();
        showFile("footer.txt");
    }

    private static void showFile(String fileName) {
        String line;
        try {
            BufferedReader bufferReader = new BufferedReader(new InputStreamReader(Main.class.getResourceAsStream("/"+fileName)));
            while ((line = bufferReader.readLine()) != null) {
                line = line.trim();
                System.out.println(line);
            }
            bufferReader.close();
        } catch (IOException e) {
            System.err.println("error: " + e.getLocalizedMessage());
            System.exit(-1);
        }
    }

    private static void readEmailConfiguration(Configuration configuration, String file) {
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(file);
        } catch (FileNotFoundException e) {
            System.err.println("not found: " + file);
            System.exit(-1);
        }
        BufferedReader bufferReader = new BufferedReader(fileReader);
        String line;
        int lineNumber = 0;
        String host = null, port = "25", username = null, email = null;
        char[] password = new char[0];
        try {
            while ((line = bufferReader.readLine()) != null) {
                if (line.startsWith(Configuration.COMMENT_CHAR)){
                    continue;
                }
                switch (lineNumber){
                    case 0:
                        email = line;
                        break;
                    case 1:
                        String[] server = line.split(":");
                        host = server[0];
                        if(server.length>1){
                            port = server[1];
                        }
                        break;
                    case 2:
                        username = line;
                        break;
                    case 3:
                        password = line.toCharArray();
                        break;
                }
                lineNumber++;
            }
            Properties prop = new Properties();
            prop.put("mail.smtp.auth", true);
            prop.put("mail.smtp.starttls.enable", "true");
            prop.put("mail.smtp.host", host);
            prop.put("mail.smtp.port", port);
            final String _username = username;
            final String _password = new String(password);
            Session mailSession = Session.getInstance(prop, new javax.mail.Authenticator() {
                @Override
                protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                    return new javax.mail.PasswordAuthentication(_username, _password);
                }
            });
            for(char c:password){c=0;}
            configuration.setEmailConfiguration(email, mailSession);
            System.out.println("Email configured (to: '"+email+"', host: '"+host+"')");
        } catch (IOException e) {
            System.err.println("error: " + e.getLocalizedMessage());
            System.exit(-1);
        }finally {
            try {
                bufferReader.close();
            }catch (IOException ioe){
                //file problem?
            }
        }
    }

    private static Configuration readConfiguration(String file){
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(file);
        } catch (FileNotFoundException e) {
            System.err.println("not found: " + file);
            System.exit(-1);
        }
        BufferedReader bufferReader = new BufferedReader(fileReader);
        String line;
        int lineNumber = 0;
        WORK work = null;
        String targetAddress = null;
        String wif = null;
        String wifStatus = null;
        Map<Integer, char[]> guess = new HashMap<>();
        int guessPosition = 0;
        try {
            while ((line = bufferReader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith(Configuration.COMMENT_CHAR)){
                    continue;
                }
                switch (lineNumber){
                    case 0:
                        work = WORK.valueOf(line);
                        break;
                    case 1:
                        String[] wifs = line.split(",");
                        wif = wifs[0];
                        testWif(wif, work);
                        if (wifs.length>1){
                            wifStatus = wifs[1];
                        }
                        break;
                    case 2:
                        targetAddress = line;
                        break;
                }
                if ((WORK.SEARCH.equals(work)||WORK.ALIKE.equals(work)) && lineNumber>2){
                    guess.put(guessPosition++, filterGuess(line));
                }
                lineNumber++;
            }
            System.out.println("Requested '"+work+"' solver, source WIF: '"+wif+"'");
            if (targetAddress!=null){
                System.out.println("Expected address: '" + targetAddress + "'");
            }
        } catch (IOException e) {
            System.err.println("error: " + e.getLocalizedMessage());
            System.exit(-1);
        }finally {
            try {
                bufferReader.close();
            }catch (IOException ioe){
                //file problem?
            }
        }
        return new Configuration(targetAddress, wif, wifStatus, work, guess);
    }

    private static char[] filterGuess(String characters){
        String candidates = characters.trim();
        if (candidates.isEmpty() || "*".equals(candidates)){
            return Base58.ALPHABET;
        }
        List<Character> list = new ArrayList<>(candidates.length());
        for (int c=0; c<candidates.length(); c++){
            if (base58contains(candidates.charAt(c))){
                list.add(candidates.charAt(c));
            }
        }
        if (list.isEmpty()){
            return Base58.ALPHABET;
        }
        char[] result = new char[list.size()];
        for (int i=0; i<list.size(); i++){
            result[i]=list.get(i);
        }
        return result;
    }

    private static void testWif(String wif, WORK work) {
        for (int c=0; c<wif.length(); c++){
           if (!base58contains(wif.charAt(c))){
               if (Configuration.UNKNOWN_CHAR==wif.charAt(c)&&
                    (WORK.SEARCH.equals(work)||WORK.JUMP.equals(work))){
                   continue;
               }
               System.out.println("Error in WIF! Character '"+wif.charAt(c)+"' at position "+(++c)+" is not allowed.");
               System.out.println("Possible characters: "+(new String(Base58.ALPHABET)));
               System.exit(-1);
           }
        }
        if (WORK.ROTATE.equals(work)){
            if (wif.length() != 51 && wif.length() != Configuration.COMPRESSED_WIF_LENGTH) {
                System.out.println("Incorrect WIF length (" + wif.length() + "), should be " + Configuration.COMPRESSED_WIF_LENGTH + " or 51.");
                System.exit(-1);
            }
        }
    }
    private static boolean base58contains(final char v) {
        for(int i : Base58.ALPHABET){
            if(i == v){
                return true;
            }
        }
        return false;
    }
}
