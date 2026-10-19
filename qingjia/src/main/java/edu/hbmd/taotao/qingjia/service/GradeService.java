package edu.hbmd.taotao.qingjia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.hbmd.taotao.qingjia.pojo.GradePojo;

import java.util.List;

public interface GradeService extends IService<GradePojo> {
    List<GradePojo> getAll();
}
