package com.pawelgorny.wifsolver;

import org.bitcoinj.core.Base58;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.LegacyAddress;

import java.util.Arrays;
import java.util.Date;

class WorkerEnd extends Worker {

    private final Configuration configuration;

    private boolean found = false;
    private String lastTested = "";

    public WorkerEnd(Configuration configuration) {
        super(configuration);
        this.configuration = configuration;
    }

    private static boolean arrFinished(int[] array) {
        for (int i : array) {
            if (i < 57) {
                return false;
            }
        }
        return true;
    }

    private static void increment58(int[] array) {
        int i = array.length - 1;
        do {
            array[i] = (array[i] + 1) % 58;
        } while (array[i--] == 0 && i >= 0);
    }

    protected void run(){
        String wif = configuration.getWif();
        String address = configuration.getTargetAddress();

        int len = 51;
        if (wif.startsWith("L")||wif.startsWith("K")){
            len = Configuration.COMPRESSED_WIF_LENGTH;
        }
        int missing = len - wif.length();
        if (missing <= 0) {
            System.out.println("nothing to do?");
            System.exit(0);
        }
        StringBuilder sb = new StringBuilder(len);
        if (missing <= Configuration.getChecksumChars()) {
            System.out.println("Missing less than " + Configuration.getChecksumChars() + " last characters, quick check launched");
            checksumCheck(missing, wif, len, null);
        } else {
            int[] arr = new int[missing - Configuration.getChecksumChars()];
            long start = System.currentTimeMillis();
            while (!arrFinished(arr)) {
                if (System.currentTimeMillis() - start > Configuration.getStatusPeriod()) {
                    System.out.println("PING! " + (new Date()) + " " + sb.toString());
                    start = System.currentTimeMillis();
                }
                sb.setLength(0);
                sb.append(wif);
                for (int anArr : arr) {
                    sb.append(Base58.ALPHABET[anArr]);
                }
                if (checksumCheck(Configuration.getChecksumChars(), sb.toString(), len, address)) {
                    return;
                }
                increment58(arr);
            }
        }
        if (!found) {
            System.out.println("WIF not found");
        }
    }

    private boolean checksumCheck(int missing, String wif, int len, String address) {
        StringBuilder sb = new StringBuilder(len);
        sb.append(wif);
        for (int m = 0; m < missing; m++) {
            sb.append("1");
        }
        byte[] bytes = Base58.decode(sb.toString());
        bytes = Arrays.copyOfRange(bytes, 1, bytes.length - 4);
        if (len == Configuration.COMPRESSED_WIF_LENGTH) {
            bytes[32] = 1;
        }
        String encoded = Base58.encodeChecked(128, bytes);
        if (lastTested.equals(encoded)) {
            return false;
        }
        lastTested = encoded;
        if (encoded.startsWith(configuration.getWif())) {
            ECKey ecKey = DumpedPrivateKey.fromBase58(Configuration.getNetworkParameters(), encoded).getKey();
            String foundAddress = len == Configuration.COMPRESSED_WIF_LENGTH ? LegacyAddress.fromKey(Configuration.getNetworkParameters(), ecKey).toString()
                    : LegacyAddress.fromKey(Configuration.getNetworkParameters(), ecKey.decompress()).toString();
            if (address != null) {
                if (foundAddress.equals(address)) {
                    found = true;
                    super.addResult(encoded + " -> " + foundAddress);
                    System.out.println(encoded + " -> " + foundAddress);
                    return true;
                }
            } else {
                found = true;
                System.out.println(encoded + " -> " + foundAddress);
                super.addResult(encoded + " -> " + foundAddress);
            }
        }
        return false;
    }
}
