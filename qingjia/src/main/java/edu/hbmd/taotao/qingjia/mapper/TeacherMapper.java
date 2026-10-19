package edu.hbmd.taotao.qingjia.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.hbmd.taotao.qingjia.pojo.TeacherPojo;
import edu.hbmd.taotao.qingjia.vo.TeacherVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TeacherMapper extends BaseMapper<TeacherPojo> {
    //新增分页查询、列表查询、基本信息查询
    public Page<TeacherVo> pageTeachers(IPage<?> page,
                                       @Param("teacher") TeacherPojo teacher);
    public List<TeacherVo> listTeachers(@Param("teacher") TeacherPojo teacher);
    public List<TeacherVo> listBaseInfoTeachers(@Param("teacher") TeacherPojo teacher);

}
