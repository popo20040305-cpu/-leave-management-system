package edu.hbmd.taotao.qingjia.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import edu.hbmd.taotao.qingjia.pojo.ClassPojo;
import edu.hbmd.taotao.qingjia.pojo.CollegePojo;
import edu.hbmd.taotao.qingjia.pojo.StudentPojo;
import edu.hbmd.taotao.qingjia.vo.ClassVo;

import java.util.List;

public interface ClassService extends IService<ClassPojo> {
    Page<ClassVo> pageClasses(ClassPojo clazz, Integer curPage);

    /**
     * 分页查询班级，可指定每页条数。
     *
     * 上面那个两参重载写死了 PageConstant.pageSize（10 条），老的 Thymeleaf 页面每页
     * 固定 10 条所以看不出问题；前后端分离后前端可以选「每页 50 条」，必须把 size
     * 传下来，否则表现为 total 正确、records 永远只有 10 行、翻页也翻不动。
     *
     * @param current 页码，从 1 开始，小于 1 按 1 处理
     * @param size    每页条数，小于 1 时退回 PageConstant.pageSize
     */
    Page<ClassVo> pageClasses(ClassPojo clazz, long current, long size);

    public List<ClassVo> listClasses(ClassPojo clazz);

    CollegePojo getByStudent(StudentPojo student);
    // 新增：根据学院ID和年级查询班级列表（用于联动）
    List<ClassPojo> listByCollegeAndGrade(Integer collegeId, Integer grade);
    //指定班主任
    boolean setHeadTeacher(Integer classId,Integer teacherId);

    List<ClassPojo> listByTeacherId(Integer id);
}
