package com.pawelgorny.wifsolver;

import org.bitcoinj.core.Base58;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.LegacyAddress;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class WorkerSearch extends Worker {

    private final Configuration configuration;
    private final Map<Integer, char[]> GUESS = new HashMap<>(0);
    private final Map<Integer, Integer> GUESS_IX = new HashMap<>(0);
    private final StringBuilder WIF;
    private final int[] STARTER;
    private final int THREADS_MIN = 2;
    private boolean START_ZERO;
    private Integer THREAD_BREAK = null;
    private String RESULT = null;
    private long start = 0;
    private int THREADS = THREADS_MIN;

    public WorkerSearch(Configuration configuration) {
        super(configuration);
        this.configuration = configuration;
        WIF = new StringBuilder(configuration.getWif());
        STARTER = new int[configuration.getWif().length()];
        configureHints();
        configureStart();
    }

    private static int findIx(char[] a, char v) {
        int i = 0;
        for (char c : a) {
            if (c == v) {
                return i;
            }
            i++;
        }
        return 0;
    }

    @Override
    protected void run() throws InterruptedException {
        start = System.currentTimeMillis();
        setLoop(0, null, false);
    }

    private void setLoop(int ix, StringBuilder ownWif, boolean inThread) throws InterruptedException {
        if (ownWif == null){
            ownWif = new StringBuilder(WIF);
        }
        if (ix==GUESS_IX.size()){
            if (!START_ZERO){
                configureStartZero();
            }
            String localResult = super.workThread(ownWif.toString());
            if (localResult!=null){
                RESULT = localResult;
            }
            return;
        }
        int position = GUESS_IX.get(ix);
        if (THREAD_BREAK == null || THREAD_BREAK>ix || inThread) {
            if (!inThread){
                if (System.currentTimeMillis()-start > Configuration.getStatusPeriod()){
                    System.out.println("Alive! "+ ownWif.toString() + " " + (new Date()));
                    start = System.currentTimeMillis();
                }
            }
            for (int i = STARTER[position]; RESULT == null && i < GUESS.get(position).length; i++) {
                ownWif.setCharAt(position, GUESS.get(position)[i]);
                setLoop(ix + 1, ownWif, inThread);
            }
        }else{
            final CountDownLatch latch = new CountDownLatch(THREADS);
            ExecutorService executorService = Executors.newFixedThreadPool(THREADS);
            final StringBuilder ownWifTemp = new StringBuilder(ownWif);
            final int maxPos = GUESS.get(position).length/THREADS;
            for (int t=0; t<THREADS; t++){
                int p = maxPos * t;
                final int rangeStart = Math.max(p, STARTER[position]);
                final int rangeEnd = t==THREADS-1?GUESS.get(position).length:Math.min(p+maxPos, GUESS.get(position).length);
                executorService.submit(() -> {
                    try {
                        StringBuilder localStringBuilder = new StringBuilder(ownWifTemp);
                        for (int i = rangeStart; i < rangeEnd; i++) {
                            localStringBuilder.setCharAt(position, GUESS.get(position)[i]);
                            setLoop(ix + 1, localStringBuilder, true);
                        }
                        latch.countDown();
                    } catch (Exception e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            latch.await();
            executorService.shutdown();
        }
    }

    private void configureStart() {
        if (configuration.getWifStatus()==null){
            START_ZERO = true;
            return;
        }
        for(Map.Entry<Integer, char[]> e:GUESS.entrySet()){
            STARTER[e.getKey()] = findIx(e.getValue(), configuration.getWifStatus().charAt(e.getKey()));
        }
        START_ZERO = false;
    }

    private void configureStartZero() {
        for (int i=0; i<STARTER.length; i++){
            STARTER[i] = 0;
        }
    }

    private void configureHints() {
        int guessIx = 0;
        List<Integer> ranges = new ArrayList<>(0);
        for (int c=0; c<configuration.getWif().length(); c++){
            if (Configuration.UNKNOWN_CHAR == configuration.getWif().charAt(c)){
                char[] hints = Base58.ALPHABET;
                if (configuration.getGuess() != null && configuration.getGuess().get(guessIx) != null) {
                    hints = configuration.getGuess().get(guessIx++);
                }
                GUESS.put(c, hints);
                GUESS_IX.put(GUESS_IX.size(), c);
                ranges.add(hints.length);
            }
        }
        long count = 0;
        for (int c=ranges.size()-1; c>=0; c--){
            if (count == 0){
                count = ranges.get(c);
            }else {
                count*=ranges.get(c);
            }
            if (count>=1_000_000 && ranges.get(c)>1){
                THREAD_BREAK = c;
                if (ranges.get(c) > THREADS_MIN) {
                    int procs = Runtime.getRuntime().availableProcessors();
                    if (procs < 1) {
                        procs = THREADS_MIN;
                    }
                    THREADS = Math.min(procs, ranges.get(c));
                }
                break;
            }
        }
    }
}
