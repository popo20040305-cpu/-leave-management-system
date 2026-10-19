package edu.hbmd.taotao.qingjia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.hbmd.taotao.qingjia.mapper.UserTokenMapper;
import edu.hbmd.taotao.qingjia.pojo.UserTokenPojo;
import edu.hbmd.taotao.qingjia.service.UserTokenService;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class UserTokenServiceImpl extends ServiceImpl<UserTokenMapper, UserTokenPojo>
        implements UserTokenService {

    @Override
    public UserTokenPojo getByToken(String token) {
        LambdaQueryWrapper<UserTokenPojo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTokenPojo::getToken, token);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public void saveOrUpdate(Integer userId, Integer userType, String token, long expireHours) {
        LambdaQueryWrapper<UserTokenPojo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTokenPojo::getUserId, userId)
                .eq(UserTokenPojo::getUserType, userType);
        UserTokenPojo existing = baseMapper.selectOne(wrapper);

        Date now = new Date();
        Date expireTime = new Date(now.getTime() + expireHours * 60L * 60L * 1000L);

        if (existing == null) {
            UserTokenPojo newToken = new UserTokenPojo();
            newToken.setUserId(userId);
            newToken.setUserType(userType);
            newToken.setToken(token);
            newToken.setExpire_time(expireTime);
            newToken.setUpdate_time(now);
            baseMapper.insert(newToken);
        } else {
            existing.setToken(token);
            existing.setExpire_time(expireTime);
            existing.setUpdate_time(now);
            baseMapper.update(existing, wrapper);
        }
    }

    @Override
    public void deleteByToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return;
        }
        LambdaQueryWrapper<UserTokenPojo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserTokenPojo::getToken, token);
        baseMapper.delete(wrapper);
    }
}
