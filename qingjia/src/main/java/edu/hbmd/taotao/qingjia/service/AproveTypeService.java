package edu.hbmd.taotao.qingjia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.hbmd.taotao.qingjia.pojo.AproveTypePojo;

import java.util.List;

public interface AproveTypeService extends IService<AproveTypePojo> {
    List<AproveTypePojo> getAll();
}
