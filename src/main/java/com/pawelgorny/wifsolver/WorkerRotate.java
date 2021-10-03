package com.pawelgorny.wifsolver;

import org.bitcoinj.core.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class WorkerRotate extends Worker {

    private final Configuration configuration;

    private boolean FOUND = false;
    private int LENGTH = 0;
    private int mistakesCount = 0;
    private int THREADS = 2;

    public WorkerRotate(Configuration configuration) {
        super(configuration);
        this.configuration=configuration;
        LENGTH = configuration.getWif().length();
        if (configuration.getWifStatus() != null) {
            mistakesCount = Integer.valueOf(configuration.getWifStatus().trim());
        }
        int procs = Runtime.getRuntime().availableProcessors();
        if (procs < 1) {
            procs = 2;
        }
        THREADS = Math.min(procs, Base58.ALPHABET.length);
    }

    @Override
    protected void run() throws InterruptedException {
        ECKey ecKey;
        StringBuilder stringBuilder = new StringBuilder(configuration.getWif());

        try {
            ecKey = DumpedPrivateKey.fromBase58(Configuration.getNetworkParameters(), configuration.getWif()).getKey();
            String foundAddress = this.configuration.isCompressed() ? LegacyAddress.fromKey(Configuration.getNetworkParameters(), ecKey).toString()
                    :LegacyAddress.fromKey(Configuration.getNetworkParameters(), ecKey.decompress()).toString();
            super.addResult(configuration.getWif() + " -> " + foundAddress);
            System.out.println(configuration.getWif() + " -> " + foundAddress);
            return;
        }catch (Exception e){
            System.out.println("Initial "+configuration.getWif()+" incorrect, starting rotation");
        }

        if (mistakesCount > 1) {
            System.out.print("Possible mistakes: " + mistakesCount);
            rotate(mistakesCount, new ArrayList<>(0), stringBuilder, true);
        } else {
            System.out.print("Possible mistakes: 1");
            rotate(1, null, stringBuilder, false);
            System.out.println(" - finished");
            System.out.println("Possible mistakes: 2");
            rotate(2, new ArrayList<>(0), stringBuilder, true);
        }
    }


    private void rotate(int depth, List<Integer> skip, StringBuilder local_stringBuilder, boolean reporter) {
        long start = System.currentTimeMillis();

        for (int position = 1; position < LENGTH && !FOUND; position++) {
            if (skip != null && skip.contains(position)) {
                continue;
            }
            for (int replacement = 0; replacement < Base58.ALPHABET.length && !FOUND; replacement++) {
                if (Base58.ALPHABET[replacement] == configuration.getWif().charAt(position)) {
                    continue;
                }
                local_stringBuilder.setCharAt(position, Base58.ALPHABET[replacement]);
                if (depth == 1) {
                    if (test(local_stringBuilder.toString())) {
                        return;
                    }
                } else {
                    List<Integer> toSkip = new ArrayList<>(skip);
                    toSkip.add(position);
                    rotate(depth - 1, toSkip, local_stringBuilder, false);
                }
                if (reporter && System.currentTimeMillis() - start > Configuration.getStatusPeriod()) {
                    System.out.println();
                    System.out.println("Alive! " + (new Date()) + " Currently found: " + getResultsCount());
                    start = System.currentTimeMillis();
                }
            }
            local_stringBuilder.setCharAt(position, configuration.getWif().charAt(position));
            if (reporter && !FOUND) {
                System.out.print('.');
            }
        }
    }


    private boolean test(String suspect) {
        try {
            ECKey ecKey = DumpedPrivateKey.fromBase58(Configuration.getNetworkParameters(), suspect).getKey();
            Address foundAddress = this.configuration.isCompressed() ? LegacyAddress.fromKey(Configuration.getNetworkParameters(), ecKey)
                    : LegacyAddress.fromKey(Configuration.getNetworkParameters(), ecKey.decompress());
            if (configuration.getTargetAddress() != null) {
                if (foundAddress.equals(configuration.getAddress())) {
                    FOUND = true;
                    super.addResult(suspect + " -> " + foundAddress);
                    System.out.println();
                    System.out.println("Expected address found:");
                    System.out.println(suspect + " -> " + foundAddress);
                    return true;
                }
            } else {
                super.addResult(suspect + " -> " + foundAddress);
                System.out.println(suspect + " -> " + foundAddress);
            }
        } catch (Exception e) {

        }
        return false;
    }

}
