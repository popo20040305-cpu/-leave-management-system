package edu.hbmd.taotao.qingjia.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import edu.hbmd.taotao.qingjia.pojo.CollegePojo;

import java.util.List;

public interface CollegeService extends IService<CollegePojo> {
    List<CollegePojo> getAll();
    /**
     * 分页条件查询学院列表
     * @param curPage  当前页
     * @param college 查询条件（封装name, num等）
     * @return 分页结果
     */
    Page<CollegePojo> queryPage(Integer curPage,int pageSize, CollegePojo college);

}
