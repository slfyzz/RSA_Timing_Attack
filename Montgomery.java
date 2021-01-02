import java.math.BigInteger;

public class Montgomery {

    /**
     * convert the BigInteger to or from the Montgomery domain
     * @param a the BigInteger needed to be converted
     * @param N the modulus
     * @param R the choosen R such that GCD(R, N) = 1, R > N
     * @return converted a
     */
    public BigInteger convertDomains(BigInteger a, BigInteger R, BigInteger N) {
        return a.multiply(R).mod(N);
    }

    /**
     * modular multiplication of a and b (Both in Montgomery domain)
     * @param a
     * @param b
     * @param N modulus
     * @param RMinusOne R - 1
     * @param NPrime -N^(-1) mod R
     * @return C' the result of modular multiplication in Montgomery domain
     */
    public BigInteger multiply(BigInteger a, BigInteger b, BigInteger N, BigInteger RMinusOne, BigInteger NPrime) {

        BigInteger t = a.multiply(b);

        // updated version (better performance taking advantage of R being power of 2)
        BigInteger m = t.multiply(NPrime).and(RMinusOne);
        t = t.add(m.multiply(N)).shiftRight(RMinusOne.bitCount());


        // old version
        /*
        BigInteger m = t.multiply(NPrime).mod(R);
        t = t.add(m.multiply(N)).divide(R);
        */

        if (t.compareTo(N) >= 0) {

            /*for (int i = 0; i < 20; i++) { // for attack
                t.subtract(N);
            }*/
            return t.subtract(N);
        }
        return t;
    }
}
