package edu.hbmd.taotao.qingjia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import edu.hbmd.taotao.qingjia.pojo.LeaveApplicationTypePojo;

import java.util.List;

public interface LeaveApplicationTypeService extends IService<LeaveApplicationTypePojo> {
   List<LeaveApplicationTypePojo> getAll();
}
