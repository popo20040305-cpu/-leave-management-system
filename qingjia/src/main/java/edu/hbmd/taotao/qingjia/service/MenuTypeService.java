package edu.hbmd.taotao.qingjia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.hbmd.taotao.qingjia.pojo.MenuTypePojo;

import java.util.List;

public interface MenuTypeService extends IService<MenuTypePojo> {
    List<MenuTypePojo> getAll();
}
