package com.shiver;

import com.shiver.models.ShiverPaths;
import net.sharksystem.asap.crypto.ASAPCryptoAlgorithms;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.interfaces.DHPublicKey;
import javax.crypto.spec.DHParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

/*
 * This program executes the Diffie-Hellman key agreement protocol between
 * 5 parties: Alice, Bob, Carol, Sara and Dave using a shared 2048-bit DH parameter.
 */
public class Main {
    private Main() {
    }

    public static void main2(String argv[]) throws Exception {
        // Alice creates her own DH key pair with 2048-bit key size
        KeyPairGenerator aliceKpairGen = KeyPairGenerator.getInstance("DH");
        aliceKpairGen.initialize(2048);
        KeyPair aliceKpair = aliceKpairGen.generateKeyPair();
        // This DH parameters can also be constructed by creating a
        // DHParameterSpec object using agreed-upon values
        DHParameterSpec dhParamShared = ((DHPublicKey) aliceKpair.getPublic()).getParams();

        // Bob creates his own DH key pair using the same params
        KeyPairGenerator bobKpairGen = KeyPairGenerator.getInstance("DH");
        bobKpairGen.initialize(dhParamShared);
        KeyPair bobKpair = bobKpairGen.generateKeyPair();
        // Carol creates her own DH key pair using the same params
        KeyPairGenerator carolKpairGen = KeyPairGenerator.getInstance("DH");
        carolKpairGen.initialize(dhParamShared);
        KeyPair carolKpair = carolKpairGen.generateKeyPair();
        // Sara creates her own DH key pair using the same params
        KeyPairGenerator saraKpairGen = KeyPairGenerator.getInstance("DH");
        saraKpairGen.initialize(dhParamShared);
        KeyPair saraKpair = saraKpairGen.generateKeyPair();
        // Dave creates her own DH key pair using the same params
        KeyPairGenerator daveKpairGen = KeyPairGenerator.getInstance("DH");
        daveKpairGen.initialize(dhParamShared);
        KeyPair daveKpair = daveKpairGen.generateKeyPair();

        //Alice initialize
        KeyAgreement aliceKeyAgree = KeyAgreement.getInstance("DH");
        //Alice computes gA
        aliceKeyAgree.init(aliceKpair.getPrivate());

        //Bob initialize
        KeyAgreement bobKeyAgree = KeyAgreement.getInstance("DH");
        //Bob computes gB
        bobKeyAgree.init(bobKpair.getPrivate());

        //Carol initialize
        KeyAgreement carolKeyAgree = KeyAgreement.getInstance("DH");
        //Carol computes gC
        carolKeyAgree.init(carolKpair.getPrivate());

        //Sara initialize
        KeyAgreement saraKeyAgree = KeyAgreement.getInstance("DH");
        //Sara computes gS
        saraKeyAgree.init(saraKpair.getPrivate());

        //Dave initialize
        KeyAgreement daveKeyAgree = KeyAgreement.getInstance("DH");
        //Sara computes gS
        daveKeyAgree.init(daveKpair.getPrivate());


        //First Pass

        //Alice computes gDA
        Key gDA = aliceKeyAgree.doPhase(daveKpair.getPublic(), false);

        //Bob computes gAB
        Key gAB = bobKeyAgree.doPhase(aliceKpair.getPublic(), false);

        //Carol computes gBC
        Key gBC = carolKeyAgree.doPhase(bobKpair.getPublic(), false);

        //Sara computes gCS
        Key gCS = saraKeyAgree.doPhase(carolKpair.getPublic(), false);

        //Dave computed gSD
        Key gSD = daveKeyAgree.doPhase(saraKpair.getPublic(), false);


        //Second Pass

        //Alice computes gSDA
        Key gSDA = aliceKeyAgree.doPhase(gSD, false);

        //Bob computes gDAB
        Key gDAB = bobKeyAgree.doPhase(gDA, false);

        //Carol computes gABC
        Key gABC = carolKeyAgree.doPhase(gAB, false);

        //Sara computes gBCS
        Key gBCS = saraKeyAgree.doPhase(gBC, false);

        //Dave computes gCSD
        Key gCSD = daveKeyAgree.doPhase(gCS, false);

        //Third Pass

        //Alice computes gCSDA
        Key gCSDA = aliceKeyAgree.doPhase(gCSD, false);

        //Bob computes gSDAB
        Key gSDAB = bobKeyAgree.doPhase(gSDA, false);

        //Carol computes gDABC
        Key gDABC = carolKeyAgree.doPhase(gDAB, false);

        //Sara Computes gABCS
        Key gABCS = saraKeyAgree.doPhase(gABC, false);

        //Dave computes gBCSC
        Key gBCSD = daveKeyAgree.doPhase(gBCS, false);

        //Fourth Pass

        //Alice computes gBCSDA
        Key gBCSDA = aliceKeyAgree.doPhase(gBCSD, true); //This is Alice's secret

        //Bob computes gSDABC
        Key gCSDAB = bobKeyAgree.doPhase(gCSDA, true); //This is Bob's secret

        //Carol computes gSABC
        Key gSDABC = carolKeyAgree.doPhase(gSDAB, true); //This is Carol's secret

        //Sara Computes gABCS
        Key gDABCS = saraKeyAgree.doPhase(gDABC, true); //This is Sara's secret


        Key gABCSD = daveKeyAgree.doPhase(gABCS, true); //This is Dave's secret


        // Alice, Bob, Carol and Sara compute their secrets
        byte[] aliceSharedSecret = aliceKeyAgree.generateSecret();
        System.out.println("Alice secret: " + ((DHPublicKey) gBCSD).getParams().getG());

        byte[] bobSharedSecret = bobKeyAgree.generateSecret();
        System.out.println("Bob secret: " + toHexString(bobSharedSecret));

        byte[] carolSharedSecret = carolKeyAgree.generateSecret();
        System.out.println("Carol secret: " + toHexString(carolSharedSecret));

        byte[] saraSharedSecret = saraKeyAgree.generateSecret();
        System.out.println("Sara secret: " + toHexString(saraSharedSecret));

        byte[] daveSharedSecret = daveKeyAgree.generateSecret();
        System.out.println("Dave secret: " + toHexString(daveSharedSecret));

        // Compare Alice and Bob
        if (!java.util.Arrays.equals(aliceSharedSecret, bobSharedSecret))
            System.out.println("Alice and Bob differ");//    throw new Exception("Alice and Bob differ");
        else
            System.out.println("Alice and Bob are the same");
        // Compare Bob and Carol
        if (!java.util.Arrays.equals(bobSharedSecret, carolSharedSecret))
            System.out.println("Bob and Carol differ");//throw new Exception("Bob and Carol differ");
        else
            System.out.println("Bob and Carol are the same");
        //Compare Carol and Sara
        if (!java.util.Arrays.equals(carolSharedSecret, saraSharedSecret))
            System.out.println("Carol and Sara differ");//throw new Exception("Carol and Sara differ");
        else
            System.out.println("Carol and Sara are the same");
        //Compare Sara and Dave
        if (!java.util.Arrays.equals(saraSharedSecret, daveSharedSecret))
            System.out.println("Sara and Dave differ");//throw new Exception("Carol and Sara differ");
        else
            System.out.println("Sara and Dave are the same");

    }

    /*
     * Converts a byte to hex digit and writes to the supplied buffer
     */
    private static void byte2hex(byte b, StringBuffer buf) {
        char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8',
                '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        int high = ((b & 0xf0) >> 4);
        int low = (b & 0x0f);
        buf.append(hexChars[high]);
        buf.append(hexChars[low]);
    }

    /*
     * Converts a byte array to hex string
     */
    private static String toHexString(byte[] block) {
        StringBuffer buf = new StringBuffer();
        int len = block.length;
        for (int i = 0; i < len; i++) {
            byte2hex(block[i], buf);
            if (i < len - 1) {
                buf.append(":");
            }
        }
        return buf.toString();
    }

    private static byte[] getPublicKeyBytes(Key publicKey) throws IOException {
        // Assuming ASAPSecurityException is a custom exception that needs to be handled or declared to be thrown by the caller.

        // Convert the public key information into a byte array.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        String format = publicKey.getFormat();
        String algorithm = publicKey.getAlgorithm();
        byte[] byteEncodedPublicKey = publicKey.getEncoded();

        // Write the public key information to the ByteArrayOutputStream.
        dos.writeUTF(format);
        dos.writeUTF(algorithm);
        dos.writeInt(byteEncodedPublicKey.length);
        dos.write(byteEncodedPublicKey);

        // Convert the ByteArrayOutputStream to a byte array.
        return baos.toByteArray();
    }

    public static PublicKey readPublicKeyFromBytes(byte[] publicKeyBytes) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        // Create a ByteArrayInputStream from the byte array.
        ByteArrayInputStream bais = new ByteArrayInputStream(publicKeyBytes);
        DataInputStream dis = new DataInputStream(bais);

        // Read the public key information from the DataInputStream.
        String format = dis.readUTF();
        String algorithm = dis.readUTF();
        int len = dis.readInt();
        byte[] byteEncodedPublicKey = new byte[len];
        dis.readFully(byteEncodedPublicKey); // Use readFully to ensure all bytes are read.

        // Generate the public key from the encoded byte array.
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(byteEncodedPublicKey);
        KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
        PublicKey publicKey = keyFactory.generatePublic(pubKeySpec);

        return publicKey; // Return the generated public key.
    }

    public static void main(String argv[]) throws Exception {

        /*
         * Alice creates her own DH key pair with 2048-bit key size
         */
        System.out.println("ALICE: Generate DH keypair ...");
        KeyPairGenerator aliceKpairGen = KeyPairGenerator.getInstance("DH");
        aliceKpairGen.initialize(2048);
        KeyPair aliceKpair = aliceKpairGen.generateKeyPair();

        // Alice creates and initializes her DH KeyAgreement object
        System.out.println("ALICE: Initialization ...");
        KeyAgreement aliceKeyAgree = KeyAgreement.getInstance("DH");
        aliceKeyAgree.init(aliceKpair.getPrivate());

        // Alice encodes her public key, and sends it over to Bob.
        byte[] alicePubKeyEnc = aliceKpair.getPublic().getEncoded();

        /*
         * Let's turn over to Bob. Bob has received Alice's public key
         * in encoded format.
         * He instantiates a DH public key from the encoded key material.
         */
        KeyFactory bobKeyFac = KeyFactory.getInstance("DH");
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(alicePubKeyEnc);

        PublicKey alicePubKey = bobKeyFac.generatePublic(x509KeySpec);

        /*
         * Bob gets the DH parameters associated with Alice's public key.
         * He must use the same parameters when he generates his own key
         * pair.
         */
        DHParameterSpec dhParamFromAlicePubKey = ((DHPublicKey) alicePubKey).getParams();

        // Bob creates his own DH key pair
        System.out.println("BOB: Generate DH keypair ...");
        KeyPairGenerator bobKpairGen = KeyPairGenerator.getInstance("DH");
        bobKpairGen.initialize(dhParamFromAlicePubKey);
        KeyPair bobKpair = bobKpairGen.generateKeyPair();

        // Bob creates and initializes his DH KeyAgreement object
        System.out.println("BOB: Initialization ...");
        KeyAgreement bobKeyAgree = KeyAgreement.getInstance("DH");
        bobKeyAgree.init(bobKpair.getPrivate());

        // Bob encodes his public key, and sends it over to Alice.
        byte[] bobPubKeyEnc = bobKpair.getPublic().getEncoded();

        /*
         * Alice uses Bob's public key for the first (and only) phase
         * of her version of the DH
         * protocol.
         * Before she can do so, she has to instantiate a DH public key
         * from Bob's encoded key material.
         */
        KeyFactory aliceKeyFac = KeyFactory.getInstance("DH");
        x509KeySpec = new X509EncodedKeySpec(bobPubKeyEnc);
        PublicKey bobPubKey = aliceKeyFac.generatePublic(x509KeySpec);
        System.out.println("ALICE: Execute PHASE1 ...");
        aliceKeyAgree.doPhase(bobPubKey, true);

        /*
         * Bob uses Alice's public key for the first (and only) phase
         * of his version of the DH
         * protocol.
         */
        System.out.println("BOB: Execute PHASE1 ...");
        bobKeyAgree.doPhase(alicePubKey, true);

        /*
         * At this stage, both Alice and Bob have completed the DH key
         * agreement protocol.
         * Both generate the (same) shared secret.
         */
        byte[] aliceSharedSecret;
        int aliceLen;
        byte[] bobSharedSecret;
        int bobLen;

        aliceSharedSecret = aliceKeyAgree.generateSecret();
        aliceLen = aliceSharedSecret.length;
        bobSharedSecret = new byte[aliceLen];
//            bobLen;
        // provide output buffer of required size
        bobLen = bobKeyAgree.generateSecret(bobSharedSecret, 0);
        System.out.println("Alice secret: " +
                toHexString(aliceSharedSecret));
        System.out.println("Bob secret: " +
                toHexString(bobSharedSecret));
        if (!java.util.Arrays.equals(aliceSharedSecret, bobSharedSecret))
            throw new Exception("Shared secrets differ");
        System.out.println("Shared secrets are the same");

        /*
         * Now let's create a SecretKey object using the shared secret
         * and use it for encryption. First, we generate SecretKeys for the
         * "AES" algorithm (based on the raw shared secret data) and
         * Then we use AES in CBC mode, which requires an initialization
         * vector (IV) parameter. Note that you have to use the same IV
         * for encryption and decryption: If you use a different IV for
         * decryption than you used for encryption, decryption will fail.
         *
         * If you do not specify an IV when you initialize the Cipher
         * object for encryption, the underlying implementation will generate
         * a random one, which you have to retrieve using the
         * javax.crypto.Cipher.getParameters() method, which returns an
         * instance of java.security.AlgorithmParameters. You need to transfer
         * the contents of that object (e.g., in encoded format, obtained via
         * the AlgorithmParameters.getEncoded() method) to the party who will
         * do the decryption. When initializing the Cipher for decryption,
         * the (reinstantiated) AlgorithmParameters object must be explicitly
         * passed to the Cipher.init() method.
         */
        System.out.println("Use shared secret as SecretKey object ...");

        if(true) {
            char[] dhSecretChars = new String(aliceSharedSecret).toCharArray(); // Convert byte[] to char[] as PBEKeySpec requires char[]
            byte[] salt = new byte[16]; // Should use a secure random salt in real applications
            int iterationCount = 65536;
            int keyLength = 256; // Key length in bits

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            PBEKeySpec spec = new PBEKeySpec(dhSecretChars, salt, iterationCount, keyLength);
            SecretKey derivedKey = factory.generateSecret(spec);
            SecretKey finalKey = new SecretKeySpec(derivedKey.getEncoded(), "AES"); // For AES, for example
            System.out.println("alicekey: " + toHexString(finalKey.getEncoded()));
        }
        if(true){
            if(true) {
                char[] dhSecretChars = new String(bobSharedSecret).toCharArray(); // Convert byte[] to char[] as PBEKeySpec requires char[]
                byte[] salt = new byte[16]; // Should use a secure random salt in real applications
                int iterationCount = 65536;
                int keyLength = 256; // Key length in bits

                SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                PBEKeySpec spec = new PBEKeySpec(dhSecretChars, salt, iterationCount, keyLength);
                SecretKey derivedKey = factory.generateSecret(spec);
                SecretKey finalKey = new SecretKeySpec(derivedKey.getEncoded(), "AES"); // For AES, for example
                System.out.println("alicekey: " + toHexString(finalKey.getEncoded()));
            }
        }

        Key keytemp = ASAPCryptoAlgorithms.generateSymmetricKey("AES", 128);

        SecretKeySpec bobAesKey = new SecretKeySpec(bobSharedSecret, 0, 16, "AES");
        SecretKeySpec aliceAesKey = new SecretKeySpec(aliceSharedSecret, 0, 16, "AES");

        /*
         * Bob encrypts, using AES in CBC mode
         */
        Cipher bobCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        bobCipher.init(Cipher.ENCRYPT_MODE, bobAesKey);
        byte[] cleartext = "This is just an example".getBytes();
        byte[] ciphertext = bobCipher.doFinal(cleartext);

        // Retrieve the parameter that was used, and transfer it to Alice in
        // encoded format
        byte[] encodedParams = bobCipher.getParameters().getEncoded();

        /*
         * Alice decrypts, using AES in CBC mode
         */

        // Instantiate AlgorithmParameters object from parameter encoding
        // obtained from Bob
        AlgorithmParameters aesParams = AlgorithmParameters.getInstance("AES");
        aesParams.init(encodedParams);
        Cipher aliceCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        aliceCipher.init(Cipher.DECRYPT_MODE, aliceAesKey, aesParams);
        byte[] recovered = aliceCipher.doFinal(ciphertext);
        if (!java.util.Arrays.equals(cleartext, recovered))
            throw new Exception("AES in CBC mode recovered text is " +
                    "different from cleartext");
        System.out.println("AES in CBC mode recovered text is same as cleartext");
    }
}

