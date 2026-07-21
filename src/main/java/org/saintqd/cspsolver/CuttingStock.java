package org.saintqd.cspsolver;

import org.apache.commons.lang3.tuple.Pair;
import org.saintqd.cspsolver.excepions.InvalidLengthException;
import org.saintqd.cspsolver.excepions.InvalidParameterException;

import java.util.*;

public class CuttingStock {

    private final int[] block;
    private final int[] qty;
    private int[] comb;
    private int[] tempComb;
    private int[] limit;
    private final int max;
    private final int total;
    private int counter=0;
    private final List<List<Pair<Integer,Integer>>> mapList = new ArrayList<>();
    private int count=0;

    public boolean hasMoreCombinations() {
        return count < counter;
    }

    public synchronized List<Pair<Integer,Integer>> nextCombination() {
        System.out.println(mapList.size());
        System.out.println(counter);
        List<Pair<Integer,Integer>> map = mapList.get(count);
        count++;
        return map;
    }

    public List<List<Pair<Integer,Integer>>> getCombinations() {
        return mapList;
    }

    public CuttingStock(int max, int[] block, int[] quantity) throws InvalidLengthException, InvalidParameterException {
        Arrays.stream(block).forEach((p) ->{
            if (p > max) {
                throw new InvalidLengthException();
            }
        });
        if (block.length != quantity.length)
            throw new InvalidLengthException();
        this.total=block.length;
        this.max=max;
        this.block=block;
        this.qty=quantity;
        this.initialize();
    }

    private void initialize() {
        List<Integer> store = new ArrayList<>();
        counter=0;
        this.sort();
        this.calculate(store);
    }

    private void sort() {
        int tmp;
        boolean swap;
        do {
            swap=false;
            for(int j=0;j<total-1;j++)
            {
                if(block[j+1]>block[j])
                {
                    tmp=block[j];
                    block[j]=block[j+1];
                    block[j+1]=tmp;

                    tmp=qty[j];
                    qty[j]=qty[j+1];
                    qty[j+1]=tmp;
                    swap=true;
                }
            }
        }
        while(swap);
    }

    private void calculate(List<Integer> store) {
        initLimit();
        boolean start=true,chaloo=true;
        int best=0,sum;
        comb=new int[total];
        while(start) {
            this.combinations();
            sum=0;
            for(int i=0; i < total; i++) {
                sum+=block[i]*comb[i];
                if(sum>max)
                {
                    sum=0;
                    break;
                }
            }


            if(sum>0) {
                if(sum==max) {
                    this.showComb(0,store);
                    resetComb();
                    updateLimit();
                    best=0;
                    sum=0;
                }
                else if (sum>best) {
                    best=sum;
                    tempComb =new int[total];
                    System.arraycopy(comb, 0, tempComb, 0, total);
                    sum=0;
                }
            }

            for(int i=0;i<total;i++) {
                if(comb[i]!=limit[i])
                {
                    chaloo=true;
                    break;
                }
                chaloo=false;
            }

            if (!chaloo) {
                this.showComb(best,store);
                updateLimit();
                resetComb();
                best=0;
            }

            for(int i=0; i < total; i++) {
                if(qty[i]==0 && i != total - 1)
                    continue;
                else if(i==total-1 && qty[i]==0)
                    start=false;
                break;
            }
        }
    }

    private void showComb(int a, List<Integer>store ) {
        counter++;

        boolean flag=false;

        List<Pair<Integer,Integer>> tempMap = new ArrayList<>();
        if(a==0) {
            for(int i=0;i<total;i++) {
                System.out.println(block[i]);
                System.out.println(qty[i]);
                System.out.println(comb[i]);
                if (comb[i] != 0) {
                    tempMap.add(Pair.of(block[i], comb[i]));
                    qty[i] = qty[i] - comb[i];
                    if ((qty[i] - comb[i]) < 0) {
                        flag = true;
                    }
                }
            }

            mapList.add(tempMap);
            if(flag) {
                return;
            }

            showComb(0,store);
        }
        else {
            for (int i=0;i<total;i++) {
                if (tempComb[i] != 0) {
                    tempMap.add(Pair.of(block[i], tempComb[i]));
                }
            }
            mapList.add(tempMap);
            store.add(max - a);
            for (int i = 0; i < total; i++)
                qty[i] = qty[i] - tempComb[i];

            for (int i=0;i<total;i++) {
                if ((qty[i]-comb[i])<0) {
                    return;
                }
            }
            showComb(a,store);
        }
    }

    private void combinations() {
        for (int i = total - 1;;) {
            if (comb[i] != limit[i]) {
                comb[i]++;
                break;
            }
            else {
                if(i==0 && comb[0] != limit[0])
                    i = total - 1;
                else {
                    comb[i] = 0;
                    i--;
                }
            }
        }
    }

    private void initLimit() {
        int div;
        limit = new int[total];
        for(int i = 0; i < total; i++) {
            div = max/block[i];
            limit[i] = Math.min(qty[i], div);
        }
    }

    private void updateLimit() {
        for (int i = 0; i < total; i++) {
            if (qty[i] < limit[i])
                limit[i] = qty[i];
        }
    }

    private void resetComb() {
        for (int i = 0; i < total; i++)
            comb[i] = 0;
    }

}
