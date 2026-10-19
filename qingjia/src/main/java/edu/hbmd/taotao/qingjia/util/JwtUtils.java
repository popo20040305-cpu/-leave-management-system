package edu.hbmd.taotao.qingjia.util;

import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;

import java.util.HashMap;
import java.util.Map;

public class JwtUtils {
    //    密钥
    private static final byte[] SECRET = "qingjia#2026#secret".getBytes();
    //    有效时间
    private static final long EXPIRE = 2 * 60 * 60 * 1000L;

    public static String createToken(String username, int userType)  {
        Map<String, Object> payload = new HashMap<>();
        payload.put("username", username);
        payload.put("userType", userType);
        payload.put("expireAt", System.currentTimeMillis() + EXPIRE);
        return JWTUtil.createToken(payload, JWTSignerUtil.hs256(SECRET));
    }

    //验证签名and验证过期
    public static boolean verify(String token) {
        try {
            JWT jwt = JWTUtil.parseToken(token);
            if (!jwt.setSigner(JWTSignerUtil.hs256(SECRET)).verify()) return false;
            Long expireAt = cn.hutool.core.convert.Convert.toLong(jwt.getPayload("expireAt"));
            return expireAt != null && expireAt > System.currentTimeMillis();
        } catch (Exception e) {
            return false;
        }
    }
    public static JWT parse(String token) { return JWTUtil.parseToken(token); }
}