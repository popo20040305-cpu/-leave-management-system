package edu.hbmd.taotao.qingjia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.hbmd.taotao.qingjia.constant.PageConstant;
import edu.hbmd.taotao.qingjia.mapper.CollegeMapper;
import edu.hbmd.taotao.qingjia.pojo.CollegePojo;
import edu.hbmd.taotao.qingjia.service.CollegeService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class CollegeServiceImpl extends ServiceImpl<CollegeMapper, CollegePojo>
implements CollegeService {
    @Override
    public List<CollegePojo> getAll() {
        return list();
    }

    @Override
    public Page<CollegePojo> queryPage(Integer curPage, int pageSize, CollegePojo college) {
        LambdaQueryWrapper<CollegePojo> wrapper = new LambdaQueryWrapper<>();
        if (college!=null&&StringUtils.hasText(college.getName())){
            wrapper.like(CollegePojo::getName,college.getName());
        }
        if (college!=null&&StringUtils.hasText(college.getNum())){
            wrapper.like(CollegePojo::getNum,college.getNum());
        }
        wrapper.orderByAsc(CollegePojo::getId);
        // 原来这里写死用 PageConstant.pageSize，把入参 pageSize 丢掉了：
        // 老的 /college/index 恰好也传 PageConstant.pageSize 所以看不出问题，
        // 但前后端分离后前端可以选“每页 50 条”，后端却仍旧只返回 10 条，
        // 表现为 total 正确、records 永远只有 10 行，翻页也翻不动。
        int page = (curPage == null || curPage < 1) ? 1 : curPage;
        int size = pageSize < 1 ? PageConstant.pageSize : pageSize;
        return this.page(new Page<>(page, size), wrapper);
    }
}
