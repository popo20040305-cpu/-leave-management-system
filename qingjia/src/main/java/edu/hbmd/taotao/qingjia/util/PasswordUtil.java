package edu.hbmd.taotao.qingjia.util;

import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.codec.Base64;
import org.apache.shiro.codec.Hex;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.DefaultHashService;
import org.apache.shiro.crypto.hash.Hash;
import org.apache.shiro.crypto.hash.HashService;
import org.apache.shiro.crypto.hash.format.Shiro1CryptFormat;
import org.apache.shiro.util.ByteSource;
import org.springframework.stereotype.Component;

import java.util.UUID;
@Component
public class PasswordUtil {
    private static final String DEFAULT_ALGORITHM = "SHA-256";
    private static final int DEFAULT_ITERATIONS = 500000;
    private static final int DEFAULT_SALT_SIZE =32;

    private final String algorithm;
    private final int iterations;
    private final int saltSize;
    private final String saltEncoding;

    private static final SecureRandomNumberGenerator srng = new SecureRandomNumberGenerator();

    public PasswordUtil() {
        this(DEFAULT_ALGORITHM, DEFAULT_ITERATIONS, DEFAULT_SALT_SIZE, "base64");
    }

    public PasswordUtil(String algorithm, int iterations) {
        this(algorithm, iterations, DEFAULT_SALT_SIZE, "base64");
    }

    public PasswordUtil(String algorithm, int iterations, int saltSize, String saltEncoding) {
        this.algorithm = algorithm;
        this.iterations = iterations;
        this.saltSize = saltSize;
        this.saltEncoding = saltEncoding;
    }

    /**
     * 生成随机私盐（默认Base64编码）
     */
    public String generatePrivateSalt() {
        return generatePrivateSalt(this.saltEncoding);
    }

    /**
     * 生成随机私盐（指定编码方式）
     * @param encoding base64 或 hex
     */
    public String generatePrivateSalt(String encoding) {
        UUID uuid = UUID.randomUUID();
        String strUuid = uuid.toString();
        srng.setSeed(strUuid.getBytes());
        ByteSource saltBytes =  srng.nextBytes();

        if ("hex".equalsIgnoreCase(encoding)) {
            return saltBytes.toHex();  // Shiro Hex工具
        } else {
            return saltBytes.toBase64();  // Shiro Base64工具
        }
    }
    /**
     * 创建带私盐的HashService
     */
    private HashService createHashServiceWithPrivateSalt(String privateSalt) {
        return  createHashServiceWithPrivateSalt(privateSalt,this.saltEncoding);
    }
    /**
     * 创建带私盐的HashService
     */
    private HashService createHashServiceWithPrivateSalt(String privateSalt, String saltEncoding) {
        DefaultHashService hashService = new DefaultHashService();
        hashService.setHashAlgorithmName(algorithm);
        hashService.setHashIterations(iterations);
        hashService.setGeneratePublicSalt(true);
        hashService.setRandomNumberGenerator(srng);

        if (privateSalt != null && !privateSalt.isEmpty()) {
            ByteSource privateSaltBytes = decodeSalt(privateSalt, saltEncoding);
            hashService.setPrivateSalt(privateSaltBytes);
        }

        return hashService;
    }
    /**
     * 解码私盐（完全使用Shiro工具类）
     */
    private ByteSource decodeSalt(String salt) {
        return decodeSalt(salt,this.saltEncoding);
    }
    /**
     * 解码私盐（完全使用Shiro工具类）
     */
    private ByteSource decodeSalt(String salt, String encoding) {
        try {
            if ("hex".equalsIgnoreCase(encoding)) {
                // Shiro Hex解码
                return ByteSource.Util.bytes(Hex.decode(salt));
            } else {
                // Shiro Base64解码
                return ByteSource.Util.bytes(Base64.decode(salt));
            }
        } catch (Exception e) {
            throw new RuntimeException("私盐解码失败: " + e.getMessage(), e);
        }
    }

    /**
     * 加密密码（自动生成私盐）
     */
    public EncryptResult encryptPassword(String plainPassword) {
        String privateSalt = generatePrivateSalt();
        return encryptPassword(plainPassword, privateSalt);
    }

    /**
     * 加密密码（使用指定的私盐）
     */
    public EncryptResult encryptPassword(String plainPassword, String privateSalt) {
        return encryptPassword(plainPassword, privateSalt, this.saltEncoding);
    }

    /**
     * 加密密码（完整参数）
     */
    public EncryptResult encryptPassword(String plainPassword, String privateSalt, String saltEncoding) {
        try {
            HashService hashService = createHashServiceWithPrivateSalt(privateSalt, saltEncoding);
            DefaultPasswordService passwordService = new DefaultPasswordService();
            passwordService.setHashService(hashService);

            String encryptedPassword = passwordService.encryptPassword(plainPassword);

            return new EncryptResult(encryptedPassword, privateSalt);

        } catch (Exception e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }

    /**
     * 验证密码
     */
    public boolean matches(String plainPassword, String encryptedPassword, String privateSalt) {
        return matches(plainPassword, encryptedPassword, privateSalt, this.saltEncoding);
    }

    /**
     * 验证密码（完整参数）
     */
    public boolean matches(String plainPassword, String encryptedPassword, String privateSalt, String saltEncoding) {
        try {
            HashService hashService = createHashServiceWithPrivateSalt(privateSalt, saltEncoding);
            DefaultPasswordService passwordService = new DefaultPasswordService();
            passwordService.setHashService(hashService);

            return passwordService.passwordsMatch(plainPassword, encryptedPassword);

        } catch (Exception e) {
            throw new RuntimeException("密码验证失败", e);
        }
    }

    /**
     * 从加密结果中提取公盐（可选功能）
     */
    public String extractPublicSalt(String encryptedPassword) {
        try {
            Shiro1CryptFormat format = new Shiro1CryptFormat();
            Hash hash = format.parse(encryptedPassword);
            return hash.getSalt() != null ? hash.getSalt().toBase64() : null;
        } catch (Exception e) {
            throw new RuntimeException("解析公盐失败", e);
        }
    }

    /**
     * 加密结果封装类
     */
    public static class EncryptResult {
        private final String encryptedPassword;
        private final String privateSalt;

        public EncryptResult(String encryptedPassword, String privateSalt) {
            this.encryptedPassword = encryptedPassword;
            this.privateSalt = privateSalt;
        }

        public String getEncryptedPassword() {
            return encryptedPassword;
        }

        public String getPrivateSalt() {
            return privateSalt;
        }
    }

}
