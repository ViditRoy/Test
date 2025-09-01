import java.io.*;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    static BigInteger parseInBase(String s, int base) {
        s = s.trim().toLowerCase();
        BigInteger res = BigInteger.ZERO;
        BigInteger B = BigInteger.valueOf(base);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int d = Character.digit(c, base);
            if (d < 0) throw new IllegalArgumentException("Invalid digit '" + c + "' for base " + base);
            res = res.multiply(B).add(BigInteger.valueOf(d));
        }
        return res;
    }

    static List<BigInteger> polyMul(List<BigInteger> A, List<BigInteger> B) {
        int n = A.size(), m = B.size();
        List<BigInteger> C = new ArrayList<>(Collections.nCopies(n + m - 1, BigInteger.ZERO));
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                C.set(i + j, C.get(i + j).add(A.get(i).multiply(B.get(j))));
            }
        }
        return C;
    }

    static BigInteger polyEval(List<BigInteger> coeffs, BigInteger x) {
        BigInteger res = BigInteger.ZERO;
        for (int i = coeffs.size() - 1; i >= 0; i--) {
            res = res.multiply(x).add(coeffs.get(i));
        }
        return res;
    }

    static List<BigInteger> polyFromRoots(List<BigInteger> roots) {
        List<BigInteger> coeffs = new ArrayList<>();
        coeffs.add(BigInteger.ONE);
        for (BigInteger r : roots) {
            List<BigInteger> factor = Arrays.asList(r.negate(), BigInteger.ONE);
            coeffs = polyMul(coeffs, factor);
        }
        return coeffs;
    }

    static class ParsedInput {
        int n;
        int k;
        List<BigInteger> roots = new ArrayList<>();
    }

    static ParsedInput parseInput(String json) {
        ParsedInput pi = new ParsedInput();

        Matcher mk = Pattern.compile("\"k\"\\s*:\\s*(\\d+)").matcher(json);
        if (!mk.find()) throw new IllegalArgumentException("k not found");
        pi.k = Integer.parseInt(mk.group(1));

        Matcher mn = Pattern.compile("\"n\"\\s*:\\s*(\\d+)").matcher(json);
        if (!mn.find()) throw new IllegalArgumentException("n not found");
        pi.n = Integer.parseInt(mn.group(1));

        for (int i = 1; i <= pi.n; i++) {
            String key = "\"" + i + "\"\\s*:\\s*\\{[^}]*\\}";
            Matcher mEntry = Pattern.compile(key).matcher(json);
            if (!mEntry.find()) throw new IllegalArgumentException("Entry for \"" + i + "\" not found");
            String block = mEntry.group();

            Matcher mb = Pattern.compile("\"base\"\\s*:\\s*\"(\\d+)\"").matcher(block);
            Matcher mv = Pattern.compile("\"value\"\\s*:\\s*\"([0-9a-zA-Z]+)\"").matcher(block);
            if (!mb.find() || !mv.find()) throw new IllegalArgumentException("base/value missing in entry " + i);

            int base = Integer.parseInt(mb.group(1));
            String val = mv.group(1);
            if (base < 2 || base > 36) throw new IllegalArgumentException("Unsupported base: " + base + " in entry " + i);

            BigInteger root = parseInBase(val, base);
            pi.roots.add(root);
        }

        return pi;
    }

    public static void main(String[] args) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
        }
        String json = sb.toString();

        ParsedInput pi = parseInput(json);

        int m = pi.k - 1;
        if (m <= 0) {
            System.out.println("k must be >= 1. Got k=" + pi.k);
            return;
        }
        if (pi.roots.size() < m) {
            System.out.println("Not enough roots to build degree " + m + " polynomial. Need " + m + ", got " + pi.roots.size());
            return;
        }

        List<BigInteger> usedRoots = pi.roots.subList(0, m);
        List<BigInteger> coeffsLowToHigh = polyFromRoots(usedRoots);

        System.out.println("Parsed:");
        System.out.println("  n = " + pi.n + ", k = " + pi.k + "  => degree m = " + m);
        for (int i = 0; i < pi.roots.size(); i++) {
            System.out.println("  root[" + (i + 1) + "] = " + pi.roots.get(i));
        }

        System.out.println("\nMonic polynomial coefficients (highest degree -> constant):");
        StringBuilder line = new StringBuilder();
        for (int i = coeffsLowToHigh.size() - 1; i >= 0; i--) {
            line.append(coeffsLowToHigh.get(i));
            if (i != 0) line.append(" ");
        }
        System.out.println(line.toString());

        System.out.println("\nP(x) = " + pretty(coeffsLowToHigh));

        if (pi.roots.size() > m) {
            System.out.println("\nVerification on extra roots:");
            for (int i = m; i < pi.roots.size(); i++) {
                BigInteger r = pi.roots.get(i);
                BigInteger val = polyEval(coeffsLowToHigh, r);
                System.out.println("  P(" + r + ") = " + val + (val.equals(BigInteger.ZERO) ? "  [OK]" : "  [FAIL]"));
            }
        }
    }

    static String pretty(List<BigInteger> coeffsLowToHigh) {
        StringBuilder sb = new StringBuilder();
        int deg = coeffsLowToHigh.size() - 1;
        for (int i = deg; i >= 0; i--) {
            BigInteger c = coeffsLowToHigh.get(i);
            if (c.equals(BigInteger.ZERO)) continue;
            if (sb.length() > 0) {
                sb.append(c.signum() >= 0 ? " + " : " - ");
            } else {
                if (c.signum() < 0) sb.append("-");
            }
            BigInteger abs = c.abs();
            if (i == 0) {
                sb.append(abs);
            } else if (i == 1) {
                if (!abs.equals(BigInteger.ONE)) sb.append(abs).append("*");
                sb.append("x");
            } else {
                if (!abs.equals(BigInteger.ONE)) sb.append(abs).append("*");
                sb.append("x^").append(i);
            }
        }
        if (sb.length() == 0) sb.append("0");
        return sb.toString();
    }
}
