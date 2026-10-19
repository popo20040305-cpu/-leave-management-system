package edu.hbmd.taotao.qingjia.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.hbmd.taotao.qingjia.mapper.LeaveApplicationTypeMapper;
import edu.hbmd.taotao.qingjia.pojo.LeaveApplicationTypePojo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeaveApplicationTypeServiceImpl extends ServiceImpl<LeaveApplicationTypeMapper, LeaveApplicationTypePojo>
implements edu.hbmd.taotao.qingjia.service.LeaveApplicationTypeService {
    @Override
    public List<LeaveApplicationTypePojo> getAll() {
        return list();
    }
}
