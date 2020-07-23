package com.pawelgorny.wifsolver;

import java.util.*;

public class WorkerAlike extends Worker {

    private final Configuration configuration;
    private final StringBuilder WIF;
    private final Map<Integer, char[]> GUESS = new HashMap<>(0);
    private final Map<Integer, Integer> GUESS_IX = new HashMap<>(0);
    private String RESULT = null;
    private long start = 0;

    public WorkerAlike(Configuration configuration) {
        super(configuration);
        this.configuration = configuration;
        WIF = new StringBuilder(configuration.getWif());
        configureHints();
    }

    private void configureHints() {
        for (int c=0; c<configuration.getWif().length(); c++){
            for (Map.Entry<Integer, char[]> similarCharacters : configuration.getGuess().entrySet()){
                if (findIx(similarCharacters.getValue(), configuration.getWif().charAt(c))!=-1){
                    GUESS.put(c, similarCharacters.getValue());
                    GUESS_IX.put(GUESS_IX.size(), c);
                    break;
                }
            }
        }
    }

    @Override
    void run() throws InterruptedException {
        start = System.currentTimeMillis();
        setLoop(0);
    }

    private void setLoop(int ix) throws InterruptedException {
        if (ix==GUESS_IX.size()){
            String localResult = super.workThread(WIF.toString());
            if (localResult!=null){
                RESULT = localResult;
            }
            return;
        }

        int position = GUESS_IX.get(ix);
        if (System.currentTimeMillis()-start > Configuration.getStatusPeriod()){
            System.out.println("Alive! "+ WIF.toString() + " " + (new Date()));
            start = System.currentTimeMillis();
        }
        for (int i = 0; RESULT == null && i < GUESS.get(position).length; i++) {
            WIF.setCharAt(position, GUESS.get(position)[i]);
            setLoop(ix + 1);
        }
    }

    private static int findIx(char[] a, char v){
        int i = 0;
        for (char c:a){
            if (c==v){
                return i;
            }
            i++;
        }
        return -1;
    }


}
