package edu.hbmd.taotao.qingjia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.hbmd.taotao.qingjia.constant.PageConstant;
import edu.hbmd.taotao.qingjia.mapper.TeacherMapper;
import edu.hbmd.taotao.qingjia.pojo.TeacherPojo;
import edu.hbmd.taotao.qingjia.service.TeacherService;
import edu.hbmd.taotao.qingjia.service.UserRoleService;
import edu.hbmd.taotao.qingjia.util.PasswordUtil;
import edu.hbmd.taotao.qingjia.vo.TeacherVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class TeacherServiceImpl extends ServiceImpl<TeacherMapper, TeacherPojo>
implements TeacherService {
    @Autowired
    PasswordUtil passwordUtil;

    @Autowired
    UserRoleService userRoleService;
    @Override
    public TeacherPojo getByNum(String username) {
        QueryWrapper<TeacherPojo> wrapper = new QueryWrapper<>();
        wrapper.eq("num",username);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    public List<TeacherVo> searchBaseInfoTeachersByName(String name) {
        TeacherPojo teacherPojo = new TeacherPojo();
        teacherPojo.setName(name);
        return searchBaseInfoTeachers(teacherPojo);
    }

    @Override
    public List<TeacherVo> searchBaseInfoTeachers(TeacherPojo teacher) {
        return baseMapper.listBaseInfoTeachers(teacher);
    }

    @Override
    public Page<TeacherVo> pageTeachers(int curPage, TeacherPojo teacherPojo) {

        Page<TeacherVo> page = new Page<>(curPage, PageConstant.pageSize);
        return baseMapper.pageTeachers(page,teacherPojo);
    }

    /**
     * 可指定每页条数的重载，给 /api/teacher/index 用。
     * 上面那个两参版本保留不动（老 Thymeleaf 的 TeacherController.query 还在调它）。
     */
    @Override
    public Page<TeacherVo> pageTeachers(long current, long size, TeacherPojo teacherPojo) {
        long safeCurrent = current < 1 ? 1 : current;
        long safeSize = size < 1 ? PageConstant.pageSize : size;
        Page<TeacherVo> page = new Page<>(safeCurrent, safeSize);
        return baseMapper.pageTeachers(page, teacherPojo);
    }

    @Override
    public List<TeacherVo> listTeachers(TeacherPojo teacherPojo) {
        return baseMapper.listTeachers(teacherPojo);
    }

    @Override
    public boolean saveTeacher(TeacherPojo teacherPojo) {

        if (teacherPojo.getId()==null){//添加

            PasswordUtil.EncryptResult result = passwordUtil.encryptPassword(teacherPojo.getPassword());
            teacherPojo.setPassword(result.getEncryptedPassword());
            teacherPojo.setSalt(result.getPrivateSalt());
            save(teacherPojo);
        }else{//修改教师
            this.updateById(teacherPojo);
        }
        return true;
    }

    @Override
    public boolean checkNumUnique(String num, Integer excludeId) {
        LambdaQueryWrapper<TeacherPojo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(num)){
            wrapper.eq(TeacherPojo::getNum,num);
        }
        if (excludeId!=null) {
            wrapper.ne(TeacherPojo::getId, excludeId);
        }

        return !(count(wrapper)>0);
    }

    @Override
    public boolean resetPassword(Integer teacherId, String newPassword) {
        TeacherPojo teacher = this.getById(teacherId);
        PasswordUtil util = new PasswordUtil();
        PasswordUtil.EncryptResult result = util.encryptPassword(newPassword);
        teacher.setPassword(result.getEncryptedPassword());
        teacher.setSalt(result.getPrivateSalt());
        return updateById(teacher);
    }
}
