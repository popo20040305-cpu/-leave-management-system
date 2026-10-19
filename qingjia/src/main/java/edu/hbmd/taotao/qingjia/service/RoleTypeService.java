package edu.hbmd.taotao.qingjia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.hbmd.taotao.qingjia.pojo.RoleTypePojo;

import java.util.List;


public interface RoleTypeService extends IService<RoleTypePojo> {
    List<RoleTypePojo> getAll();
}
