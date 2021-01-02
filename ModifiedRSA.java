import java.math.BigInteger;
import java.util.Random;

public class ModifiedRSA {


    private final Montgomery montgomery; // for Modular multiplication

    // parameters for Montgomery
    private BigInteger NPrime;
    private BigInteger R;

    // modulus
    private BigInteger N;


    ModifiedRSA() {
        montgomery = new Montgomery();
    }

    /**
     * Initialize the parameters for montgomery, using modulus
     * @param modulus
     */
    public void init(BigInteger modulus) {

        if (modulus.equals(N)) return;

        N = modulus;

        int len = modulus.bitLength();
        for (int i = len - 1; i >= 0; i--) {
            if (N.testBit(i)) {
                R = BigInteger.ZERO.setBit(i + 1);
                break;
            }
        }

        NPrime = modulus.modInverse(R).multiply(BigInteger.valueOf(-1));
    }



    public BigInteger encrypt (BigInteger m, BigInteger e, BigInteger modulus) {
        init(modulus);
        return modExp(m, e, modulus);
    }

    public BigInteger decrypt (BigInteger c, BigInteger d, BigInteger modulus) {
        init(modulus);
        return modExp(c, d, modulus);
    }

    private BigInteger modExp(BigInteger a, BigInteger exponent, BigInteger N) {

        // caching the values to use it multiple times
        BigInteger RMinus = R.subtract(BigInteger.ONE);
        BigInteger RInverse = R.modInverse(N);

        // convert the message to montgomery domain
        a = montgomery.convertDomains(a, R, N);
        BigInteger result = a;


        int expBitLength = exponent.bitLength();

        for (int i = expBitLength - 2; i >= 0; i--) {
            result = montgomery.multiply(result, result, N, RMinus, NPrime);
            if (exponent.testBit(i)) {
                result = montgomery.multiply(result, a, N, RMinus, NPrime);
            }
        }

        return montgomery.convertDomains(result, RInverse, N); // convert it back
    }

    public static void main(String[] args) {

        // RSA modulus
        BigInteger modulus = new BigInteger(
                "a12360b5a6d58b1a7468ce7a7158f7a2562611bd163ae754996bc6a2421aa17d3cf6d4d46a06a9d437525571a2bfe9395d440d7b09e9912a2a1f2e6cb072da2d0534cd626acf8451c0f0f1dca1ac0c18017536ea314cf3d2fa5e27a13000c4542e4cf86b407b2255f9819a763797c221c8ed7e7050bc1c9e57c35d5bb0bddcdb98f4a1b58f6d8b8d6edb292fd0f7fa82dc5fdcd78b04ca09e7bc3f4164d901b119c4f427d054e7848fdf7110352c4e612d02489da801ec9ab978d98831fa7f872fa750b092967ff6bdd223199af209383bbce36799a5ed5856f587f7d420e8d76a58b398ef1f7b290bc5b75ef59182bfa02fafb7caeb504bd9f77348aea61ae9",
                16);

        // private exponent
        BigInteger d = new BigInteger(
                "1801d152befc69b1134eda145bf6c94e224fa1acee36f06826436c609840a776a532911ae48101a460699fd9424a1d51329804fa23cbec98bf95cdb0dbc900c05c5a358f48228ab03372b25610b0354d0e4a8c57efe86b1b2fb9ff6580655cdabddb31d7a8cfaf99e7866ba0d93f7ee8d1aab07fc347836c03df537569ab9fcfca8ebf5662feafbdf196bb6c925dbc878f89985096fabd6430511c0ca9c4d99b6f9f5dd9aa3ddfac12f6c2d3194ab99c897ba25bf71e53cd33c1573e242d75c48cd2537d1766bbbf4f7235c40ce3f49b18e00c874932412743dc28b7d3d32e85c922c1d9a8e5bf4c7dd6fe4545dd699295d51945d1fc507c24a709e87561b001",
                16);

        // public exponent (just provided for illustration. The focus in this assignment is on the decryption process)
        BigInteger e = new BigInteger("10001", 16);
        ModifiedRSA modifiedRSA = new ModifiedRSA();

        Random rnd = new Random();
        BigInteger m = new BigInteger(modulus.bitLength() - 1, rnd);
        BigInteger c = modifiedRSA.encrypt(m, e, modulus);

        // total time for all 1000 runs
        double totalTime = 0;
        BigInteger m2 = null;
        for (int i = 0; i < 1000; i++) {
            double begin = System.nanoTime();
            m2 = modifiedRSA.decrypt(c, d, modulus);
            totalTime += (System.nanoTime() - begin);
        }
        System.out.println("Original message = " + m.toString(16));
        System.out.println("Ciphertext = " + c.toString(16));
        System.out.println("Decrypted message = " + m2.toString(16));
        if (!m.equals(m2)) {
            System.err.println("There is an error.");
        }
        System.out.println("\n");

        // get the average time
        System.out.println("Total time is : " + totalTime/1000.0  + " nanoseconds");


    }
}
