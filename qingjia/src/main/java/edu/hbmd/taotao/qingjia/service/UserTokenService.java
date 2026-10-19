package edu.hbmd.taotao.qingjia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.hbmd.taotao.qingjia.pojo.UserTokenPojo;

public interface UserTokenService extends IService<UserTokenPojo> {
    UserTokenPojo getByToken(String token);

    void saveOrUpdate(Integer userId, Integer userType, String token, long expireHours);

    /**
     * 按 token 删除记录（user_token 表没有主键，所以用条件删除）。
     */
    void deleteByToken(String token);
}
