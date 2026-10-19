package edu.hbmd.taotao.qingjia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.hbmd.taotao.qingjia.constant.PageConstant;
import edu.hbmd.taotao.qingjia.exception.ResultEnum;
import edu.hbmd.taotao.qingjia.exception.ResultException;
import edu.hbmd.taotao.qingjia.mapper.StudentMapper;
import edu.hbmd.taotao.qingjia.pojo.StudentPojo;
import edu.hbmd.taotao.qingjia.service.StudentService;
import edu.hbmd.taotao.qingjia.util.PasswordUtil;
import edu.hbmd.taotao.qingjia.vo.StudentVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Service
public class StudentServiceImpl extends ServiceImpl<StudentMapper,StudentPojo>
implements StudentService {
    @Autowired
    PasswordUtil passwordUtil;
    @Override
    public StudentPojo getByNum(String username) {
        QueryWrapper<StudentPojo> wrapper = new QueryWrapper<>();
        wrapper.eq("num",username);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public Page<StudentVo> pageStudents(int curPage, StudentVo student) {
        Page<StudentVo> page = new Page<>(curPage,PageConstant.pageSize);
        return baseMapper.pageStudents(page,student);
    }

    /**
     * 可指定每页条数的重载，给 /api/student/index 用。
     * 两参版本保留不动（老 Thymeleaf 的 StudentController.query 还在调它）。
     */
    @Override
    public Page<StudentVo> pageStudents(long current, long size, StudentVo student) {
        long safeCurrent = current < 1 ? 1 : current;
        long safeSize = size < 1 ? PageConstant.pageSize : size;
        Page<StudentVo> page = new Page<>(safeCurrent, safeSize);
        return baseMapper.pageStudents(page, student);
    }

    @Override
    public List<StudentVo> listStudents(StudentVo student) {
        return baseMapper.listStudents(student);
    }

    @Override
    public StudentVo selectById(Integer id) {
        return baseMapper.selectById(id);
    }

    @Override
    public boolean checkNumUnique(StudentPojo studentPojo) {
        LambdaQueryWrapper<StudentPojo> wrapper = new LambdaQueryWrapper<>();
        if (studentPojo.getNum() != null){
            wrapper.eq(StudentPojo::getNum,studentPojo.getNum());
        }
        if (studentPojo.getId()!=null){
            wrapper.ne(StudentPojo::getId,studentPojo.getId());
        }
        return this.count(wrapper)==0;
    }

    @Override
    public boolean saveStudent(StudentPojo studentPojo) {
        if (studentPojo.getId()==null){//添加
            PasswordUtil.EncryptResult result = passwordUtil.encryptPassword(studentPojo.getPassword());
            studentPojo.setPassword(result.getEncryptedPassword());
            studentPojo.setSalt(result.getPrivateSalt());
            return  this.save(studentPojo);
        }else{
            return this.updateById(studentPojo);
        }

    }

    @Override
    public boolean resetPassword(Integer studentId, String newPassword) {
        StudentPojo studentPojo = this.getById(studentId);
        if (studentPojo==null){
            throw new ResultException(ResultEnum.STUDENT_NOT_EXIST);
        }
        PasswordUtil.EncryptResult result = passwordUtil.encryptPassword(newPassword);
        studentPojo.setPassword(result.getEncryptedPassword());
        studentPojo.setSalt(result.getPrivateSalt());
        return updateById(studentPojo);
    }
}
