package com.pawelgorny.wifsolver;

import org.bitcoinj.core.Base58;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.LegacyAddress;

import java.math.BigInteger;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class WorkerJump extends Worker {

    private final Configuration configuration;
    private final long ITERATIONS_PER_THREAD = 2_000_000L;
    private BigInteger LIMIT;
    private BigInteger DIFF;
    private String STARTER;
    private String RESULT = null;


    public WorkerJump(Configuration configuration) {
        super(configuration);
        this.configuration = configuration;
        setup();
    }


    @Override
    protected void run() throws InterruptedException {
        final int NUMBER_OF_THREADS = 2;
        long start = System.currentTimeMillis();
        byte[] bytes = Base58.decode(STARTER);
        String hex = bytesToHex(bytes);
        BigInteger priv = new BigInteger(hex, 16);
        final String suffix = hex.substring(hex.length() - 8);
        long count = 0;
        long jumpNb = 0;
        long falseJump =0;
        long firstJump =0;
        Set<Long> jumpLength = new HashSet<>(2);
        while(jumpNb<10) {
            do {
                count++;
                priv = priv.add(DIFF);
                bytes = hexStringToByteArray(priv.toString(16));
            } while (count < 8192 && (bytes[33] != 1 || !priv.toString(16).endsWith(suffix)));
            if (count==8192){
                System.out.println(":-( Skipping " + STARTER + ", cannot find the proper ending");
                return;
            }else{
                if (bytes[33] != 1){
                    falseJump++;
                    if(falseJump==2048){
                        System.out.println(":-( Skipping " + STARTER + ", cannot find the proper compression byte");
                        return;
                    }
                }else {
                    if (firstJump==0){
                        firstJump = count;
                    }
                    if (++jumpNb<10) {
                        jumpLength.add(count);
                        count = 0;
                    }
                }
            }
        }
        if (jumpLength.size()>2){
            System.out.println(":-( Skipping " + STARTER + ", cannot find the jump length");
            return;
        }

        ExecutorService executorService;
        System.out.println("Jumping " + count+ " for " + STARTER);
        BigInteger toAdd =DIFF.multiply(BigInteger.valueOf(count));
        BigInteger threadDiff = toAdd.multiply(BigInteger.valueOf(ITERATIONS_PER_THREAD));
        priv = new BigInteger(hex, 16).add(DIFF.multiply(BigInteger.valueOf(firstJump)));
        System.out.println("Root: " + priv.toString(16) + " " + Base58.encode(hexStringToByteArray(priv.toString(16))));
        if (check(priv)){
            return;
        }
        do {
            if (System.currentTimeMillis()-start > Configuration.getStatusPeriod()){
                System.out.println("Alive! "+ priv.toString(16) + " " + Base58.encode(hexStringToByteArray(priv.toString(16))) + " " + (new Date()));
                start = System.currentTimeMillis();
            }
            executorService = Executors.newFixedThreadPool(NUMBER_OF_THREADS);
            final CountDownLatch latch = new CountDownLatch(NUMBER_OF_THREADS);
            BigInteger privThread1 = priv;
            executorService.submit(() -> {
                try {
                    workerThread(privThread1, toAdd);
                    latch.countDown();
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
            });
            BigInteger privThread2 = priv.add(threadDiff);
            executorService.submit(() -> {
                try {
                    workerThread(privThread2, toAdd);
                    latch.countDown();
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
            });
            latch.await();
            executorService.shutdown();
            for (int i=0;i<NUMBER_OF_THREADS;i++) {
                priv = priv.add(threadDiff);
            }
        }while (LIMIT.compareTo(priv)>0 && RESULT==null);
        if(RESULT!=null){
            System.out.println("RESULT:"+RESULT);
        }else {
            System.out.println("NO RESULT for "+configuration.getWif());
        }

    }

    private void workerThread(BigInteger privThread, BigInteger step){
        int iterations = 0;

        while(iterations<ITERATIONS_PER_THREAD && RESULT==null){
            iterations++;
            privThread = privThread.add(step);
            check(privThread);
        }
    }

    private boolean check(BigInteger privThread){
        byte[] bytes = hexStringToByteArray(privThread.toString(16));
        try {
            ECKey ecKey = DumpedPrivateKey.fromBase58(Configuration.getNetworkParameters(), Base58.encode(bytes)).getKey();
            String encoded =  Base58.encode(bytes);
            String foundAddress = encoded.length()==52?LegacyAddress.fromKey(Configuration.getNetworkParameters(), ecKey).toString()
                    :LegacyAddress.fromKey(Configuration.getNetworkParameters(), ecKey.decompress()).toString();
            if(configuration.getTargetAddress().equals(foundAddress)){
                super.addResult(encoded + " -> " + foundAddress);
                System.out.println(encoded + " -> " + foundAddress);
                RESULT = encoded;
                return true;
            }else{
                super.addResult(encoded + " -> " + foundAddress);
                System.out.println(encoded + " -> " + foundAddress);
            }
        }catch (Exception e){
            //incorrent wif
        }
        return false;
    }


    private void setup() {
        for (int p=0, i=configuration.getWif().length()-1; i>=0; i--, p++){
            if (configuration.getWif().charAt(i)==Configuration.UNKNOWN_CHAR){
                DIFF = BigInteger.ONE.multiply(BigInteger.valueOf(58L)).pow(p);
                break;
            }
        }
        if (configuration.getWifStatus()==null){
            STARTER = configuration.getWif().replaceAll(String.valueOf(Configuration.UNKNOWN_CHAR),String.valueOf(Base58.ALPHABET[0]));
        }else {
            STARTER = configuration.getWifStatus();
        }
        String wifTop = configuration.getWif().replaceAll(String.valueOf(Configuration.UNKNOWN_CHAR),String.valueOf(Base58.ALPHABET[Base58.ALPHABET.length-1]));
        byte[] bytes = Base58.decode(wifTop);
        String hex = bytesToHex(bytes);
        LIMIT = new BigInteger(hex, 16);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }

    private byte[] hexStringToByteArray(String s) {
        byte[] data = new byte[38];
        for (int i = 0; i < 76; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }


}
