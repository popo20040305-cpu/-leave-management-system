package edu.hbmd.taotao.qingjia.shiro;

import edu.hbmd.taotao.qingjia.util.PasswordUtil;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.authz.SimpleAuthorizationInfo;

import java.net.PasswordAuthentication;
import java.nio.charset.StandardCharsets;

public class QingjiaCredentialsMatcher implements CredentialsMatcher {
    @Override
    public boolean doCredentialsMatch(AuthenticationToken authenticationToken, AuthenticationInfo authenticationInfo) {
        QingjiaUsernamePasswordToken token = (QingjiaUsernamePasswordToken) authenticationToken;
        SimpleAuthenticationInfo info = (SimpleAuthenticationInfo) authenticationInfo;
        PasswordUtil util = new PasswordUtil();
        String password = new String(token.getPassword());
        String salt = new String(info.getCredentialsSalt().getBytes(), StandardCharsets.UTF_8);
        String encryptedPassword = info.getCredentials().toString();

        return util.matches(password,encryptedPassword,salt);
    }
}
