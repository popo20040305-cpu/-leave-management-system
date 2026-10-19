package edu.hbmd.taotao.qingjia.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.hbmd.taotao.qingjia.mapper.RoleTypeMapper;
import edu.hbmd.taotao.qingjia.pojo.RoleTypePojo;
import edu.hbmd.taotao.qingjia.service.RoleTypeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleTypeServiceImpl extends ServiceImpl<RoleTypeMapper, RoleTypePojo>
implements RoleTypeService {
    @Override
    public List<RoleTypePojo> getAll() {
        return list();
    }
}
