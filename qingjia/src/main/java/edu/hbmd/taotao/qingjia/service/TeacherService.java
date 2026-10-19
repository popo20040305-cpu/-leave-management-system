package edu.hbmd.taotao.qingjia.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import edu.hbmd.taotao.qingjia.pojo.TeacherPojo;
import edu.hbmd.taotao.qingjia.vo.TeacherVo;

import java.util.List;

public interface TeacherService extends IService<TeacherPojo> {
    TeacherPojo getByNum(String username);
    //新增，根据教师姓名模糊查询教师基本信息与复合条件查询教师信息
    List<TeacherVo> searchBaseInfoTeachersByName(String name);
    List<TeacherVo> searchBaseInfoTeachers(TeacherPojo teacher);

    //分页查询
    Page<TeacherVo> pageTeachers(int curPage, TeacherPojo teacherPojo);

    /**
     * 分页查询，可指定每页条数。
     *
     * 上面那个两参重载写死了 PageConstant.pageSize（10 条），老的 Thymeleaf 页面
     * 每页固定 10 条所以看不出问题；Vue 页面可以选「每页 50 条」，必须把 size
     * 真正传给 MyBatis-Plus，否则表现为 total 正确、records 永远只有 10 行。
     * （和 ClassService.pageClasses 的重载是同一个理由。）
     *
     * @param current 页码，从 1 开始，小于 1 按 1 处理
     * @param size    每页条数，小于 1 时退回 PageConstant.pageSize
     */
    Page<TeacherVo> pageTeachers(long current, long size, TeacherPojo teacherPojo);

    //列表查询
    List<TeacherVo> listTeachers(TeacherPojo teacherPojo);

    // 保存教师（新增或修改）
    boolean saveTeacher(TeacherPojo teacherPojo);

    // 检查编号是否唯一
    boolean checkNumUnique(String num, Integer excludeId);
    // 修改教师密码
    boolean resetPassword(Integer teacherId, String newPassword);


}
