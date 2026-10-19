package edu.hbmd.taotao.qingjia.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.hbmd.taotao.qingjia.mapper.SexTypeMapper;
import edu.hbmd.taotao.qingjia.pojo.SexTypePojo;
import edu.hbmd.taotao.qingjia.service.SexTypeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SexTypeServiceImpl extends ServiceImpl<SexTypeMapper, SexTypePojo>
implements SexTypeService {
    @Override
    public List<SexTypePojo> getAll() {
        return list();
    }
}
