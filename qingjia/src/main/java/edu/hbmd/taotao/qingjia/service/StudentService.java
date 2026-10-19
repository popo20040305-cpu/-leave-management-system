package edu.hbmd.taotao.qingjia.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import edu.hbmd.taotao.qingjia.pojo.StudentPojo;
import edu.hbmd.taotao.qingjia.vo.StudentVo;

import java.util.List;

public interface StudentService extends IService<StudentPojo> {
    StudentPojo getByNum(String username);
    // 分页查询
    Page<StudentVo> pageStudents(int curPage, StudentVo student);

    /**
     * 分页查询，可指定每页条数。
     *
     * 理由同 TeacherService / ClassService 里的同签名重载：两参版本写死了
     * PageConstant.pageSize（10 条），Vue 页面选「每页 50 条」时后端收不到 size，
     * 会出现 total 正确但 records 永远只有 10 行的情况。
     *
     * @param current 页码，从 1 开始，小于 1 按 1 处理
     * @param size    每页条数，小于 1 时退回 PageConstant.pageSize
     */
    Page<StudentVo> pageStudents(long current, long size, StudentVo student);

    // 列表查询（不分页）
    List<StudentVo> listStudents(StudentVo student);

    // 根据ID获取StudentVo（含学院、班级名称）
    StudentVo selectById(Integer id);

    // 检查学号唯一性
    boolean checkNumUnique(StudentPojo studentPojo);
    // 保存学生（新增或修改，带密码）
    boolean saveStudent(StudentPojo studentPojo);
    // 重置密码
    boolean resetPassword(Integer studentId, String newPassword);

}
