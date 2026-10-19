package edu.hbmd.taotao.qingjia.shiro;

import org.apache.shiro.authc.AuthenticationToken;

public class QingjiaBearerToken implements AuthenticationToken {
    private String token;
    public QingjiaBearerToken(String token) {
        this.token = token;
    }
    @Override
    public Object getPrincipal() {
        return token;
    }
    @Override
    public Object getCredentials() {
        return token;
    }
}
