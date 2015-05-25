package se.lagrummet.rinfo.base.feed.type;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by christian on 5/21/15.
 */
public final class Md5Sum {

    public static Md5Sum create(String md5Sum) {return new Md5Sum(md5Sum);}
    public static Md5Sum calculate(String inputData) throws NoSuchAlgorithmException {return calculate(inputData.getBytes());}
    public static Md5Sum calculate(byte[] inputData) throws NoSuchAlgorithmException {return new Md5Sum(MD5(inputData));}
    public static Md5SumCalculator calculator() throws NoSuchAlgorithmException {return new MyMd5SumCalculator(); }

    private String md5sum;

    private Md5Sum(String md5sum) {
        this.md5sum = md5sum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Md5Sum md5Sum = (Md5Sum) o;

        if (md5sum != null ? !md5sum.equals(md5Sum.md5sum) : md5Sum.md5sum != null) return false;

        return true;
    }

    @Override
    public int hashCode() {return md5sum != null ? md5sum.hashCode() : 0;}

    @Override
    public String toString() {return md5sum;}

    public static String MD5(byte[] md5) throws NoSuchAlgorithmException {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
        byte[] array = md.digest(md5);
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < array.length; ++i) {
            sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
        }
        return sb.toString();
    }

    public interface Md5SumCalculator {
        void update(byte[] data);
        void update(byte[] data, int off, int len);
        Md5Sum create();
    }

    static class MyMd5SumCalculator implements Md5SumCalculator {
        java.security.MessageDigest md;

        MyMd5SumCalculator() throws NoSuchAlgorithmException {
            this.md = java.security.MessageDigest.getInstance("MD5");
        }

        @Override
        public void update(byte[] data) {
            md.update(data);
        }

        @Override
        public void update(byte[] data, int off, int len) {
            md.update(data, off, len);
        }

        @Override
        public Md5Sum create() {
            StringBuffer sb = new StringBuffer();
            byte[] array = md.digest();
            for (int i = 0; i < array.length; ++i) {
                sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
            }
            return Md5Sum.create(sb.toString());
        }
    }

}
