package edu.hbmd.taotao.qingjia.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import edu.hbmd.taotao.qingjia.constant.PageConstant;
import edu.hbmd.taotao.qingjia.exception.ResultEnum;
import edu.hbmd.taotao.qingjia.exception.ResultException;
import edu.hbmd.taotao.qingjia.mapper.ClassMaper;
import edu.hbmd.taotao.qingjia.pojo.*;
import edu.hbmd.taotao.qingjia.service.ClassService;
import edu.hbmd.taotao.qingjia.service.CollegeService;
import edu.hbmd.taotao.qingjia.service.RoleService;
import edu.hbmd.taotao.qingjia.service.UserRoleService;
import edu.hbmd.taotao.qingjia.shiro.LoginUser;
import edu.hbmd.taotao.qingjia.shiro.ShiroProperties;
import edu.hbmd.taotao.qingjia.vo.ClassVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClassServiceImpl extends ServiceImpl<ClassMaper, ClassPojo>
implements ClassService {
    @Autowired
    CollegeService collegeService;
    @Autowired
    UserRoleService userRoleService;
    @Autowired
    ShiroProperties shiroProperties;
    @Autowired
    RoleService roleService;

    @Override
    public Page<ClassVo> pageClasses(ClassPojo clazz, Integer curPage) {

        Page<ClassVo> page = new Page<>(curPage, PageConstant.pageSize);
        return baseMapper.pageClasses(page,clazz);
    }

    /**
     * 可指定每页条数的重载。上面那个两参版本保留不动（老 Thymeleaf 的
     * ClassController.index 还在调它），这里把 current / size 真正传给 MyBatis-Plus，
     * 前端选「每页 50 条」时才拿得到 50 行。
     */
    @Override
    public Page<ClassVo> pageClasses(ClassPojo clazz, long current, long size) {
        long safeCurrent = current < 1 ? 1 : current;
        long safeSize = size < 1 ? PageConstant.pageSize : size;
        Page<ClassVo> page = new Page<>(safeCurrent, safeSize);
        return baseMapper.pageClasses(page, clazz);
    }

    @Override
    public List<ClassVo> listClasses(ClassPojo clazz) {
        return baseMapper.listClasses(clazz);
    }

    @Override
    public CollegePojo getByStudent(StudentPojo student) {
        ClassPojo classPojo = getById(student.getClassId());
        if (classPojo!=null){
            return  collegeService.getById(classPojo.getBelongToCollegeId());
        }
        return null;
    }

    @Override
    public List<ClassPojo> listByCollegeAndGrade(Integer collegeId, Integer grade) {
        LambdaQueryWrapper<ClassPojo> wrapper = new LambdaQueryWrapper<>();
        if (collegeId!=null){
            wrapper.eq(ClassPojo::getBelongToCollegeId,collegeId);
        }
        if (grade!=null){
            wrapper.eq(ClassPojo::getGrade,grade);
        }
        wrapper.orderByDesc(ClassPojo::getNum);
        return list(wrapper);
    }

    @Override
    public boolean setHeadTeacher(Integer classId, Integer teacherId) {
        LambdaQueryWrapper<RolePojo> roleWapper = new LambdaQueryWrapper<>();
        roleWapper.eq(RolePojo::getName,shiroProperties.getBanzhurenDefaultRoleName());
        RolePojo banzhurenRole = roleService.getOne(roleWapper);
        if (banzhurenRole==null){
            throw new ResultException(ResultEnum.CLASS_BANZHUREN_ROLE_NOT_EXIST);
        }
        ClassPojo classPojo = this.getById(classId);
        if (classPojo==null){
            throw new ResultException(ResultEnum.CLASS_NOT_EXIST);
        }
        if (classPojo.getBanZhuRenId() == teacherId){
            return  true;
        }
        LambdaQueryWrapper<ClassPojo> classWapper = new LambdaQueryWrapper<>();
        classWapper.eq(ClassPojo::getBanZhuRenId,classPojo.getBanZhuRenId());
        if (this.count(classWapper)==1){
            LambdaQueryWrapper<UserRolePojo> userRoleWapper = new LambdaQueryWrapper<>();
            userRoleWapper.eq(UserRolePojo::getRoleid,banzhurenRole.getId())
            .eq(UserRolePojo::getUserType,LoginUser.TYPE_TEACHER )
            .eq(UserRolePojo::getUserid,classPojo.getBanZhuRenId());
            userRoleService.remove(userRoleWapper);
        }
        classPojo.setBanZhuRenId(teacherId);
        updateById(classPojo);
        //指定班主任色
        LambdaQueryWrapper<UserRolePojo> userRoleWapper = new LambdaQueryWrapper<>();
        userRoleWapper.eq(UserRolePojo::getRoleid,banzhurenRole.getId())
                .eq(UserRolePojo::getUserType,LoginUser.TYPE_TEACHER )
                .eq(UserRolePojo::getUserid,teacherId);
        long count = userRoleService.count(userRoleWapper);
        if (count>1){
            userRoleService.remove(userRoleWapper);
        }
        if (count==0||count>1){
            UserRolePojo userRolePojo = new UserRolePojo();
            userRolePojo.setRoleid(banzhurenRole.getId());
            userRolePojo.setUserType(LoginUser.TYPE_TEACHER);
            userRolePojo.setUserid(teacherId);
            userRoleService.save(userRolePojo);
        }
        return true;
    }

    @Override
    public List<ClassPojo> listByTeacherId(Integer teacherId) {
        LambdaQueryWrapper<ClassPojo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ClassPojo::getBanZhuRenId,teacherId);
        return this.list(wrapper);
    }
}
