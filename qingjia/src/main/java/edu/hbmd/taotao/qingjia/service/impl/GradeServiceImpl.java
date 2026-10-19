package edu.hbmd.taotao.qingjia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.hbmd.taotao.qingjia.mapper.GradeMapper;
import edu.hbmd.taotao.qingjia.pojo.GradePojo;
import edu.hbmd.taotao.qingjia.service.GradeService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GradeServiceImpl extends ServiceImpl<GradeMapper, GradePojo>
implements GradeService {
    @Override
    public List<GradePojo> getAll() {//降序排序
        return this.list(new QueryWrapper<GradePojo>().orderByDesc("grade"));
    }
}
