package edu.hbmd.taotao.qingjia.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.hbmd.taotao.qingjia.mapper.MenuTypeMapper;
import edu.hbmd.taotao.qingjia.pojo.MenuTypePojo;
import edu.hbmd.taotao.qingjia.service.MenuTypeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MenuTypeServiceImpl extends ServiceImpl<MenuTypeMapper,MenuTypePojo>
implements MenuTypeService {
    @Override
    public List<MenuTypePojo> getAll() {
        return list();
    }
}
