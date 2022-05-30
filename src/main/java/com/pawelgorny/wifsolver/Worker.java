package com.pawelgorny.wifsolver;

import org.bitcoinj.core.*;

import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

class Worker {

    private static final SortedSet<String> WIF_RESULTS  = Collections.synchronizedSortedSet(new TreeSet<>());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
    private final Configuration configuration;
    private final long timeId = System.currentTimeMillis();

    protected boolean DUMMY_CHECKSUM = false;

    public Worker(Configuration configuration) {
        this.configuration = configuration;
    }

    void run() throws InterruptedException {
        Worker worker = null;
        System.out.println("--- Starting worker --- "+(new Date())+" ---");
        sendEmail("Starting worker '"+configuration.getWork().name()+"'", configuration.getWif());

        switch (configuration.getWork()){
            case END:
                worker = new WorkerEnd(configuration);
                break;
            case ROTATE:
                worker = new WorkerRotate(configuration);
                break;
            case SEARCH:
                worker = new WorkerSearch(configuration);
                break;
            case JUMP:
                worker = new WorkerJump(configuration);
                break;
            case ALIKE:
                worker = new WorkerAlike(configuration);
                break;
        }
        worker.run();
        System.out.println("");
        System.out.println("");
        System.out.println("--- Work finished ---");
        System.out.println("Worker '"+configuration.getWork().name()+"' ended, "+WIF_RESULTS.size()+" result(s) "+(new Date()));
        WIF_RESULTS.forEach(System.out::println);
        if (!WIF_RESULTS.isEmpty()) {
            resultToFile();
        }
        sendEmail("Worker '"+configuration.getWork().name()+"' ended, "+WIF_RESULTS.size()+" result(s)", String.join("\n", WIF_RESULTS));
    }

    private void resultToFile() {
        try {
            FileWriter fileWriter = new FileWriter(this.configuration.getWork().name() + "_result_" + dateFormat.format(new Date()) + ".txt", false);
            for (String s : WIF_RESULTS) {
                fileWriter.write(s);
                fileWriter.write("\r\n");
            }
            fileWriter.close();
        } catch (IOException e) {
            System.out.println("Cannot write to file: " + e.getLocalizedMessage());
        }
    }

    protected synchronized void resultToFilePartial(String data) {
        try {
            FileWriter fileWriter = new FileWriter(this.configuration.getWork().name() + "_resultPartial_" + timeId + ".txt", true);
            fileWriter.write(data);
            fileWriter.write("\r\n");
            fileWriter.close();
        } catch (IOException e) {
            System.out.println("Cannot write to file: " + e.getLocalizedMessage());
        }
    }

    void addResult(String data){
        WIF_RESULTS.add(data);
    }

    private void sendEmail(String subject, String body){
        if (configuration.getEmailConfiguration()==null){
            return;
        }
        try {
            MimeMessage message = new MimeMessage(configuration.getEmailConfiguration().getMailSession());
            message.setFrom(new InternetAddress(configuration.getEmailConfiguration().getEmailFrom()));
            message.setRecipients(javax.mail.Message.RecipientType.TO,
                    InternetAddress.parse(configuration.getEmailConfiguration().getEmailTo()));
            message.setSubject(subject);
            message.setText(body);
            Transport.send(message);
        } catch (MessagingException e) {
            System.err.println(e.getLocalizedMessage());
            e.printStackTrace();
            System.exit(0);
        }
    }


    protected String workThread(String suspect){
        try{
            ECKey ecKey = null;
            if (this.configuration.isCompressed() || DUMMY_CHECKSUM) {
                byte[] bytes = Base58.decode(suspect);
                if (this.configuration.isCompressed() && bytes[33] != 1) {
                    return null;
                }
                if (DUMMY_CHECKSUM) {
                    ecKey = ECKey.fromPrivate(Arrays.copyOfRange(bytes, 1, bytes.length - 4), this.configuration.isCompressed());
                }
            }
            if (ecKey == null) {
                ecKey = DumpedPrivateKey.fromBase58(Configuration.getNetworkParameters(), suspect).getKey();
                if (!this.configuration.isCompressed()) {
                    ecKey = ecKey.decompress();
                }
            }
            if (configuration.getAddress() != null) {
                if (Arrays.equals(configuration.getAddressHash(), ecKey.getPubKeyHash())) {
                    Address foundAddress = LegacyAddress.fromKey(Configuration.getNetworkParameters(), ecKey);
                    String data = suspect + " -> " + foundAddress.toString() + " [" + ecKey.getPrivateKeyAsHex() + "]";
                    if (DUMMY_CHECKSUM) {
                        suspect = rewriteWIF(suspect);
                    }
                    addResult(data);
                    System.out.println(data);
                    return suspect;
                }
                if (!WORK.END.equals(configuration.getWork()) && !(WORK.SEARCH.equals(configuration.getWork()) && DUMMY_CHECKSUM)) {
                    Address foundAddress = LegacyAddress.fromKey(Configuration.getNetworkParameters(), ecKey);
                    String data = suspect + " -> " + foundAddress.toString() + " [" + ecKey.getPrivateKeyAsHex() + "]";
                    addResult(data);
                    System.out.println(data);
                    resultToFilePartial(data);
                }
            } else {
                Address foundAddress = LegacyAddress.fromKey(Configuration.getNetworkParameters(), ecKey);
                if (DUMMY_CHECKSUM) {
                    suspect = rewriteWIF(suspect);
                }
                String data = suspect + " -> " + foundAddress.toString() + " [" + ecKey.getPrivateKeyAsHex() + "]";
                addResult(data);
                System.out.println(data);
                resultToFilePartial(data);
            }
        }catch (Exception ex){
            //wif incorrect (checksum?)
        }
        return null;
    }

    private String rewriteWIF(String suspectFakeChecksum) {
        byte[] bytes = Base58.decode(suspectFakeChecksum);
        bytes = Arrays.copyOfRange(bytes, 1, bytes.length - 4);
        return Base58.encodeChecked(128, bytes);
    }

    protected long getResultsCount() {
        return WIF_RESULTS.size();
    }

}


