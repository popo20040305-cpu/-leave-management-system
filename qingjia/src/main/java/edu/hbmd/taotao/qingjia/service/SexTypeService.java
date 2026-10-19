package edu.hbmd.taotao.qingjia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.hbmd.taotao.qingjia.pojo.SexTypePojo;

import java.util.List;

public interface SexTypeService extends IService<SexTypePojo> {
    List<SexTypePojo> getAll();
}
