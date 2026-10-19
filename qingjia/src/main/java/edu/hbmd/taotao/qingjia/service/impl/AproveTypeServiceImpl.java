package edu.hbmd.taotao.qingjia.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.hbmd.taotao.qingjia.mapper.AproveTypeMapper;
import edu.hbmd.taotao.qingjia.pojo.AproveTypePojo;
import edu.hbmd.taotao.qingjia.service.AproveTypeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AproveTypeServiceImpl extends ServiceImpl<AproveTypeMapper, AproveTypePojo>
implements AproveTypeService {
    @Override
    public List<AproveTypePojo> getAll() {
        return list();
    }
}
