import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

public class TimingAttack {
    int[] portions;
    double[] sets;
    int[] numOfIterations;
    Montgomery montgomery;

    TimingAttack() {

        portions = new int[]{3, 5, 10, 20, 50, 100};
        sets = new double[4]; // contains the time of each set
        numOfIterations = new int[4]; // contains the number of samples for each set
        this.montgomery = new Montgomery();
    }

    /**
     *  initialize the arrays
     */
    public void reset() {
        Arrays.fill(sets, 0.0);
        Arrays.fill(numOfIterations, 0);
    }

    /**
     *  if the bit is 1 : if it needs additional reduction then set 0, otherwise set 1. Same if the bit is 0 but for set 2, 3
     * @param decryptedMessage
     * @param N modulus
     * @param RMinusOne R - 1
     * @param NPrime -N^(-1) mod R
     * @return an array of size 2 with indices of the corresponding sets
     */

    public int[] whichSet (BigInteger decryptedMessage, BigInteger N, BigInteger RMinusOne, BigInteger NPrime) {

        int[] ans = new int[2];

        decryptedMessage = montgomery.convertDomains(decryptedMessage, RMinusOne.add(BigInteger.ONE), N);
        BigInteger res = montgomery.multiply(decryptedMessage, decryptedMessage, N, RMinusOne, NPrime);

        // if bit is 1
        BigInteger guessBitIsOne = montgomery.multiply(decryptedMessage, res, N, RMinusOne, NPrime);

        if (!needReduction(guessBitIsOne, guessBitIsOne, N, RMinusOne, NPrime))
            ans[0] = 1;


        // if bit is 0
        if (needReduction(res, res, N, RMinusOne, NPrime))
            ans[1] = 2;
        else
            ans[1] = 3;
        return ans;
    }

    /**
     * add running time to a given set
     * @param time
     * @param set
     */
    public void addTime (double time, int set) {
        sets[set] += time;
        numOfIterations[set]++;
    }

    /**
     * get average running time in a given set
     * @param set
     * @return avg time of the set
     */
    private double getAvg (int set) {
        return sets[set] / numOfIterations[set];
    }

    /**
     * perform the criteria to determine whether the second bit is 1 or 0 for this Experiment
     * @return true if bit is 1, false otherwise
     */
    public boolean isOne () {
        double[] y = new double[sets.length];

        for (int i = 0; i < sets.length; i++)
            y[i] = getAvg(i);

        System.out.println("if bit 1 : ");
        System.out.println("Set 0(needs a reduction) : " + y[0] + " Set 1 : " + y[1]);
        System.out.println("if bit 0 : ");
        System.out.println("Set 2(needs a reduction) : " + y[2] + " Set 3 : " + y[3]);
        return (y[0] - y[1]) > (y[2] - y[3]);
    }


    /**
     * Checks if the given modular multiplication needs reduction
     * @param a
     * @param b
     * @param N modulus
     * @param RMinusOne R - 1
     * @param NPrime -N^(-1) mod R
     * @return true if it needs reduction
     */
    private boolean needReduction(BigInteger a, BigInteger b, BigInteger N, BigInteger RMinusOne, BigInteger NPrime) {
        BigInteger t = a.multiply(b);
        BigInteger m = t.multiply(NPrime).and(RMinusOne);
        t = t.add(m.multiply(N)).shiftRight(RMinusOne.bitCount());

        return t.compareTo(N) >= 0;
    }

    public static void main (String[] args) {

        // private exponent
        BigInteger d = new BigInteger(
                "1801d152befc69b1134eda145bf6c94e224fa1acee36f06826436c609840a776a532911ae48101a460699fd9424a1d51329804fa23cbec98bf95cdb0dbc900c05c5a358f48228ab03372b25610b0354d0e4a8c57efe86b1b2fb9ff6580655cdabddb31d7a8cfaf99e7866ba0d93f7ee8d1aab07fc347836c03df537569ab9fcfca8ebf5662feafbdf196bb6c925dbc878f89985096fabd6430511c0ca9c4d99b6f9f5dd9aa3ddfac12f6c2d3194ab99c897ba25bf71e53cd33c1573e242d75c48cd2537d1766bbbf4f7235c40ce3f49b18e00c874932412743dc28b7d3d32e85c922c1d9a8e5bf4c7dd6fe4545dd699295d51945d1fc507c24a709e87561b001",
                16);

        // RSA modulus
        BigInteger modulus = new BigInteger(
                "a12360b5a6d58b1a7468ce7a7158f7a2562611bd163ae754996bc6a2421aa17d3cf6d4d46a06a9d437525571a2bfe9395d440d7b09e9912a2a1f2e6cb072da2d0534cd626acf8451c0f0f1dca1ac0c18017536ea314cf3d2fa5e27a13000c4542e4cf86b407b2255f9819a763797c221c8ed7e7050bc1c9e57c35d5bb0bddcdb98f4a1b58f6d8b8d6edb292fd0f7fa82dc5fdcd78b04ca09e7bc3f4164d901b119c4f427d054e7848fdf7110352c4e612d02489da801ec9ab978d98831fa7f872fa750b092967ff6bdd223199af209383bbce36799a5ed5856f587f7d420e8d76a58b398ef1f7b290bc5b75ef59182bfa02fafb7caeb504bd9f77348aea61ae9",
                16);

        // R is chosen such that R > N and GCD(R, N) = 1 and R is a power of 2
        BigInteger R = BigInteger.ZERO.setBit(2048);
        BigInteger NPrime = modulus.modInverse(R).multiply(BigInteger.valueOf(-1));


        Random rnd = new Random();
        TimingAttack timingAttack = new TimingAttack();
        ModifiedRSA modifiedRSA = new ModifiedRSA();


        // total number of success tests
        int totalSuccess = 0;

        // loop over each portion
        for (int lengthOfKey : timingAttack.portions) {

            // get the new key (portion of the old key)
            BigInteger key = d.shiftRight(d.bitLength() - lengthOfKey);

            // get the local number of success with that portion
            int success = 0;

            // repeat the test 20 times
            for (int test = 0; test < 20; test++) {
                // reset the arrays to calculate the average time
                timingAttack.reset();

                // get 10000 samples
                for (int i = 0; i < 10000; i++) {

                    // generate random decrypted message
                    BigInteger randomDecryptedMessage = new BigInteger(modulus.bitLength() - 1, rnd);

                    // get the corresponding sets
                    int[] sets = timingAttack.whichSet(randomDecryptedMessage, modulus, R.subtract(BigInteger.ONE), NPrime);

                    // calculate the running time
                    double begin, end;

                    begin = System.nanoTime();

                    modifiedRSA.decrypt(randomDecryptedMessage, key, modulus);

                    end = System.nanoTime();

                    // add it to the corresponding set
                    timingAttack.addTime(end - begin, sets[0]);
                    timingAttack.addTime(end - begin, sets[1]);

                }


                if (timingAttack.isOne()) {
                    success++;
                    System.out.println("SUCCESS : the second bit is 1 for Length " + lengthOfKey);
                } else
                    System.out.println("Failed : the second bit is 0 for Length " + lengthOfKey);

                System.out.println("*********************************************************************");
            }
            System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
            System.out.println("Results For length :" + lengthOfKey);
            System.out.println("Success to get the Second MSb right : " + success + " Failed : " + (20 - success));
            System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
            totalSuccess += success;
        }
        System.out.println("FINAL ANSWER: Accuracy : " + (totalSuccess * 100.0) / (20.0 * timingAttack.portions.length));

    }

}
