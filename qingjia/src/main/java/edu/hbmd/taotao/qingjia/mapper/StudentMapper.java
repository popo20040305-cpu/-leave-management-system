package edu.hbmd.taotao.qingjia.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import edu.hbmd.taotao.qingjia.pojo.StudentPojo;
import edu.hbmd.taotao.qingjia.vo.StudentVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StudentMapper extends BaseMapper<StudentPojo> {
    // 分页查询
    Page<StudentVo> pageStudents(IPage<?> page, @Param("student") StudentVo student);

    // 列表查询
    List<StudentVo> listStudents(@Param("student") StudentVo student);

    // 基础信息列表（可选）
    List<StudentVo> listBaseInfoStudents(@Param("student") StudentVo student);

    //根据id查询
    StudentVo selectById(@Param("id") Integer id);

}
