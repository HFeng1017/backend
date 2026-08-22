package com.resume.platform.utils;

import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

/**
 * RSA 加解密工具类
 * 功能：
 * 1. 启动时生成 RSA-2048 密钥对，缓存到 Redis（24小时）和内存中
 * 2. 下发公钥（PEM格式，便于前端 jsencrypt 直接使用）
 * 3. 使用私钥解密前端加密后的密码密文（Base64字符串）
 * 注意：对齐前端 jsencrypt 默认配置：PKCS#1 v1.5 padding + Base64 编码输出
 *
 * @author system
 */
@Slf4j
@Component
public class RsaUtil {

    /**
     * Redis 中存储私钥的 Key
     */
    private static final String RSA_PRIVATE_KEY_KEY = "rsa:private_key";

    /**
     * Redis 中存储公钥的 Key
     */
    private static final String RSA_PUBLIC_KEY_KEY = "rsa:public_key";

    /**
     * RSA 密钥对缓存时间（24小时，到期自动轮换）
     */
    private static final long RSA_KEY_EXPIRE_HOURS = 24;

    /**
     * RSA 密钥长度：2048（安全推荐）
     */
    private static final int RSA_KEY_SIZE = 2048;

    /**
     * 内存中缓存的私钥 Base64（避免每次 Redis 读取）
     */
    private volatile String cachedPrivateKey;

    /**
     * 内存中缓存的公钥（PEM格式）
     */
    private volatile String cachedPublicKeyPem;

    private final RedisTemplate<String, String> redisTemplate;

    public RsaUtil(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
        initRsaKeys();
    }

    /**
     * 初始化 RSA 密钥对
     * 优先从 Redis 读取，不存在则生成新密钥对并缓存
     */
    private void initRsaKeys() {
        try {
            String redisPublicKey = redisTemplate.opsForValue().get(RSA_PUBLIC_KEY_KEY);
            String redisPrivateKey = redisTemplate.opsForValue().get(RSA_PRIVATE_KEY_KEY);
            if (redisPublicKey != null && redisPrivateKey != null) {
                this.cachedPublicKeyPem = toPemPublicKey(redisPublicKey);
                this.cachedPrivateKey = redisPrivateKey;
                log.info("RSA密钥对已从Redis恢复");
                return;
            }
            generateAndStoreKeyPair();
        } catch (Exception e) {
            log.warn("从Redis读取RSA密钥失败，重新生成: {}", e.getMessage());
            generateAndStoreKeyPair();
        }
    }

    /**
     * 生成新的 RSA 密钥对并存储到 Redis 和内存缓存
     */
    private synchronized void generateAndStoreKeyPair() {
        try {
            KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
            keyPairGen.initialize(RSA_KEY_SIZE, new SecureRandom());
            KeyPair keyPair = keyPairGen.generateKeyPair();

            String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
            String privateKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

            // 缓存到 Redis（24小时后过期，自动轮换）
            redisTemplate.opsForValue().set(RSA_PUBLIC_KEY_KEY, publicKeyBase64,
                    RSA_KEY_EXPIRE_HOURS, TimeUnit.HOURS);
            redisTemplate.opsForValue().set(RSA_PRIVATE_KEY_KEY, privateKeyBase64,
                    RSA_KEY_EXPIRE_HOURS, TimeUnit.HOURS);

            // 缓存到内存
            this.cachedPublicKeyPem = toPemPublicKey(publicKeyBase64);
            this.cachedPrivateKey = privateKeyBase64;
            log.info("RSA-{} 密钥对已生成并缓存（Redis TTL={}小时）", RSA_KEY_SIZE, RSA_KEY_EXPIRE_HOURS);
        } catch (Exception e) {
            log.error("生成RSA密钥对失败", e);
            throw new IllegalStateException("RSA密钥初始化失败", e);
        }
    }

    /**
     * 获取公钥（PEM格式，直接可被 jsencrypt 使用）
     *
     * @return PEM 格式公钥字符串，含 -----BEGIN PUBLIC KEY----- 头尾
     */
    public String getPublicKeyPem() {
        // 双重检查：Redis 中存在但内存缓存被清（理论上不会），则刷新内存缓存
        if (cachedPublicKeyPem == null) {
            String redisPublicKey = redisTemplate.opsForValue().get(RSA_PUBLIC_KEY_KEY);
            if (redisPublicKey != null) {
                cachedPublicKeyPem = toPemPublicKey(redisPublicKey);
            } else {
                generateAndStoreKeyPair();
            }
        }
        return cachedPublicKeyPem;
    }

    /**
     * 使用私钥解密密文
     * 与前端 WebCrypto 对齐：RSA-OAEP + SHA-256 填充
     *
     * @param encryptedBase64 前端加密后的 Base64 字符串
     * @return 解密后的明文字符串
     */
    public String decryptPassword(String encryptedBase64) {
        if (encryptedBase64 == null || encryptedBase64.isEmpty()) {
            return null;
        }
        try {
            String privateKeyBase64 = cachedPrivateKey;
            if (privateKeyBase64 == null) {
                privateKeyBase64 = redisTemplate.opsForValue().get(RSA_PRIVATE_KEY_KEY);
                if (privateKeyBase64 == null) {
                    log.warn("RSA私钥已过期，重新生成密钥对（本次登录将失败，客户端需重试获取公钥）");
                    generateAndStoreKeyPair();
                    return null;
                }
                cachedPrivateKey = privateKeyBase64;
            }
            // 前端 WebCrypto 使用 RSA-OAEP + SHA-256，此处 Hutool 需使用 "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
            RSA rsa = new RSA("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", privateKeyBase64, null);
            byte[] decryptedBytes = rsa.decrypt(Base64.getDecoder().decode(encryptedBase64), KeyType.PrivateKey);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("RSA-OAEP解密密文失败: {}（可能密文格式错误、填充不匹配或密钥不匹配）", e.getMessage());
            return null;
        }
    }

    /**
     * 将 Base64 公钥转换为 PEM 格式（加换行和头尾标记）
     * jsencrypt 默认按 PEM 格式解析公钥
     */
    private String toPemPublicKey(String base64PublicKey) {
        StringBuilder pem = new StringBuilder(2048);
        pem.append("-----BEGIN PUBLIC KEY-----\n");
        // 每 64 字符换行（符合 PEM 规范）
        int length = base64PublicKey.length();
        for (int i = 0; i < length; i += 64) {
            pem.append(base64PublicKey, i, Math.min(i + 64, length)).append('\n');
        }
        pem.append("-----END PUBLIC KEY-----");
        return pem.toString();
    }

    /**
     * 从 Base64 私钥还原 PrivateKey 对象（预留：未来如需直接用 JCE API）
     */
    @SuppressWarnings("unused")
    private PrivateKey parsePrivateKey(String base64PrivateKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64PrivateKey);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(spec);
    }

    /**
     * 从 Base64 公钥还原 PublicKey 对象（预留）
     */
    @SuppressWarnings("unused")
    private PublicKey parsePublicKey(String base64PublicKey) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }
}
